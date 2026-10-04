# Power Beta

A configurable Minecraft Beta 1.7.3 pack for Babric, built around a shared Options
menu. The base profile keeps Beta gameplay, with fixes, performance improvements,
modern inventory controls and the creative interface available by default.
Optional building, movement, recipe and world-editing changes start disabled.

All gameplay and utility modules compile from source in this repository. The
three custom components and 17 enabled utility modules are embedded in one
Power Beta JAR. The optional recipe browser also compiles here and ships disabled.
Mod Menu is removed. Platform, authentication and performance libraries remain
pinned binary dependencies; see [the upstream inventory](docs/upstreams.md).

## Options

Open **Options** from the title or pause menu. Pages group settings by function.
Each pane scrolls independently. Reopening Options remembers the last page and scroll
position during the game session. Search finds settings across every page.
Left-click cycles forward; right-click cycles backward. The reset button restores one setting. Bindings use a circular-arrow icon.
Numeric values use sliders; `...` opens the exact-value editor. Apply saves while
keeping the current page, search and scroll. Done saves and exits. The unsaved
changes counter filters the list to changed settings. Cancel discards the draft,
including live previews. A `*` marks settings that need a restart.

All Video settings, GUI scale, audio, mouse sensitivity, field of view and camera distance
preview immediately. Auto scale chooses the largest integer scale that leaves
at least 320 by 240 GUI pixels; a smaller window can limit the requested scale.
The menu is centered and capped at a 16:9 aspect ratio. Hover any setting for an
explanation. A binding's `!` lists conflicts; clicking it opens a fixed list that
stays visible while you resolve them. Gear icons jump to related settings.
Disabled-feature bindings stay hidden until Show Disabled is selected.

Key bindings accept keyboard and mouse buttons with Ctrl, Shift and Alt, including
either left or right modifier. Actions fire on key-down. A press retains its
original combination until released; removing Ctrl cannot trigger the plain key.
More specific combinations take priority, and shared base keys are marked.
Escape cancels capture; Delete clears the binding. Plain modifier keys are also
supported. Only the binding editor waits for release to identify a modifier alone.

All settings, native key bindings and saved camera positions live in `config/power-beta.json`.
Settings saves keep recovery copies under `power-beta-data/settings-backups`. If a save
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
- `modules/`: all owned creative, building, editing and utility source, assets and tests.
- `input-api/`: shared modifier matching, press ownership and mouse input routing.
- `source-build/`: pinned mappings and sequential builds for imported utilities.
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
profile. Owned modules use Power Beta IDs and packages. The configuration API keeps its
platform API identity for compatibility with retained performance modules.

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
preferences, without accounts or worlds. New features keep their pack defaults, except slash-to-chat and the assigned auto-walk
action are enabled in Configured.
The latest explicit two-view camera cycle overrides an older front-view preference.
Use `--java /path/to/java` to pin the launcher runtime when needed.

## Container tools

Inventory includes **Move containers**, off by default. Hold Sneak with an empty
hand and use a chest, dispenser or furnace to pick it up. Release Use, then use a
block face to place it. Double chests move both halves and all 54 slots together;
their placement axis follows your facing direction. Both spaces must be empty and
clear of entities. A carried single chest can join one existing single chest.
Placement beside a double chest or between two singles remains blocked. Spectator, freecam, riding and active edits cannot
move containers. A centered container model and raised-arm carry pose show what
you are holding. Pickup and placement preserve full block-entity NBT, including
item identity, count, damage and furnace progress.

Transfers write and flush recovery records before changing the world. Held
containers survive world reloads. A failure blocks further movement and preserves
the record. Successful transfers retain an archive in the world's `data` folder.
Keep these records when restoring a damaged save; do not manually delete a held
record. No software can guarantee recovery after disk or backup corruption.

**Container contents preview**, also off by default, has a configurable held key.
It reads the targeted inventory without opening it or moving items. Double-chest
halves use their normal inventory order; storage minecarts are supported.

## Updating existing profiles

`tools/update_profiles.py INSTANCE [--configured]` refuses a running instance,
backs up its configs and mod artifacts, imports legacy preferences into the one
config, and installs the built pack. It leaves worlds and resource packs alone.
Old configuration filenames and world keys exist only in migration records and
upstream provenance. Recovery records and audit reports are under `power-beta-data`,
separate from live configuration.
