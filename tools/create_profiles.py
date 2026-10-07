#!/usr/bin/env python3
"""Create fresh Prism profiles from the built pack, without copying worlds or account data."""
from pathlib import Path
import argparse, json, shutil

ROOT = Path(__file__).resolve().parents[1]


def properties(path, sep='='):
    return dict(line.split(sep, 1) for line in path.read_text().splitlines()
                if sep in line and not line.lstrip().startswith('#')) if path.exists() else {}


def update_properties(path, changes, sep='='):
    values = properties(path, sep)
    values.update(changes)
    path.write_text(('[General]\n' if path.suffix == '.cfg' else '') + '\n'.join(k + sep + str(v) for k, v in values.items()) + '\n')



def create(output, reference, java=None):
    stage = ROOT / 'dist/Power Beta'
    if not (stage / 'PACK-MANIFEST.json').exists():
        raise RuntimeError('Build the pack before creating profiles')
    targets = [output / 'power_beta_defaults', output / 'power_beta_configured']
    if any(p.exists() for p in targets):
        raise FileExistsError('Profile already exists; existing instances are never overwritten')
    for target, configured in zip(targets, [False, True]):
        shutil.copytree(stage, target)
        update_properties(target / 'instance.cfg', {'name': 'Power Beta - ' + ('Configured' if configured else 'Defaults')})
        if java:
            update_properties(target / 'instance.cfg', {'OverrideJavaLocation': 'true', 'JavaPath': java})
        game = target / '.minecraft'
        if configured:
            source = reference / 'config/power-beta.json'
            document = json.loads(source.read_text())
            if document.get('schemaVersion') != 3 or not isinstance(document.get('settings'), dict):
                raise ValueError('Reference must use the current Power Beta configuration')
            shutil.copy2(source, game / 'config/power-beta.json')
            selected = document['settings'].get('native', {}).get('skin', 'Default')
            if selected != 'Default':
                texture = reference / 'texturepacks' / Path(selected).name
                if texture.is_file():
                    (game / 'texturepacks').mkdir(exist_ok=True)
                    shutil.copy2(texture, game / 'texturepacks' / texture.name)
        # Resources contain only downloaded game audio/icons, never profile credentials.
        if (reference / 'resources').is_dir(): shutil.copytree(reference / 'resources', game / 'resources', dirs_exist_ok=True)
        (target / 'PROFILE.json').write_text(json.dumps({
            'profile':'configured' if configured else 'defaults',
            'configuration':'copied reference' if configured else 'pack defaults',
            'worlds_copied':False, 'account_data_copied':False,
        }, indent=2) + '\n')
        print(target)
    return targets


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--reference', type=Path, required=True, help='Read-only .minecraft directory or settings snapshot')
    p.add_argument('--java')
    a = p.parse_args()
    create(a.output, a.reference, a.java)
