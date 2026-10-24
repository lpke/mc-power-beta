- `creative-440` checks the six default tabs, seven native fill groups, pinned
  ID-ordered Search and supported variants, silent navigation/deletion, cursor-only
  palette wheel exchange, shared selectable fuzzy search, custom holes, remembered
  tab pages and isolated editor drafts with the one-click shared picker. It captures
  the palette and editor at 320x240 and 640x420. Run in a focused muted disposable
  world. It restores rules, mode, inventory, cursor, wheel settings and layout without
  writing configuration. Physical checks still cover Ctrl/Shift pointer selection,
  multi-cell drag release and Shift-delete click audio. The automatic check renders
  normal and F3+H damaged-sword tooltips through the native event and text pipeline,
  checking ID/metadata, durability and narrow-GUI clipping.
  `creative-440 editor` opens a seeded, isolated pointer draft and logs GUI/display
  coordinates. Follow its single, Ctrl-multiple, Shift-range and tab-drag instructions
  with native mouse input, then run each `creative-440 pointer-check STAGE`. Each
  stage checks the resulting slots, selection, release ownership and unchanged native
  inventory/configuration. `creative-440 stop` verifies preservation and restores the
  previous screen. Do not press Save while using this pointer fixture.

# Runtime checks

- `minimap-446 capture|run` captures the session mob-head toggle, then checks middle-click, hold-key handoff, menu ownership, disabled-work gates and settings preservation in a muted clone. `prepare keyboard|mouse`, `check-on|off|held|latched-held|inactive` and `stop` observe physical focus/hold gestures without saving settings.

- `ui-446 capture|run` adds native empty-cell feedback, clearing-drag, Undo and cancellation checks to the recipe/Search suite. Capture before acceptance; reports are under `power-beta-data/reports/ui-446`, with Search images under `ui-445`.

- `ui-445 capture` writes near-final recipe/Search images before acceptance checks.
  `ui-445 run` checks ghost cleanup, native slot/font pixels, current caret geometry and
  empty-cell selection/Undo isolation at 320/640 logical widths. Reports are under
  `power-beta-data/reports/ui-445`. Use a focused muted Survival clone at 1280×720
  or larger, with no inventory transaction in progress.
- `inventory-look-445 run` captures actual player-model poses in Survival, Creative,
  Spectator and recipe/palette views at narrow/wide sizes. `prepare survival|creative|spectator|recipe|palette`,
  held/released `check` and `stop` cover real pointer looking. Keep the clone muted;
  follow the logged originating press coordinates and restore through `stop`.

- `text-inputs-444` checks live On/Off right-click clearing in native/config widgets
  and recipe searches at 320 and 640 pixels, including bounds, disabled fields,
  selection, text limits, callbacks, modal ownership and native inventory preservation.
  Run in a focused, muted, disposable Survival world. `palette-442` also checks
  Creative Search autofocus only on tab entry, remembered-tab reopening, clearing
  and cursor decrement over matching, different and empty catalog cells.

- `creative-444` checks direct tab-name editing and focus-only confirmation at
  320x240 and 640x420, diamond icons for new tabs, plain/Ctrl-right drag clearing
  committed on the owning release, unknown-ID retention and one-step Undo.
  It captures a three-item group preview and checks the spaced group drop, plus
  photo framing across resolutions and zoom/daylight retention through resizing.
  Requires a focused, muted disposable singleplayer client at least 1280x840.
  Configuration and native inventory snapshots must remain unchanged.

- `gui-items-444` compares custom GUI and shared picker/editor icons against native
  inventory pixels for grass, stone, leaves, metadata wool, redstone and a damaged
  counted sword. It checks normal/scaled rendering after deliberately dark prior
  lighting and verifies light, material, color, texture and matrix restoration.
  Requires framebuffer support in a disposable client and never edits inventory.

- `creative-443-revision` checks selected-origin Ctrl deselection and Ctrl-right
  item clearing with one Undo step. Native cases use `prepare deselect-ctrl-first`,
  `deselect-button-first`, `clear-ctrl-first` or `clear-button-first`, then the logged
  pointer gesture, `check`, optional `undo`, and `stop`.

- `creative-443` checks scrollbar event ownership, immediate item dragging, Ctrl-drag
  selection, editable names and the Reset/Cancel/Undo/Save footer at small and wide sizes.
  Use `creative-443 prepare list|grid|item|paint`, `hold-check`, `check` and `stop`
  for real pointer checks, following its coordinates and modifier release instructions.
- `creative-443-containers run` requires either Shift key physically held and checks
  native container clear hints, rendering order, GL state and item preservation.
  `prepare`, `check` and `stop` exercise a physical Shift-click with isolated items.

- `creative-443-movement prepare focused|focused-flying|unfocused|recipe` opens an isolated native
  inventory with inventory movement enabled. Keep physical W held for at least one
  second, then run `creative-443-movement check` while still holding it. The focused
  Creative and recipe searches must accept W with zero horizontal displacement
  across actual world ticks; the unfocused Search control must move over 0.1 blocks.
  Release W and run `creative-443-movement stop` before another case. The fixture
  restores inventory, preferences, bindings, mode, flight, velocity and position.
  Use a muted disposable world with clear, loaded ground ahead of the player.
  `creative-443-movement sorting` checks current immediate-editor Escape and
  screen-change cleanup, then confirms a stale sorting close preserves recipe-search focus.

- `current-formats-442` validates schema 7, both format-4 templates and every saved
  world metadata/previous/dimension file, including native offhand/kept-item decoding
  after registry initialization. It checks byte preservation, old/future rejection
  and fresh native-world creation. Requires a muted disposable singleplayer clone.

- `water-sounds-432` requires a muted disposable world routed to the verified silent
  Pulse sink. It checks native head crossings at source/flowing surfaces in both
  perspectives, 40-tick fade-in and 20-tick fade-out, actual streaming playback and
  gain, exclusive random bands, immediate toggle cleanup, pause/world/mode boundaries,
  settings preview/Cancel and all 45 modern water decodes through EOS.
- `fluid-sounds-440` checks water entry for every native creature, items and vehicles,
  first-tick and repeat suppression, rider volume and boat shielding, independent lava
  ambience/pop switches, remote ownership and
  individual previews. Requires the same muted disposable world and verified silent sink.
  `release-300-extra-sounds` also tests every water/lava bucket toggle combination.
- `extra-sound-decoders-432` compares all 88 bundled extra-effect variants against
  the unchanged decoder on the same bytes, verifies exact source frame counts and
  writes disposable PCM evidence for comparison with FFmpeg.

- `minimap-444 run` checks quick actions and pause inheritance. Use `minimap-444 aim`
  in the disposable client to look down before `prepare left|right|middle0..middle4|disabled-middle`,
  then hold Enlarge and send the indicated native mouse press before `check`.
  `stop` restores a prepared case.

- `minimap-441 prepare CASE`, `check`, `check-release` and `stop` verify real wheel
  ownership while Enlarge minimap is held. Cases cover enabled/disabled, hidden maps,
  F3, zoom limits, camera zoom, Spectator speed, chord release order and hotbar rows.
  Follow the logged physical-key instructions; release keys after each case.
- `recipe-441` checks Alt-click variants, untouched-icon wheel page scrolling, last-batch
  removal and reuse, and Shift-right-click maximum crafting. It blocks the journal
  worker to prove no early publication, then checks all batches complete in one tick.
  Partial/zero capacity, tagged ingredients, native container returns, stale owners,
  per-batch callback failures, external-owner guards and unsupported subclasses are
  included. Use a focused, muted disposable Survival world with one nearby loaded air
  block. The fixture restores settings, inventory and its temporary crafting table.
  `recipe-441 prepare variants|max|wheel`, `check` and `stop` also check physical
  Alt-click, Shift-right-click and wheel input at the logged client coordinates.
  Use a wide native GUI and follow each logged action before checking.
- `recipe-availability-444` measures recipe-book startup and the slowest refresh tick,
  compares the completed display with uncached native checks, and verifies ingredient
  removal invalidates it without writing inventory. Use a focused, muted disposable
  Survival world. The fixture restores its inventory and settings.

- `sorting-worlds-440` checks separate active layouts and named presets across two
  world IDs, retained legacy/global preferences and unchanged native inventory data.
  It restores the complete settings document and runtime caches.
- `recipe-402` includes personal-grid fallback with crafting storage disabled and
  queued Shift-drag sources. `tool-whitelist-420` checks saving a retained historical
  selection. `offhand-actions-400 native` checks count-only equip continuity and
  normal animation for changed native data or an empty hand. Run these in a focused,
  muted disposable world. The offhand native fixture needs its ordinary air-only scene.

- `menu-423 check` checks the actual inventory and pause menu at all four minimap
  corners, modal ownership, immediate cave cancellation, selected cave-mode restoration
  and exclusion of unrelated screens. It captures every corner and restores preferences.
  Run in a focused disposable world. `menu-423 pause` and `menu-423 inventory` open the
  native hosts for pointer, focus-loss and waypoint-layering checks.

- `release-420 commands`, `release-420 sprint` and `release-420 spectator` check live
  command dispatch/help permissions, native one-hit attacks, ladder ascent/stamina,
  inventory sprint ownership and loaded camera interaction rays at configured reach.
  They restore settings previews, rules, mode, native player/input state, inventory and
  their bounded terrain scene, including block-entity data. Run in a focused disposable world.
- `release-420 hold-use` arms the real binding and observes ordinary native ticks for
  initial block use, held repeats, offhand food and one Creative boat/minecart per
  activation. Wait for `HOLD420 done`; `hold-use-status` and `hold-use-stop` inspect or
  cancel the run. A final Fast placement phase checks partial-stack restocking and
  resumed batches. The fixture restores its input, inventory and terrain on completion.
- `release-420 debug-toggles` watches F3-owned F20–F23 presses for AutoMine, AutoWalk, FastPlace and FakeSneak. `debug-toggles-ordinary` verifies activation inside each native key-down callback; automatic mining/walking stop immediately after observation. Use `debug-toggles-check` after releasing the keys and `debug-toggles-stop` to restore bindings/settings.
- `release-420 debug-hotbar` arms distinct tagged native inventory rows and watches
  every normal tick for changed bytes, a prepared/queued row or a saved row selection.
  Send fast native F3+Q in one input batch with each release order, then run
  `release-420 debug-hotbar-check` after each. Use `release-420 debug-hotbar-stop`
  to restore the inventory, keys, settings preview and debug overlay.
  `debug-hotbar-preheld` admits a Q preview before F3 and checks its cancellation;
  `debug-hotbar-ordinary` is the positive control for one immediate ordinary Q-release swap.
  Stop the preceding watcher before starting either variant.
- `release-420 line-lock` uses the real Auto-walk and line-lock bindings, installed
  Entity.move clamp and native travel. It checks forward/backward diagonal displacement
  in both lane axes, unchanged view, real/ghost-wall sprint, other-entity exclusion and
  toggle-off movement. It restores player, input, rules, previews and bounded terrain.

- `crawling-420` checks real trapdoor entry with all water/sprint combinations,
  native crawl distance and wall collision, single input scaling, safe disabled
  exits, crouch/standing heights, saved feet and player/armor model isolation.
  It compares the default open-air crouch camera against native Beta through repeated
  presses, releases and frame interpolation in both perspectives. It also checks the
  optional deeper camera, live changes and native-smoothed save/reload feet.
  It checks multiplayer, Spectator, other-player and unloaded-terrain exclusions.
  Run in a focused muted disposable world; it restores its scene and settings.

- `debug-410 controls420` checks the new temporary control registrations, live chicken
  hitbox dimensions, Delete-to-clear binding capture and narrow/wide prompt captures.
  Physical checks still cover F3+H in inventories, F3+Q without ordinary shortcuts,
  both F3 release orders, F3+Esc release/repress, F3+T progress/fade, and forward/backward
  auto-walk line lock against lane edges and real walls while sprinting.
- `debug-410 minimap420` checks the gear's modal pointer/keyboard ownership, native
  inventory retention, Escape cleanup and an actual menu capture above the container.

- `performance-421 load FOLDER` opens an existing cloned save without generating a substitute world.
  `preview ID JSON` and `restore` use an isolated Options preview, without saving preferences.
  `remember`, `return`, and `position X Y Z YAW PITCH` control repeatable views. `autosave SECONDS`
  explicitly changes the live test world's interval, which the ordinary option initializes at load.
  `held ID` and `held -1` temporarily replace and restore the selected stack in the clone.
  `map` opens the World Map; `resume` closes it. `state` records the actual light source and view.
  Always start comparisons from the same saved world/config/cache snapshot, outside normal instances,
  with a muted audio sink. Keep builds out of timed runs and separate warmup from steady samples.
- `performance-420 ui LABEL [SECONDS]` measures the whole native drawing call, including HUD/GUI,
  and keeps the current screen fixed. Other actions measure world rendering only. Reports also retain
  main-thread allocated-byte deltas and frame counts over 8.33, 16.67 and 33.33 ms.
  The allocation counter includes observer overhead. Parent/child CPU timings overlap.
  Native display-update intervals are recorded separately from world-render start intervals;
  neither measures physical monitor scanout or input-to-photon latency.
- `mesh-water-421 LABEL` compares native rebuilds against the frozen 4.2.0 water-lighting reference.
  It warms both arms, checks exact packed GPU bytes and alternates ABBA batches over identical
  sections and fixed simulation. Scratch meshes and byte readbacks use a private GPU pool so
  driver allocation/readback state cannot contaminate later live-world travel samples.
  The reference exists only inside this validation mod.
- `mesh-water-430 LABEL` uses the same paired native/GPU-byte comparison against
  frozen 4.2.3 water lighting. `water-columns-430` compares exact shade bits across
  ascending, descending and shuffled height queries at three daylight levels.
  `leaf-lighting-430` checks the transformed native brightness method with the leaf
  option on/off, different sampled materials and emissions, and exact shade bits.
  Repeat in cloned Bliss and the water/roof fixture scene. For timed VirtualGL
  runs, use `VGL_READBACK=none` to exclude test-display pixel copies; retain normal
  readback for visual inspection. Do not compare timings from different display modes.
- `map-hot-421` checks exact coordinate and UV bits against 4.2.0 for 2,000 clipped polygons.
  `culling-421` races native world setup against immutable renderer-capability reads, checks inherited
  and missing renderers and rejects any observed culling-worker access to mutable native resolution
  or native chunk loading. Run after a real world has rendered for at least a few seconds.
- `mesh-cache-420 LABEL` rebuilds twelve loaded native sections in one tick with
  caching enabled and bypassed. It warms both arms, checks identical packed GPU
  bytes, then records 24 ABBA pairs with frozen simulation/daylight in the performance
  report directory. Run without other builds or CPU-heavy tests.
- `vbo-421` queues 40,000 native draw commands, grows a private vertex pool and verifies
  all five command words, bounded command storage, retained queue ownership and no GL error.
  `performance-421 vbo` reports the live pool capacities without reading its GPU bytes.
  Performance reports include overlapping upload, compaction and growth timers.
- `terrain-cache-420` compares native render-region reads across every mesh-border
  cell, tests active-mesh invalidation after a real block edit, and checks that
  dynamic custom chunks bypass the cache. It restores its single edited block.
- `performance-420 start LABEL [SECONDS]` records actual world-frame CPU/GPU,
  frame intervals, ticks, terrain builds, map work, chunk disk reads, generation
  and population. The default is ten seconds; reports stay in the clone under
  `power-beta-data/reports/performance-420`. GPU queries never block and sample
  storage is bounded. `turn LABEL` rotates two degrees per tick; `travel LABEL`
  requires an already flying or Spectator player and moves half a block east per
  tick. Both restore their owned pose on completion. Run the same travel twice
  after settling to distinguish new terrain from explored terrain. Use `status`
  or `stop` to inspect or cancel. Keep the disposable window focused and use a
  real GPU for performance comparisons; the report records its renderer.

- After `new-world`, run `spectator-inventory-404`, `spectator-containers-404` and
  `commands-403` for 4.0.4. These check blocked drops, retained full-data cursors,
  immutable native-style container views, fuzzy token replacement and captures.
  The old spectator-inventory-401 command now follows the no-dropping behavior.

- After `new-world`, `release-322 ui` checks direct Options return, manual drafts and
  the single-row zoom footer. Use `sidebar-prepare`, physical Tab hold, `sidebar-held`,
  physical release and `sidebar-released` for sidebar input and the world-only Mobs lock.
  For both `flight-creative` and `flight-survival`, hold W, tap left Ctrl, run `land`,
  wait for native ground collision, then `land-check`. Release W and run `stopped`.
  These phases edit only the disposable test world.
  After saving Mobs On and restarting the clone, `release-322 lock-read` checks persistence.

Build after the root JAR with `./gradlew -p validation-mod --no-daemon build`.
Install the resulting validation JAR only in a disposable Power Beta clone.
Fixtures can replace settings and edit worlds. They are never part of the export.
Mute the clone before launch and route any audio checks to a silent sink or capture device.

After `new-world`, `release-321 ui` checks direct Minimap sidebar entry, narrow/disabled
fallbacks, manual drafts, native Escape/reopen/Cancel/Apply, toolbar ownership and slider
save boundaries. It also checks exact zoom levels, Undo, Defaults and dirty exits at narrow
and wide sizes. `release-321 audio` checks every Extra sounds preview and full decoding of
all twelve current bucket variants through the game's codec. Keep the clone on a silent
sink. `release-321 steps` checks native footstep thresholds, flight, multiplayer and the
live Freecam HUD provider. `release-321 zoom` checks exact decimal endpoints, bounded
terrain paints and retained coverage while moving during blocked target loading.
`release-321 zoom-write`, quit/restart, then `zoom-read` checks durable exact levels and
restores the original preferences. `zoom-watch`, a physical Zoom in press, then
`zoom-watch-check` records intermediate native rendered frames without per-frame I/O.


4.0.1 native checks, after `new-world`:

- `spectator-inventory-401` checks Creative permission switching, native item data, offhand, crafting, open-inventory dropping and closed/world restrictions. It captures the inventory views and restores fixture state.
- `sorting-401` retains historical pre-4.0.3 draft and geometry checks. Use `sorting-403` for the current sorting editor.
- `recipe-401` reproduces empty-offhand swapping with the book open, native dropping during a prepared fill, subsequent crafting and accurate capacity/busy/recovery feedback. Run in actual Survival. Its final controlled callback-failure check verifies rollback and failure shutdown.
- `held-light-400 mining` checks rendered lighting while breaking a real block with an offhand torch, including section seams and separate refresh/preparation budgets. Keep the muted clone focused until `held-light-400 status` reports completion.

4.0.2 checks, after `new-world`:

- `sorting-layer-402` retains historical hotkey draft/save expectations from 4.0.2. Use `sorting-403` for current immediate publication, modal ownership and captures.
- `commands-403` checks live Wooden Planks aliases, the last partial/complete suggestion row and native Tab completion without executing a command.
- `sorting-403` checks immediate hotkey layout/preset publication, slot double-clicks, the Item/Clear/Reset row, ordinary menu drafts and Escape returning to the same native container. Captures and a native render observer verify that modal layers skip the underlying inventory.
- Run `controls-403 open`, hold W and Tab in the disposable window, then run `controls-403 check` and release both keys. It checks actual held movement during recipe search, inventory mob-head priority, compact glyph bounds, both Creative Survival recipe-button styles, hidden Creative hotbars and pause-menu map/photo returns. `controls-403 cancel` restores an unfinished setup.
- `recipe-402` checks recipe-cell craft-one, grid ingredient removal, wheel quantity/inversion, durable 2x2 storage fallback, capacity refusal, native item data, callbacks and stale-plan cancellation.
- `container-offhand-402` checks offhand geometry and native ownership across every Beta container, double-click gathering, pickup refill priority, transfers, disabled extraction and narrow/wide captures.
- `fluid-sounds-420` requires the verified silent Pulse sink and muted client. It checks native lava/splash/swim conditions and particles, all eighteen registered decodes against the pinned native codec through EOS, previews, subtitles and selected-base settings without saving preferences. Codec evidence reports source granules and decoded durations separately because Beta's native decoder can omit tail packets or retain padding.
- `kept-load-420` checks native signed-byte count wraps, exact item/slot conflicts and source-evidence retention on unregistered scratch players. It leaves live recovery state untouched.
- `tool-whitelist-420` captures the Allowed tools picker at 320 and 640 pixels and checks isolated sword exclusions, Done and Discard without changing items or saved settings.
- `command-names-402` checks live registry parsing and completion for plain and explicitly qualified item/block names, preserving inventory and settings.

4.0.0 native checks, after `new-world`:

- `recipe-400 registry`, `crafting-native`, `furnace-native` and `visual` check native recipes, metadata, safe placement and wide/narrow UI captures. Run `furnace-rollback` last because it deliberately exercises failure shutdown.
- `inventory-400 native` checks offhand full data, death snapshots, journal-lock failure, slow durable writes and callback rollback. Run `persist-start`, quit and restart the disposable client, load the same world, then run `persist-finish` for process persistence.
- `inventory-tools-400 native` checks sorting, pointer-selected hotkey targets, matching transfers and native slot ownership. `headers` captures single/double chests at narrow and wide sizes, verifies button clearance and checks that native item tooltips cover the controls. `modal` checks Creative and recipe-modal input exclusion.
- `auto-tools-400 native` exercises tool selection/whitelists, instant-break exclusions, borrowed shovels, vein ownership, main/offhand restocking and cancellation through real native callbacks.
- `subtitles-400 native` checks muted sound events, native/freecam directional bases, lifecycle clearing and bounded event bursts.
- `saved-hotbars-400` checks all nine native rows, slow durable loading and foreign/permission cancellation. `death-restoration-400` checks owned rollback, foreign callbacks, pending claims and kept-player transfer.
- `settings-400 native` exercises actual Options resets in all three bases, nested HUD drafts, Undo/Discard/Apply, saved-hotbar retention and subtitle renderer visibility. It captures narrow and wide controls for review. `placement` checks default subtitle avoidance and the first drag/nudge plus Undo.
- `input-400 prepare CASE`, `state`, `check-held`, `check-repeat`, `check-partial`, `check` and `stop` support OS-driven Save/Load, repeat/release-order, row-priority and offhand checks. The fixture observes real production handlers without calling them. Preserve ownership until `stop`; it restores temporary bindings, inventory and saved user data.
- `offhand-actions-400 native` checks actual bow, ammunition, fishing, food, placement, bucket and map ownership through Minecraft's use pipeline. `visual` captures normal first/third-person frames and map equip transitions; wait until `status` reports completion. `stop` restores the captured state. Its temporary maps use memory-only storage and never allocate saved map IDs.
- `held-light-400 start` runs timed native renderer phases and writes captures and CPU/GPU measurements. Keep the disposable window focused until `status` reports completion. The fixture waits only for its own stereo texture reload. `capture-pair` runs a bounded multi-tile cold/warm pair; `capture-cold` and `capture-warm` check capture ownership.
- At the title screen, `world-tools-400 native` checks actual menu visibility, verified backup/copy, concurrency, unsupported data and symlink refusal. `create`, then `created`, opens its recreated test world; `cleanup` removes only files bearing that run's token.

Inspect failure counts as well as `COMMAND DONE`. Keep ownership records until fixture cleanup completes.

Write a command to `.minecraft/power-beta-validation.command`. Results append to
`power-beta-validation.log`; wait for `COMMAND DONE` and inspect failures.

- `creation-rules` now renders the actual Custom editor at narrow and wide sizes,
  checks isolated drafts, preset copying and world save/reload. At the title screen,
  `release-300-rule-editors` renders the same standalone editors at three GUI sizes,
  including a saved test world without opening it. `release-300-mining-air` exercises native empty
  clicks, temporary preference inversion, immediate held mining, unrelated cooldowns
  and multiplayer authority. `release-300-map-light` checks radius and scan budgets,
  native spawning floors, clipped overlay pixels and zero disabled survey work.
  Run mining and map-light checks only in a loaded disposable world.

- After `new-world`, `release-300-exact-frames` compares both border shapes pixel-for-pixel
  against reference atlas operations at four sizes and two scales. `release-300 refinement`
  also checks stationary visible updates behind missing tiles with one upload per frame.
  `release-300-ui-polish`, `release-300-waypoint-polish`, `release-300-sprint-polish`
  and `release-300-extra-sounds` check native picker modals, pause shortcuts, waypoint
  actions/drafts/commands, mode-owned stamina/FOV/HUD, and successful action sound events.
  `release-300-sprint-polish` also checks native-height stamina sprites, pause visibility,
  combined/separate HUD anchors and visibility, immediate drag/nudge behavior, preview
  pixels with gameplay disabled, Undo and Discard. `release-300-stamina show|pause|editor`
  temporarily arms a visible partial reserve for visual checks; `release-300-stamina restore`
  restores settings, world rules and player mode afterward.
  `release-300-freecam-commands` checks native chat selection/edit/undo, denied rules
  and camera-ray navigation in a restored loaded chunk. These checks restore settings
  and use only the muted disposable world.

- After `new-world`, `release-300-rerelease` checks active-tab highlight clearing,
  native minimap drag/release and Cancel, RGBA/wheel draft transfer and native release
  ownership at 320×180 and 640×360, minimap arrow policy and defaults. It also invokes
  the actual camera setup across walking phases and compares nearby/800m waypoint
  labels with bobbing on/off. Optional phases: `ui`, `hud`, `colors`, `camera`, `defaults`.
  Focus the muted disposable client first; preferences and native camera state restore.

- After `new-world`, `release-300 ui` checks the actual Related control/setting
  navigation and retained HUD draft, minimap appearance Undo/Cancel/Done and temporary
  toast suppression, nested RGB/opacity editing for every stored color type, World Map
  context actions, Waypoints modal ownership and native screenshot output. Individual
  phases are `navigation`, `hud`, `colors` and `map`. Captures use 1280x720 and, when
  the isolated display is large enough, 5120x2160. Resize and repeat `map` if the wide
  check reports SKIP. Reports stay in `power-beta-data/reports/release-300`; the native
  map screenshot also stays in the clone's `screenshots` folder.
  `caves` checks native loaded-column floors, thick ceilings, exact same-layer height
  namespaces and surveyed void, restoring every edited block. It also checks fast/fancy
  native tree canopies versus solid roofs and verifies full-bright toggle/restore pixels
  in retained minimap, World Map and HUD-preview buffers without rewriting terrain. `sprint` checks real
  Creative/Survival takeoff, held/released sprint keys at 100% ground speed, actual
  flight displacement, `/fly` reset order, movement/menu cancellation and denied flight.
  `background` checks independent minimap/World Map buffer release, immediately inert
  sampling/palette/radar/path, bounded dirty-cache retirement, zero accepted worker tasks
  across fresh all-off ticks, independent world/death waypoint storage and explicit
  Waypoints loading. It polls once per native tick and restores the original screen and
  preferences after `RELEASE300 BACKGROUND COMPLETE`; `COMMAND DONE` only confirms
  dispatch. `background-stop` restores immediately. `state` includes this phase and the
  accepted-task counter. Captures temporarily use consistent native/enhanced GUI scale
  2 and restore it without saving; test actual scale 4 separately at the native window.
  `map-interactions` checks scale-line native pointer offsets/release ownership, the first
  destination Clear hit target, four-second waypoint deletion confirmation and notice expiry.
  It shadows waypoint state and restores it before any store save. `map-interactions focus-arm`
  leaves a scale drag armed; move focus physically away, then run `map-interactions focus-check`.
  `map-interactions paused` observes real native ticks until paused world time remains fixed
  while terrain sampling progresses; wait for `RELEASE300 MAP PAUSED COMPLETE`.
  `map-interactions stop` restores an armed interaction screen without verification.
  For native 5K drag timing, open the map, run `framecap 60` then `pan-start`, perform
  a continuous physical left drag, release and run `pan-check`. Repeat at `framecap 125`.
  The 1024-frame in-memory ring reports actual map render FPS, native tick rate, distinct
  view-update Hz and median/p95 frame/change intervals; CSV/JSON are written only by
  `pan-check`. It asserts view updates exceed 20 Hz and distinguishes slow full-map
  rendering from tick-bound panning. `framecap restore` restores the original in-memory
  cap. No cap command saves preferences. `state` reports the OpenGL vendor/renderer.
  Prefix these commands with `release-300 `.
  Focus the muted isolated client first. Keys, settings, player data and world rules
  restore afterward. `state` prints read-only map/cache/worker/sprint metrics;
  `open map|hud|waypoints` leaves the requested UI open, and `capture NAME` saves its
  current logical size at 2x resolution when the isolated display has room.

- After `new-world`, `release-261 terrain` checks native shallow/deep water, glass,
  ice and thin/full snow in loaded columns. `cadence` checks 1/5/20 Hz sampling,
  periodic native edits and column budgets using disposable caches. `pose` checks
  interpolated native positions and wrapped heading without changing simulation.
  `render` checks the minimap FBO and GL restoration across GUI scales, shapes,
  rotation, opacity and terrain styles, followed by native text rendering.
  `textures` runs the native texture-pack switch/restore check and needs the bundled
  `1.14 Textures.zip`. Prefix each phase with `release-261 `.
  `rules` runs `/gamerule allow_sprinting true` and `/gamerule allowSprinting false`,
  verifies each durable metadata save and immediate cancellation with chat open,
  checks unchanged global settings, other rules, difficulty and player mode, then
  restores and verifies the original rules. It does not prepare the map scene.
  `scene` prepares water, glass, ice and snow patches at X/Z 16–31, Y 105, and opens
  the HUD for screenshots. `open` opens the world map; `state` reports its cache,
  column count and errors. These fixtures replace blocks only in the loaded scene
  chunk and leave the scene/map preview preferences available for inspection.

- `release-260 menus` checks Map/HUD routing, isolated drafts, preview rollback and
  sprint defaults. After `new-world`, `map` checks loaded-only sampling, native
  terrain edits, caves and GL restoration; `sprint` compares actual native travel,
  jump impulses, cancellation and stamina NBT. Prefix every phase with
  `release-260 `. Use `save`, restart and `saved` for waypoint/stamina persistence;
  `open` and `state` support physical map/HUD input and bounded-cache checks.
  `boats` verifies that using a held boat on empty or occupied boats preserves native
  interaction and the held item in Survival and Creative.
  `ui` checks pointer closure, context ownership, coordinate validation and border
  export; `effects` checks native dust, combat knockback and independent sprint FOV.
  `lifecycle` checks actual Nether travel and retained data while maps are disabled;
  `textures` switches native texture packs and verifies restored map colors.
  `perf` measures bounded sampling, radar and drawing during repeated traversal.
  `native` and `raw` select GUI mouse helpers for physical map gestures.

- After `new-world`, `release-251 boats` checks native item placement with the fix
  off/on, both steering and speed settings, source/flowing-water blocks, cardinal
  and diagonal bearings, first-tick interpolation, actual mounted forward movement,
  multiplayer authority, defaults and durable Apply. Restart the muted clone and run
  `release-251 saved` to verify the saved Off preference.

- After `new-world`, `water-253` compares actual player ticks and displacement
  with modern water response, including currents, bindings, modes and live changes.
- `swimming-300` checks official 1.21.1 water/land sprint equations against native
  ticks, all four water-setting/rule combinations, pitch/Jump/descend, one-block
  tunnels, surface starts and exits, forced crouch, revocation, save/reload,
  native player/armor model rendering, particles and unloaded-terrain checks.
  Focus the muted disposable client first. The fixture restores its scene blocks,
  player, keys, world rules and preferences; it never saves global settings.
- After `new-world`, `release-253` exercises native Creative boat/cart press and
  held-use paths, quick clicks, native item preservation, Survival use, ridden-boat
  frustum/occlusion rendering and cape alignment. Run `release-252` for cart and
  inventory regressions. Use only the muted disposable clone.
- After `new-world`, `release-252` checks Spectator/freecam selection and targeting,
  unchanged access rules, moving ridden-cart rendering across culling samples with
  either speed, native armor Shift-drags, cursor/NBT/count preservation, slot limits,
  full inventories, disabled gestures and cancellation.

- `release-250 setup` checks the fresh first-launch chooser and one-time save.
  `defaults` checks all three complete loads, 4x GUI scale, reset bases and delayed
  foreground tooltips, and exports their full setting comparison; `ui` checks nested F3
  selection, range dragging, presets, exit prompts, compact toolbar and pane visibility,
  launcher search/reset behavior, scrollable Defaults choices and responsive rule editors.
  `audio` checks skip labels. After `new-world`, `world` verifies existing rules
  survive default loads and captures the title reset modal with world depth enabled.
  Restart and run `saved` for durable preset/setup checks. Use a silent output.

- `release-242 ui` checks Interface grouping, responsive toolbar rows, preview-click
  selection, draft isolation and centered creation rules. After five seconds,
  `release-242 selection` checks persistent selection and deselection through empty
  preview space, another element, setting labels and controls. `release-242 audio` checks
  waiting-track stability through 80 settings edits, exclusions and playback.
  After `new-world` and `title`, `release-242 saved-rules` checks centered saved-world
  rules without metadata writes. Use a muted disposable client and silent output.

- `release-241 ui` checks Interface search placement, transient preview toggles,
  responsive layout, editor memory, linked F3 resets, Clear logo/Undo and entity tags.
  `gameplay` checks pause across all dimension policies, native dimension travel,
  paused auditions and the initial photo zoom. `drag-setup` reports native mouse
  coordinates; perform real drags, then `drag-check` and `drag-save`. Restart and run
  `saved`, load the world and run `live` to check both persisted columns.
  `hotbar-setup`, hold F8 until `NATIVE HOTBAR PREPARED`, release, then `hotbar-result`
  checks first-release-tick publication. Run `release-171 hotbar` for rollback,
  cancellation and stalled-disk coverage. `forest`, `tag-project` and `tag-hide`
  prepare the native forest/cow capture and record its native entity-tag projection.

- `release-240 hud` checks search editing/navigation, responsive previews, draft isolation,
  individual resets, title grid sliders, click sounds and temporary F3 reorder/Undo/gaps.
  `release-240 menus` checks title versions, logo clicks while the settings writer is
  stalled, Interface rows, native world rename and metadata preservation. Create a
  disposable world first, then return with `title`. Run `save`, restart and `saved`
  through `release-240` for F3 ordering/visibility/size persistence.
- `release-240 forest` locates untouched generated Forest terrain with 12-chunk
  rendering, noon and clear weather. Capture the settled native window at 2560x1440
  without overlays or image edits for the HUD asset.

- `release-230 refinements` checks native title clicks through every preset/style, reverse cycling and retained retro data; inline custom version text; a continuous panorama clock; and pixel-identical native dirt headers/footers.
- `hud-responsive-230 layout`, `ghosts` and `drafts` cover responsive splits, the background switch, delayed tooltips, disabled-cluster outlines and dragging, individual resets with Undo, and nested color isolation with automatic/manual Apply and Save/Discard. Use an isolated display and client of at least 3440×1920 for the large captures.
- `hud-responsive-230 forest` prepares an anonymous forest-biome scene for a clean native-renderer background capture. Use a muted disposable instance at 2560×1440; keep the HUD hidden and first-person view. Capture only the scene, never menus or fixture output.
- `release-230 menus` checks native/raw pointer preservation through menu returns in windowed/fullscreen modes, native music-button and status-link dispatch, complete HUD selector coverage, shared group collapse/expand, all four reset scopes through the Undo button, audited HUD previews and modal input. Run on the isolated display after `set native.music 1`, retaining Master volume zero and the muted null output.
- `release-230 check` exercises HUD dragging and every editor tab, grayscale dropdown pixels, paint/erase strokes, filtered instant brush selection, swatches, inline rotations, individual appearance resets, divider resizing, preset save/load/removal, undo, draft warnings, Apply/Discard and main-menu panorama backgrounds. It captures narrow and wide screens under `reports/editors-230`. Run `save`, restart, then `saved` for editor persistence.
- `light-performance-230` compares batched light numbers against the previous native font renderer at all four rotations, checks pixel differences and records median/p95 draw timings for 2,048 labels. Run `overlay-setup`, `overlay-check` and `toggle-saves` for native light sampling, GL restoration and deferred writes under a stalled disk. Use a muted disposable world.

- `release-220`: flight units/order/defaults, retired switches, settings/library searches, in-group divider geometry at narrow/wide sizes, reported command origins, mode-aware camera collisions and solid/translucent head draws. Run after `new-world`.
- `release-211`: daytime and nighttime bed respawn, native sleeping and bed occupancy, HUD order, merged panorama settings, live preview, immediate selection and manual Back/Escape. Run after `new-world`. Then use `title` and `release-211-editor` for the native Select world > Edit route, dirty-exit confirmation, durable saves, preserved world name/player data and future-format recovery UI.
- `title-alignment`: captures every title style at narrow, wide and odd GUI widths, measures the rendered lettering against the screen center and verifies retro customization is preserved. Requires a muted disposable client at least 1280×840 pixels.
- `release-210`: world-rule sliders/defaults, retired globals, HUD group visibility, Creative/Survival/Spectator `/fly`, native non-flight gravity/jumping/walking, permission revocation, saved native flight data, sleep thresholds, immediate panorama selection, official texture uploads, all title styles and narrow/wide picker screenshots. Run after `new-world`.
- `release-202`: camera/leaf reset defaults and actual saved-world Creative badges,
  including old cheats, Spectator-only and unsupported metadata without changing saves.
  `freecam-motion setup|start|stop` records bounded frame timing, camera movement and
  rendered origins for physical ascent/descent checks; reports stay in the clone.
- `release-201 check`: patch defaults, retired controls, chest choices, percent editing,
  creation geometry, native player/mob trampling with every footwear class and server
  ownership, and front-view/freecam HUD combinations. `keys` and `state` support
  physical F4 and F3+F4 checks. `export-icons` captures the bundled grayscale
  mode icons with the native renderer; this is an offline asset-generation command. Run after `new-world` in the muted clone.

- `world-rules`, `rule-behavior`, `creation-rules` and `quick-transfer`: rule storage,
  permissions, native gameplay, searchable creation drafts and item/NBT interactions.
  Run after `new-world`. `rules-restart prepare`, restart the clone, then
  `rules-restart verify` checks all rules, difficulty, modes, dimensions, presets
  and crafting inputs with native item data and full counts.
- `compatibility-defaults`: crafting-input persistence and legacy reads, live
  door/sign stack limits, and vanilla-server overrides without changing preferences.
- `python3 validation-mod/tools/quick_transfer.py GAME_DIR --display :98` checks
  real Shift-double-click gestures with native/raw input, both release orders,
  cancellation and matching-item conservation. Use the isolated muted Xvfb clone.

- `water-terrain`: first place the disposable camera near X/Z 32/32 and let its render
  grid settle, so the fixture section at 32/80/32 exists. Inspect GPU color endpoints, 1000 crossings without queued builds or
  uploads, slider preview/Cancel, shader uniforms, night/roofs and native terrain edits,
  including torch-lit deep water at dawn and roof changes below native light's reach.

- `release-176 camera`, `flight`: rendered-camera water fog/FOV, third-person distance,
  free look, stereo/freecam, separate darkness preview/Cancel and actual auto-flight
  displacement with manual camera input isolated. `save`, restart, then `saved` checks
  independent persistence. `scene-above` and `scene-water` prepare visual comparisons.
  `scene-zoom`, hold F6, then `zoom-check` verifies real zoom input. `scene-walk`,
  unfocus the test window, then `focus-check` verifies inventory flight displacement.

- `release-175 oxygen`, `lighting`, `flight`: oxygen refills, native Survival drowning,
  bubble suppression, day/night and roof lighting, unchanged simulation light, render-thread
  isolation, live preview/Cancel, and frozen-flight animation continuity. Run after `new-world`.
  Use `save`, restart, then `saved` for underwater-darkness persistence.

- `release-171 hotbar`: blocked journal writes, commit ordering, native NBT, bounded
  rapid input, selection preservation, inventory edits, menus, focus, world/player
  changes and submission/commit timings. `failure` injects a full journal and verifies
  failure shutdown; restart afterward. `camera` checks floor/slab head coordinates,
  spectator terrain clipping and free look. `fire` checks creative fire priority on
  every face, held mining and mode permissions. Run after `new-world`.
  `spectator-scene` and `freecam-floor` prepare actual renderer captures.
  Include `freecam-handoff` to check close-range player-body rendering.

- `interface-170 check`: shared F3/HUD eye/feet/head-base policy, ghost visibility,
  non-cheat full bright, search chevrons and cluster labels, native/custom text selection,
  and independent UI cursor preview/rollback. Run after `new-world`.
  `ghost-freecam`, `ghost-spectator` and their `-look` variants prepare visual head checks.
  `text`/`state` support real mouse/clipboard checks with native and raw input.
  `keys`/`key-state` bind F20 to the temporary click-mining override for physical input checks.
  Use `save`, restart, then `saved` to verify persisted preferences and cleared temporary state.
- `mining-170`: actual torch/shovel mining, most-worn tool selection, breakage and exclusions;
  vein time percentage/minimum, obsidian-speed composition, per-block wear, tool breakage,
  limits, cancellation, torch integration, fire spread, and temporary click-mining lifecycle.
  Run in a focused disposable client after `new-world`.

- `release-162`: creative preview reach in survival, creative, spectator and freecam,
  independent block/entity limits, interpolation and occlusion; slab settings placement;
  F3/custom HUD camera coordinates, facing, biome, chunk, light and slime-chunk data;
  unloaded terrain and immediate restoration on exit. Run after `new-world`.

- `freecam-preview check`: detached chest, double-chest, furnace, dispenser and
  minecart targeting, obstruction, reach, unloaded chunks, native NBT and HUD
  entry/exit restoration. Run after `new-world`. `setup` binds C to freecam and
  P to preview, then enters freecam facing a test chest. Use `state`, `hud-hidden`,
  `normal`, `off` and `exit` for physical F1, preview-key, focus, menu and world-exit checks.
  `python3 validation-mod/tools/freecam_preview.py GAME --display :N` runs those
  physical checks on a muted Xvfb clone and captures the actual rendered HUD/preview.
  Requires ImageMagick. Inspect the menu/focus captures for unwanted preview panels.

- `release-152 check`: forward/backward auto-walk, cancellation, free look,
  freecam, inventory setting transactions and narrow/wide GUI bounds, including
  creative tabs and destroy slots. Run after `new-world`. `setup`, `state`,
  `camera-on`, `camera-off`, `cursor`, `look-off`, `look-on`, `invert`, `chord`, `plain` and `menu`
  support physical key/mouse checks. Use `save`, restart, then `saved` for persistence.
  Run the inventory mouse checks with both `raw` and `native` helpers; `raw-delta`
  injects a device delta to check gameplay and stale input after GUI closure.
  Check that only presses outside the GUI start looking, captured drags continue
  across the GUI with a hidden cursor, and release restores the cursor position.
  Check focus/screen changes and feature toggles while held; these cancel until a
  new press. `creative` and `survival` switch the disposable player for GUI checks.
- `audio-previous setup`, `start`, `active`, `second`, `state`, `gap`, `again`,
  `paused`, `preview`, `gap-preview`, `finish`: actual Previous controls during
  playback and natural gaps, history, previews and queue preservation. Supply
  two short silent OGG files named `song_1.ogg` and `song_2.ogg` under
  `power-beta-data/validation/audio-previous-152`. Wait for a blank track with a
  positive delay before gap checks. Use a muted null sink before enabling test playback.
- `release-151 check`: TNT ignition modes, collision equivalence and live toggles,
  custom collision boxes, chunk movement/removal and resumable merging with changing
  native NBT. Run after `new-world`. `release-151 fast` and `release-151 slow`
  compare dense-item ticks, explosion work and bounded merging up to 8,192 items.
  `release-151 text` checks the loaded F3, status and chat colors through actual
  glyph pixels; run immediately after launch. `release-151 light` checks full-bright
  occlusion samples and unchanged simulation light. Use `release-151 save`, restart,
  then `release-151 saved` to verify the two new settings persist.
- `release-151 sphere setup` builds a ground-level `//sphere tnt 5` scene. Wait for
  WorldEdit to finish, then run `release-151 sphere hidden` or `visible`. Append
  `-unmerged`, `-slow`, `-unlimited` or `-unprotected` to compare each setting's off
  state. Wait for `SPHERE DONE` after 600 ticks before changing the scene; the report
  includes item/TNT counts and entity tick/render median, p95 and maximum timings.

- `freecam-distance`: dropped-item and block-entity render distance follows the
  detached camera, including player movement mode, exit and disabling the feature.
  Run after `new-world`. `worldedit-navigation` also checks pillar sides, top faces
  and a corner crossing that the old quarter-block ray missed.
- `audio-exclusions`: library and preset filters hide individual excluded songs,
  preserve live and draft selections, retain queue requests, restore view state and
  share row geometry across narrow and wide layouts.

- `world-settings-check`: difficulty transactions, scope notes, permission-gated rows/tabs,
  command exemptions, input priority and Peaceful behavior. Run after `new-world`.
  `world-settings-create` creates four disposable saves through the real creation UI,
  checking the initial Creative/Survival mode against the rules preset, resize, save/reload
  and isolation from the previous world's mode or later rule edits.
  `world-settings-reload` checks those saves across switching, dimensions and restarts.
- `cleanup`: current config, world-cycle persistence, simulation and scheduled
  updates, sleep combinations, sign editing, spectator exits, item validation and
  inventory screenshots. Run after `new-world`.
- `sign-editing`: Escape and Done save/close new and reopened standing/wall signs,
  retaining all four lines and resetting key repeat. Run after `new-world`, with
  sign text improvements enabled and again after restarting with them disabled.
- `obsidian-mining`: slider endpoints, Cancel/Apply, tool and block exclusions,
  multiplayer guard, airborne penalty, real mining ticks, drops and durability.
  Run after `new-world`. For restart coverage, use `set tweaks.obsidianBreakingSpeed 61`,
  restart, and inspect `dump` before resetting to 0.
- `water-landings`: shallow source/flowing/falling water, actual gravity, bucket
  clutches, hitbox edges, fast passes, mobs, dry/lava damage, multiplayer authority
  and Cancel/Apply. Run after `new-world`, then restart and run `water-landings saved`.
- `bucket-stacks`: stack limits, native pickup/clicking/storage, fluid use, cow/squid
  milking, overflow drops, creative reuse, existing stacks while disabled and
  Cancel/Apply. Run after `new-world`; restart, run `new-world` to reload the save,
  then `bucket-stacks saved` to check settings and inventory/storage persistence.
- `freecam-handoff`: compare the actual rendered world-view matrices before and
  after the hotkey while walking, with bobbing, interpolation, sneaking, third
  person, vertical views and anaglyph eyes. Check first-frame timing, continued
  auto-walk, normal exit, first-frame player visibility and stale-pose rejection.
  Run after `new-world`.
- `death-checks`: real death causes and drops, coordinates, inventory/equipment
  snapshots, NBT/damage/full counts, queries, cheat permissions in all modes, ownership,
  full-inventory cancellation, repeated recovery and multiplayer exclusions.
  Run after `new-world`; restart and run `death-checks saved` after `new-world`.
  Run `death-checks rollback` last for partial-write rollback and failure shutdown.
  Restart, load the same save and run `death-checks retry` to recover through the inventory GUI once.
  `death-checks inventory` checks original slots, armor, overflow, occupied slots,
  native NBT, locked viewer inputs, individual/bulk claims, live permission changes,
  decimal IDs, chronological logs and narrow/wide screenshots. Restart and run
  `death-checks inventory-saved` after `new-world` for slot and partial-claim persistence.
- `keep-inventory`: cheat gating, Apply/Cancel, world NBT and dimension copies,
  real deaths/drops, inventory/cursor/crafting preservation, kept log and locked GUI,
  native NBT/full counts and death-record independence. Run after `new-world`.
  Use `keep-inventory save`, quit while dead, restart and run `new-world`, then
  `keep-inventory reload` for a complete dead-player restart and respawn check.
- `help-audit`: index and completion coverage, aliases, optional features, world
  rules, multiplayer/operator access and safe help for every available command.
- `toggle-messages`: all eleven defaults, live providers, HUD independence,
  duplicate suppression, menu silence and Cancel/Apply. Run after `new-world`.
  Use `toggle-messages save`, restart, then `toggle-messages reload` to verify
  persistence and restore every message switch to off.
- `toggle-performance`: compare toggle and HUD timings with the HUD enabled and disabled.
  `toggle-saves` checks immediate state while the disk writer is blocked, linked values,
  held keys and menu Cancel/Apply. Run after `new-world`. `toggle-saves setup` binds
  K to fast placement and J to edge protection; `setup-chord` adds Ctrl to K and F7
  for both release-order checks. `toggle-saves state` reports live state.
  `toggle-saves save` queues settings and closes the client without an explicit flush.
  Restart, run `new-world`, then `toggle-saves reload` to check and restore those settings.
- `worldedit-hand`: held variants, command snapshots, masks, inventory preservation,
  undo/redo, invalid hands and cheats. Run after `new-world`.
- `worldedit-navigation`: repeated jumps across chunk boundaries up to 255 blocks,
  real world rendering, safe cancellation, chunk comparator contracts and culling
  reads without chunk generation or canceled unloads. Run after `new-world`.
- `death-crafting`: four crafting-input snapshots, normal drops, native data,
  original-slot GUI recovery, locked views, conflicts, overflow and narrow/wide UI.
  Restart, run `new-world`, then `death-crafting saved` for partial-claim persistence.
  Run `death-crafting rollback` last for a failed recipe callback, then restart and
  run `new-world`, `death-crafting retry` for protected GUI recovery exactly once.
- `creative-vehicles`: one-hit deletion of boats and every minecart type, loaded
  inventories, repeat attacks, survival drops and spectator/multiplayer guards.
- `release-110`: history/caret/completion, spawning rules and Apply/Cancel, NBT and
  dimension copies, summon exemption, WorldEdit states/undo/redo, calculator and F3 setting.
  `release-110-pick` checks redstone in creative and survival. `release-110-flight`,
  `release-110-freecam` and `release-110-state` support physical movement and clipboard
  checks. Test both sprint-key release orders, focus loss and menus.
- `release-111`: cursor/color defaults, preview, Cancel/Apply and world override groups.
  Restart and run `release-111-saved` for persistence. `release-111-debug-on`, then
  `release-111-debug` checks facing, music and platform rows for overlaps.
  Include `release-110-spectator` in physical sprint checks for every axis.
- `portal-inventory`: default, Cancel/Apply, warm-up, screen exclusions, item counts,
  damage/NBT, and survival/creative/spectator travel with source-world cleanup.
  Run after `new-world`. Restart, load the same save with `new-world`, and run
  `portal-inventory saved` for preference and carried-item persistence.
- `crafting-grid`: crafting-table transfers, 2×2 exclusion, NBT/damage, partial/full
  grids and Cancel/Apply. `crafting-grid rollback` injects one partial-transfer failure
  and checks restoration/shutdown. Restart and run `crafting-grid saved` for persistence.
- `freecam-walk`: both activation orders, independent camera controls, opposing keys,
  player movement mode, exit and menus. `freecam-walk physical`, `state` and `focus`
  support physical input and focus-loss checks. Run these fixtures after `new-world`.
- `low-fire`: default, Cancel/Apply and pixel comparison through the actual fire renderer.
  `damage-camera`: burning, fire contact, other hits while burning, lava, global toggle,
  unchanged health/timers and death tilt. Restart and run each with `saved` for persistence.
- `auto-mine`: settings, lifecycle cancellation and live HUD state. `auto-mine setup`
  assigns M and prepares a stone wall for physical input. Use `state`, `mined`, `off`,
  `menu` and `focus` to check actual held attacks, tool wear and cancellation. Test both
  chord release orders and held keys across focus changes in the disposable window.
- `release-300-chat-break [all|chat|break]`: native wrapped/scrolled history selection,
  Ctrl+C and independent chat input; rapid Creative presses, held-repeat delays and
  unsupported, Survival and multiplayer cooldowns. Focus the disposable client first.
  The break fixture needs three empty blocks above and beside the player.
  `physical-setup` saves state and places three stones along the native view ray.
  Send two real left-button press/release pairs less than 200 ms apart, then run
  `physical-state` to check native dispatch and one break per press. Always finish
  with `physical-cleanup` to restore blocks, player state, mode, inventory and rules.
  `chat-physical-setup` selects a native history line and saves chat/clipboard state.
  Send a real left or right Ctrl+C press/release sequence, run `chat-physical-state`,
  then `chat-physical-cleanup`. Setup also prints GUI coordinates for a real selection drag.
- `command-completion`: arrow selection, immediate Tab completion and subsequent
  typing for WorldEdit and singleplayer commands.
- `chat-wrap`: all chat colors, repeated wraps, color changes, resets, plain text
  and the WorldEdit cheats warning. Run after `new-world`.
- `freecam-culling`: entity/block entity visibility, repeated toggles, player movement,
  setting disable, preserved culling preferences and world exit. Run after `new-world`.
- `creative-borders`: compare catalog row pixels and the destroy-slot junction
  at narrow and wide GUI sizes; screenshots go under `reports/creative-borders`.
- `creative-drops`: click outside the diamond tab at narrow and wide GUI sizes;
  check whole-stack and single-item drops, damage/NBT, hotbar pickup, UI borders,
  view/category tabs, destroy-slot borders and closure without duplication.
- `creative-paint`: middle-button previews and release in catalog, inventory,
  armor, chest, crafting table, furnace and dispenser; stack limits, damage/NBT,
  incompatible/output slots, cancellation and ordinary clicks. Run after `new-world`.
  Restart, load the same save and run `creative-paint saved`. Run `rollback` last
  to check partial-write recovery and shutdown until restart. `setup`,
  `physical-check` and `focus-check` support real middle-button and focus-loss checks.
- `redstone-visuals`: default, preview, Cancel/Apply, world-scope labels and all
  sixteen power sprites with default and Faithful textures, including four-pixel
  wire widths and shaded edges. Inspect the screenshots
  under `reports/redstone`. Run these four fixtures after `new-world`.
  For restart coverage, use `set visual.redstonePowerLevels true`, restart, inspect
  `dump`, then reset it to false.
- `redstone-sounds`: optional stone placement sound, survival/creative placement,
  failed placements, volume category and Cancel/Apply. Run after `new-world`.
  For restart coverage, set `audio.redstonePlacementSound` to true, restart and
  inspect `dump`, then restore false.
- `audit`, `roundtrip`, `screens`: schema, save/restore and multi-resolution checks.
- `settings-snapshot NAME`: capture all sixteen tabs, their complete row order,
  catalog and PNGs under `power-beta-data/reports/settings-review/NAME`. Use a
  disposable client window at least 1280 x 840 pixels. Captures include every
  scroll position at 640 x 420 GUI size and the first view at 320 x 240.
  Capture `before` in a world before updating the clone, then `settings-review`
  checks retained IDs, defaults, ranges, row coverage, binding groups and links.
- `new-world`, then `indicators-hud` and `indicators-creative`: live HUD states,
  inclusion/Cancel, eight catalog rows, hotbar transfers and recipe tooltips.
  `indicators-menu` checks title controls and layout. `indicators-mix`, `indicators-world`
  or `indicators-custom` starts a track; wait for playback, then `indicators-exit` and
  `indicators-playing` or `indicators-stopped`. `indicators-exit-paused` and
  `indicators-paused` check pause retention. All action names use the prefix.
- `movement-keys`, then `new-world`, `movement-setup`, `movement-checks`: key mapping,
  container reach and item conservation. `movement-inventory`, `movement-chest` and
  `movement-state` support physical-input checks in the disposable client.
- `audio-maintenance`: preset-pool isolation, folder browsing, draft confirmation,
  duplicate imports, single-group expansion and FFmpeg detection. This fixture
  expects FFmpeg on PATH; the unit suite covers an unavailable executable.
- `audio-presets-setup`, `audio-presets-ui`, `audio-presets-finish`: preset editing.
  Playback steps in `AudioPresetChecks` need separate ticks between commands.
- `audio-groups-setup`, `audio-groups-ui`, `audio-groups-menu`,
  `audio-groups-finish`: group gains, layout and pause controls.
- `new-world`: create an isolated test world; `options PAGE`, `pause`, `title`
  open screens for inspection. `quit` closes the test client normally.

See the dispatch in `Validation.java` for focused inventory, commands, navigation
and input fixtures. Some older checks assume a particular setup; inspect their
preconditions before running them. Physical input tests require focused game
input and both modifier release orders.

- `gallery setup`, then `gallery worldedit`, `chat`, `sign`, `redstone`, `preview`,
  `carry` or `view` stages anonymous feature screenshots in a new disposable world.
  Prefix each action with `gallery `. The setup replaces nearby terrain.

For isometric capture, bind Isometric screenshot in Options, test chunk distances
2 and 8, and capture again after lighting and chunks finish updating. At scale 4,
expect 640 × 832 and 2176 × 1600 PNGs. Check negative coordinates, repeated
captures, and normal gameplay rendering after capture.

## Release 1.5 checks

- `release-150` checks settings, item merging and native item data, explosion
  protection, TNT breaking, normal/fast leaf decay across modes, catalog
  transfer, entity visibility, full bright and `/spawn`.
- `interaction-150` checks actual catalog Shift-clicks, stack settings, native
  item data and 4,096-item merging with conservation and save checks.
- `release-150 spawn-dimension` checks returning from the Nether.
- `release-150 save` and `release-150 saved` check settings across a restart.
- `tnt-performance` measures native detonation and fuse work with limits off/on.
  `tnt-performance save`, restart, then `tnt-performance reload` checks pending
  TNT recovery. `save-disabled`, then enabling `gameplay.limitTnt` at the title
  screen before reopening, tests main-menu recovery. Dispatch the reload check immediately after opening the world.
- `render-150 setup` binds J to full bright and K to entity visibility for physical
  input tests. `chord` adds Ctrl; `on`, `off`, `hidden`, `visible`, `spectator`,
  `creative`, `disable`, `menu` and `close` exercise state transitions.
- `audio-150 setup`, `preview`, `next`, `check`, `finish` checks preview restoration
  and song-label destinations. `menus` covers title/pause controls; `stress` and
  `alive` check rapid decoder cancellation. Run phases promptly on a silent output.
- `gallery-150` prepares separate scenes and captures real renderer screenshots.
  Wait for chunks to render before `gallery-150 capture NAME`.

- `gallery-web SCENE` prepares anonymous website scenes in separate locations.
  Menu actions include `menu General`, `search`, `modes`, `death`, `chat` and
  `sign-editor`. Use `gallery-150 capture NAME` after the rendered view settles.
- `gallery-detail ACTION` prepares detailed website views: `placement` and
  `placement-check`, `fence`, `slabs`, `conflicts`, `conflicts-filter`,
  `chat-select`, `chat-cursor`, `pause-music`, `music-seek`, `music-queue`,
  `items`, `items-merge`, `items-hidden`, `items-visible`, `buckets`,
  `textures-on`, `textures-off`, and related settings views. Hold G for the
  diagonal placement preview. `iso-scene` followed by `iso ANGLE` captures
  native isometric renders at successive angles for an orbit animation.
  Use a muted disposable clone and wait for rendering before each capture.
  These fixtures change worlds and settings; use only in a muted disposable clone.

- `gallery-motion section PAGE|GROUP;GROUP` opens a related-settings view for a
  feature. `scroll` records native scrolling or setting tooltips for short groups.
  `page N` exposes the remaining settings pages for modal screenshots. `pan` moves
  the player view; `freecam` flies the detached camera out from the castle. `stop`
  ends the current motion. `creative N` and `creative survival` open catalog
  categories and the survival inventory. `music-all` and `music-presets` open the
  corresponding native library views.
- `gallery-action` stages gameplay recordings for the five `placement MODE`
  previews, `roof` and `roof-row N`, `boat`, `minecart`, inventory transfers and
  equipment swaps, leaf decay, gold tools, water landings and explosion-protected
  items. Use physical input to demonstrate placement, mining and vehicle controls.
  For the mode picker, hold its modifier while recording. Water landings start
  on a diving ledge; walk off it with edge protection disabled. Prefer recordings
  that show the actual operation, such as carrying and placing a chest or pasting
  a copied selection.
  Texture captures must wait for the atlas reload to finish before recording.
  Keep a static poster for every animated gallery view and inspect recordings
  before publishing them. Never ship the fixture JAR.

- `gallery-scene ACTION` stages focused native website demonstrations, including
  the hotbar selector, slab completion, tunnel mining, sleeping, modern stacking,
  spectator speed, leaf decay and vehicle acceleration. Inspect both gameplay
  state and framing before publishing. Keep full captures before cropping previews.
  Camera fixtures include a small positioning margin above native collision surfaces;
  decimal eye-height rounding must never put a standing player inside the floor.
- Main menu recordings scroll immediately for four seconds. Capture each complete
  tab in the `settings-layout.json` order; do not animate settings-only modal extras.

- `release-160 check` checks tab collapse, cluster drafts and Cancel, HUD offsets,
  F3 daylight time and dropped-item lava protection. Run on fresh defaults.
  `release-160 save`, `release-160 exit`, `new-world`, `release-160 inactive`,
  then `release-160 stop` exercise world and game exit. Restart and run
  `new-world` followed by `release-160 saved` to verify persistence and resets.
  Flexible placement and Complete slabs retain their saved enablement on both exits.
  `placement-lifecycle check` covers all four preference combinations, unchanged
  unrelated values, held-mode cancellation, removed slab binding and menu Cancel/Apply.
  `placement-lifecycle exit N` and `placement-lifecycle stop N` queue preferences
  immediately before world exit or shutdown. After loading another world or restarting,
  run `placement-lifecycle saved N`. Use N=0..3, bit 1 for flexible placement and
  bit 2 for slab completion. These commands also verify other temporary tweaks clear.
  `release-160 hud-scene` displays independently styled clusters; `release-160 debug`
  toggles F3. `release-160 placement-water` and `release-160 placement-lava`
  place fluid in front of a target block; hold G to inspect the grid and preview.
  Use a muted capture output for every fixture. Physically test right-drag cursor
  restoration with both `release-152 native` and `release-152 raw`, and test
  auto-walk focus loss with each pause and cancel preference.
  Include complete press/move/release gestures within one tick, and held movement
  across focus loss and return. `release-160 background-key` checks queued movement
  while unfocused with auto-walk active. `release-160 hud-key`, `hud-chord` and
  `hud-mouse` bind the HUD toggle to H, Ctrl+H and middle mouse; `hud-state` records
  visibility, cluster settings and text. Test both chord release orders, key repeat,
  menus, focus loss, F1 and F3. `hud-toggle-check` verifies queued saves and Cancel.

- On an isolated Xvfb display with a muted validation clone and a world open, run
  `python3 validation-mod/tools/input_regressions.py /path/to/clone/.minecraft --display :95`.
  It checks 420 consecutive fast drags in native/raw modes, 60 gestures with motion
  immediately after release, both OS and game cursor positions, camera deltas,
  and actual auto-walk displacement through inventory focus loss. It changes
  disposable settings and terrain. The script refuses the desktop display and
  the normal Prism instance directory.
- Also test KDE/Xwayland using `tools/wayland_drags.py` and `tools/wayland_input.c`.
  Generate `fake-input.h` and `fake-input.c` with `wayland-scanner` from Plasma's
  `fake-input.xml`, then compile the helper with `wayland-client` and `libm`.
  Start a separate KWin with a private D-Bus session, config/data directories and
  runtime directory under `/tmp`, a virtual output, and Xwayland. Set
  `KWIN_WAYLAND_NO_PERMISSION_CHECKS=1` only for that disposable compositor to
  expose its test input protocol. Never set it in the user's session.
  Launch the muted clone there and open a disposable world, then pass its game
  directory, `--runtime-dir`, `--socket` and `--input-helper` to the script.
  It injects compositor input and checks another pointer movement after release;
  XTest inside Xwayland and cached LWJGL coordinates alone can miss failed warps.
- `skin NAME` looks up a public skin profile and assigns it to the disposable
  player. Wait for the download, then `skin check` verifies decoded pixels and
  the uploaded GPU texture. `skin default` checks that a bundled fallback does
  not start an HTTP download. No authentication tokens are needed.

`photo-controls open|state|layout|resize|closed` exposes photo-mode state for physical input
checks. Run `tools/photo_controls.py GAME_DIR --display :95` on a muted disposable
Xvfb clone to test native/raw panning, rotation, sliders, zoom, focus, screenshots,
exits, held free-look perspective changes and the inventory model during looking.
`menu-scrub` needs an initialized sound engine, such as after `set native.music 1`;
keep the clone routed to the muted null sink, then restore `set native.music 0`.
`menu-scrub`, `music-collapse`, `filter-navigation`, `cactus-items` and `cape-checks`
exercise the corresponding runtime controls. Load a skin with `skin NAME` and verify
`skin check` before `cape-checks`; it captures actual renderer angles without changing
saved movement data. `release-160 save|saved` includes cactus-protection persistence.

For 1.7.1 frame checks, run `release-171 spectator-scene`, wait for normal frames, then `release-171 perspective-held`. Use `normal-scene` then `normal-perspective` to check Beta still forces first person for an ordinary player inside terrain. `release-171 head-probe` captures one native head render, and `release-171 head-result` verifies changed skin pixels and writes `ghost-pass.png` in the disposable game directory.

`release-172` checks native outline draws in survival, creative and spectator, the live
outline preference and Cancel, and removal of the camera/sign UI settings. Run
`sign-editing` for native sign layout, editing limits, save/reopen and packet roundtrips,
and `release-171 picker` for ranked fuzzy item searches and all picker draft checks.

`release-173 water`, `overlay`, `editors` and `tooltip` check native splash sounds,
underwater/fire/block overlay draws, full-bright restoration, unsaved nested drafts,
and final tooltip ordering over slots/cursor stacks in both creative inventory views.
Run in a muted disposable world with the test window focused.

`release-173 fog` checks native OpenGL fog color/distance underwater with full bright,
fog-density and photo-mode controls, toggle restoration, and unchanged lava/cloud fog.
Use `fog-scene`, `fog-bright`, `fog-native` and `fog-above` for a submerged/above-water
comparison with distant markers. These scene commands change the disposable world.

`release-174 fog` also checks the underwater density slider, live percentages, full-bright
overrides and Cancel. Run `fog-save`, restart, then `fog-saved` for persistence and rollback.
`world-settings-create` checks silent initial mode selection and normal later mode messages.
`release-174 keys default|shown|debug-remap|modifier-remap|cycle-remap|chord|disabled|no-cheats`
prepares physical debug-key checks; `keys-state` writes debug state and rendered-frame counts.
Run `python3 validation-mod/tools/debug_keys.py /path/to/clone/.minecraft --display :97`
for press/release, cycling, remaps, focus/menu/world cancellation and held movement.

- `release-221 check` checks acceleration sliders and Cancel, consistent Survival/Creative feet and eye Y through flight, falls and slab/full-block
  landings, actual native flight and detached displacement in every direction, lava sinking with native item data,
  water and multiplayer guards, and native head pixels/depth behind terrain.
  `release-221 save`, restart, then `release-221 saved` verifies acceleration persistence.
  Run in a focused, muted disposable world.

- `release-222 check` compares native and flattened axe mining across all leaf metadata
  and tool tiers, diamond-shovel timing, per-block wear and tool break cleanup, seeded
  fist-equivalent drops, mode/server exclusions, mining penalties and precise flight
  response entry through right-click editors. `save`, restart, then `saved` verify
  the leaf preference and fractional values in all six flight response settings.

- `release-300 refinement` checks native coarse-to-fine pixels after panning while fine data is pending, with no world sampling or map file work.

`release-310 suite` checks map sidecar scope, related navigation, color return, manual previews, resize fallback, F3 preview and template review. `release-310 notes-write` then a real restart and `release-310 notes-read` check persisted local messages. Disposable clones only.

`release-310 polish` checks sidebar motion/release ownership, footer widths, the unhighlighted Minimap launcher and automatic untouched template changes with draft preservation. For a real pointer test, run `release-310 polish-scroll-arm`, drag the thumb at the physical coordinates written to `reports/release-310/scroll.json`, then `release-310 polish-scroll-held` while holding. Release outside the sidebar and run `release-310 polish-scroll-release` to verify cleanup and map ownership.

`release-330 ui` checks search scope/routes, sidebar source returns, shared manual drafts,
conditional World Map appearance and zoom Undo. `release-330 inventories` checks native
player, chest, crafting, dispenser and furnace minimaps, panel wheel ownership and the
observed dimming/minimap/container draw order. Use only the muted disposable clone.

`input-400` prepares and observes real Save/Load hotbar and offhand bindings without
invoking their actions. With a muted disposable client and a world open, run
`python3 validation-mod/tools/modern_inventory.py --test-root /path/to/test-root --game /path/to/test-root/clone/.minecraft --pid JAVA_PID --window X_WINDOW_ID --display :95 --suite all`.
The driver verifies the Java process, game directory, isolated Xvfb display and
window before sending input. It covers both release orders, repeat suppression,
existing row/scroll priority, Creative hover swaps, text ownership and mouse
slot/header/outside guards. Every case restores its captured settings and inventory.

`debug-410` checks Debug controls, actions and JFR recording. `debug-410 setup` prepares native key testing; `debug-410 state` reports the current key/overlay state.
