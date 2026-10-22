#!/usr/bin/env python3
"""Package source PNGs into a Prism texture pack without building or changing settings."""
import argparse
import os
from pathlib import Path
import tempfile
import time
from zipfile import ZIP_DEFLATED, ZipFile

ROOT = Path(__file__).resolve().parents[1]
PACK_NAME = 'Power Beta Texture Preview.zip'
DEFAULT_SOURCE = ROOT / 'input-api/src/main/resources'


def scan(sources):
    files = []
    for source in sources:
        for path in sorted(source.rglob('*')):
            if path.is_file() and path.suffix.lower() == '.png':
                stat = path.stat()
                files.append((path.relative_to(source).as_posix(), path, stat.st_mtime_ns, stat.st_size))
    if not files:
        raise ValueError('No source PNGs found')
    return files


def read_images(files):
    images = {}
    for name, path, modified, size in files:
        data = path.read_bytes()
        stat = path.stat()
        if (stat.st_mtime_ns, stat.st_size) != (modified, size):
            raise OSError('Image changed while reading: ' + str(path))
        if not data.startswith(b'\x89PNG\r\n\x1a\n') or not data.endswith(b'\x00\x00\x00\x00IEND\xaeB`\x82'):
            raise ValueError('PNG save is incomplete or invalid: ' + str(path))
        if name in images and images[name] != data:
            raise ValueError('Different source images share a pack path: ' + name)
        images[name] = data
    return images


def write_pack(target, images):
    target.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=target.parent, prefix='.power-beta-preview-', suffix='.tmp', delete=False) as stream:
        temporary = Path(stream.name)
    try:
        with ZipFile(temporary, 'w', ZIP_DEFLATED) as archive:
            archive.writestr('pack.txt', 'Power Beta texture preview\nEditable source textures\n')
            for name, data in sorted(images.items()):
                archive.writestr(name, data)
        with temporary.open('rb') as stream:
            os.fsync(stream.fileno())
        os.replace(temporary, target)
    finally:
        temporary.unlink(missing_ok=True)


def preview(sources, target, watch):
    previous = None
    last_error = None
    while True:
        try:
            files = scan(sources)
            if files != previous:
                if watch:
                    time.sleep(0.25)
                    if scan(sources) != files:
                        continue
                images = read_images(files)
                if scan(sources) != files:
                    raise OSError('Source files changed while preparing the pack; retry after saving')
                write_pack(target, images)
                previous, last_error = files, None
                print(f'Updated {len(images)} textures: {target}', flush=True)
        except (OSError, ValueError) as error:
            if not watch:
                raise
            if str(error) != last_error:
                print(f'Waiting for a complete save; previous pack retained. {error}', flush=True)
                last_error = str(error)
        if not watch:
            return
        time.sleep(0.5)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--instance', type=Path, required=True, help='Prism instance directory')
    parser.add_argument('--source', type=Path, action='append', help='Resource root containing PNGs; repeat to combine roots. Defaults to shared GUI resources')
    parser.add_argument('--watch', action='store_true', help='Refresh the ZIP after source images are saved; Ctrl+C stops watching')
    args = parser.parse_args()
    instance = args.instance.expanduser().resolve()
    if not (instance / 'instance.cfg').is_file() or not (instance / '.minecraft').is_dir():
        parser.error('--instance must name an existing Prism instance directory')
    sources = [path.expanduser().resolve() for path in (args.source or [DEFAULT_SOURCE])]
    for source in sources:
        if not source.is_dir():
            parser.error('Source directory does not exist: ' + str(source))
        print('Editable source:', source, flush=True)
    target = instance / '.minecraft/texturepacks' / PACK_NAME
    print(f'In Options > Texture pack, select Default, then {PACK_NAME} to reload.', flush=True)
    if args.watch:
        print('Watching source PNGs. Press Ctrl+C to stop.', flush=True)
    try:
        preview(sources, target, args.watch)
    except KeyboardInterrupt:
        print('\nStopped watching.')
    except (OSError, ValueError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
