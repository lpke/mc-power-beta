#!/usr/bin/env python3
"""Update existing idle Power Beta instances, retaining worlds and user preferences."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, tempfile, uuid, zipfile
from create_profiles import ROOT, migrate


def assert_idle(instance):
    game = (instance / '.minecraft').resolve()
    for process in Path('/proc').iterdir():
        try:
            if process.name.isdigit() and (process / 'comm').read_text().strip() == 'java' and (process / 'cwd').resolve() == game:
                raise RuntimeError(f'Instance is running: {instance.name}')
        except (FileNotFoundError, PermissionError, ProcessLookupError):
            continue


def atomic_copy(source, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.is_symlink():
        raise RuntimeError(f'Refusing symbolic link: {target}')
    with tempfile.NamedTemporaryFile(dir=target.parent, prefix='.power-beta-', delete=False) as temp:
        temporary = Path(temp.name)
        with source.open('rb') as data:
            shutil.copyfileobj(data, temp)
        temp.flush()
        os.fsync(temp.fileno())
    try:
        os.replace(temporary, target)
    finally:
        temporary.unlink(missing_ok=True)


def update(instance, configured=False):
    instance = instance.resolve()
    assert_idle(instance)
    stage = ROOT / 'dist/Power Beta'
    manifest = json.loads((stage / 'PACK-MANIFEST.json').read_text())
    game = instance / '.minecraft'
    if not (game / 'mods/power-beta-1.0.0.jar').is_file():
        raise RuntimeError('Not an existing Power Beta instance')
    backups = instance / 'power-beta-update-backups'
    backups.mkdir(exist_ok=True)
    backup = backups / ('before-' + str(uuid.uuid4()) + '.zip')
    files = [p for base in [game / 'config', game / 'mods'] for p in base.rglob('*') if p.is_file()]
    files += [p for p in [game / 'options.txt', instance / 'PACK-MANIFEST.json', instance / 'PROFILE.json'] if p.is_file()]
    with zipfile.ZipFile(backup, 'x', zipfile.ZIP_DEFLATED) as archive:
        for path in files:
            if path.is_symlink():
                raise RuntimeError(f'Refusing symbolic link: {path}')
            archive.write(path, path.relative_to(instance))
    with zipfile.ZipFile(backup) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('Backup verification failed')
        for path in files:
            if archive.read(str(path.relative_to(instance))) != path.read_bytes():
                raise RuntimeError('Profile changed during backup')
    with backup.open('rb') as data:
        os.fsync(data.fileno())
    assert_idle(instance)
    migrate(game)
    path = game / 'config/power-beta.json'
    document = json.loads(path.read_text())
    if not configured:
        # The shipped Defaults profile follows the revised pack default.
        document['settings'].setdefault('building', {}).setdefault('placement', {})['slabMode'] = 'DOUBLE'
    if configured:
        document['settings'].setdefault('visual', {})['slashChat'] = True
        document['settings'].setdefault('building', {})['autoWalk'] = True
    with tempfile.TemporaryDirectory(prefix='power-beta-profile-') as temporary:
        source = Path(temporary) / 'power-beta.json'
        source.write_text(json.dumps(document, indent=2) + '\n')
        atomic_copy(source, path)
    # Remove only superseded, positively identified owned/configuration artifacts.
    owned = set(json.loads((ROOT / 'tools/legacy_modules.json').read_text())) | {'gcapi3'}
    for path in (game / 'mods').iterdir():
        if not path.name.endswith(('.jar', '.jar.disabled')):
            continue
        with zipfile.ZipFile(path) as archive:
            metadata = json.loads(archive.read('fabric.mod.json')) if 'fabric.mod.json' in archive.namelist() else {}
        if metadata.get('id') in owned:
            path.unlink()
    for entry in manifest['mods']:
        source = stage / '.minecraft/mods' / entry['file']
        if hashlib.sha256(source.read_bytes()).hexdigest() != entry['sha256']:
            raise RuntimeError('Pack artifact hash differs')
        atomic_copy(source, game / 'mods' / entry['file'])
    for name in ['PACK-MANIFEST.json', 'README.md', 'UPSTREAMS.md', 'CHANGELOG.md', 'COMPONENTS.json', 'NOTICE.txt']:
        if (stage / name).exists():
            atomic_copy(stage / name, instance / name)
    print(f'Updated {instance.name}; backup {backup}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('instance', type=Path)
    parser.add_argument('--configured', action='store_true', help='Enable slash chat and the assigned auto-walk action')
    args = parser.parse_args()
    update(args.instance, args.configured)
