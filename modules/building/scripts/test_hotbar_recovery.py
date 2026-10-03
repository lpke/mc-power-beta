import contextlib
import io
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch
import zlib
import hotbar_recovery as recovery


def record(data):
    return struct.pack('>IIIQ', recovery.MAGIC, 1, len(data), zlib.crc32(data)) + data


class RecoveryTests(unittest.TestCase):
    def test_damaged_tail_keeps_earlier_complete_snapshot(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'inventory.journal'
            path.write_bytes(record(b'first') + record(b'second')[:-2])
            payloads, error = recovery.read_records(path)
            self.assertEqual([b'first'], payloads)
            self.assertIsNotNone(error)

    def test_extract_refuses_to_overwrite_existing_file(self):
        with tempfile.TemporaryDirectory() as folder:
            path, output = Path(folder) / 'inventory.journal', Path(folder) / 'save.nbt'
            path.write_bytes(record(b'original inventory'))
            output.write_bytes(b'leave this save alone')
            with patch.object(sys, 'argv', ['recovery', str(path), '--extract', '-1', '--output', str(output)]):
                with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                    self.assertEqual(2, recovery.main())
            self.assertEqual(b'leave this save alone', output.read_bytes())

    def test_bad_checksum_cannot_be_extracted(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'inventory.journal'
            path.write_bytes(record(b'original')[:-1] + b'x')
            records, error = recovery.read_records(path)
            self.assertEqual([], records)
            self.assertIsNotNone(error)


if __name__ == '__main__':
    unittest.main()
