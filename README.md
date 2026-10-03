# Power Beta

A configurable Minecraft Beta 1.7.3 pack for Babric, built around a shared Options
menu. The base profile keeps Beta gameplay, with fixes, performance improvements,
modern inventory controls and the creative interface available by default.
Optional building, movement, recipe and world-editing changes start disabled.

This repository contains the integration mod, all three custom mod components,
hash-pinned third-party artifacts, upstream source snapshots and exact decompiled
references where matching source was unavailable. Mod Menu is removed.

## Options

Open **Options** from the title or pause menu. Pages group settings by function.
Each pane scrolls independently. Search finds settings across every page.
Left-click cycles forward; right-click cycles backward. `R` resets one setting.
Numeric values also have an exact-value editor. Changes stay in a draft until
saved. Cancel discards them. A `*` marks settings that need a restart.

Key bindings accept keyboard and mouse buttons. Escape cancels capture; Delete
clears the binding. Shared bindings are marked so conflicts are visible.

Settings saves keep recovery copies under `config/power-beta/backups`. If a save
fails, previous files and live values are restored. An interrupted transaction
is recovered before configuration and recipe initialization on the next launch.

## Audio

Audio combines master, category and individual sound volumes with the existing
ambient-volume and music-timing settings. Custom music supports OGG, WAV and MUS.
MP3 is not supported by Beta's sound engine.

Choose music folders in the in-game folder browser, then select **Add custom
tracks** or **Replace soundtrack**. Menu music has its own folder list. Scans are
read-only, run off the game thread, skip malformed headers and symlinks, and have
file/depth limits. Empty or unavailable replacement libraries fall back to the
built-in soundtrack. Changes do not move or alter music files.

## Repository layout

- `src/`: shared menu, configuration adapters, audio integration and tests.
- `components/`: LpkeTweaks, LpkeCreative and WorldEdit Beta source and tests.
- `vendor/`: pinned third-party artifacts, provenance and source references.
- `tools/`: reproducible catalog, integration patch and packaging tools.
- `validation-mod/`: disposable-instance runtime checks; never shipped.
- `docs/`: coverage, design decisions and validation evidence.

Requires Java 17 or newer; development and testing use Java 21. Build components
sequentially with their Gradle wrappers, then build the root integration. The
packaging tool assembles an importable Prism instance without accounts, worlds,
logs, private configuration or validation fixtures.

See [NOTICE](NOTICE) for component licenses and the BTA layout reference.
