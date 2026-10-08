#!/usr/bin/env python3
"""Install a built update into an idle Power Beta instance, with verified rollback."""
from datetime import datetime
from pathlib import Path
import argparse
import json
import re
import tempfile
import zipfile
from instance_files import ROOT, assert_idle, atomic_copy
from release_data import VERSION, sha256


def protected_files(instance):
    game = instance / '.minecraft'
    paths = [p for name in ['config', 'saves'] for p in (game / name).rglob('*') if p.is_file()]
    if (game / 'options.txt').is_file():
        paths.append(game / 'options.txt')
    return {str(p.relative_to(instance)): sha256(p) for p in paths}


def mod_id(path):
    with zipfile.ZipFile(path) as jar:
        return json.loads(jar.read('fabric.mod.json'))['id']


def update(instance, stage=None):
    instance = instance.resolve()
    assert_idle(instance)
    stage = stage or ROOT / 'dist/Power Beta'
    manifest = json.loads((stage / 'PACK-MANIFEST.json').read_text())
    if not re.fullmatch(VERSION, manifest['version']):
        raise RuntimeError('Invalid pack version')
    new_name = f'power-beta-{manifest["version"]}.jar'
    if not any(row['file'] == new_name for row in manifest['mods']):
        raise RuntimeError('Manifest does not include the Power Beta artifact')
    if len(manifest['mods']) != 7 or len({row['file'] for row in manifest['mods']}) != 7:
        raise RuntimeError('Pack must contain seven distinct artifacts')
    for row in manifest['mods']:
        if Path(row['file']).name != row['file'] or not row['enabled']:
            raise RuntimeError('Invalid artifact name or disabled artifact')
        if sha256(stage / '.minecraft/mods' / row['file']) != row['sha256']:
            raise RuntimeError('Pack artifact hash differs')
        if row['file'] != new_name:
            if sha256(instance / '.minecraft/mods' / row['file']) != row['sha256']:
                raise RuntimeError('Platform artifacts differ; create a fresh profile from the export')
    if mod_id(stage / '.minecraft/mods' / new_name) != 'power_beta':
        raise RuntimeError('Not a Power Beta artifact')
    existing = [p for p in (instance / '.minecraft/mods').glob('power-beta-*.jar') if mod_id(p) == 'power_beta']
    if len(existing) != 1:
        raise RuntimeError('Expected exactly one installed Power Beta JAR')
    protected = protected_files(instance)
    files = ['.minecraft/mods/' + new_name, 'PACK-MANIFEST.json', 'README.md',
             'CHANGELOG.md', 'COMPONENTS.json', 'LICENSE.txt']
    files.extend(str(p.relative_to(stage)) for p in sorted((stage / 'screenshots').glob('*.png')))
    obsolete = {'UPSTREAMS.md', str(existing[0].relative_to(instance))} - set(files)
    obsolete.update(str(p.relative_to(instance)) for p in (instance / 'licenses').rglob('*') if p.is_file())
    obsolete.update(str(p.relative_to(instance)) for p in (instance / 'screenshots').glob('*.png')
                    if str(p.relative_to(instance)) not in files)
    affected = sorted(set(files) | obsolete)
    for name in affected:
        path = instance / name
        if any(p.is_symlink() for p in [path, *path.parents] if p != instance.parent):
            raise RuntimeError('Refusing symbolic link: ' + name)
        if path.exists() and not path.is_file():
            raise RuntimeError('Expected a file: ' + name)
    backup = instance / 'power-beta-update-backups' / (
        'before-artifact-update-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f') + '.zip')
    backup.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(backup, 'x', zipfile.ZIP_DEFLATED) as archive:
        for name in affected:
            if (instance / name).exists():
                archive.write(instance / name, name)
    with zipfile.ZipFile(backup) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('Backup verification failed')
        for name in archive.namelist():
            if archive.read(name) != (instance / name).read_bytes():
                raise RuntimeError('Instance changed during backup')
    try:
        for name in files:
            assert_idle(instance)
            atomic_copy(stage / name, instance / name)
        for name in obsolete:
            assert_idle(instance)
            (instance / name).unlink(missing_ok=True)
        if protected_files(instance) != protected:
            raise RuntimeError('Configuration or world files changed during update')
        for row in manifest['mods']:
            if sha256(instance / '.minecraft/mods' / row['file']) != row['sha256']:
                raise RuntimeError('Installed artifact hash differs')
    except BaseException:
        assert_idle(instance)
        with zipfile.ZipFile(backup) as archive, tempfile.TemporaryDirectory() as temporary:
            restored = set(archive.namelist())
            for name in restored:
                source = Path(temporary) / 'restore'
                source.write_bytes(archive.read(name))
                atomic_copy(source, instance / name)
            for name in set(affected) - restored:
                (instance / name).unlink(missing_ok=True)
        raise
    return {'instance': str(instance), 'version': manifest['version'],
            'artifact': sha256(instance / '.minecraft/mods' / new_name),
            'protectedFiles': len(protected), 'backup': str(backup)}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('instance', type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(update(args.instance), indent=2))
    except (RuntimeError, OSError, ValueError) as error:
        parser.exit(1, str(error) + '\n')
