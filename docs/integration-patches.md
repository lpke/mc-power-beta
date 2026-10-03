# Integration decisions

- Options are a new shared screen with independent sidebar/content scrolling,
  collapsible sections, search, draft edits, exact numeric entry and key capture.
  Its layout follows BTA 7.3_04. Native pause-menu button handlers remain intact,
  including incremental world saving and normal quit behavior.
- Module IDs remain unchanged. This preserves mixin integrations, config loading,
  block/item registrations and creative-mode compatibility checks.
- Utility modules compile from editable sources through `source-build`. Their JARs
  are build outputs nested inside Power Beta, not external utility dependencies.
  The retained binaries are the platform, authentication and performance layer.
- QuickAdditions' sound-manager and title-music mixins are disabled in its source
  configuration. The shared audio player owns timing, ambient volume, playlists,
  menu music and safe folder scanning. Its unused BetterF3 adapter is removed.
- ZastavkaAPI's audio helper mixin is replaced by the shared player. Its public
  song metadata, stop requests and dimension/biome filename rules remain supported.
  Empty playlists now return safely. Entity, rendering and networking hooks remain
  unchanged. The original helper could dereference an empty playlist.
- Existing cached sounds load before the legacy resource-download request, with
  bounded network timeouts and duplicate-safe sound pool insertion. This avoids
  a stalled legacy resource server leaving the game silent.
- The config library's two Mod Menu compatibility mixins are removed from the
  packaged artifact. Its config loader, serializers and save hooks are retained.
- Defaults and interrupted-save recovery run when Fabric constructs the pack's
  language adapter, before mixin plugins and config-library preLaunch entrypoints.
  Installing them in an ordinary preLaunch hook was too late for these libraries.
- Settings with unsafe or misleading upstream ranges have reviewed bounds.
  Random-delay ranges cannot be zero; minimum leaf-decay time must be strictly
  below maximum. Recipe and registry settings are marked for restart.
- Creative and building implementations, including durable inventory recovery
  journals and guarded atomic hotbar swaps, remain in their tested components.
  The integration menu never reads or writes item slots to edit settings.
- Component build tools are aligned inside this repository. Frozen standalone
  repositories and the `separate mods final` instance are not modified.

- RetroCommands no longer loads unused CryonicConfig and AccessoryAPI adapters.
  World-editing and creative commands own execution through a single outer method
  wrapper. The command registry still supplies syntax colouring and completion.
- Mouse and keyboard bindings share standard modifier matching. A press retains
  its initial modifier combination, preventing fallback actions when modifiers
  are released first. Movement rechecks held modifiers each tick.
- Photo Mode is an icon beside native Options. The imported menu source owns this
  placement; no competing menu-layout mixin is required. The Minecraft logo remains.
- AppleSlices' unavailable source was reconstructed from its pinned artifact.
  Its two injection descriptors were remapped to Barn. The food preview is unchanged.
- Platform compile copies normalize Loom/access-widener metadata for the pinned
  build toolchain. Runtime platform code is not changed by that normalization.
  GCAPI's obsolete Mod Menu hook removal is the only packaged binary patch.

## Chunk writes and compact buttons

StationAPI replaces vanilla chunk writes. World editing suppresses its placement
and removal callbacks inside the existing scoped transaction, matching the vanilla
chunk adapter. This prevents slab merging and chest drop callbacks during edits.
The fixture checks actual chest removal before checking undo, so a rolled-back
failed edit cannot masquerade as a successful inventory restoration.

The shared Options renderer retains all four native texture edges and tiles the
interior. Native Beta buttons crop the bottom at compact heights and omit a column
on odd widths. The custom renderer fixes both without changing hitboxes.
