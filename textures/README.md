# Editable textures

Keep editable game textures here, grouped by feature. Edit these PNGs directly;
the build and preview tool both use `manifest.json` to restore their runtime paths.
Screenshots and test fixtures belong outside this directory.

Each manifest group has:

- `source`: a file or folder relative to this directory.
- `project`: the source project owning the resource, with `.` for the main pack.
- `target`: its existing resource path, relative to that project's resource root.

Folder mappings include all PNGs recursively and preserve their relative names.
File mappings can rename an individual resource, such as a module icon. New files
in an existing mapped folder need no manifest edit. Register a new group when
adding a new feature or resource destination. Keep existing runtime paths stable
so texture packs and saved references continue working.

Legacy reference artwork remains under `legacy/`; its source module's enablement
still determines whether it ships.

See [the preview workflow](../docs/texture-editing.md) to edit and reload textures
without a mod rebuild.
