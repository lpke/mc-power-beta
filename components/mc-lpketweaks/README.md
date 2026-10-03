# LpkeTweaks

Building, movement, camera and mining controls for singleplayer Minecraft Beta
1.7.3 on Babric. Replaces Beta Auto Walk, Beta Transport, the local Omnilook build,
Beta Fast Place, Beta Flexible Placement, Beta Fake Sneak, Beta Slab Placement and
Click Mining Forever with one mod and one configuration menu.

## Settings and controls

Open **Mods > LpkeTweaks > Configure**. Left/right arrows move between grouped
pages. Done saves all pages together; Cancel or Escape discards the draft.
Hotkeys appear beside their related settings and also in Minecraft's Controls.
Existing key names are retained, so current `options.txt` bindings carry over.

Settings are stored atomically in `config/lpketweaks.properties`. If that file
does not exist, the mod imports the old Fast Place, Flexible Placement, Fake
Sneak, Slab Placement and Omnilook properties files. It leaves those originals
unchanged. Auto Walk and Transport retain their existing enabled defaults.

| Group | Controls |
| --- | --- |
| Fast placement | Toggle, rate, slab mode, remembered orientation |
| Placement restrictions | Face, plane, layer, column, line, diagonal; tie to fast placement |
| Placement filters | Blacklist/whitelist with item IDs and optional metadata |
| Slab completion | Complete matching halves through adjacent block faces |
| Flexible placement | Offset, adjacent bridging, rotation, reverse, into-face, container placement |
| Placement visuals | Target grid, destination preview, colour, opacity |
| Movement | Auto Walk, Fake Sneak and their hotkeys |
| Camera | Free look, first/third person, hold/toggle mode and hotkey |
| Hotbar swap | Preview modifier, direct row keys, modifier + 1/2/3 |
| Hotbar scroll | Hold, scroll through rows, release to swap |
| Hotbar preview | Visibility, alignment and X/Y offsets |
| Transport | Boat steering/collision protection and faster minecarts |
| Mining | Click Mining Forever's block-break delay reset |

### Slab modes

These apply while Fast Place is enabled:

1. **Continuous layers**, the default. Holding right-click can place a half slab,
   complete it, and continue onto the next block layer without releasing.
2. **Double slabs**. Each successful placement creates a full slab block. An
   empty destination costs two matching slabs; an existing matching half costs
   one. Insufficient survival inventory or collision rejects the whole placement.
   BHCreative keeps its stack, including a stack containing only one item.
3. **Match first placement**. A hold keeps its starting height and action. Start
   on empty space to lay half slabs; start by completing a half to keep completing
   matching halves. Release to change the action or layer.

Every mode respects the selected restriction. Double slabs use the same Face
rule as full blocks: a hold cannot switch from a roof's side face to its top.
For half-slab rows, Face also accepts exposed side faces on the starting layer
when the hold began on the top or bottom. This exception never changes layers.
Use Plane or Layer when the destination itself must stay on one plane; Face
restricts the clicked face, not the destination plane.

Adjacent completion obeys its own on/off setting in every mode. Native top-face
completion still works when adjacent completion is off. Different slab variants
never merge. Blacklisted slabs keep native half-slab behavior and previews.

### Flexible placement

Hold Offset to display five target regions. An edge selects a diagonal placement;
the centre leaves a one-block gap. Adjacent moves placement into the clicked
block's plane for bridging. Rotation chooses a facing from the selected region.
Reverse flips normal facing; Into points into the clicked face. Into overrides
Rotation, then Reverse applies. Offset and Adjacent displacements add.

Pistons support six directions. Furnaces, dispensers, stairs, pumpkins and
repeaters retain Beta's horizontal directions. Impossible directions are rejected.
Release modifiers after menus or focus interruptions. Freecam suspends placement.

Fake Sneak clips unsupported walking movement without setting sneak state or
slowing normal input. Jumping, airborne movement, flight and riding stay native.
Auto Walk stops for manual movement, menus and focus/world changes.

### Hotbar swapping

Bind **Hotbar preview**, **Hotbar scroll**, or **Swap hotbar with row 1/2/3** in
Mod Menu or native Controls. New bindings start unbound. The features are enabled
but do nothing until bound. Rows count from the top of the main inventory.

Hold Preview to see the inventory rows; press 1, 2 or 3 while holding it to swap
that row. Direct row bindings also work without the modifier. Hold Scroll, use
the wheel to select a row, then release to swap. The selected hotbar slot stays
unchanged. Preview + number handling also integrates with UniTweaks' extra bindings.

Options include independent swap/scroll toggles, number-row shortcuts, reverse
scrolling, remembering the selected row, preview visibility, five alignments,
and X/Y offsets. The remembered/default row is the bottom inventory row. Menus,
focus loss, death, world/player changes, Freecam, spectator and occupied cursors
cancel or refuse swapping. Containers must be closed.

Swaps preserve existing stack references and custom NBT. The mod plans a complete
array before publishing it with one reference assignment; it never drags, drops,
merges or temporarily clears live slots. Native NBT must round-trip unchanged.
An append-only recovery snapshot must be flushed and verified before each swap.
Unexpected changes stop the operation; a failed commit restores a preallocated
snapshot when the transaction still owns the live array. A detected failure
blocks further swaps until Minecraft restarts.

Recovery journals live under `.minecraft/lpketweaks-hotbar-backups/`, separated
by world name, seed and player. They contain before/after main inventories, armour,
selected slot and time. No snapshots are automatically deleted. A 64 MiB limit
per journal prevents unbounded growth; a full, damaged or unwritable journal
blocks new swaps. Archive a full file outside that folder before restarting.
See [recovery instructions](HOTBAR-RECOVERY.md). Backups are recovery evidence;
they are never automatically replayed over newer gameplay inventory.

### Free look

Choose **Camera > Perspective**: **First person** is the default and keeps the camera at eye level.
**Follow current third person** defaults ON, so starting free look from F5 third
person keeps that view. You can also force **Third person**. Both support Hold and Toggle activation, with the
same binding in this menu and native Controls. Existing Omnilook key assignments
and toggle settings are retained.

The camera module derives from Omnilook's separate camera/input approach. It
never changes the player's rotation fields. First person preserves Minecraft's
frame interpolation and restores the player view immediately on release. Hands, map tilt,
underwater overlays, particle billboards and labels use the camera angles.
StationAPI's Arsenic hand renderer and vanilla rendering have separate hooks.
Movement and interactions continue to use the player's facing while looking.

Menus, lost focus, death, sleep, Freecam and world changes reset free look.
F5 cancels it and keeps the perspective you selected. Releasing the key is
required before restarting after an interruption.

## Install and build

Install `lpketweaks-1.2.1.jar` and remove the replaced individual mod JARs. The
loader rejects duplicate installations to prevent double movement/placement hooks.
WorldEdit Beta is separate and optional. No shared helper JAR is required.

Runtime requires Java 17+, Babric and MixinExtras 0.5+, already supplied by the
tested Fabric Loader 0.18.1. Mod Menu is optional but provides the settings entry.

Build with JDK 21:

```sh
./gradlew --no-daemon build
```

See [provenance](PROVENANCE.md), `licenses/`, the earlier research under
`docs/previous-mods/`, and [validation](VALIDATION.md). The grass-block icon was
generated for this mod. The development validation JAR is not for normal gameplay.

Version 1.2.0: [changelog](CHANGELOG.md) and [validation report](VALIDATION-1.2.0.md).

Latest fixes: [validation report](VALIDATION-1.2.1.md). Left-click option
values to increase or cycle forward; right-click to decrease or cycle backward.
