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


class ArchiveTest(unittest.TestCase):
    def setUp(self):
        import io
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / 'pack.zip'
        jar_bytes = io.BytesIO()
        with zipfile.ZipFile(jar_bytes, 'w') as jar:
            jar.writestr('fabric.mod.json', json.dumps({'id': 'power_beta', 'version': '1.0.1'}))
        self.files = {'power-beta-1.0.1.jar': jar_bytes.getvalue()}
        self.files.update({f'platform-{i}.jar': bytes([i]) for i in range(6)})

    def write(self, extra=None):
        import hashlib
        manifest = {'version': '1.0.1', 'mods': [
            {'file': name, 'sha256': hashlib.sha256(data).hexdigest(), 'enabled': True}
            for name, data in self.files.items()]}
        with zipfile.ZipFile(self.path, 'w') as archive:
            for name, data in self.files.items():
                archive.writestr('Power Beta/.minecraft/mods/' + name, data)
            archive.writestr('Power Beta/PACK-MANIFEST.json', json.dumps(manifest))
            if extra:
                archive.writestr(extra, 'private')

    def test_only_verified_clean_seven_jar_archives_pass(self):
        from release_data import verify_archive
        self.write()
        verify_archive(self.path, '1.0.1')
        for extra in ['Power Beta/accounts.json', 'Power Beta/.minecraft/logs/latest.log',
                      'Power Beta/.minecraft/saves/World/level.dat', 'Power Beta/.minecraft/mods/test.jar',
                      'Power Beta/.minecraft/mods/power-beta-validation.jar', '../escape']:
            self.write(extra)
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                verify_archive(self.path, '1.0.1')
        self.write()
        with self.assertRaises(ValueError):
            verify_archive(self.path, '1.0.2')


class GitHubFeedTest(unittest.TestCase):
    def test_uploaded_download_waits_for_visibility_but_rejects_wrong_content(self):
        import release
        with patch.object(release, 'verify_download', side_effect=[False, False, True]) as verify, patch.object(release.time, 'sleep'):
            release.wait_for_download('https://example.invalid/pack.zip', 'a' * 64)
            self.assertEqual(3, verify.call_count)
        with patch.object(release, 'verify_download', return_value=False), patch.object(release.time, 'sleep'), self.assertRaises(ValueError):
            release.wait_for_download('https://example.invalid/pack.zip', 'a' * 64)
        with patch.object(release, 'verify_download', side_effect=ValueError('checksum differs')) as verify, patch.object(release.time, 'sleep') as sleep, self.assertRaises(ValueError):
            release.wait_for_download('https://example.invalid/pack.zip', 'a' * 64)
        self.assertEqual(1, verify.call_count)
        sleep.assert_not_called()

    def test_snapshot_uses_github_notes_and_retains_old_downloads(self):
        from release import website_feed
        old_url = 'https://example.invalid/verified-old.zip'
        releases = []
        for version in ['1.0.0', '1.1.0']:
            releases.append({'tag_name': 'v' + version, 'draft': False, 'prerelease': False,
                'published_at': '2026-10-08T12:00:00Z', 'body': '- GitHub notes for ' + version + '\n\nMetadata.',
                'html_url': 'https://github.com/lpke/mc-power-beta/releases/tag/v' + version,
                'assets': [{'name': 'Power-Beta-' + version + '-Prism.zip', 'state': 'uploaded',
                    'size': 123, 'digest': 'sha256:' + 'a' * 64, 'browser_download_url': 'https://github.com/archive'}]})
        feed = website_feed('lpke/mc-power-beta', '1.1.0', 'b' * 40, None, 'a' * 64,
                            'https://example.invalid/new.zip', {'releases': [{'version': '1.0.0', 'download': old_url}]}, releases)
        self.assertEqual(['1.1.0', '1.0.0'], [r['version'] for r in feed['releases']])
        self.assertEqual(['GitHub notes for 1.1.0'], feed['releases'][0]['changes'])
        self.assertEqual(old_url, feed['releases'][1]['download'])

    def test_new_draft_lookup_tolerates_github_propagation(self):
        import release
        draft = {'tag_name': 'v1.1.0', 'draft': True}
        with patch.object(release, 'releases_for', side_effect=[[], [], [draft]]), patch.object(release.time, 'sleep'):
            self.assertEqual(draft, release.find_release('owner/repo', 'v1.1.0'))
