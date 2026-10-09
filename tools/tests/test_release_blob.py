import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release
from release_blob import BlobStore, check_upload_budget, prune_candidates

ORIGIN = 'https://example.public.blob.vercel-storage.com'


def blob(version, size=400_000_000):
    path = f'releases/{version}/aaaaaaaaaaaa/Power-Beta-{version}-Prism.zip'
    return {'pathname': path, 'url': ORIGIN + '/' + path, 'size': size}


class RetentionTest(unittest.TestCase):
    def test_private_upload_preserves_production_download_until_deployment(self):
        old, new = blob('1.0.0'), blob('1.1.0')
        artifact = Mock(name='artifact')
        artifact.name = 'Power-Beta-1.1.0-Prism.zip'
        artifact.stat.return_value.st_size = new['size']
        with patch.object(release, 'blob_store') as context, \
                patch.object(release, 'pack_version', return_value='1.1.0'), \
                patch.object(release, 'urlopen', return_value=io.BytesIO(json.dumps({'download': old['url']}).encode())), \
                patch.object(release, 'verify_download', return_value=False), \
                patch.object(release, 'wait_for_download') as verify:
            store = context.return_value.__enter__.return_value
            store.list.return_value = store.prune.return_value = [old]
            result = release.upload_blob(artifact, {'blobOrigin': ORIGIN, 'website': 'https://example.invalid'}, None, 'a' * 64)
            self.assertEqual(new['url'], result)
            store.prune.assert_called_once_with([old['url']])
            store.put.assert_called_once_with(artifact, new['pathname'])
            verify.assert_called_once_with(new['url'], 'a' * 64)

    def test_private_retry_checks_budget_without_uploading_duplicate(self):
        latest = blob('1.1.0')
        artifact = Mock(name='artifact')
        artifact.name = 'Power-Beta-1.1.0-Prism.zip'
        artifact.stat.return_value.st_size = latest['size']
        with patch.object(release, 'blob_store') as context, \
                patch.object(release, 'pack_version', return_value='1.1.0'), \
                patch.object(release, 'urlopen', return_value=io.BytesIO(json.dumps({'download': latest['url']}).encode())), \
                patch.object(release, 'verify_download', return_value=True):
            store = context.return_value.__enter__.return_value
            store.list.return_value = store.prune.return_value = [latest]
            self.assertEqual(latest['url'], release.upload_blob(artifact,
                             {'blobOrigin': ORIGIN, 'website': 'https://example.invalid'}, None, 'a' * 64))
            store.put.assert_not_called()

    def test_prunes_old_releases_and_preserves_unrelated_files(self):
        old, latest = blob('1.0.0'), blob('1.1.0')
        other = {'pathname': 'images/photo.png', 'url': ORIGIN + '/images/photo.png', 'size': 50}
        wrong_version = blob('1.0.1')
        wrong_version['pathname'] = wrong_version['pathname'].replace('/1.0.1/', '/2.0.0/')
        wrong_version['url'] = ORIGIN + '/' + wrong_version['pathname']
        foreign = {**old, 'url': old['url'].replace('example.', 'other.')}
        rows = [old, latest, other, wrong_version, foreign]
        self.assertEqual([old], prune_candidates(rows, ORIGIN, [latest['url'] + '?download=1']))
        self.assertEqual([old, latest], prune_candidates(rows, ORIGIN, []))

    def test_missing_or_foreign_retained_download_stops_cleanup(self):
        for keep in [[blob('1.1.0')['url']], ['https://elsewhere.example/pack.zip']]:
            with self.subTest(keep=keep), self.assertRaises(ValueError):
                prune_candidates([blob('1.0.0')], ORIGIN, keep)

    def test_budget_includes_overlap_and_unrelated_files_without_double_counting_retries(self):
        old, new = blob('1.0.0'), blob('1.1.0')
        check_upload_budget([old], new['pathname'], new['size'])
        check_upload_budget([old, new], new['pathname'], new['size'])
        with self.assertRaises(ValueError):
            check_upload_budget([old, blob('1.0.1')], new['pathname'], new['size'])
        other = {'pathname': 'unrelated.bin', 'size': 150_000_000}
        with self.assertRaises(ValueError):
            check_upload_budget([old, other], new['pathname'], new['size'])
        with self.assertRaises(ValueError):
            check_upload_budget([], new['pathname'], 900_000_001)
        with self.assertRaises(ValueError):
            check_upload_budget([new], new['pathname'], new['size'] - 1)

    def test_cleanup_requires_the_deployed_snapshot(self):
        expected = {'download': blob('1.1.0')['url'], 'version': '1.1.0'}
        with patch.object(release, 'urlopen', return_value=io.BytesIO(b'{}')), \
                patch.object(release, 'blob_store') as store, self.assertRaises(ValueError):
            release.prune_release_mirrors({'website': 'https://example.invalid'}, None, expected)
        store.assert_not_called()
        for download, keep in [(expected['download'], [expected['download']]),
                               ('https://github.com/owner/repo/pack.zip', [])]:
            feed = {**expected, 'download': download}
            with self.subTest(download=download), \
                    patch.object(release, 'urlopen', return_value=io.BytesIO(json.dumps(feed).encode())), \
                    patch.object(release, 'blob_store') as store:
                store.return_value.__enter__.return_value.prune.return_value = []
                release.prune_release_mirrors({'website': 'https://example.invalid', 'blobOrigin': ORIGIN}, None, feed)
                store.return_value.__enter__.return_value.prune.assert_called_once_with(keep)


class BlobApiTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.work = Path(temp.name)
        (self.work / '.env.local').write_text('BLOB_READ_WRITE_TOKEN="vercel_blob_rw_example_fake"\n')
        self.run = Mock()
        self.store = BlobStore(self.work, {'blobOrigin': ORIGIN, 'vercelScope': 'test'}, self.run)

    def test_wrong_store_credentials_are_rejected(self):
        with self.assertRaises(ValueError):
            BlobStore(self.work, {'blobOrigin': ORIGIN.replace('example.', 'other.')}, self.run)

    def test_inventory_follows_every_page_and_encodes_cursor(self):
        pages = [{'blobs': [blob('1.0.0')], 'hasMore': True, 'cursor': 'page+2/'},
                 {'blobs': [blob('1.1.0')], 'hasMore': False}]
        with patch('release_blob.urlopen', side_effect=[io.BytesIO(json.dumps(p).encode()) for p in pages]) as request:
            self.assertEqual([blob('1.0.0'), blob('1.1.0')], self.store.list())
        self.assertIn('cursor=page%2B2%2F', request.call_args.args[0].full_url)

    def test_repeated_cursor_aborts_without_deleting(self):
        page = {'blobs': [], 'hasMore': True, 'cursor': 'same'}
        with patch('release_blob.urlopen', side_effect=[io.BytesIO(json.dumps(page).encode()) for _ in range(2)]), \
                self.assertRaises(ValueError):
            self.store.prune()
        self.run.assert_not_called()

    def test_cleanup_is_idempotent_and_checks_deletion(self):
        old, latest = blob('1.0.0'), blob('1.1.0')
        with patch.object(self.store, 'list', side_effect=[[old, latest], [latest], [latest], [latest]]):
            self.store.prune([latest['url']])
            self.store.prune([latest['url']])
        self.run.assert_called_once_with('vercel', 'blob', 'del', old['url'], '--scope', 'test', cwd=self.work)
        with patch.object(self.store, 'list', return_value=[old, latest]), self.assertRaises(ValueError):
            self.store.prune([latest['url']])
