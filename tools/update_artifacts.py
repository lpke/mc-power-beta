#!/usr/bin/env python3
"""Install a built update into an idle Power Beta instance without changing settings or worlds."""
from datetime import datetime
from pathlib import Path
import argparse
import hashlib
import json
import zipfile
from instance_files import ROOT, assert_idle, atomic_copy


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def protected_files(instance):
    game = instance / '.minecraft'
    paths = [p for name in ['config', 'saves'] for p in (game / name).rglob('*') if p.is_file()]
    if (game / 'options.txt').is_file():
        paths.append(game / 'options.txt')
    return {str(p.relative_to(instance)): digest(p) for p in paths}


def update(instance):
    instance = instance.resolve()
    assert_idle(instance)
    stage = ROOT / 'dist/Power Beta'
    manifest = json.loads((stage / 'PACK-MANIFEST.json').read_text())
    for row in manifest['mods']:
        if Path(row['file']).name != row['file']:
            raise RuntimeError('Invalid artifact name')
        if digest(stage / '.minecraft/mods' / row['file']) != row['sha256']:
            raise RuntimeError('Pack artifact hash differs')
        if row['file'] != 'power-beta-1.0.0.jar':
            if digest(instance / '.minecraft/mods' / row['file']) != row['sha256']:
                raise RuntimeError('Platform artifacts differ; create a fresh profile from the export')
    jar = instance / '.minecraft/mods/power-beta-1.0.0.jar'
    if not jar.is_file() or jar.is_symlink():
        raise RuntimeError('Not an existing Power Beta instance')
    protected = protected_files(instance)
    files = ['.minecraft/mods/power-beta-1.0.0.jar', 'PACK-MANIFEST.json', 'README.md',
             'UPSTREAMS.md', 'CHANGELOG.md', 'COMPONENTS.json', 'LICENSE.txt']
    files.extend(str(path.relative_to(stage)) for path in sorted((stage / 'screenshots').glob('*.png')))
    backup = instance / 'power-beta-update-backups' / (
        'before-artifact-update-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f') + '.zip')
    backup.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(backup, 'x', zipfile.ZIP_DEFLATED) as archive:
        for name in files:
            path = instance / name
            if path.is_symlink():
                raise RuntimeError('Refusing symbolic link: ' + str(path))
            if path.exists():
                archive.write(path, name)
    with zipfile.ZipFile(backup) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('Backup verification failed')
        for name in archive.namelist():
            if archive.read(name) != (instance / name).read_bytes():
                raise RuntimeError('Instance changed during backup')
    for name in files:
        assert_idle(instance)
        atomic_copy(stage / name, instance / name)
    if protected_files(instance) != protected:
        raise RuntimeError('Configuration or world files changed during update')
    for row in manifest['mods']:
        if digest(instance / '.minecraft/mods' / row['file']) != row['sha256']:
            raise RuntimeError('Installed artifact hash differs')
    return {'instance': str(instance), 'artifact': digest(jar),
            'protectedFiles': len(protected), 'backup': str(backup)}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('instance', type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(update(args.instance), indent=2))
    except (RuntimeError, OSError, ValueError) as error:
        parser.exit(1, str(error) + '\n')
