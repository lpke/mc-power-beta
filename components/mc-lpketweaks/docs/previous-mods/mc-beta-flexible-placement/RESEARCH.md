# Tweakeroo research and Beta adaptation

Reviewed Tweakeroo's maintained `26.3` branch at commit `4ffd48d86267a2c3904a88e962bc3c7fbed92d05` on 2 October 2026,
and the original [placement explanation by masa](https://gist.github.com/maruohon/0fcfa9b5a6900f174bc2503ef2c63444).

## Placement controls

[PlacementTweaks.java](https://github.com/sakura-ryoko/tweakeroo/blob/4ffd48d86267a2c3904a88e962bc3c7fbed92d05/src/main/java/fi/dy/masa/tweakeroo/tweaks/PlacementTweaks.java) splits the feature across
`tryPlaceBlock`, `getRotatedFacing`, `handleFlexibleBlockPlacement` and the accurate
placement path. The [hotkeys](https://github.com/sakura-ryoko/tweakeroo/blob/4ffd48d86267a2c3904a88e962bc3c7fbed92d05/src/main/java/fi/dy/masa/tweakeroo/config/Hotkeys.java) are distinct from the
[feature toggles](https://github.com/sakura-ryoko/tweakeroo/blob/4ffd48d86267a2c3904a88e962bc3c7fbed92d05/src/main/java/fi/dy/masa/tweakeroo/config/FeatureToggle.java).

- `flexibleBlockPlacementOffset` moves the normal destination toward the selected
  edge of the five-region overlay. The centre moves it one block farther from the
  clicked surface, leaving a gap. Outer regions give diagonal placement.
- `flexibleBlockPlacementAdjacent` moves an outer-region destination back into
  the clicked block's plane. This supports extending a bridge from its top face.
- `flexibleBlockPlacementRotation` changes the placement direction using the
  selected region. The overlay comes from MaLiLib's `PositionUtils.getHitPart`
  and `RenderUtils.renderBlockTargetingOverlay`. Its centre is a half-width
  square; diagonal lines divide the surrounding four regions.
- `accurateBlockPlacementReverse` reverses a block's ordinary facing.
- `accurateBlockPlacementInto` points suitable blocks into the clicked face.
  Combining Into and Reverse points them out from that face.

The modern implementation uses placement contexts, block states, temporary
player yaw and the accurate-placement protocol. Those APIs and server protocols
do not exist in Beta. This port calculates a destination, calls native Beta item
placement, and applies directional metadata inside native placement callbacks.
Piston facing is changed before its placement power check. The camera, player
position and network packets are never spoofed.

Pistons and sticky pistons support six directions. Furnaces, dispensers, stairs,
pumpkins, jack-o'-lanterns and repeaters retain Beta's horizontal directions.
Impossible vertical facings are rejected. There are no observers, droppers,
upper slabs, inverted stairs or modern log axes in this port.

Offset and Adjacent can be combined; their displacements add. Rotation follows
the selected edge. Into takes precedence over Rotation, then Reverse flips the
result. This explicit ordering avoids ambiguity when modifiers are combined.

The actual entity-aware crosshair target remains required. Destination and
source must be loaded, within Beta height/coordinate bounds and normal reach.
Native collision, support, block updates and inventory handling remain active.
The five-region overlay and destination wireframe use the same geometry as the
placement calculation; invalid destinations are red. GL state is restored after
rendering. The overlay stays on the clicked surface, including partial blocks.

## Other useful related options

[Configs.java](https://github.com/sakura-ryoko/tweakeroo/blob/4ffd48d86267a2c3904a88e962bc3c7fbed92d05/src/main/java/fi/dy/masa/tweakeroo/config/Configs.java) also contains overlay colour,
`rememberFlexibleFromClick`, `fastPlacementRememberOrientation`, placement grid
size, placement count limit, restriction mode, restriction tied to fast placement,
and fast-placement item lists. Fake sneak placement bypasses a container's use
action while placing. It is separate from movement edge protection.

This port includes overlay colour/opacity, destination preview and optional
placement against containers. Existing Beta Fast Place supplies placement rate,
restrictions, orientation memory and item filters. Grid placement, after-clicker,
restocking and remembered flexible modifiers are not added. Modifiers must stay
held and must be released after menu/focus interruptions. This keeps their state
visible and avoids unexpected placement after returning to the game.

## Fake sneak

[MixinPlayer_fakeSneak.java](https://github.com/sakura-ryoko/tweakeroo/blob/4ffd48d86267a2c3904a88e962bc3c7fbed92d05/src/main/java/fi/dy/masa/tweakeroo/mixin/entity/MixinPlayer_fakeSneak.java) changes
only the predicate inside edge-backoff movement. It does not make the player's
public sneak state true and does not apply sneak input scaling.

Beta handles sneak edge prevention in `Entity.move`, using collision probes one
block below the player's bounding box and 0.05-block reductions. It checks X and
Z separately and lacks the modern combined diagonal check. Beta Fake Sneak
clips these movement arguments, includes that diagonal check, then runs native
movement. It affects only the live local player on the ground. Jumps, airborne
motion, creative flight, riding, noclip, Freecam and multiplayer are excluded.
It does not prevent intentional jumps, ladder descent or falls while airborne.

## Existing mod integration

Beta Fast Place provided the Gradle/Barn setup, native Mod Menu screens, atomic
properties files, shared native key bindings and guarded optional Freecam lookup.
Each mod builds independently. There is no shared helper JAR or Tweakeroo/MaLiLib
runtime dependency. Fake Sneak uses MixinExtras 0.5.0, already bundled with the
installed Fabric Loader 0.18.1.

Fast Place 1.1.0 optionally calls Flexible Placement's `resolveClick` and Slab
Placement's `SlabMerge.target` through cached reflection. These return primitive
coordinate arrays, so none of the mods requires the others to be installed.
Flexible modifiers take precedence over Fast Place's remembered yaw. Otherwise,
turning the camera would select one overlay region while placing in another.

Beta Slab Placement adds adjacent-face merging independently of Tweakeroo. It
wraps the item-use call, so it coexists with UniTweaks Tels Addons' slab item
override and BHCreative's stack restoration. Existing addon configuration is
retained. Matching variants merge, entity collision is checked, one slab is
consumed in survival and creative counts stay unchanged.
