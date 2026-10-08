#!/usr/bin/env python3
"""Check, build and publish a release after the owner explicitly authorizes publication."""
import argparse
import hashlib
import json
import os
import re
import time
from pathlib import Path
import shutil
import subprocess
import tempfile
from urllib.error import HTTPError
from urllib.request import urlopen
from release_data import ROOT, pack_version, release_notes, sha256, verify_archive


def run(*args, cwd=ROOT, capture=False):
    result = subprocess.run(list(map(str, args)), cwd=cwd, check=True, text=True,
                            stdout=subprocess.PIPE if capture else None)
    return result.stdout.strip() if capture else None


def remote_head(repository, ref='heads/main'):
    return run('gh', 'api', f'repos/{repository}/git/ref/{ref}', '--jq', '.object.sha', capture=True)


def releases_for(repository):
    pages = json.loads(run('gh', 'api', f'repos/{repository}/releases', '--paginate', '--slurp', capture=True))
    return [release for page in pages for release in page]


def find_release(repository, tag):
    for attempt in range(6):
        release = next((r for r in releases_for(repository) if r['tag_name'] == tag), None)
        if release is not None:
            return release
        time.sleep(2)
    raise ValueError('GitHub has not exposed the new draft yet; rerun to resume')


def website_feed(repository, version, commit, artifact, digest, download, prior, releases):
    history = []
    old = {r['version']: r for r in prior.get('releases', [])}
    for release in releases:
        v = release['tag_name'].removeprefix('v')
        if release['draft'] or release['prerelease'] or not re.fullmatch(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)', v):
            continue
        file = f'Power-Beta-{v}-Prism.zip'
        asset = next((a for a in release['assets'] if a['name'] == file and a['state'] == 'uploaded'), None)
        if asset is None:
            continue
        changes = [line[2:].strip() for line in release['body'].splitlines() if line.startswith('- ')]
        entry = {'version': v, 'date': release['published_at'][:10], 'changes': changes,
                 'github': release['html_url'], 'file': file, 'bytes': asset['size'],
                 'sha256': asset['digest'].removeprefix('sha256:'),
                 'download': download + '?download=1' if v == version else old.get(v, {}).get('download', asset['browser_download_url']),
                 'checksum': '/download.sha256' if v == version else asset['browser_download_url'] + '.sha256'}
        history.append(entry)
    history.sort(key=lambda r: tuple(map(int, r['version'].split('.'))), reverse=True)
    if not history or history[0]['version'] != version:
        raise ValueError('GitHub release list is stale; rerun to resume website publication')
    return {'schemaVersion': 1, 'commit': commit, **history[0], 'releases': history}


def clean_commit(directory):
    if run('git', 'status', '--porcelain', cwd=directory, capture=True):
        raise ValueError('Commit the work before publishing: ' + str(directory))
    if run('git', 'branch', '--show-current', cwd=directory, capture=True) != 'main':
        raise ValueError('Publish from main')
    return run('git', 'rev-parse', 'HEAD', cwd=directory, capture=True)


def verify_download(url, digest):
    try:
        with urlopen(url, timeout=120) as response:
            actual = hashlib.file_digest(response, 'sha256').hexdigest()
    except HTTPError as error:
        if error.code == 404:
            return False
        raise
    if actual != digest:
        raise ValueError('Remote artifact differs; refusing to replace it')
    return True


def upload_blob(artifact, config, site, digest):
    pathname = f'releases/{pack_version()}/{digest[:12]}/{artifact.name}'
    url = config['blobOrigin'] + '/' + pathname
    if verify_download(url, digest):
        return url
    # The CLI resolves Blob credentials locally. Keep its temporary env file outside
    # both repositories and discard it even if publication fails.
    with tempfile.TemporaryDirectory(prefix='power-beta-publish-') as directory:
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
        run('vercel', 'blob', 'put', artifact, '--access', 'public', '--multipart', 'true',
            '--pathname', pathname, '--scope', config['vercelScope'], cwd=work)
    if not verify_download(url, digest):
        raise ValueError('Uploaded download is not available')
    return url


def publish(args):
    config = json.loads((ROOT / 'tools/publish_config.json').read_text())
    site = args.site.resolve()
    if site == ROOT or not (site / 'tools/sync_release.py').is_file():
        raise ValueError('--site must point to the Power Beta website checkout')
    link = json.loads((site / '.vercel/project.json').read_text())
    if link['projectId'] != config['vercelProjectId'] or link['orgId'] != config['vercelOrgId']:
        raise ValueError('Website is linked to a different Vercel project')
    run('vercel', 'project', 'inspect', '--non-interactive', '--scope', config['vercelScope'], cwd=site)
    commit, site_commit = clean_commit(ROOT), clean_commit(site)
    if remote_head(config['repository']) != commit or remote_head(config['websiteRepository']) != site_commit:
        raise ValueError('Push the reviewed commits before publishing')
    if not args.skip_build:
        run('python3', '-m', 'unittest', 'discover', '-s', 'tools/tests')
        run('python3', 'tools/build_pack.py')
    version = pack_version()
    notes = release_notes()
    artifact = ROOT / f'dist/Power-Beta-{version}-Prism.zip'
    verify_archive(artifact, version)
    digest = sha256(artifact)
    receipt = json.loads((ROOT / 'dist/build-receipt.json').read_text())
    if receipt != {'commit': commit, 'clean': True, 'version': version, 'sha256': digest}:
        raise ValueError('Build receipt is stale or came from uncommitted work; rebuild')
    tag = 'v' + version
    repository = config['repository']
    releases = releases_for(config['repository'])
    existing = next((r for r in releases if r['tag_name'] == tag), None)
    if existing and (existing['target_commitish'] if existing['draft'] else remote_head(repository, 'tags/' + tag)) != commit:
        raise ValueError('Release tag points at another commit; use a new version')
    for release in releases:
        other = release['tag_name'].removeprefix('v').split('.')
        if len(other) == 3 and all(p.isdigit() for p in other) and tuple(map(int, other)) > tuple(map(int, version.split('.'))):
            raise ValueError('Refusing to replace a newer release with an older one')
    checksum = artifact.with_name(artifact.name + '.sha256')
    expected_checksum = digest + '  ' + artifact.name + '\n'
    if checksum.read_text() != expected_checksum:
        raise ValueError('Checksum file differs')
    body = '\n'.join('- ' + change for change in notes[0]['changes'])
    body += f'\n\nBuilt from `{commit}`.\n\nSHA-256: `{digest}`\n'
    with tempfile.TemporaryDirectory(prefix='power-beta-release-') as directory:
        note_file = Path(directory) / 'notes.md'
        note_file.write_text(body)
        if not existing:
            run('gh', 'release', 'create', tag, '--repo', repository, '--target', commit,
                '--title', 'Power Beta ' + version, '--notes-file', note_file, '--draft')
            # GitHub does not create a draft's tag until publication.
            existing = find_release(repository, tag)
        elif existing['body'].strip() != body.strip():
            raise ValueError('Existing release notes differ; do not rewrite a published release')
        for path in [artifact, checksum]:
            found = next((a for a in existing['assets'] if a['name'] == path.name), None)
            if found:
                expected = 'sha256:' + sha256(path)
                if found.get('digest') != expected:
                    raise ValueError('Existing GitHub asset differs or has no verifiable digest')
            elif existing['draft']:
                run('gh', 'release', 'upload', tag, path, '--repo', repository)
            else:
                raise ValueError('Published release is missing an asset; publish a new version')
        download = upload_blob(artifact, config, site, digest)
        if existing['draft']:
            run('gh', 'release', 'edit', tag, '--repo', repository, '--draft=false', '--latest')
        prior = json.loads((site / 'public/release.json').read_text())
        feed = None
        for attempt in range(6):
            try:
                feed = website_feed(repository, version, commit, artifact, digest, download, prior, releases_for(repository))
                break
            except ValueError:
                if attempt == 5: raise
                time.sleep(2)
        feed_path = Path(directory) / 'release.json'
        feed_path.write_text(json.dumps(feed, indent=2) + '\n')
        # The public snapshot uses GitHub release contents, including owner edits.
        # The website also refreshes these records directly once the repository is public.
        run('python3', 'tools/sync_release.py', feed_path, cwd=site)
        run('python3', '-m', 'unittest', 'discover', '-s', 'tests', cwd=site)
        run('node', '--test', cwd=site)
    if run('git', 'status', '--porcelain', cwd=site, capture=True):
        run('git', 'add', 'public/index.html', 'public/release.json', 'public/download.sha256', 'vercel.json', cwd=site)
        run('git', 'commit', '-m', 'Publish Power Beta ' + version, cwd=site)
        run('git', '-c', 'credential.helper=', '-c', 'credential.helper=!gh auth git-credential',
            'push', 'https://github.com/' + config['websiteRepository'] + '.git', 'main', cwd=site)
    run('vercel', 'deploy', '--prod', '--yes', '--scope', config['vercelScope'], cwd=site)
    print('Published ' + feed['github'])
    print(config['website'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    actions = parser.add_mutually_exclusive_group(required=True)
    actions.add_argument('--check', action='store_true', help='Validate versions and notes without publishing')
    actions.add_argument('--publish', action='store_true', help='Publish only with explicit owner permission')
    parser.add_argument('--site', type=Path, help='Local checkout of the linked website repository')
    parser.add_argument('--skip-build', action='store_true', help='Reuse a verified build of this exact clean commit')
    args = parser.parse_args()
    try:
        release_notes()
        if args.check:
            print('Release metadata valid: ' + pack_version())
        else:
            if args.site is None:
                raise ValueError('--publish requires --site')
            publish(args)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
