#!/usr/bin/env python3
"""Extract original title backgrounds from Mojang's verified Java client downloads."""
import hashlib
import io
import json
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'modules/controls/src/main/resources/assets/power_controls'
TEXTURES = ROOT / 'textures/title/panoramas'
MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'


def fetch(url):
    with urllib.request.urlopen(url, timeout=120) as response:
        return response.read()


def main():
    versions = json.loads(fetch(MANIFEST))
    indexed = {v['id']: v for v in versions['versions']}
    selected = ['b1.7.3', 'b1.8.1', '1.0']
    for n in range(1, 22):
        prefix = f'1.{n}'
        candidates = [v for v in versions['versions'] if v['type'] == 'release'
                      and (v['id'] == prefix or v['id'].startswith(prefix + '.'))]
        selected.append(min(candidates, key=lambda v: v['releaseTime'])['id'])
    selected += ['26.1', '26.2', versions['latest']['release']]
    rows, scenes = [], {}
    for version in dict.fromkeys(selected):
        metadata = json.loads(fetch(indexed[version]['url']))
        client = metadata['downloads']['client']
        data = fetch(client['url'])
        if hashlib.sha1(data).hexdigest() != client['sha1']:
            raise RuntimeError('Client checksum mismatch: ' + version)
        with zipfile.ZipFile(io.BytesIO(data)) as jar:
            pattern = next((p for p in ['assets/minecraft/textures/gui/title/background/panorama{}.png',
                           'assets/minecraft/textures/gui/title/background/panorama_{}.png', 'title/bg/panorama{}.png']
                           if p.format(0) in jar.namelist()), None)
            row = {'id': version.replace('.', '_'), 'label': 'Minecraft ' + version,
                   'version': version, 'metadata': indexed[version]['url'], 'client': client}
            row['scene'] = 'b1_8_1' if version.startswith('1.') and int(version.split('.')[1]) <= 12 else row['id']
            if version.startswith('b'):
                row['label'] = 'Beta ' + version[1:]
            if pattern is None:
                if version != 'b1.7.3':
                    raise RuntimeError('Missing official panorama: ' + version)
                row['folder'] = ''
                row['label'] = 'Original dirt background'
            else:
                faces = [jar.read(pattern.format(i)) for i in range(6)]
                # Modern launchers overlay these resource-index files on the client JAR.
                if 'assetIndex' in metadata:
                    index_bytes = fetch(metadata['assetIndex']['url'])
                    if hashlib.sha1(index_bytes).hexdigest() != metadata['assetIndex']['sha1']:
                        raise RuntimeError('Asset-index checksum mismatch: ' + version)
                    objects = json.loads(index_bytes)['objects']
                    row['assetIndex'] = metadata['assetIndex']
                    row['resources'] = []
                    for i in range(6):
                        key = f'minecraft/textures/gui/title/background/panorama_{i}.png'
                        if key in objects:
                            asset = objects[key]
                            url = f"https://resources.download.minecraft.net/{asset['hash'][:2]}/{asset['hash']}"
                            image = fetch(url)
                            if hashlib.sha1(image).hexdigest() != asset['hash']:
                                raise RuntimeError('Panorama checksum mismatch: ' + version)
                            faces[i] = image
                            row['resources'].append({'file': key, 'url': url, **asset})
                digest = hashlib.sha256(b''.join(faces)).hexdigest()
                folder = scenes.setdefault(digest, 'official_' + row['id'])
                row['folder'] = folder
                row['faces'] = []
                directory = TEXTURES / folder
                directory.mkdir(parents=True, exist_ok=True)
                for i, face in enumerate(faces):
                    (directory / f'panorama{i}.png').write_bytes(face)
                    row['faces'].append({'file': f'panorama{i}.png', 'sha256': hashlib.sha256(face).hexdigest()})
            rows.append(row)
            print(version + ': ' + (row['folder'] or 'native dirt'), flush=True)
    (ASSETS / 'panoramas.json').write_text(json.dumps(rows, indent=2) + '\n')


if __name__ == '__main__':
    main()
