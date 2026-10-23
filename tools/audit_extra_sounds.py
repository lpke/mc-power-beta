#!/usr/bin/env python3
"""Verify bundled action sounds against their pinned official Mojang resource index."""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / 'src/main/resources'


def fetch(url, digest):
    with urllib.request.urlopen(url, timeout=60) as response:
        data = response.read()
    assert hashlib.sha1(data).hexdigest() == digest, url
    return json.loads(data)


def main():
    manifest = json.loads((RESOURCES / 'assets/powerbeta/extra-sounds.json').read_text())
    sources = {}
    for name in ('source', 'bucketSource', 'waterSource'):
        source = manifest[name]
        metadata = fetch(source['metadata']['url'], source['metadata']['sha1'])
        assert metadata['assetIndex'] == source['assetIndex']
        index = fetch(source['assetIndex']['url'], source['assetIndex']['sha1'])['objects']
        sound = index['minecraft/sounds.json']
        assert sound == source['sounds']
        sounds = fetch(f"https://resources.download.minecraft.net/{sound['hash'][:2]}/{sound['hash']}", sound['hash'])
        sources[name] = (index, sounds)
    for event, definition in manifest['events'].items():
        index, sounds = sources['waterSource' if event.startswith(('ambient.underwater.', 'entity.player.'))
                                else 'bucketSource' if 'bucket' in event else 'source']
        assert sounds[event] == definition, event
        expected = {'minecraft/sounds/' + (entry if isinstance(entry, str) else entry['name']) + '.ogg'
                    for entry in definition['sounds']}
        assert expected == {asset['asset'] for asset in manifest['assets'] if asset['event'] == event}, event
    for asset in manifest['assets']:
        index, _ = sources[asset.get('source', 'source')]
        assert index[asset['asset']] == {'hash': asset['sha1'], 'size': asset['size']}, asset['file']
        data = (RESOURCES / asset['file']).read_bytes()
        assert len(data) == asset['size'] and hashlib.sha1(data).hexdigest() == asset['sha1'], asset['file']
    index, _ = sources['source']
    for asset in manifest['existingAssets']:
        assert index[asset['asset']] == {'hash': asset['sha1'], 'size': asset['size']}, asset['file']
        data = (ROOT / asset['file']).read_bytes()
        assert len(data) == asset['size'] and hashlib.sha1(data).hexdigest() == asset['sha1'], asset['file']
    print(f"PASS: {len(manifest['events'])} official events, {len(manifest['assets'])} new assets, "
          f"{len(manifest['existingAssets'])} existing assets; every byte verified against Mojang {manifest['source']['version']} and {manifest['bucketSource']['version']}.")


if __name__ == '__main__':
    main()
