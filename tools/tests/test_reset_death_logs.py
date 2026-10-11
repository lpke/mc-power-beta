import struct
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from reset_death_logs import reset


class ResetDeathLogsTest(unittest.TestCase):
    def test_resets_only_previous_format_with_verified_backup(self):
        with tempfile.TemporaryDirectory() as directory:
            instance = Path(directory)
            folder = instance / '.minecraft/power-beta-data/deaths'
            folder.mkdir(parents=True)
            old = struct.pack('>II', 0x50424448, 1) + b'original old journal bytes'
            current = struct.pack('>II', 0x50424448, 2) + b'current journal bytes'
            (folder / 'old.journal').write_bytes(old)
            (folder / 'current.journal').write_bytes(current)
            unrelated = instance / '.minecraft/power-beta-data/carry.journal'
            unrelated.write_bytes(old)
            result = reset(instance)
            self.assertEqual(1, result['resetLogs'])
            self.assertFalse((folder / 'old.journal').exists())
            self.assertEqual(current, (folder / 'current.journal').read_bytes())
            self.assertEqual(old, unrelated.read_bytes())
            with zipfile.ZipFile(result['backup']) as archive:
                self.assertIsNone(archive.testzip())
                self.assertEqual(old, archive.read('.minecraft/power-beta-data/deaths/old.journal'))
            self.assertEqual({'resetLogs': 0}, reset(instance))

    def test_idle_failure_before_second_delete_restores_first_journal(self):
        with tempfile.TemporaryDirectory() as directory:
            instance = Path(directory)
            folder = instance / '.minecraft/power-beta-data/deaths'
            folder.mkdir(parents=True)
            data = struct.pack('>II', 0x50424448, 1) + b'original bytes'
            for name in ['a.journal', 'b.journal']:
                (folder / name).write_bytes(data)
            calls = 0

            def fail_once(_):
                nonlocal calls
                calls += 1
                if calls == 3:
                    raise RuntimeError('Injected idle-check failure')

            with patch('reset_death_logs.assert_idle', side_effect=fail_once):
                with self.assertRaises(RuntimeError):
                    reset(instance)
            for name in ['a.journal', 'b.journal']:
                self.assertEqual(data, (folder / name).read_bytes())

    def test_running_instance_and_symlink_never_reset_logs(self):
        with tempfile.TemporaryDirectory() as directory:
            instance = Path(directory)
            folder = instance / '.minecraft/power-beta-data/deaths'
            folder.mkdir(parents=True)
            target = instance / 'original.journal'
            data = struct.pack('>II', 0x50424448, 1)
            target.write_bytes(data)
            (folder / 'linked.journal').symlink_to(target)
            with patch('reset_death_logs.assert_idle', side_effect=RuntimeError('Running')):
                with self.assertRaises(RuntimeError):
                    reset(instance)
            with self.assertRaises(RuntimeError):
                reset(instance)
            self.assertEqual(data, target.read_bytes())


if __name__ == '__main__':
    unittest.main()
