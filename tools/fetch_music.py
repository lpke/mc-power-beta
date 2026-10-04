#!/usr/bin/env python3
"""Verify/download the pinned overworld game assets. Never fetch during gameplay."""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/powerbeta/music'

def main():
    manifest = json.loads((ASSETS / 'manifest.json').read_text())
    for track in manifest['tracks']:
        path = ASSETS / track['file']
        if path.exists() and hashlib.sha1(path.read_bytes()).hexdigest() == track['sha1']:
            continue
        digest = track['sha1']
        with urllib.request.urlopen(f'https://resources.download.minecraft.net/{digest[:2]}/{digest}', timeout=60) as response:
            data = response.read(track['size'] + 1)
        if len(data) != track['size'] or hashlib.sha1(data).hexdigest() != digest:
            raise ValueError(f'Invalid asset: {track["file"]}')
        temp = path.with_suffix('.tmp')
        temp.write_bytes(data)
        temp.replace(path)
        print(track['file'], len(data), flush=True)

if __name__ == '__main__':
    main()
