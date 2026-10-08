# Power Beta development

## Build and layout

- Java 21 and Python 3. Run `python3 tools/build_pack.py` from this directory.
  It builds sequentially, runs unit tests and exports a clean Prism ZIP. Do not run
  Loom builds concurrently; their mapping caches are shared.
- `src/` owns the Options UI, config adapters, audio, overlays and integration.
  `modules/creative`, `building` and `worldedit` have their own builds.
  Other modules use `source-build/modules.json`; their original Gradle files are
  upstream reference, not the pack build. `input-api/` is the shared input bridge.
- Utility JARs are compiled here and nested inside Power Beta. Six platform,
  authentication and performance JARs remain external. Disabled vendor artifacts
  and the recipe-browser source are retained for reference, not shipped.
  Review pinned dependency manifests before changing platform binaries.
- Root tests: `./gradlew --no-daemon test`. Utility build:
  `python3 tools/build_utilities.py [MODULE ...]`. For a pack already built,
  `python3 tools/build_pack.py --skip-build` only assembles the export.

## Changes that need care

- Keep gameplay labels functional, with no upstream mod branding. Defaults should
  preserve Beta gameplay. Cheats gate game modes, item spawning, teleportation,
  world editing and other world-changing commands; QoL tools remain independent.
  Disabling access must preserve warps, history and inventories.
- `PowerConfig` is the only settings writer: `config/power-beta.json`.
  Defaults and interrupted-save recovery run from the language adapter **before**
  mixin plugins and configuration entrypoints. Preserve stable setting, world,
  track and preset IDs. Only the current configuration format is supported.
  Fresh installs copy `defaults/power-beta.json`; do not reintroduce historical importers.
  Update existing instances explicitly when their stored data needs to change.
  The config API retains its `gcapi3` binary identity for external dependencies.
- The game uses Gson 2.8.9. Do not rely on record deserialization without an explicit
  adapter. Test persisted data with that version, including rollback and restart.
- Options edits use `ConfigSession`. Apply commits; Cancel restores live previews.
  Update linked values together. Preserve search, scroll, related-view history and
  modifier ownership when rebuilding rows. Share render and hit-test geometry.
- `settings-layout.json` owns tab/group order; catalogue `order` values arrange
  settings inside groups. Presentation metadata applies to every adapter without
  changing storage IDs. Keep shared flight/freecam sprint controls linked by ID.
  The focus-loss pause preference is `power_controls:general.pauseOnLostFocus`.
- Ctrl, Shift and Alt are the only chord modifiers. Gameplay acts on key-down.
  Test both release orders, held movement, focus loss and open menus.
  Linux key repairs must update native events and polling together. F20–F24 use
  reserved LWJGL codes 114–118. Inventory movement only admits known screens without
  text input, excludes mouse/Shift, and uses normal container closure for item cleanup.
- `AudioController` owns all music sources, pauses, previews and timing.
  World/menu transitions preserve streams and pause state in WORLD/MIX modes;
  only CUSTOM resets playback. Preview restoration must not revive old jukeboxes.
  Never scan folders or decode audio from a render loop. Preset drafts and saved
  snapshots must stay separate from live library edits; only Load copies a preset
  into live state. Track pools prevent new folders silently expanding presets.
  Keep missing track IDs, queues and user music files intact.
- Difficulty is per-world, independent of cheats, including creation, world switches,
  dimension copies and commands. Never copy it from global GameOptions. Defaults
  are Normal; menu edits take effect only on Apply. Keep multiplayer server-owned.
- Hide cheat-only rows and empty tabs when cheats are unavailable. Shared freecam
  controls and non-cheat commands remain independent. Hidden bindings must not
  claim input priority or mouse events. Only world-specific settings need scope
  notes in tooltips and setting or group names; global settings have no scope boilerplate.
- World cycles are per-world cheats. Keep the simulation clock running; daylight
  uses an offset saved in world metadata. Sky rendering, day counters, time commands
  and photo mode use the daylight clock. Sleep must wake players without advancing
  frozen daylight or clearing frozen weather. Test normal and accelerated sleep,
  scheduled updates, pause/resume, save/reload and cheats being disabled.
- Numeric display units do not change stored values. Duration editors use seconds;
  processing budgets remain per tick. Validate item/filter IDs before accepting
  drafts, and keep temporary errors outside row layout.
- Item transfers are transactions, including full counts, damage and NBT. Preserve
  journal flushes, rollback, ownership checks and failure shutdown. Menu edits must
  never touch inventory slots. WorldEdit and carrying suppress StationAPI block
  callbacks only inside their scoped transaction to avoid drops and slab merging.
  Keep old carry-journal formats recoverable. See `docs/recovery.md`.
- Compact buttons must retain all four texture borders. Check narrow GUI sizes,
  sticky headings, clipping and click targets, not only a wide screenshot.
  Creative catalogue rows grow upward; hotbar and survival slots never move.
  The final catalogue row and hotbar use the same 22-pixel origin spacing as survival.
  Leaving spectator keeps the current position, including inside solid blocks.
- `TweakIndicators` exposes live module state across mappings. HUD inclusion is
  separate from gameplay settings. Do not infer active auto-walk/free look from
  feature availability or poll action keys while rendering.
  Per-feature toggle messages default off and observe transitions on ticks,
  independently of HUD inclusion. Menus, focus changes and world loading stay silent.

## Verification and delivery

- Test in a disposable clone outside the user's normal instance list. Never launch
  or edit an active instance. Stop only the exact process you started, verified by
  PID and game directory. Preserve worlds, recovery journals and original music.
- `validation-mod/` contains runtime fixtures, including `audio-maintenance`.
  Build with `./gradlew -p validation-mod --no-daemon build` after the root JAR.
  Install only in the disposable clone. Its README explains command dispatch.
  Fixtures can change settings and worlds; never ship them.
- For an existing Power Beta instance on matching dependencies, use
  `python3 tools/update_artifacts.py /path/to/instance`. It refuses running clients,
  verifies a backup and checks that settings and worlds remain byte-identical.
  Configuration changes are separate, backed-up operations through `PowerConfig`.
- Verify the final export contains seven enabled JARs and no test fixtures, accounts,
  worlds, logs or disabled mods. Keep README human-facing; add lasting implementation
  constraints here rather than another session diary.
- Commit and push only when the current user request authorizes it.

## Releases

- `gradle.properties` owns the pack version. Use semantic versions and tags `vX.Y.Z`.
  Module/API versions are independent; preserve them unless that module needs a change.
- Keep `CHANGELOG.md` newest first, with `## [Unreleased]` followed by dated version
  headings and plain bullets describing user-visible changes. Move Unreleased entries
  into the next version when preparing a release. Never invent past release versions.
- Follow `docs/releases.md`. Publishing requires explicit user permission in the
  current task. A code push alone does not authorize publishing a release.
- `python3 tools/release.py --check` validates release notes and versioning.
  The publish command builds and tests sequentially, verifies the export, publishes
  GitHub release assets and notes, and updates/deploys the website from the same data.
  Do not hand-edit generated website release details or overwrite published artifacts.
- Never commit runtime reports, logs, instance backups, account files, personal paths,
  `.env` files or Git bundles. Runtime fixture source stays in `validation-mod/`.
- Keep the top-level AGPL license and the README Inspired by links. Source modules
  have no separate license files or attribution metadata. Preserve binary dependency
  hashes and runtime IDs, especially `gcapi3`.
- Optional redstone textures use four-pixel wires with shaded edges and six-pixel
  junctions; the power number panel occupies one quarter of a block.
