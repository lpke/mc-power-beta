"""Bounded, latest-release-only storage for the private repository's public mirror."""
from contextlib import contextmanager
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import tempfile
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, urlopen

# Leave room below Hobby's 1 GB allowance, including the previous live ZIP
# while a new release is uploaded and the website deployment is verified.
STORAGE_BUDGET = 900_000_000
RELEASE_PATH = re.compile(
    r'releases/((?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*))/[a-f0-9]{12}/'
    r'Power-Beta-\1-Prism\.zip')


def mirror_url(url, origin):
    parsed = urlsplit(url or '')
    return (parsed.scheme + '://' + parsed.netloc == origin
            and RELEASE_PATH.fullmatch(parsed.path.lstrip('/'))
            and not parsed.fragment and parsed.query in ('', 'download=1'))


def prune_candidates(blobs, origin, keep):
    retained = {url.split('?')[0] for url in keep}
    if any(not mirror_url(url, origin) for url in keep):
        raise ValueError('Retention URL is not a Power Beta mirror in this store')
    if not retained.issubset({blob['url'] for blob in blobs}):
        raise ValueError('The retained release mirror is missing; refusing cleanup')
    return [blob for blob in blobs if blob['url'] not in retained
            and mirror_url(blob['url'], origin)
            and blob['url'] == origin + '/' + blob['pathname']]


def check_upload_budget(blobs, pathname, size):
    existing = next((blob for blob in blobs if blob['pathname'] == pathname), None)
    if existing and existing['size'] != size:
        raise ValueError('Existing release mirror size differs')
    total = sum(blob['size'] for blob in blobs) + (0 if existing else size)
    if total > STORAGE_BUDGET:
        raise ValueError(f'Blob upload would use {total} bytes; budget is {STORAGE_BUDGET}. '
                         'Use public GitHub release downloads or reduce the ZIP size.')


class BlobStore:
    def __init__(self, work, config, run):
        self.work, self.config, self.run = work, config, run
        self.origin = config['blobOrigin']
        self._token = shlex.split((work / '.env.local').read_text().partition('=')[2])[0]
        parts = self._token.split('_')
        if len(parts) < 5 or parts[:3] != ['vercel', 'blob', 'rw']:
            raise ValueError('Invalid Blob credential format')
        self._store_id = parts[3]
        if self.origin != f'https://{self._store_id.lower()}.public.blob.vercel-storage.com':
            raise ValueError('Blob credential belongs to another store')

    def list(self):
        # The CLI only prints a table. Use the versioned list API used by
        # @vercel/blob so pagination and byte counts remain machine-readable.
        blobs, seen = [], set()
        query = {'limit': 1000, 'mode': 'expanded'}
        while True:
            request = Request('https://vercel.com/api/blob?' + urlencode(query), headers={
                'Authorization': 'Bearer ' + self._token, 'x-api-version': '12',
                'x-vercel-blob-store-id': self._store_id})
            with urlopen(request, timeout=30) as response:
                page = json.load(response)
            blobs.extend(page['blobs'])
            if not page['hasMore']:
                return blobs
            cursor = page.get('cursor')
            if not cursor or cursor in seen:
                raise ValueError('Blob inventory pagination is incomplete')
            seen.add(cursor)
            query['cursor'] = cursor

    def prune(self, keep=()):
        blobs = self.list()
        for blob in prune_candidates(blobs, self.origin, keep):
            self.run('vercel', 'blob', 'del', blob['url'],
                     '--scope', self.config['vercelScope'], cwd=self.work)
        remaining = self.list()
        if prune_candidates(remaining, self.origin, keep):
            raise ValueError('Obsolete Power Beta mirrors remain; rerun cleanup')
        return remaining

    def put(self, artifact, pathname):
        check_upload_budget(self.list(), pathname, artifact.stat().st_size)
        self.run('vercel', 'blob', 'put', artifact, '--access', 'public', '--multipart', 'true',
                 '--pathname', pathname, '--scope', self.config['vercelScope'], cwd=self.work)


@contextmanager
def blob_store(config, site, run):
    link = json.loads((site / '.vercel/project.json').read_text())
    if link['projectId'] != config['vercelProjectId'] or link['orgId'] != config['vercelOrgId']:
        raise ValueError('Website is linked to a different Vercel project')
    with tempfile.TemporaryDirectory(prefix='power-beta-blob-') as directory:
        work = Path(directory)
        (work / '.vercel').mkdir()
        shutil.copy2(site / '.vercel/project.json', work / '.vercel/project.json')
        env_file = work / '.env.local'
        run('vercel', 'env', 'pull', env_file, '--environment', 'production', '--yes',
            '--scope', config['vercelScope'], cwd=work)
        os.chmod(env_file, 0o600)
        lines = [line for line in env_file.read_text().splitlines() if line.startswith('BLOB_READ_WRITE_TOKEN=')]
        if len(lines) != 1:
            raise ValueError('The linked project needs a Blob read-write credential')
        env_file.write_text(lines[0] + '\n')
        yield BlobStore(work, config, run)
