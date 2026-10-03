# Beta Fast Place

Tweakeroo-style fast block placement for singleplayer Minecraft Beta 1.7.3 on
Babric. The default restriction keeps placement on the same face you first
clicked until you release right-click.

## Use

1. Open **Options > Controls** and bind **Fast place (toggle)**. The key starts
   unbound to avoid conflicts with existing controls.
2. Press the key in-game to enable fast place, then hold right-click while moving
   the crosshair across blocks. Press the key again to disable it.
3. Open **Mods > Beta Fast Place > Configure** for the related settings. The same
   toggle key can also be edited there. **Done** saves; **Cancel** or Escape
   discards pending changes.

Fast place starts disabled. The chat displays its on/off state when toggled.
It uses native placement, inventory consumption, collision and block-update code.

## Settings

| Setting | Default | Behaviour |
| --- | --- | --- |
| Blocks per tick | 2 | Maximum successful placements per tick, including the initial click. Range 1–16. Left-click increases; right-click decreases. |
| Placement restriction | On | Limits subsequent positions while right-click is held. |
| Restriction mode | Face | Face, Plane, Layer, Column, Line or Diagonal, using Tweakeroo's coordinate rules. |
| Keep half-slab layer | On | Held placement keeps slabs at the initial height, accepts side faces to extend a row and avoids accidental double slabs. Fresh clicks can still complete slabs. Turn off to allow held completion. |
| Remember orientation | On | Keeps the initial player-facing direction for directional blocks. |
| Tie restriction to fast place | On | Switching fast place on/off also switches the restriction on/off. Turn this off to keep your restriction preference independently. |
| Item filter | Blacklist | None, Blacklist or Whitelist. An empty whitelist disables accelerated placement for every item. |
| Blacklist / whitelist | Empty | Comma-separated Beta item IDs, optionally with metadata. `1, 44:2` means stone and wooden slabs. |

Face means the same clicked side, such as the top face, rather than a fixed
height. Plane keeps the coordinate perpendicular to the first face fixed. Layer
keeps height fixed. Column extends perpendicular to the first face. Line and
Diagonal constrain positions within its plane.

Fast Place 1.1.0 optionally integrates with Beta Flexible Placement and Beta Slab
Placement. It predicts offset destinations and adjacent-face slab merges. Held
flexible modifiers take precedence over remembered yaw, keeping placement and
the visible overlay aligned when you turn. Both companion mods remain optional.

Configuration is in `config/beta-fastplace.properties`. The shared hotkey uses
Minecraft's normal `options.txt`. Config writes are atomic and happen only when
saving settings or toggling the feature, never during block placement.

## Beta behaviour and compatibility

- Native Beta slabs and the installed UniTweaks Tels Addons slab/plant fixes keep
  their own placement rules. Matching slab variants merge; different variants
  remain separate. The mod does not add upside-down slabs or stairs.
- Ordinary block items, redstone, repeaters, doors, signs, beds, sugar cane, seeds
  and cake can use fast placement. Buckets, vehicles, food and editor tools do not.
- Menus, focus loss, death, world changes, Freecam and changing the held item or
  hotbar slot stop the held placement. Release right-click before resuming.
- The normal raycast supplies the target, including entities that obstruct it.
  The mod does not extend reach, load distant chunks or bypass collision rules.
- Failed placements and unchanged blocks end the current tick's attempt loop.
  The previous placement position cannot repeat until a different position is
  placed or right-click is released. This also limits repeated falling blocks.
- Survival and BHCreative inventory behaviour remain native. No block IDs,
  recipes, packets or saved world data are added. Removing the JAR needs no world
  conversion.
- This port only activates in local worlds. Multiplayer is deliberately excluded.

The separate Prism instance **Beta 1.7.3 - Fast Place Test** contains the same
previously enabled mods as the primary instance. No other mod settings need to
change for Fast Place. See [validation results](VALIDATION.md) for the tested
versions, cases and practical limits.

## Source and build

The port adapts the bounded placement loop, click state, restriction predicates
and settings from Tweakeroo. See [upstream research](RESEARCH.md) for pinned
source references and Beta-specific differences. Licensed LGPL-3.0-only.

Use JDK 21 and the included Gradle wrapper:

```sh
./gradlew --no-daemon build
```

Install `build/libs/beta-fastplace-1.1.0.jar`. The sources JAR and validation JAR
are not needed for gameplay. The build targets Java 17 bytecode and uses Babric
Loom 1.5.3, Barn build 8, Fabric Loader and the optional Mod Menu API.

`./gradlew test` runs the unit tests. The separate `validation-mod/` project
contains game tests and must only be installed in a disposable instance. See its
[README](validation-mod/README.md).
