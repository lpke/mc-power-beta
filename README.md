# <img src="docs/logo.svg" width="40" height="40" alt=""> Power Beta

A configurable modpack for Minecraft Beta 1.7.3 with bug fixes, quality-of-life
improvements and optional tweaks in one searchable Options menu.

[Website and download](https://mc-power-beta.vercel.app/)

| Unified options menu | Creative mode |
| --- | --- |
| [<img src="docs/screenshots/unified-menu.png" width="480" alt="Searchable options with settings grouped by category">](docs/screenshots/unified-menu.png) | [<img src="docs/screenshots/creative-inventory.png" width="480" alt="Creative catalog above a snowy mountain valley">](docs/screenshots/creative-inventory.png) |

| WorldEdit | Isometric screenshots |
| --- | --- |
| [<img src="docs/screenshots/worldedit.png" width="480" alt="WorldEdit selection around a castle">](docs/screenshots/worldedit.png) | [<img src="docs/screenshots/isometric.png" width="480" alt="Isometric view of a lake and forest">](docs/screenshots/isometric.png) |

| Death recovery | Music library |
| --- | --- |
| [<img src="docs/screenshots/death-inventory.png" width="480" alt="Saved death inventory with recovery controls">](docs/screenshots/death-inventory.png) | [<img src="docs/screenshots/music-library.png" width="480" alt="Music library with track previews and volume controls">](docs/screenshots/music-library.png) |

## Features

- One searchable Options menu with categories, linked settings, tooltips, live previews, reset controls, automatic saving and optional Apply/Cancel editing.
- Configurable keyboard and mouse bindings, Ctrl/Shift/Alt chords, conflict detection and a shared view for resolving overlapping bindings.
- A persistent world map and HUD minimap with surface and cave layers, terrain shading, entity radar, waypoints, deathpoints, chunk and slime overlays, recent paths and screenshots.
- Optional modern sprinting on foot, enabled per world, with global stamina and combat limits, sprint jumping and surface particles. Separate optional sprint FOV effects for ground sprinting and each flight mode. Power Beta enables sprinting with stamina and combat limits; Beta and Refined Beta leave it off.
- Creative and spectator modes, a mode picker, configurable reach, flight speed, drift and sprint, and adjustable spectator speed.
- Creative item categories and a survival inventory view. Shift-click for full stacks, middle-drag to fill slots, and use a destroy slot or clear the inventory.
- WorldEdit selections, outlines, fills, replacement, copying, pasting, shapes, undo and redo, with configurable work budgets and history.
- Commands with completion, editable history and help, including teleportation, saved waypoints, world spawn, time, weather, spawning and item commands.
- Chat selection, copy/paste, cursor navigation, scrolling and fade controls. Keep your own messages across sessions with a separate history limit from other chat.
- Death records with locations, causes and original inventory layouts. View lost items and recover them when the world allows it, including crafting inputs. Independent keep-inventory rule.
- Free camera with adjustable speed, drag, collisions and separate player movement; free look, cinematic camera, field of view, adjustable third-person distance and front view.
- Isometric captures with rotation, scale and mirroring, custom-resolution screenshots, clipboard copying and photo mode.
- Fully lit rendering with a full-bright hotkey and optional automatic spectator lighting. Mob spawning and simulated light remain unchanged.
- Entity visibility controls for items, mobs, players, vehicles, projectiles and TNT. Entity and block-entity culling skips hidden rendering work.
- Dropped-item merging and global TNT work limits available before loading a struggling world. Separate world rules control TNT and explosion damage to dropped items.
- Music libraries, custom tracks and folders, era selections, favorites, inclusion filters, per-track and per-folder volume, queues and saved presets.
- Music previews, seeking and playback controls in Options and the pause menu, with separate sound categories, ambience controls and optional extra interaction sounds.
- Flexible block placement with offset, adjacent, rotation, reverse and into-face modes, target overlays and destination previews.
- Fast placement with placement restrictions, item filters, orientation retention and slab rules. Complete slabs through adjacent faces.
- Container previews and carrying that preserves contents, including double chests. Optional inventory access in Nether portals.
- Quick inventory transfers, bulk crafting, drag distribution, double-click collection, held-stack Shift-double-click transfers, scroll transfers, number-key hotbar swaps and stack dropping.
- Direct tool and armor swapping, Shift-click and right-click equipping, hotbar row swapping with previews, and optional use of the four crafting inputs as storage.
- Optional offhand inventory and native item use, automatic tool selection, hotbar restocking, inventory sorting and transfers matching chest contents. Save and load nine Creative hotbars.
- Searchable crafting and smelting recipe book with categories, craftable filtering and ingredient placement. Choose inventory access or a pause-menu viewer; the recipe book starts off.
- Smooth held-item lighting that changes rendering only, and directional sound subtitles with placement controls in the HUD editor.
- Verified saved-world backups, duplication with independent world ownership, and recreation from the original seed and rules.
- Movement while compatible inventories are open, auto-walk, auto-mine, continuous click mining, step assist, edge protection and fence jumping.
- Faster boats and minecarts, boat steering and collision protection, boat elevators, minecart boosters and optional faster obsidian mining.
- Optional water landings and bucket clutches, including flowing water. Empty buckets stack to 16; doors and signs have optional modern stack limits.
- Individual modern recipes, shapeless recipes, tool repair, wool redyeing, adjusted slab/stair/trapdoor/ladder yields, material recovery, soul sand and otherwise unobtainable blocks.
- Per-world difficulty and searchable game rules, editable in Options or with /gamerule. Separate permissions cover game modes, editing, items, travel and recovery. Time freezing preserves the simulation clock.
- Editable Survival and Cheat defaults for new worlds, plus a Custom creation preset with its own searchable rule editor and starting-mode choice. Existing worlds keep their own rules.
- World rules for monsters, animals, dungeon spawners and sleep ambushes. Global spawn limits, light thresholds, group sizes and despawning preferences, including saddled-pig preservation.
- Sleep controls for night skipping or acceleration, a per-world sleeping percentage, wake-on-click and respawn-only beds; configurable rain, thunder, snow height and weather timing.
- Fast leaf decay, creative sapling drops, persistent placed leaves, leaf apple drops, dead-bush sticks, shears harvesting, log rotation and farmland protection.
- Sign editing and dye coloring, unsupported and glued trapdoors, blocked-chest opening, stackable chests, fence connections and pressure plates on fences.
- Optional gold-tool silk touch, hoe fortune, sword looting, fishing luck, gold-armor abilities, fire-curing golden apples and water-absorbing sponges.
- Independent creeper and ghast block-damage rules, global explosion drop chance, TNT ignition and defusing, snowballs that extinguish fire, fire spread between entities and flaming arrows.
- Optional equipment durability and defense changes, mob drops, sheep punching for wool, grass hoeing for seeds, squid milking and food-waste prevention.
- Older block textures, softer rain and snow, low fire, redstone power numbers, leaf rendering, particle controls, camera shake and fog options.
- Light-level overlays, configurable active-tweak indicators and toggle messages, hotbar item names, HUD positioning, crosshair and vignette controls.
- An interactive HUD editor with dragging, anchors, snapping and undo. Arrange the minimap, native HUD elements, four custom info clusters, active tweaks, version labels and F3 information with texture-pack previews.
- Expanded F3 information for coordinates, facing, biome, day, play time, slime chunks, light, music and culling, with shared text color and layout.
- Paint custom block lettering for the title screen, preview it in 3D and save presets. Choose panorama backgrounds for the title screen or all main-menu screens. Achievement display options include recovery controls for missing achievements.
- Configurable autosaving, focus-loss pausing, raw mouse input and network options. Optional tall-grass and dead-bush generation controls for new terrain.
- Fixes for rendering, lighting, item and block drops, mining, boats, stairs, fences, liquids, mob behavior, sleeping, multiplayer jitter and legacy client behavior.

The Power Beta defaults start with fixes, performance improvements and common inventory conveniences on.
Survival worlds start with game-changing command permissions off. New-world
presets and individual world rules let you choose the permissions you want.

## Settings and worlds

On first launch, choose Power Beta, Refined Beta 1.7.3 or Beta 1.7.3 defaults.
Power Beta enables ore vein mining, faster vehicles, clearer water and a blurred
Beta 1.8 panorama, with standard controls and modern flight response. All three
start at GUI scale 4.
Options > Defaults lets you load another setup or choose Use as base. Changing the
base keeps your preferences and changes what each reset button restores. Unedited
new-world rules follow that base automatically; customized rules keep their review
and existing worlds stay unchanged. Loading
resets preferences and new-world templates; existing worlds and saved presets stay intact.

Options changes global preferences. The Game rules tab changes the open singleplayer
world; from the main menu it edits Survival defaults and Cheat defaults for future
worlds. Custom world creation lets you adjust those rules before starting. There is
no shared Cheats switch or layer of world overrides. Turning a permission off hides
its tools and controls while keeping saved items, waypoints and history.

Use `/gamerule <rule> [value]` to read or change a world rule. Both `fire_spread`
and `doFireTick`, for example, refer to the same rule. `/gamerule help` lists the
rules; Options includes descriptions and reset controls. Multiplayer rules belong
to the server. `/fly` toggles flight without changing game mode. Outside Creative,
it requires `allow_flight`; Spectator keeps its own flight.

Settings save automatically by default. Right-click Apply to use manual editing;
leaving with unsaved edits offers Save, Discard or Keep editing. World-creation
edits stay in the draft until you create the world.

Version 4.0.0 uses configuration format 7 and supports formats 3, 4, 5 and 6, converting older formats
with a verified backup. World rules use format 4 and support formats 1, 2, 3 and
Power Beta's earlier world settings. Existing worlds retain their rules and item
protection. Old footwear-specific crop protection needs an explicit trampling choice. Conflicting old permissions or unsupported formats stop conversion
and show recovery guidance. Keep a full instance backup before upgrading; world
folders alone do not include global preferences, music libraries or recovery history.

The recipe book and automatic restocking start off in every defaults base. The other
4.0.0 additions start on in Power Beta and off in Beta and Refined Beta. Set their
unbound shortcuts in Controls. Disabling offhand retains its contents for removal;
saved Creative hotbars are user data and survive loading defaults.

The recipe book reads the installed Beta crafting and smelting registries, including
enabled custom recipes. Beta recipes are available without an advancement unlock
system. Recipes from extensions with custom matching logic need a display provider
before the book can safely place their ingredients.

Saved-world backups appear in `backups/power-beta`. They include the save and known
world-owned map and recovery files, with a verified manifest. Duplication copies
terrain, players and maps under a new identity; recovery claims stay with the original.
Recreate opens a fresh creation draft with the seed, difficulty and rules, without
copying terrain or player inventories. World operations require an unloaded world.

## Maps and sprinting

Press **M** for the world map, **B** to create a waypoint and **U** to manage waypoints.
Drag the map to pan, scroll to zoom around the pointer, press **Home** to center,
and right-click for waypoint, destination, coordinate and teleport actions. **G** opens
coordinate navigation. Hold **Z** to enlarge the minimap; **I** and **O** change its zoom.
All gameplay bindings can be changed in Controls. `/waypoint` also adds, removes, lists
and teleports to saved waypoints; `/way` and `/wp` are aliases. `/waypoint dest [x y z]`
sets a destination, using the current camera position when coordinates are omitted.
Teleporting uses the
same world permission as the menus.

Options > Map contains map switches, caves, waypoints and performance settings.
Terrain updates per second controls sampling work; minimap movement, rotation and
world-map panning follow every rendered frame. World-map detail follows physical
window pixels, with separate automatic-detail and smoothing controls.
On wide windows, Settings in the world map opens a sidebar beside the live map and
minimap. Search covers map controls and minimap appearance; related-control jumps
keep a Back action inside the sidebar. Smaller windows use the full Options screen.
Its appearance button opens the minimap section of the HUD editor for position,
size, zoom, colors, radar, overlays and information. Shared appearance controls also
affect the world map. These edits use the same Done, Undo and
Cancel controls as the rest of the HUD. Power Beta defaults show the minimap;
Refined Beta and Beta defaults leave it hidden. Turning both maps off stops terrain,
radar and path work after pending terrain saves finish. Independently enabled world
waypoints and deathpoints retain their storage.

Explored terrain and waypoints are stored in `power-beta-data/maps`, separately
from preferences and inventory recovery. Map sampling reads loaded terrain only.
Terrain format 2 reads format 1 and retains a verified `.v1.bak` before replacing
an older tile. Previously explored areas gain the new distant relief as their
detailed tiles are read again.
Screenshot saves the visible map at window resolution in the instance's
`screenshots` folder. In multiplayer, name each server map in the map screen when
the server has multiple worlds. Teleportation requires a local singleplayer world's
teleport permission and the player's view. Maps do not grant access permissions.

Options > Game rules > Allow sprinting enables ground sprinting for the current
singleplayer world. `/gamerule allow_sprinting true` changes the same rule. Both
creation presets and all defaults bases start it off; existing worlds without the
rule also start it off. Options > Gameplay > Sprinting keeps its global tuning. The default
activation uses Ctrl or a forward double tap, with modern acceleration, jump boost,
dust and sprint-hit knockback. Sprint stamina and combat contains optional stamina,
recharge delays and combat limits that can balance it without adding a hunger system.
These preferences apply across worlds that allow sprinting. Ground sprinting stays disabled in
multiplayer. Ground, Creative and Spectator sprint share dynamic FOV and its amount in
Gameplay > Sprinting; freecam has its own FOV preference. All start off. Creative and
flight never consume stamina. Depleted Survival stamina appears as ten gold lightning
icons beside the hotbar. Their pale notch marks the recovery threshold after exhaustion.
Stationary and moving stamina recovery have separate rates.

With Modern water movement and Allow sprinting enabled together, sprint underwater
to swim using a low pose, look-directed dives and modern speed and strokes. The low
collision box fits one-block passages. Leaving swimming keeps the player crouched or
low until there is room to stand. Creative and flight still bypass stamina.

## Install

1. Install [Prism Launcher](https://prismlauncher.org/) and add your Minecraft account.
2. [Download the Prism ZIP](https://mc-power-beta.vercel.app/download). If you have the source code
   instead, build that ZIP using the steps below.
3. In Prism, choose **Add Instance > Import**, then select the ZIP.
4. Open the instance's **Edit > Settings > Java**, enable the Java installation
   override and select **Java 21**. Download Java through Prism if needed.
5. Launch the instance. Open **Options** to browse or search settings and hover
   them for help. Auto apply is on by default. Turn it off to use **Apply** and **Cancel** for grouped edits.

The pack includes all required mods. Import the whole ZIP into a new instance.
Back up your worlds before replacing an existing installation.

## Build the ZIP from source

1. Install Java 21 and Python 3.
2. Download this repository with **Code > Download ZIP** and extract it, or clone it.
3. Open a terminal in the extracted folder. Set `JAVA_HOME` to your Java 21
   installation, then run:

   ```sh
   python3 tools/build_pack.py
   ```

4. Import the resulting `dist/Power-Beta-<version>-Prism.zip` into Prism as described above.

Development and update instructions are in [AGENTS.md](AGENTS.md). See
[releases](docs/releases.md) for publishing and [recovery](docs/recovery.md)
for inventory recovery. [Chat and WorldEdit](docs/commands.md) covers editing,
completion and block states. Changes are listed in the [changelog](CHANGELOG.md)
and [GitHub releases](https://github.com/lpke/mc-power-beta/releases).

Power Beta is maintained by its owner and does not accept contributions.
You are welcome to fork it and adapt it to your needs under the AGPL license.

## Inspired by

[BHCreative](https://github.com/paulevsGitch/BHCreative),
[Tweakeroo](https://github.com/maruohon/tweakeroo),
[WorldEdit](https://github.com/EngineHub/WorldEdit),
[Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap),
[Xaero's World Map](https://modrinth.com/mod/xaeros-world-map),
[Better Than Adventure](https://github.com/Better-than-Adventure),
[OmniLook](https://github.com/rhysdh540/Omnilook),
[UniTweaks](https://github.com/danygames2014/UniTweaks),
[StationAPI utilities](https://github.com/telvarost),
[Light Overlay](https://github.com/shedaniel/LightOverlay),
[Click Mining Forever](https://github.com/snowtyler/click-mining-forever) and
[VeinMiner](https://github.com/MiraculixxT/Veinminer).

[AGPL-3.0-only](LICENSE). This is not an official Minecraft product.
