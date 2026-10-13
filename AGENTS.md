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
  Gameplay hotkeys queue settings snapshots through `PowerConfig`; never flush files
  on the input or render path. Flush queued saves before menu backups and on shutdown.
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
- Auto-walk retains the player's activation direction, independently of freecam's
  manual keys. Optional inventory looking uses native sensitivity and rotation,
  checks the originating press against all GUI panels, creative tabs and destroy
  slots. Capture and hide the pointer for that drag, including GUI hover/cursor
  stacks, until release. Restore it on release, lost focus, screen/world changes
  or lost eligibility; holding the button must not restart a cancelled drag.
  Dispatch the mouse event batch before grabbing; quick gestures keep their press
  coordinates and send their final motion through the native camera renderer.
  Right-click looking must not drop cursor items; supported screens and focus rules
  remain shared with inventory movement. Both native and raw mouse helpers must
  supply cursor deltas in GUIs; raw device deltas remain gameplay-only.
- `AudioController` owns all music sources, pauses, previews and timing.
  World/menu transitions preserve streams and pause state in WORLD/MIX modes;
  only CUSTOM resets playback. Preview restoration must not revive old jukeboxes.
  Synchronize sound-source cleanup with decoder reads on the sound threads.
  Never scan folders or decode audio from a render loop. Preset drafts and saved
  snapshots must stay separate from live library edits; only Load copies a preset
  into live state. Track pools prevent new folders silently expanding presets.
  Keep missing track IDs, queues and user music files intact.
  Previous during a between-track gap replays the last played track without
  moving back in history or consuming queued tracks. Paused tracks remain current.
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
- Creative middle-click dragging previews compatible, writable slots and commits
  together on release. Use normal item and slot limits; preserve cursor damage/NBT.
  Never paint hidden catalogue slots, survival, spectator or multiplayer inventories.
  Cancel on focus loss, screen/container changes, lost cheats or cursor changes.
- `TweakIndicators` exposes live module state across mappings. HUD inclusion is
  separate from gameplay settings. Do not infer active auto-walk/free look from
  feature availability or poll action keys while rendering.
  Per-feature toggle messages default off and observe transitions on ticks,
  independently of HUD inclusion. Menus, focus changes and world loading stay silent.

- F3 providers share the debug text layout and colour. Capture only the F3 block,
  never chat or other HUD text. World overrides identify scope at group level.
- Flight sprint covers all three movement axes in creative, spectator and freecam.
  Opposing inputs cancel; stopping movement resets Toggle sprint.
- Chat editing shares a buffer across input and completion. History keeps arrow
  ownership until editing or explicit completion; rendering must not change text.
  Wrapped chat keeps the last Beta colour code from the preceding line.
  Help and completion share command availability. Group WorldEdit under //help,
  retain aliases, and keep per-command help free of gameplay changes.
  Every new command must appear in /help and completion, with /<command> help
  describing its syntax and permissions without changing gameplay.
  Optional death records are scoped by stable world ID and player. Recovery needs
  cheats, plans full capacity first and protects native item NBT and original slots
  with a flushed journal. GUI recovery uses per-stack claims; viewing never changes
  inventory. Occupied original slots cancel recovery; GUI is the only recovery entry.
  Interrupted claims stay blocked; disabling keeps history. Decimal death logs
  start fresh in 1.4.2. Keep inventory is a per-world cheat rule which clears when
  cheats are disabled. Preserve native stacks through death, respawn and dead-player
  saves; kept deaths claim their recorded items without creating drops.
  Death snapshots include the four player crafting inputs, never the recipe output.
  Normal deaths drop those inputs once; kept deaths preserve them through respawn.
  Recovery restores empty original crafting slots and rolls back recipe callback failures.
- Freecam bypasses entity and block entity culling independently of player movement.
  Background culling only reads loaded chunks and uses its own vectors and boxes;
  never run chunk loading or touch Beta's shared geometry pools from that worker.
  Keep culling results current and preserve preferences so normal culling resumes on exit.
  Entity render-distance checks use the detached camera position; simulation stays player-owned.
  Enter from the rendered camera pose, including interpolation, eye height and bobbing.
  Keep the first detached frame still, reset movement timing and reject old-world poses.
- Natural spawning rules are saved with world properties and copied across
  dimensions. They require cheats, preserve existing mobs and never gate /summon.
- Optional modern water landings reset fall distance before landing damage, for
  source and flowing water. Match fluid height and hitbox contact, including fast
  movement through water. Keep Beta defaults and multiplayer server authority.
- Optional empty bucket stacks cap at 16; filled buckets remain unstackable.
  Filling and milking exchange one bucket, with inventory insertion or a normal
  overflow drop. Preserve existing stacks when disabled and never split inventories
  while editing settings. Keep creative buckets reusable.
- WorldEdit named states map only to supported Beta metadata. Reject unsupported
  properties before starting a transaction; commas inside [] are state separators.

## Verification and delivery

- Test in a disposable clone outside the user's normal instance list. Never launch
  or edit an active instance. Stop only the exact process you started, verified by
  PID and game directory. Preserve worlds, recovery journals and original music.
- Mute every test instance before launch. Route audio tests to a silent output or
  capture device so they never play through the user's speakers or headphones.
  Never mute or change the audio routing of the user's own instances.
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

- This project does not accept external contributions. Keep that policy in the
  README and encourage forks under the AGPL; do not add a contribution guide.
- `gradle.properties` owns the pack version. Use semantic versions and tags `vX.Y.Z`.
  Module/API versions are independent; preserve them unless that module needs a change.
- Keep `CHANGELOG.md` newest first, with `## [Unreleased]` followed by dated version
  headings and plain bullets describing modpack features and fixes. Exclude website,
  release-management and development-process changes. Move Unreleased entries
  into the next version when preparing a release. Never invent past release versions.
- Follow `docs/releases.md`. Publishing requires explicit user permission in the
  current task. A code push alone does not authorize publishing a release.
- `python3 tools/release.py --check` validates release notes and versioning.
  The publish command builds and tests sequentially, verifies the export, publishes
  GitHub release assets and notes, and updates/deploys the website from the same data.
  Do not hand-edit generated website release details or overwrite published artifacts.
- GitHub hosts every release ZIP and checksum. Public repositories use GitHub
  downloads and retain no Blob mirrors. Private repositories mirror only the
  latest ZIP; older website entries keep their notes without a download link.
  Prune obsolete pack mirrors after verifying the deployed snapshot. Keep the
  previous live ZIP during deployment and enforce the 900 MB store budget before
  upload, including that temporary overlap. Never delete unrelated Blob files.
  After quota cleanup, check the store and account block state; an empty store
  does not prove that Vercel has lifted a suspension.
- Never commit runtime reports, logs, instance backups, account files, personal paths,
  `.env` files or Git bundles. Runtime fixture source stays in `validation-mod/`.
- Keep the top-level AGPL license and the README Inspired by links. Source modules
  have no separate license files or attribution metadata. Preserve binary dependency
  hashes and runtime IDs, especially `gcapi3`.
- Optional redstone textures use four-pixel wires with shaded edges and six-pixel
  junctions; the power number panel occupies one quarter of a block.
- Isometric captures use the actual renderer chunk grid and Beta's 128-block
  height. Centre both horizontal axes on the camera's chunk; never use camera Y
  for Z or infer capture size from the obsolete four-step distance option.

- Entity visibility changes only rendering. Full bright overrides float shading only
  on the render thread, never saved or simulated light. Spectator overrides last
  for one visit; leaving restores manual state, and lost cheats clear it.
- Item merging preserves native NBT, damage, normal stack limits and total counts.
  TNT limits retain native entities and saved fuses, bound work each tick and stay
  available from the main menu for recovery. Both leave multiplayer server-owned.

- Item/TNT collision indexing must cover native and StationAPI flattened chunks,
  including insertion, section changes and removal while the fix is off. Filter
  only exact native classes with null collision boxes; retain custom subclasses,
  vehicles and the original multiplayer query. The Fixes toggle takes effect live.
- Merge sweeps use terrain render distance for their threshold, resume within a
  tick budget and back off when idle. Revalidate native item data between batches
  and exclude removed entities. TNT interpolation changes render coordinates only.

- Leaving a world or quitting clears active tweaks through `TweakLifecycle`; preserve
  feature preferences, bindings and saved user data. Queue changed toggle snapshots.
  Auto-walk survives focus loss by default, including the automatic pause screen.
  In supported inventories, retain actual auto-walk movement while unfocused;
  only manual movement keys require focus. Test player displacement, not just the toggle.
- Inventory looking restores the originating cursor position inside the native
  grab transition. Test native and raw input; avoid a second post-release warp.
  Dispatch complete mouse batches before changing capture, preserve every drag's
  deltas and remap later clicks to the restored cursor. Test consecutive fast drags
  and movement immediately after release. Linux capture must retain the event
  baseline, since XQueryPointer can be ahead of queued motion and button events.
  Complete quick drags also need a hidden native cursor transition on Xwayland;
  verify another real pointer movement after release, not only LWJGL's cached position.
- Reject incomplete skin-provider responses so the next provider can supply the
  texture. Keep bundled fallback skin paths out of the HTTP downloader.
- Custom HUD clusters keep stable IDs and independent content, anchors, offsets,
  colour, size, opacity and F3 visibility. Sample world data on ticks and use the
  daylight clock. Native HUD previews and Cancel update positions immediately.
