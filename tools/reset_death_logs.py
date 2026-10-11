#!/usr/bin/env python3
"""Back up and reset pre-1.4.2 death journals in an idle updated instance."""
from datetime import datetime
from pathlib import Path
import argparse
import struct
import tempfile
import zipfile
from instance_files import assert_idle, atomic_copy


def reset(instance):
    instance = instance.resolve()
    assert_idle(instance)
    folder = instance / '.minecraft/power-beta-data/deaths'
    for part in [folder, *folder.parents]:
        if part.is_symlink():
            raise RuntimeError('Refusing a symbolic link in the death journal path')
    originals = {}
    for journal in sorted(folder.glob('*.journal')):
        if journal.is_symlink() or not journal.is_file():
            raise RuntimeError('Unsafe death journal')
        data = journal.read_bytes()
        if data[:8] == struct.pack('>II', 0x50424448, 1):
            originals[str(journal.relative_to(instance))] = data
    if not originals:
        return {'resetLogs': 0}
    backup = instance / 'power-beta-update-backups' / (
        'before-death-log-reset-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f') + '.zip')
    backup.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(backup, 'x', zipfile.ZIP_DEFLATED) as archive:
        for name, data in originals.items():
            archive.writestr(name, data)
    with zipfile.ZipFile(backup) as archive:
        if archive.testzip() is not None or any(archive.read(name) != data for name, data in originals.items()):
            raise RuntimeError('Death log backup verification failed')
    removed = []
    try:
        for name, data in originals.items():
            assert_idle(instance)
            if (instance / name).read_bytes() != data:
                raise RuntimeError('Death log changed during reset')
            (instance / name).unlink()
            removed.append(name)
    except BaseException:
        assert_idle(instance)
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / 'restore'
            for name in removed:
                source.write_bytes(originals[name])
                atomic_copy(source, instance / name)
        raise
    return {'resetLogs': len(removed), 'backup': str(backup)}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('instance', type=Path)
    args = parser.parse_args()
    print(reset(args.instance))
