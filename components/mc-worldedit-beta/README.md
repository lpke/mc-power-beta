# WorldEdit Beta

A singleplayer subset of WorldEdit for Minecraft Beta 1.7.3 on Babric. This is an
independent implementation of familiar WorldEdit commands, not an official
EngineHub release or the complete modern WorldEdit mod.

## Start

Type `//wand` in chat to get a wooden axe. Left-click a block for position 1 and
right-click for position 2. The wand selects without mining or placing.
Use `//set stone`, then `//undo` to restore the selection.

**Mods > WorldEdit Beta > Configure** has left/right pages for access, the wand,
selection outline and edit limits. Choose any registered item as the wand, or use Set wand
to held item. The outline supports colour, opacity, line width and visibility
through blocks. Red and green boxes mark the selected corners. `//drawsel`
toggles the outline; `//toggleeditwand` toggles selection with the held item.

Done saves `config/worldedit-beta.properties`; Cancel/Escape discards edits.

Double-slash commands register with RetroCommands for normal command colouring
and suggestions. Tab cycles completions; Shift+Tab cycles backwards. Block names,
patterns, masks, directions and entity types are supported. Enter follows normal
chat submission so MojangFix retains command history. Without these optional
mods, WorldEdit provides Tab completion and a 100-entry session history itself.

## Access

The Access page defaults to **WorldEdit ON** and **Require creative when available ON**.
The priority is master switch, then this world's override, then creative integration.

- Master OFF disables commands, wand and selection rendering everywhere.
- This world can Inherit, Enable or Disable. Enable bypasses creative integration;
  Disable blocks WorldEdit even in creative. The master switch still wins.
- With Inherit, BHCreative or LpkeCreative installed means creative mode is required.
  Without either mod, normal singleplayer access remains available.

The world override saves in `level.dat` as `LpkeWorldEditAccess` and carries across
dimensions. It does not depend on the world's display name. Changing access while
an edit is unfinished cancels it and completes rollback. Freecam and multiplayer
restrictions remain regardless of the world override.

## Navigation

| Action | Commands |
| --- | --- |
| Move upward | `//up [-fg] distance`, `/up` |
| Next/previous floor | `/ascend [levels=1]`, `/descend [levels=1]`, aliases `/asc`, `/desc` |
| Reach a ceiling | `/ceil [-fg] [clearance=0]` |
| Escape a solid block | `/unstuck`, `/!` |
| Move through a wall | `/thru` |
| Reach the block you face | `/jumpto`, `/j` |

Every navigation command also accepts double slashes. Up and ceil enable creative
flight when available; otherwise they place one undoable glass platform in empty
space. `-g` forces glass, `-f` requires flight. Disabled creative flight falls back
to glass. Up rejects a ceiling in the path; ascend finds the next floor instead.
Navigation checks loaded terrain, headroom, dangerous blocks, Beta height and
mounts. Through-wall and aimed navigation stop within 256 loaded blocks.

## Commands

Arguments in brackets are optional. Directions are `north`, `south`, `east`,
`west`, `up`, `down`, `me` or `back`, with cardinal single-letter aliases.
`me` follows your facing, including steep upward/downward views.

| Selection | Syntax |
| --- | --- |
| Wand | `//wand`, `//toggleeditwand` |
| Position | `//pos1 [x,y,z]`, `//pos2 [x,y,z]`, `//hpos1`, `//hpos2` |
| Clear | `//sel`, `//desel`, `//deselect` |
| Chunk and size | `//chunk`, `//size` |
| Expand | `//expand amount [reverse-amount] [direction]`, `//expand vert` |
| Contract | `//contract amount [reverse-amount] [direction]` |
| Shift | `//shift amount [direction]` |
| All sides | `//outset [-h|-v] amount`, `//inset [-h|-v] amount` |

Coordinates also accept three space-separated numbers and relative coordinates
such as `//pos1 ~-2 ~ ~3`. With no coordinates, position commands use your feet.
Only cuboid selections are supported. `//expand vert` selects Beta's full 0–127
height. North is negative Z and east is positive X.

| Region and fill | Syntax |
| --- | --- |
| Fill selection | `//set pattern` |
| Replace | `//replace [mask] pattern`, `//re [mask] pattern` |
| Walls | `//walls pattern` |
| All six faces | `//faces pattern`, `//outline pattern` |
| Centre | `//center pattern` |
| Top surfaces | `//overlay pattern` |
| Hollow cuboid | `//hollow [thickness] [interior-pattern]` |
| Fill hole | `//fill pattern radius [depth]` |
| Recursive hole fill | `//fillr pattern radius [depth]` |
| Global mask | `//gmask [mask]` |
| Counts | `//count mask`, `//distr` |

`//replace pattern` replaces non-air blocks. `//fill` starts at your feet and
spreads across connected air at that height, then down each column. Depth defaults
to one. `//fillr` also spreads sideways as it descends, filling undercuts; its
default depth reaches the world floor. Neither travels above the starting height.
`//toggleplace` switches hole/shape placement between your feet and position 1.
`//hollow` clears the cuboid's interior while retaining its outer thickness.

| Clipboard and history | Syntax |
| --- | --- |
| Copy/cut | `//copy`, `//cut` |
| Paste | `//paste [-aos]` |
| Rotate around Y | `//rotate degrees` |
| Mirror | `//flip [direction]` |
| Clear clipboard | `//clearclipboard` |
| Repeat selection | `//stack [-as] [count] [direction]` |
| Move selection | `//move [-as] [distance] [direction]` |
| Undo/redo | `//undo [steps]`, `//redo [steps]` |
| Clear history | `//clearhistory` |
| Progress/cancel | `//status`, `//cancel` |

Copies retain their offset from your feet, as WorldEdit does. Paste `-a` skips
air, `-o` uses the original copy position, and `-s` selects the pasted bounds.
Stack/move `-a` skips source air; `-s` shifts the selection to the final copy.
Rotations use multiples of 90 degrees. Beta directional metadata rotates with
the clipboard. Unsupported mirror orientations are rejected before pasting.
Block entities, including chest contents and sign text, survive copy and undo.

| Shapes and utilities | Syntax |
| --- | --- |
| Sphere | `//sphere [-r] pattern radius`, `//hsphere [-r] pattern radius` |
| Cylinder | `//cyl pattern radius [height]`, `//hcyl pattern radius [height]` |
| Drain liquids | `//drain radius` |
| Nearby replacement | `//replacenear radius mask pattern` |
| Nearby removal | `//removenear mask radius` |
| Vertical clearing | `//removeabove [size] [height]`, `//removebelow [size] [height]` |
| Help | `//help [1-6]` |

Sphere `-r` raises the centre by its vertical radius. Ellipsoids accept three
comma-separated radii; cylinders accept one or two horizontal radii. Nearby
utilities act within a cuboid around the placement point. Shape radius is capped
at 128, with normal edit limits still applying.

Patterns accept Beta names or numeric `ID:metadata`, comma mixtures, and weighted
mixtures such as `75%stone,25%cobble`. Examples include `44:2`, `red_wool`,
`oak_slab`, `spruce_log` and `glass`. Masks accept comma-separated block types,
metadata, `!` negation, `*` or `#existing`. Modern blocks/states are unavailable.
The transient moving-piston blocks and locked chest cannot be created by patterns.

## Entities

| Action | Syntax |
| --- | --- |
| Count before clearing | `//countentities type[,type] [radius=32]` |
| Remove selected types | `//remove type[,type] [radius=32]` |
| Remove hostile mobs | `//butcher [radius=32]` |
| Include friendly mobs | `//butcher [-apwf] [radius=32]` |

`/remove`, `/rem`, `/rement` and `/butcher` also accept a single slash.
Examples: `//remove items 20`, `//remove mobs,projectiles 32`, `//butcher -a 48`.

Types: `items`, `mobs`, `hostile`, `animals`, `pets`, `water`, `projectiles`,
`vehicles`, `boats`, `minecarts`, `tnt`, `fallingblocks`, `paintings`, `all`, or
Beta species names such as `zombie`, `creeper`, `cow`, `wolf`, `pigzombie` and
`squid`. Commas combine types. Butcher flags: `-a` adds animals, `-p` pets,
`-w` water creatures, `-f` all friendly mobs. Tamed wolves are protected unless
`pets`, `all`, `-p` or `-f` is explicit, even with a `wolf` or `mobs` filter.

The radius is a sphere measured from your feet, from 1 to 256 blocks. Only loaded
entities are considered. Players and any connected mount/passenger chain are
always protected. Removal produces no vanilla drops; storage carts and their
contents are deleted together. Entity removal cannot be undone. Use
`//countentities` to check the filter and radius first. Entity commands use your
position regardless of `//toggleplace`.

`//help 5` lists these commands in game.

## Edit behaviour and limits

The default limit is 65,536 changed blocks, processed in batches of 2,048 per
tick. Limits, batch size and undo depth are configurable. Planning checks bounds,
loaded chunks, block-entity serialization and limits before writing anything.
`//cancel` restores any blocks already written by an unfinished edit. Leaving a
world rolls back an unfinished edit before the game's save and clears the session.

Edits update native chunks, lighting and rendering without triggering placement,
removal or neighbor-physics callbacks. Replacing containers does not drop their
contents. Normal gameplay updates can still change blocks afterward; undo records
the blocks written by the command. No custom world format or block IDs are added.

Undo/redo history is limited to 20 edits by default and 64 MiB overall. A single
edit's planned history is capped at 32 MiB; clipboard data at 24 MiB. History and
clipboard are session-only and clear on world changes. Multiplayer, Freecam,
biomes, brushes, schematics, arbitrary-angle rotations and modern block
states are outside this initial implementation.

## Install and build

Install `worldedit-beta-1.2.1.jar` and remove CreativeEditorWands. LpkeTweaks is
optional. The mod uses existing Babric, Java 17+ and MixinExtras 0.5+, supplied by
the tested loader. Build with JDK 21 and `./gradlew --no-daemon build`.

Syntax references: [EngineHub command reference](https://worldedit.enginehub.org/en/latest/commands/),
[utilities](https://worldedit.enginehub.org/en/latest/usage/utilities/),
[clipboard](https://worldedit.enginehub.org/en/latest/usage/clipboard/).
See [validation](VALIDATION.md) for tests and their limits.

Version 1.2.0: [changelog](CHANGELOG.md) and [validation report](VALIDATION-1.2.0.md).

Latest fixes: [validation report](VALIDATION-1.2.1.md). Left-click option
values to increase or cycle forward; right-click to decrease or cycle backward.
