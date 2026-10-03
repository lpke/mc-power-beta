# Power Beta

A configurable Minecraft Beta 1.7.3 pack for Babric, built around a shared Options
menu. The base profile keeps Beta gameplay, with fixes, performance improvements,
modern inventory controls and the creative interface available by default.
Optional building, movement, recipe and world-editing changes start disabled.

All gameplay and utility modules compile from source in this repository. The
three custom components and 16 enabled utility modules are embedded in one
Power Beta JAR. The optional recipe browser also compiles here and ships disabled.
Mod Menu is removed. Platform, authentication and performance libraries remain
pinned binary dependencies; see [the upstream inventory](docs/upstreams.md).

## Options

Open **Options** from the title or pause menu. Pages group settings by function.
Each pane scrolls independently. Search finds settings across every page.
Left-click cycles forward; right-click cycles backward. `R` resets one setting.
Numeric values use sliders; `...` opens the exact-value editor. Apply saves while
keeping the current page, search and scroll. Done saves and exits. The unsaved
changes counter filters the list to changed settings. Cancel discards the draft,
including live previews. A `*` marks settings that need a restart.

Scale, audio, mouse sensitivity, field of view, camera distance and textures
preview immediately. Auto scale chooses the largest integer scale that leaves
at least 320 by 240 GUI pixels; a smaller window can limit the requested scale.
The menu is centered and capped at a 16:9 aspect ratio. Hover any setting for an
explanation; hover a binding's `!` for the other actions sharing that key.

Key bindings accept keyboard and mouse buttons with Ctrl, Shift and Alt, including
either left or right modifier. Actions fire on key-down. A press retains its
original combination until released; removing Ctrl cannot trigger the plain key.
More specific combinations take priority, and shared base keys are marked.
Escape cancels capture; Delete clears the binding. Plain modifier keys are also
supported. Only the binding editor waits for release to identify a modifier alone.

Settings saves keep recovery copies under `config/power-beta/backups`. If a save
fails, previous files and live values are restored. An interrupted transaction
is recovered before configuration and recipe initialization on the next launch.

## Audio

Audio combines master, category and individual sound volumes with the existing
ambient-volume and music-timing settings. Custom music supports OGG, WAV and MUS.
MP3 is not supported by Beta's sound engine. Dimension and biome tags such as
`theme-nether-specific.ogg` keep their existing behavior.

Choose music folders in the in-game folder browser, then select **Add custom
tracks** or **Replace soundtrack**. Menu music has its own folder list. Scans are
read-only, run off the game thread, skip malformed headers and symlinks, and have
file/depth limits. Empty or unavailable replacement libraries fall back to the
built-in soundtrack. Changes do not move or alter music files.

## Repository layout

- `src/`: shared menu, configuration adapters, audio integration and tests.
- `components/`: LpkeTweaks, LpkeCreative and WorldEdit Beta source and tests.
- `input-api/`: shared modifier matching, press ownership and mouse input routing.
- `source-build/`: pinned mappings and sequential builds for imported utilities.
- `vendor/sources/`: editable utility source, assets and upstream provenance.
- `vendor/jars/`, `vendor/libraries/`: retained platform/performance dependencies.
- `tools/`: reproducible catalog, integration patch and packaging tools.
- `validation-mod/`: disposable-instance runtime checks; never shipped.
- `docs/`: coverage, design decisions and validation evidence.

The Prism profile requires Java 21. Run `python3 tools/build_pack.py` with
`JAVA_HOME` pointing to Java 21. It builds components sequentially and assembles an importable Prism instance without accounts, worlds,
logs, private configuration or validation fixtures.

See [NOTICE](NOTICE) for attribution and [docs/upstreams.md](docs/upstreams.md) for update sources.
The standalone mod repositories and `separate mods final` instance remain frozen.

The previously disabled recipe browser, shader module and LAN hosting module are
retained as disabled optional artifacts. They are not part of the tested enabled
profile. Existing modules retain their IDs and licenses for compatibility.

## Textures and profiles

Video includes Alpha, 1.14 and Faithful 32 alongside Default. Soft rain, soft snow,
old cobblestone and old bricks are independent overrides on the selected pack.
Both base-pack selection and overrides preview immediately and support Discard.
Camera includes the perspective cycle and third-person distance; vanilla distance
is four blocks and nearby walls still move the camera closer.

`tools/create_profiles.py --output INSTANCE_DIRECTORY --reference MINECRAFT_DIRECTORY`
creates `Power Beta - Defaults` and `Power Beta - Configured` from `dist/Power Beta`.
It refuses existing destinations. Defaults changes only GUI scale to 4 and mouse
sensitivity to 80. Configured migrates supported options and legacy feature/key
preferences, without accounts or worlds. New features keep their pack defaults.
The latest explicit two-view camera cycle overrides an older front-view preference.
Use `--java /path/to/java` to pin the launcher runtime when needed.
