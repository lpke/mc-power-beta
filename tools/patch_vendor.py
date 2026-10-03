#!/usr/bin/env python3
"""Apply reviewed mixin changes to hash-pinned artifacts without altering their classes."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def patch_one(mod_id, mixin_file, removed):
    manifest = json.loads((ROOT / "vendor/manifest.json").read_text())
    expected = next(entry for entry in manifest if entry["id"] == mod_id)
    source = ROOT / expected["file"]
    destination = ROOT / "build/patched" / (source.stem + "-powerbeta.jar")
    destination.parent.mkdir(parents=True, exist_ok=True)
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    if digest != expected["sha256"]:
        raise ValueError(f"Artifact changed; review the patch: {source.name}")

    patched = False
    with zipfile.ZipFile(source) as upstream, zipfile.ZipFile(destination, "w") as output:
        for entry in upstream.infolist():
            data = upstream.read(entry.filename)
            if entry.filename == mixin_file:
                config = json.loads(data)
                found = set()
                for section in ["client", "mixins"]:
                    found.update(set(config.get(section, [])) & removed)
                    config[section] = [name for name in config.get(section, []) if name not in removed]
                if found != removed:
                    raise ValueError(f"Audio mixin layout changed: {mod_id}")
                data = json.dumps(config, indent=2).encode()
                patched = True
            elif entry.filename == "fabric.mod.json":
                metadata = json.loads(data)
                metadata["version"] += "+powerbeta.1"
                data = json.dumps(metadata, indent=2).encode()
            fixed = zipfile.ZipInfo(entry.filename, (1980, 1, 1, 0, 0, 0))
            fixed.compress_type = zipfile.ZIP_DEFLATED
            output.writestr(fixed, data)
    if not patched:
        raise ValueError(f"Missing mixin configuration: {mod_id}")
    print(destination)


def patch():
    patch_one("gcapi3", "gcapi3.mixins.json",
              {"client.ModMenuMixin", "client.ModMenuBabricMixin"})


if __name__ == "__main__":
    patch()
