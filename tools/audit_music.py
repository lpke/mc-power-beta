#!/usr/bin/env python3
"""Compare the bundled catalog with official historical music and record events."""
import hashlib
import json
from pathlib import Path
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(tempfile.gettempdir()) / 'power-beta-music-audit'


def read_json(url, expected_hash=None):
    CACHE.mkdir(exist_ok=True)
    path = CACHE / (hashlib.sha256(url.encode()).hexdigest() + '.json')
    if not path.exists():
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        if expected_hash and hashlib.sha1(data).hexdigest() != expected_hash:
            raise ValueError('Mismatched official metadata: ' + url)
        json.loads(data)
        path.write_bytes(data)
    data = path.read_bytes()
    if expected_hash and hashlib.sha1(data).hexdigest() != expected_hash:
        raise ValueError('Mismatched cached metadata: ' + str(path))
    return json.loads(data)


def resolve(sounds, event, stack=()):
    if event in stack:
        raise ValueError('Cyclic sound event: ' + event)
    result = set()
    for entry in sounds[event]['sounds']:
        if isinstance(entry, str):
            result.add(entry.removeprefix('minecraft:'))
        elif entry.get('type') == 'event':
            result.update(resolve(sounds, entry['name'].removeprefix('minecraft:'), stack + (event,)))
        else:
            result.add(entry['name'].removeprefix('minecraft:'))
    return result


def main():
    manifest = json.loads((ROOT / 'src/main/resources/assets/powerbeta/music/manifest.json').read_text())
    bundled = {t['file']: t for t in manifest['tracks']}
    union, survival, creative, official, extra = set(), set(), set(), set(), set()
    aliases = manifest['legacyAliases']
    for source in manifest['sources']:
        assets = read_json(source['assetIndex'])['objects']
        official.update((name, item['hash'], item['size']) for name, item in assets.items())
        digest = assets['minecraft/sounds.json']['hash']
        assert digest == source['soundsSha1']
        sounds = read_json(f'https://resources.download.minecraft.net/{digest[:2]}/{digest}', digest)
        found = set()
        for event in sounds:
            background = event in {'music.game', 'music.creative', 'music.game.creative', 'music.under_water'} or event.startswith('music.overworld.')
            additional = event in {'music.menu', 'music.credits', 'music.game.end.credits'} or event.startswith(('music_disc.', 'records.'))
            if not background and not additional:
                continue
            for name in resolve(sounds, event):
                assert 'minecraft/sounds/' + name + '.ogg' in assets, name
                stem = name.rsplit('/', 1)[-1]
                file = aliases.get(stem, stem) + '.ogg'
                found.add(file)
                (creative if 'creative' in event else survival if background else extra).add(file)
        union.update(found)
        print(source['version'], len(found), 'background/menu/credits/record tracks')
    assert union == bundled.keys(), f'Missing: {union - bundled.keys()}; extra: {bundled.keys() - union}'
    assert creative - survival == {t['file'] for t in bundled.values() if t['usage'] == 'Creative'}
    for track in bundled.values():
        assert (track['asset'], track['sha1'], track['size']) in official, track['file']
    print(f'PASS: {len(union)} unique tracks; {len(survival | creative)} overworld; {len(extra - survival - creative)} additional; every asset verified.')


if __name__ == '__main__':
    main()
