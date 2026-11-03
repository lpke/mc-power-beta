#!/usr/bin/env python3
"""Compare the muted native decoder fixture's PCM with an independent FFmpeg decode."""
import argparse
import array
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def granule(data):
    position = 0
    final = None
    while position < len(data):
        assert data[position:position + 4] == b'OggS', 'Invalid Ogg page'
        segments = data[position + 26]
        size = sum(data[position + 27:position + 27 + segments])
        if data[position + 5] & 4:
            final = struct.unpack_from('<Q', data, position + 6)[0]
        position += 27 + segments + size
    assert position == len(data) and final is not None, 'Missing complete Ogg EOS page'
    return final


def compare(folder, row):
    source = ROOT / row['file']
    if not source.is_file():
        source = ROOT / 'src/main/resources' / row['file']
    data = source.read_bytes()
    assert len(data) == row['size'] and hashlib.sha1(data).hexdigest() == row['sha1'], source
    assert granule(data) == row['frames'], f'Source granule disagrees with manifest: {source}'
    decoded = subprocess.check_output([
        'ffmpeg', '-v', 'error', '-threads', '1', '-i', str(source),
        '-f', 's16le', '-acodec', 'pcm_s16le', 'pipe:1'])
    native = (folder / (row['resource'].replace('/', '_') + '.pcm')).read_bytes()
    channels = row['channels']
    expected = row['frames'] * channels
    actual = array.array('h'); actual.frombytes(native)
    independent = array.array('h'); independent.frombytes(decoded)
    if sys.byteorder != 'little':
        actual.byteswap(); independent.byteswap()
    assert len(actual) == expected, f'Native source frame loss or padding: {source}'
    assert row['decodedFrames'] == row['streamedFrames'] == row['frames'], source
    ffmpeg_frames = len(independent) // channels
    # FFmpeg can remove codec preroll or retain final page padding. The source EOS
    # granule remains authoritative. Report both lengths and align only either end.
    independent = independent[:expected]
    missing = expected - len(independent)
    candidates = []
    for offset in {0, missing}:
        part = actual[offset:offset + len(independent)]
        assert len(part) == len(independent) and part, source
        delta = [abs(x - y) for x, y in zip(part, independent)]
        candidates.append((sum(delta) / len(delta), max(delta), offset))
    mean, peak, offset = min(candidates)
    assert peak <= 3, f'Independent PCM mismatch: {source}, mean={mean}, peak={peak}, offset={offset}'
    return {'file': row['resource'], 'sourceFrames': row['frames'],
            'nativeFrames': len(actual) // channels, 'ffmpegFrames': ffmpeg_frames,
            'meanDifference': mean, 'peakDifference': peak, 'alignedSampleOffset': offset}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folder', type=Path, help='power-beta-validation-pcm from extra-sound-decoders-432')
    parser.add_argument('--output', type=Path, help='Detailed JSON comparison, kept outside the repository')
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'src/main/resources/assets/powerbeta/extra-sounds.json').read_text())
    assets = manifest['assets'] + manifest['existingAssets']
    rows = json.loads((args.folder / 'decoded.json').read_text())
    assert {row['file'] for row in rows} == {asset['file'] for asset in assets}, 'Native report does not cover the current manifest'
    assert len(rows) == len(assets), 'Native report has duplicate assets'
    with ThreadPoolExecutor(max_workers=2) as pool:
        result = list(pool.map(lambda row: compare(args.folder, row), rows))
    if args.output:
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'assets': len(result),
                      'maxMeanDifference': max(row['meanDifference'] for row in result),
                      'maxPeakDifference': max(row['peakDifference'] for row in result),
                      'frameDifferenceCount': sum(row['ffmpegFrames'] != row['sourceFrames'] for row in result),
                      'alignedPrerollCount': sum(row['alignedSampleOffset'] != 0 for row in result)}))


if __name__ == '__main__':
    main()
