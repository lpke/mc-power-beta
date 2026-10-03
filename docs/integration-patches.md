# Integration decisions

- Options are a new shared screen with independent sidebar/content scrolling,
  collapsible sections, search, draft edits, exact numeric entry and key capture.
  Its layout follows BTA 7.3_04. Native pause-menu button handlers remain intact,
  including incremental world saving and normal quit behavior.
- Owned code uses Power Beta packages, module IDs and assets. Legacy config and
  world-data identifiers are recognized only by migration adapters. The configuration
  API keeps its binary identity for the retained platform and performance modules.
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
- The configuration API is compiled from source. Its storage adapter owns sections
  in the unified JSON. Mod Menu hooks and duplicate Glass Networking code are removed.
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
  Runtime binaries are shipped without the previous configuration-JAR patch.

## Chunk writes and compact buttons

StationAPI replaces vanilla chunk writes. World editing suppresses its placement
and removal callbacks inside the existing scoped transaction, matching the vanilla
chunk adapter. This prevents slab merging and chest drop callbacks during edits.
The fixture checks actual chest removal before checking undo, so a rolled-back
failed edit cannot masquerade as a successful inventory restoration.

The shared Options renderer retains all four native texture edges and tiles the
interior. Native Beta buttons crop the bottom at compact heights and omit a column
on odd widths. The custom renderer fixes both without changing hitboxes.

## Unified storage and container recovery

- `PowerConfig` is the sole settings writer. Native options and key mappings,
  camera positions and all owned modules share `config/power-beta.json`.
- Migration verifies a byte-for-byte archive before committing the new document.
  A pending-import record resumes old-file cleanup after interruption. Changed or
  malformed originals are preserved. Settings transactions remain recoverable.
- Archives move under `power-beta-data`; world-local container journals stay with
  their world. Compatibility readers preserve older creative/editing NBT values.
- Container transfer uses the existing edit scope to suppress item-drop callbacks.
  It flushes original NBT before removal and target intent before placement. Loaded
  chunks, reach, block metadata, inventory checksums and placement occupancy are
  checked before mutation. Successful transfers retain full recovery archives.
  Paired chest records keep each half separate and checksum both payloads and
  target coordinates. Replay checks both locations before touching either one;
  mismatches stop the transfer and preserve the journal. Carry rendering uses
  read-only block views and a scoped pose on player and armor models.
- Position records use an explicit Gson decoder because the game's Gson 2.8.9
  predates Java record support. Journal tests use that exact Gson generation.
- Retired the one-off upstream catalog/default import generators. The reviewed
  catalog, code defaults and legacy migration fixtures are now maintained directly.
