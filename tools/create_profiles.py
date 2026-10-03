#!/usr/bin/env python3
"""Create fresh Prism profiles from the built pack, without copying worlds or account data."""
from pathlib import Path
import argparse, json, re, shutil, zipfile

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
        migrated = []
        if configured:
            # Copy only settings files for components in this pack; keep obsolete mods out.
            for dest in (game / 'config').rglob('*'):
                rel = dest.relative_to(game)
                source = reference / rel
                if dest.is_file() and source.is_file():
                    shutil.copy2(source, dest)
                    migrated.append(str(rel))
            opts = properties(reference / 'options.txt', ':')
            known = properties(game / 'options.txt', ':')
            update_properties(game / 'options.txt', {k:v for k,v in opts.items() if k in known and k != 'lastServer'}, ':')
            legacy = properties(reference / 'config/beta-fastplace.properties')
            fast = {
                'enabled': 'enabled', 'fastBlockPlacementCount': 'attemptsPerTick',
                'fastPlacementItemBlackList': 'blacklist', 'fastPlacementItemWhiteList': 'whitelist',
                'fastPlacementItemListType': 'listMode', 'fastPlacementRememberOrientation': 'rememberOrientation',
                'placementRestriction': 'restrictionEnabled', 'placementRestrictionMode': 'restrictionMode',
                'placementRestrictionTiedToFast': 'restrictionTiedToFast',
            }
            update_properties(game / 'config/lpketweaks.properties',
                              {'placement.' + new: legacy[old] for old, new in fast.items() if old in legacy})
            omni = properties(reference / 'config/omnilook.properties')
            if 'toggleMode' in omni:
                update_properties(game / 'config/lpketweaks.properties', {'freeLookToggle': omni['toggleMode']})
            oldkeys = properties(reference / 'options.txt', ':')
            for old, new in {'key_key.fastplace.toggle':'key_Fast place (toggle)'}.items():
                if old in oldkeys: update_properties(game / 'options.txt', {new:oldkeys[old]}, ':')
            # The selected weather pack becomes independent switches, keeping the base textures selectable.
            selected = opts.get('skin', 'Default')
            if selected == 'Soft-Weather-Beta-1.7.3.zip':
                p = game / 'config/power-beta/visual.json'
                visual = json.loads(p.read_text()); visual.update(softRain=True, softSnow=True)
                p.write_text(json.dumps(visual, indent=2) + '\n')
                update_properties(game / 'options.txt', {'skin':'Default'}, ':')
            elif selected != 'Default':
                source = reference / 'texturepacks' / Path(selected).name
                if source.is_file(): shutil.copy2(source, game / 'texturepacks' / source.name)
            enabled = []
            if (reference / 'enabled-mods.json').exists():
                enabled = json.loads((reference / 'enabled-mods.json').read_text())
            else:
                for jar in (reference / 'mods').glob('*.jar'):
                    with zipfile.ZipFile(jar) as archive:
                        if 'fabric.mod.json' in archive.namelist():
                            enabled.append(json.loads(archive.read('fabric.mod.json'))['id'])
            features = {}
            if 'omnilook' in enabled: features['freeLook'] = 'true'
            if 'click-mining-forever' in enabled: features['clickMining'] = 'true'
            if 'beta_transport' in enabled: features.update(boatSteering='true', fastMinecarts='true')
            update_properties(game / 'config/lpketweaks.properties', features)
            wand = reference / 'config/creativeeditorwands/config.yml'
            if 'creativeeditorwands' in enabled and wand.exists() and re.search(r'^disableAllEditingTools: false$', wand.read_text(), re.M):
                update_properties(game / 'config/worldedit-beta.properties', {'enabled':'true'})
            migrated += ['options.txt', 'beta-fastplace -> placement settings', 'omnilook -> free-look activation',
                         'enabled standalone features -> integrated controls']
            # User's latest explicit camera-cycle preference supersedes the old profile.
            p = game / 'config/unitweaks/userinterface.yml'
            p.write_text(re.sub(r'^frontViewThirdPerson:.*$', 'frontViewThirdPerson: 0', p.read_text(), flags=re.M))
        else:
            update_properties(game / 'options.txt', {'gui_scale':'0.5', 'guiScale':'4', 'mouseSensitivity':'0.4'}, ':')
        # Resources contain only downloaded game audio/icons, never profile credentials.
        if (reference / 'resources').is_dir(): shutil.copytree(reference / 'resources', game / 'resources', dirs_exist_ok=True)
        (target / 'PROFILE.json').write_text(json.dumps({
            'profile':'configured' if configured else 'defaults', 'migrated':migrated,
            'new_features':'Pack defaults unless a matching legacy preference exists',
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
