"""Resolve editable source textures to their unchanged runtime paths and JAR owners."""
import json
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / 'textures'
MANIFEST = TEXTURES / 'manifest.json'


def relative_path(value):
    path = PurePosixPath(value)
    if not value or path.is_absolute() or '\\' in value or any(part in ('', '.', '..') for part in value.split('/')):
        raise ValueError('Invalid texture mapping path: ' + value)
    return path


def mapped_textures(features=()):
    """Return (runtime path, source file, source project) for each mapped PNG."""
    selected = [relative_path(feature) for feature in features]
    entries = []
    covered = set()
    destinations = set()
    for group in json.loads(MANIFEST.read_text())['groups']:
        source = TEXTURES / relative_path(group['source'])
        target = relative_path(group['target'])
        project = group['project']
        if project != '.':
            relative_path(project)
        if not (ROOT / project).is_dir():
            raise ValueError('Unknown texture project: ' + project)
        if not source.exists():
            raise ValueError('Missing texture source: ' + group['source'])
        files = [source] if source.is_file() else sorted(source.rglob('*.png'))
        for path in files:
            if not path.resolve().is_relative_to(TEXTURES.resolve()) or path.suffix.lower() != '.png':
                raise ValueError('Invalid texture source: ' + str(path))
            runtime = target if source.is_file() else target / path.relative_to(source).as_posix()
            key = (project, runtime.as_posix())
            if path in covered or key in destinations:
                raise ValueError('Duplicate texture mapping: ' + str(path))
            covered.add(path)
            destinations.add(key)
            location = PurePosixPath(path.relative_to(TEXTURES).as_posix())
            if not selected or any(location.is_relative_to(feature) for feature in selected):
                entries.append((runtime.as_posix(), path, project))
    unmapped = set(TEXTURES.rglob('*.png')) - covered
    if unmapped:
        raise ValueError('Unmapped source textures: ' + ', '.join(str(p.relative_to(TEXTURES)) for p in sorted(unmapped)))
    if not entries:
        raise ValueError('No source textures match the selected features')
    return entries
