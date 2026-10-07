"""Shared file operations for idle Prism instances."""
from pathlib import Path
import os, shutil, tempfile

ROOT = Path(__file__).resolve().parents[1]

def assert_idle(instance):
    game = (instance / '.minecraft').resolve()
    for process in Path('/proc').iterdir():
        try:
            if process.name.isdigit() and (process / 'comm').read_text().strip() == 'java' and (process / 'cwd').resolve() == game:
                raise RuntimeError(f'Instance is running: {instance.name}')
        except (FileNotFoundError, PermissionError, ProcessLookupError):
            continue


def atomic_copy(source, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.is_symlink():
        raise RuntimeError(f'Refusing symbolic link: {target}')
    with tempfile.NamedTemporaryFile(dir=target.parent, prefix='.power-beta-', delete=False) as temp:
        temporary = Path(temp.name)
        with source.open('rb') as data:
            shutil.copyfileobj(data, temp)
        temp.flush()
        os.fsync(temp.fileno())
    try:
        os.replace(temporary, target)
    finally:
        temporary.unlink(missing_ok=True)


