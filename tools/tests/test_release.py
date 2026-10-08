from pathlib import Path
import json
import sys
import tempfile
import unittest
import zipfile
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_data import changelog, pack_version, sha256
import update_artifacts as updater


class ReleaseNotesTest(unittest.TestCase):
    def test_versions_are_unique_and_ordered(self):
        good = '# Changelog\n\n## [Unreleased]\n\n## [1.1.0] - 2026-10-08\n\n- Added a feature.\n\n## [1.0.0] - 2026-10-07\n\n- Initial release.\n'
        self.assertEqual('1.1.0', changelog(good)[0]['version'])
        for bad in [good.replace('1.1.0', '1.0.0'), good.replace('1.1.0', '01.1.0'),
                    good.replace('- Added a feature.', '### Added'), good.replace('2026-10-08', '2026-13-08'),
                    good.replace('## [Unreleased]', ''), good.replace('1.1.0', '0.1.0')]:
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                changelog(bad)


class UpdateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.stage, self.instance = self.root / 'stage', self.root / 'instance'
        for folder in [self.stage, self.instance]:
            (folder / '.minecraft/mods').mkdir(parents=True)
            (folder / 'screenshots').mkdir()
        self.old = self.instance / '.minecraft/mods/power-beta-1.0.0.jar'
        self.new = self.stage / '.minecraft/mods/power-beta-1.0.1.jar'
        for path, version in [(self.old, '1.0.0'), (self.new, '1.0.1')]:
            with zipfile.ZipFile(path, 'w') as jar:
                jar.writestr('fabric.mod.json', json.dumps({'id': 'power_beta', 'version': version}))
        self.original = self.old.read_bytes()
        mods = [{'file': self.new.name, 'sha256': sha256(self.new), 'enabled': True}]
        for i in range(6):
            name = f'platform-{i}.jar'
            for folder in [self.stage, self.instance]:
                (folder / '.minecraft/mods' / name).write_bytes(bytes([i]))
            mods.append({'file': name, 'sha256': sha256(self.stage / '.minecraft/mods' / name), 'enabled': True})
        (self.stage / 'PACK-MANIFEST.json').write_text(json.dumps({'version': '1.0.1', 'mods': mods}))
        for name in ['README.md', 'CHANGELOG.md', 'COMPONENTS.json', 'LICENSE.txt']:
            (self.stage / name).write_text('new ' + name)
            (self.instance / name).write_text('old ' + name)
        (self.instance / '.minecraft/config').mkdir()
        (self.instance / '.minecraft/config/power-beta.json').write_text('untouched')
        (self.instance / 'UPSTREAMS.md').write_text('old documentation')

    def test_upgrade_removes_old_jar_and_preserves_settings(self):
        result = updater.update(self.instance, self.stage)
        self.assertFalse(self.old.exists())
        self.assertFalse((self.instance / 'UPSTREAMS.md').exists())
        self.assertEqual('1.0.1', result['version'])
        self.assertEqual('untouched', (self.instance / '.minecraft/config/power-beta.json').read_text())
        with zipfile.ZipFile(result['backup']) as backup:
            self.assertEqual(self.original, backup.read('.minecraft/mods/' + self.old.name))

    def test_partial_failure_rolls_back_new_and_existing_files(self):
        copy = updater.atomic_copy
        calls = 0
        def interrupted(source, target):
            nonlocal calls
            calls += 1
            if calls == 3:
                raise OSError('simulated interrupted update')
            copy(source, target)
        with patch.object(updater, 'atomic_copy', interrupted), self.assertRaises(OSError):
            updater.update(self.instance, self.stage)
        self.assertEqual(self.original, self.old.read_bytes())
        self.assertFalse((self.instance / '.minecraft/mods' / self.new.name).exists())
        self.assertEqual('old README.md', (self.instance / 'README.md').read_text())
        self.assertTrue((self.instance / 'UPSTREAMS.md').exists())

    def test_dependency_mismatch_and_running_client_stop_before_backup(self):
        (self.instance / '.minecraft/mods/platform-1.jar').write_bytes(b'changed')
        with self.assertRaises(RuntimeError):
            updater.update(self.instance, self.stage)
        self.assertFalse((self.instance / 'power-beta-update-backups').exists())
        with patch.object(updater, 'assert_idle', side_effect=RuntimeError('running')), self.assertRaises(RuntimeError):
            updater.update(self.instance, self.stage)

    def test_symlink_rejected_before_mutation(self):
        (self.instance / 'README.md').unlink()
        (self.instance / 'README.md').symlink_to(self.stage / 'README.md')
        with self.assertRaises(RuntimeError):
            updater.update(self.instance, self.stage)
        self.assertEqual(self.original, self.old.read_bytes())
