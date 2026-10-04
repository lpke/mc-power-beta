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
Each pane scrolls independently; its scrollbar supports dragging. Clicking the active page scrolls to the top. Reopening Options remembers the last page and scroll
position during the game session. Search finds settings across every page. Click a
search section heading to open that section; Back restores the results.
Left-click cycles forward; right-click cycles backward. The reset button restores one setting. Bindings use the same R reset button.
Numeric values use sliders; `...` opens the exact-value editor. Apply saves while
keeping the current page, search and scroll. Done saves and exits. The unsaved
changes counter filters the list to changed settings. Cancel discards the draft,
including live previews. A `*` marks settings that need a restart.

Fog key distances opens an ordered list of labelled sliders. Add, remove or move
distances, and use `...` for exact values. Done accepts the list; Cancel discards
it. Use 1 to 16 different distances between 2 and 32 chunks.

All Video settings, GUI scale, audio, mouse sensitivity, field of view and camera distance
preview immediately. Auto scale chooses the largest integer scale that leaves
at least 320 by 240 GUI pixels; a smaller window can limit the requested scale.
The menu is centered and capped at a 16:9 aspect ratio. Hover any setting for an
explanation. A binding's `!` lists conflicts; clicking it opens a fixed list that
stays visible while you resolve them. Gear icons jump to related settings.
Controls hide only when their feature is disabled. Live toggles such as Fast Place
remain visible while off. Related settings and controls open filtered lists, with
Back restoring the previous view.

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

## Command access

**General > World > Cheats enabled** is the per-world master switch. New worlds
start in survival with cheats off. The creation screen lets you choose the initial
flag. Only worlds with cheats on show `[Cheats]` in the world list.

Cheats unlock mode switching, item spawning, teleportation, healing/invulnerability,
world-changing commands and WorldEdit. There are no creative-only restrictions:
these features work in survival when cheats are enabled. Freecam, placement tools,
container carrying and other QoL features remain independent. Shared freecam/flight
sprint preferences also remain editable.

**Commands** separates cheat and non-cheat commands. Each has an Enabled/Disabled
global rule and a Use global/Allow/Block world override. Creative and spectator
entry have their own global and per-world controls, shared by `/gamemode`, `/gm`
and the F3+F4 picker. Returning to survival is always allowed. The cheat-command
switch and WorldEdit's global switch can further restrict access, but neither a
command nor a world override can bypass Cheats enabled.

Locked settings stay visible with a tooltip explaining the lock. Apply or Done
commits the world switch; auto-apply also supports it. Disabling cheats returns
the player to survival. An unsafe spectator exit or carried container prevents
that transition and leaves cheats on. Access changes preserve inventories, saved
warps, editor history and per-world rules; god-mode protection is suspended when
blocked. WorldEdit cancels and rolls back pending edits after access is removed.

The cheats flag lives in world metadata. Existing worlds without the flag inherit
cheats on only if saved in creative or spectator. Existing creative-only command
rules migrate to Enabled. The editor's old default-off preference becomes enabled
behind the new world gate once; later global edits are retained. Per-world command
rules live in the shared configuration under the world's persistent ID. Renaming
a world keeps its rules and dimensions share them.

Singleplayer commands support modern target-first `/give`, inventory `/clear`,
`/tp` and `/teleport` with player names, selectors, relative `~` and local `^`
coordinates, rotation and facing. Added `/weather`, `/seed`, `/difficulty`,
`/spawnpoint` and `/setworldspawn`; updated `/time`, `/kill`, `/summon` and `/ride`.
`/clearchat` clears chat. `/help <command>` documents each supported form.

Selectors cover the local player and loaded entities, with type, name, distance,
box, sorting and limit filters. Modern NBT/component arguments and newer mobs are
not implemented. Beta uses one world clock and a shared difficulty option;
command-set respawn points are limited to the Overworld. Existing saved warps
remain readable, and duplicate names are rejected instead of overwritten.

Help uses coloured command names and headings. With scrolling chat enabled,
`/help`, `//help` and other list commands show their full lists. Explicit page
numbers retain pagination. WorldEdit help lines omit the `[WE]` prefix.


## Light overlay and inventory

Press **F7** to toggle light numbers on nearby block tops. Rebind it in Controls.
**Building → Light overlay** controls the range, size, colour threshold, RGB colours,
light source and spawnable-surface filter. It starts off and does no scans while off.
Block light is the default, with levels below 8 red and levels 8–15 green. Combined
light also includes daylight. These numbers describe lighting, not every mob's
spawn rules. Scans use loaded terrain only and refresh in bounded batches.

**Inventory → Clicking → Swap tools and armour** starts on. In singleplayer,
clicking the same type of damageable equipment with different durability swaps
both original items. Armour and crafting-output restrictions remain in force.

## Audio

The music toolbar and embedded library work while Options stays open. Tracks and
Queue have separate scroll positions. The library remembers its view, folder filter
and search when Options reopens. Cycle the folder filter forward with left-click
or backward with right-click. Queue arrows move individual requests; disabling a
track's rotation never deletes its file or queued requests.

Preview speakers turn yellow while active. Click again to stop the preview;
music previews resume the prior track afterward. Play/Pause reflects playback state.

Audio combines master, category, individual sound and track volumes. Custom music
supports OGG, WAV and MUS, with MP3 conversion available in the library. Dimension and biome tags such as
`theme-nether-specific.ogg` keep their existing behavior.

Choose a World music set: Vanilla (12 original tracks), Alpha and Beta (18 tracks),
or All Minecraft (21 overworld tracks through 1.14). Custom music can be off,
mixed with that set, or used alone. Menu music can use the world soundtrack,
prefer menu folders, or mix both pools. Scans are
read-only, run off the game thread, skip malformed headers and symlinks, and have
file/depth limits. Empty or unavailable replacement libraries fall back to the
built-in soundtrack. Changes do not move or alter music files.

Each folder has an On/Off switch. Off removes it from automatic selection while
keeping its path, track preferences, previews and explicit queue requests intact.
The library starts on Active tracks, following the current world/menu, soundtrack
and enabled-folder settings. Individually excluded tracks stay visible so they
can be re-enabled. All tracks, Custom tracks and folder filters remain available.
Playback controls sit on the left of the toolbar; Queue, Library and Reload sit
on the right. Queue shows its request count when non-empty and is greyed out without a count when empty.
Library opens the track list. Clicking the active Queue or Library button closes
that panel. Back, Escape and the Audio sidebar tab return to settings. Back sits
beside the search field or queue title. Prev and Next have skip icons; narrow
layouts retain compact icons and wrap the two toolbar groups when needed.

Speaker buttons preview individual sounds, built-in music and custom tracks.
Music previews pause the current song; click the same speaker again to resume it.
Previous, Next and Play / pause exit the preview. Track volume changes apply live,
and newly scanned custom tracks appear without reopening Options.

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

Video's Texture pack button opens Minecraft's native picker, including its folder
button for your own ZIP packs. Alpha, 1.14 and Faithful 32 ship as bundled packs
alongside Default. Soft rain, soft snow,
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
block face to place it. Target either half of a double chest to carry just that
half and its 27 slots. The other half stays in place with its own inventory.
A carried chest can join one existing single chest again.
Placement beside a double chest or between two singles remains blocked. Spectator, freecam, riding and active edits cannot
move containers. A centered container model and raised-arm carry pose show what
you are holding. Pickup and placement preserve full block-entity NBT, including
item identity, count, damage and furnace progress.

Transfers write and flush recovery records before changing the world. Held
containers survive world reloads. A failure blocks further movement and preserves
the record. Double chests already being carried by an older version can still be
recovered and placed together. Successful transfers retain an archive in the world's `data` folder.
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

### Music library and MP3

Audio → Music library combines tracks from the world and menu folders. Each track
has a rotation switch and volume control. Speaker previews preserve the current
song; Play starts a track immediately. Explicit queue requests may include excluded
tracks. Queue edits are saved immediately in `config/power-beta.json`, independently
of unsaved settings. Missing files stay listed in the queue for repair or removal.

OGG, WAV and MUS play directly. **Convert MP3** uses `ffmpeg` from PATH to create WAV
files in `power-beta-data/music-cache`. Originals are never changed. FFmpeg is an
optional external executable, not a required mod or Java library. Conversion runs
in the background, up to 256 files per batch, with a three-minute and 1 GiB output
limit per file. Cancel stops only the converter process started by the game.

Music gaps contains Wait before queued tracks, Wait between tracks, and the
minimum/maximum gap in seconds. The default random gap is 600–1200 seconds.
Turning Wait between tracks off bypasses both natural and queue gaps. Manual
Play, Prev and Next start immediately. Quiet ends the current track while leaving
automatic playback enabled. The music volume slider is the single mute control.

The built-in OGG assets are bundled and streamed directly from the JAR. Era and
folder accordions group the library. The music-note button toggles automatic
rotation; a red slash marks excluded tracks. Queue rows share the library volume
and keep all controls on one line. Song names appear in brackets only where they
differ from the game filename. See [music sources](docs/music-assets.md).

### Menu preferences

Right-click Apply to toggle auto-apply. Runtime slider previews remain immediate;
continuous edits save on release. Auto-apply and saved colour swatches persist in
the shared configuration. General → Game and input can make pausing open Options
directly. The Menu button returns to the normal pause menu. Search, page, scroll and
navigation history are remembered while the game remains open.

For an existing Power Beta instance on the same platform versions,
`python3 tools/update_artifacts.py /path/to/instance` installs the built JAR and
documentation with a verified backup. It refuses running instances and verifies
that configuration and world files remain unchanged.
