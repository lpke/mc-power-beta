#!/usr/bin/env python3
"""Restore the pinned official title textures from verified Mojang client archives."""
import hashlib
import io
import json
from pathlib import Path
from urllib.request import urlopen
import zipfile

ASSETS = Path(__file__).resolve().parents[1] / 'modules/title/src/main/resources/assets/power_title/logos'


def main():
    sources = json.loads((ASSETS / 'sources.json').read_text())
    for url in dict.fromkeys(row['client']['url'] for row in sources):
        rows = [row for row in sources if row['client']['url'] == url]
        with urlopen(url, timeout=120) as response:
            data = response.read()
        if hashlib.sha1(data).hexdigest() != rows[0]['client']['sha1']:
            raise ValueError('Client checksum mismatch')
        with zipfile.ZipFile(io.BytesIO(data)) as jar:
            images = [(row, jar.read(row['source'])) for row in rows]
        for row, image in images:
            if hashlib.sha256(image).hexdigest() != row['sha256']:
                raise ValueError('Title texture checksum mismatch: ' + row['file'])
        for row, image in images:
            (ASSETS / row['file']).write_bytes(image)
            print(row['version'] + ': ' + row['file'])


if __name__ == '__main__':
    main()
