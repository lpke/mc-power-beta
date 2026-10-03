#!/usr/bin/env python3
"""Read or extract complete LpkeTweaks hotbar snapshots. Never writes to a save."""
import argparse
from pathlib import Path
import struct
import sys
import zlib

HEADER = struct.Struct('>IIIQ')
MAGIC = 0x4C504B48
MAX_RECORD = 1024 * 1024
MAX_JOURNAL = 64 * 1024 * 1024


def read_records(path):
    if path.stat().st_size > MAX_JOURNAL:
        raise ValueError('Journal exceeds the supported 64 MiB limit')
    records = []
    error = None
    with path.open('rb') as stream:
        while raw := stream.read(HEADER.size):
            if len(raw) != HEADER.size:
                error = 'Incomplete final header'
                break
            magic, version, size, checksum = HEADER.unpack(raw)
            if magic != MAGIC or version != 1 or not 0 < size <= MAX_RECORD:
                error = 'Invalid record header'
                break
            payload = stream.read(size)
            if len(payload) != size or zlib.crc32(payload) != checksum:
                error = 'Incomplete or damaged record'
                break
            records.append(payload)
    return records, error


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('journal', type=Path)
    parser.add_argument('--extract', type=int, help='Zero-based record number; -1 selects the last complete record')
    parser.add_argument('--output', type=Path, help='New uncompressed NBT file to create')
    args = parser.parse_args()
    if (args.extract is None) != (args.output is None):
        parser.error('--extract and --output must be provided together')
    try:
        records, error = read_records(args.journal)
        print(f'{len(records)} complete, checksummed records')
        if error:
            print(f'Stopped at record {len(records)}: {error}. Earlier records remain readable.', file=sys.stderr)
        if args.extract is not None:
            if not -len(records) <= args.extract < len(records):
                raise ValueError('Record index is outside the complete records')
            payload = records[args.extract]
            with args.output.open('xb') as output:
                output.write(payload)
            print(f'Extracted {len(payload)} bytes to {args.output}')
        else:
            for index, payload in enumerate(records):
                print(f'{index}: {len(payload)} NBT bytes')
        return 1 if error else 0
    except (OSError, ValueError) as error:
        print(error, file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
