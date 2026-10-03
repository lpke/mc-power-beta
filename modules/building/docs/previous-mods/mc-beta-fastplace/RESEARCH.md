# Upstream references

- Original Tweakeroo: https://github.com/maruohon/tweakeroo/tree/a348bcabe83a291824ba1717a0912068e71bfa16
- Maintained 1.21.11 branch: https://github.com/sakura-ryoko/tweakeroo/tree/9d520d6bd6f27217bc77204b3efa1b828caf31ca

Relevant code is in `tweaks/PlacementTweaks.java`, `config/Configs.java`,
`config/FeatureToggle.java` and `config/Callbacks.java`.

The port retains the bounded attempts per tick, fresh raycasts after success,
first successful click state, no repeat at the last placed position, placement
restriction modes, orientation memory and item filters. Beta placement remains
authoritative for inventory consumption, metadata, slabs, replaceable blocks,
collisions and height limits.

## Pinned code paths

- [Original placement loop and first-click state](https://github.com/maruohon/tweakeroo/blob/a348bcabe83a291824ba1717a0912068e71bfa16/src/main/java/fi/dy/masa/tweakeroo/tweaks/PlacementTweaks.java): `onUsingTick`, `onProcessRightClickBlock`, `clearClickedBlockInfoUse`, and `isPositionAllowedByRestrictions`.
- [Maintained placement implementation](https://github.com/sakura-ryoko/tweakeroo/blob/9d520d6bd6f27217bc77204b3efa1b828caf31ca/src/main/java/fi/dy/masa/tweakeroo/tweaks/PlacementTweaks.java).
- [Maintained settings](https://github.com/sakura-ryoko/tweakeroo/blob/9d520d6bd6f27217bc77204b3efa1b828caf31ca/src/main/java/fi/dy/masa/tweakeroo/config/Configs.java): `FAST_BLOCK_PLACEMENT_COUNT`, `FAST_PLACEMENT_REMEMBER_ALWAYS`, `PLACEMENT_RESTRICTION_MODE`, `PLACEMENT_RESTRICTION_TIED_TO_FAST` and the three fast-placement item-list settings.
- [Toggle callbacks](https://github.com/maruohon/tweakeroo/blob/a348bcabe83a291824ba1717a0912068e71bfa16/src/main/java/fi/dy/masa/tweakeroo/config/Callbacks.java) link the fast-placement toggle to the restriction toggle.

The maintained settings default to two placements per tick, orientation memory
on, Face restriction and restriction tied to fast place. The port enables the
restriction by default and starts the feature itself off with an unbound key.

## Adaptation decisions

The port preserves the native Beta interaction path through `InteractionManager`
and each item's use method. It observes the initial successful placement, stores
only primitive click data and block coordinates, then reraycasts between bounded
subsequent attempts. It verifies the destination's block and metadata changed;
Beta's boolean return value alone can report success without changing a block.

Beta has no modern item-placement context, offhand or modern block-state
rotation protocol. The destination adapter handles snow, the installed plant
replacement fix and matching slab merges. Native item code still makes the final
placement decision. Orientation memory restores player yaw in `finally` and keeps
the actual clicked attachment face. It does not invent modern rotations or
unsupported Beta slab halves.

The port counts the initial successful click toward the tick budget, which makes
the configured maximum a strict bound. It stops on item or slot changes and
requires release after a GUI/focus interruption. These limits favour predictable
singleplayer behaviour over automatically continuing across unrelated actions.

The item lists use Beta's actual item IDs and optional metadata instead of modern
registry names. Tweakeroo's default blacklist contains Ender chests and shulker
boxes. Neither exists in this instance, so the Beta blacklist is empty.

Grid placement, placement limits, flexible placement, fake sneak placement,
accurate-placement protocols, after-clicker, restocking and Fast Right Click are
separate Tweakeroo features and are outside this focused port. Restriction
settings here affect fast placement only; turning fast place off restores normal
placement.

## Building mods integration, 1.1.0

The optional Flexible Placement adapter supplies the transformed native click
coordinates before Fast Place predicts and checks the destination. Slab Placement
supplies adjacent-face merge targets. Both APIs are resolved once through
reflection and require no additional runtime JAR when absent.

Keep half-slab layer defaults on. It prevents held clicks from merging slabs or
climbing above the first slab's height and allows side-face continuation under
the Face restriction. Disabling it permits a half-to-double transition even at
the previous destination, without removing repeated-position suppression for
other blocks. Flexible modifiers take precedence over remembered yaw.
