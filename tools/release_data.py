"""Version, changelog and archive checks shared by build and release tools."""
from datetime import date
from pathlib import Path
import hashlib
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)'


def pack_version(root=ROOT):
    match = re.search(r'^mod_version\s*=\s*(.+)$', (root / 'gradle.properties').read_text(), re.M)
    if not match or not re.fullmatch(VERSION, match[1]):
        raise ValueError('Pack version must use major.minor.patch without leading zeroes')
    return match[1]


def changelog(text):
    releases = []
    current = None
    unreleased = False
    for line in text.splitlines():
        if line == '## [Unreleased]':
            if unreleased or releases:
                raise ValueError('Unreleased must appear once, before releases')
            unreleased = True
            current = None
        elif line.startswith('## '):
            match = re.fullmatch(r'## \[(' + VERSION + r')\] - (\d{4}-\d{2}-\d{2})', line)
            if not match:
                raise ValueError('Use ## [X.Y.Z] - YYYY-MM-DD release headings')
            version, published = match[1], match[5]
            date.fromisoformat(published)
            if releases and tuple(map(int, version.split('.'))) >= tuple(map(int, releases[-1]['version'].split('.'))):
                raise ValueError('Release versions must be unique and newest first')
            current = {'version': version, 'date': published, 'changes': []}
            releases.append(current)
        elif current is not None and line.strip():
            if not line.startswith('- ') or not line[2:].strip():
                raise ValueError('Each release change must be one plain bullet')
            current['changes'].append(line[2:].strip())
    if not unreleased or not releases or any(not r['changes'] for r in releases):
        raise ValueError('Changelog needs Unreleased and nonempty versioned release notes')
    return releases


def release_notes(root=ROOT):
    releases = changelog((root / 'CHANGELOG.md').read_text())
    if releases[0]['version'] != pack_version(root):
        raise ValueError('Newest changelog version differs from gradle.properties')
    return releases


def sha256(path):
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def verify_archive(path, version):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(set(names)) != len(names) or any('..' in Path(n).parts or n.startswith('/') for n in names):
            raise ValueError('Unsafe or duplicate archive paths')
        forbidden = {'accounts.json', 'level.dat', 'session.lock', 'servers.dat', '.env', 'power-beta-validation.command'}
        for name in names:
            parts = Path(name).parts
            if any(p in forbidden or p in {'logs', 'saves', 'reports', 'backups'} for p in parts):
                raise ValueError('Private/runtime files in export: ' + name)
            if name.endswith(('.disabled', '.log')) or 'validation' in name.lower():
                raise ValueError('Development artifact in export: ' + name)
        manifest = json.loads(archive.read('Power Beta/PACK-MANIFEST.json'))
        if manifest['version'] != version:
            raise ValueError('Archive version differs')
        mods = {n for n in names if n.startswith('Power Beta/.minecraft/mods/')}
        expected = {'Power Beta/.minecraft/mods/' + row['file'] for row in manifest['mods']}
        if len(mods) != 7 or mods != expected or not all(row['enabled'] for row in manifest['mods']):
            raise ValueError('Export must contain exactly seven enabled JARs')
        for row in manifest['mods']:
            if Path(row['file']).name != row['file'] or not row['file'].endswith('.jar'):
                raise ValueError('Invalid artifact filename')
            data = archive.read('Power Beta/.minecraft/mods/' + row['file'])
            if hashlib.sha256(data).hexdigest() != row['sha256']:
                raise ValueError('Artifact hash differs: ' + row['file'])
        import io
        with zipfile.ZipFile(io.BytesIO(archive.read(f'Power Beta/.minecraft/mods/power-beta-{version}.jar'))) as jar:
            metadata = json.loads(jar.read('fabric.mod.json'))
            if metadata['id'] != 'power_beta' or metadata['version'] != version:
                raise ValueError('Power Beta JAR metadata differs')
    return manifest
