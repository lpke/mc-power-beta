# Runtime checks

Build after the root JAR with `./gradlew -p validation-mod --no-daemon build`.
Install the resulting validation JAR only in a disposable Power Beta clone.
Fixtures can replace settings and edit worlds. They are never part of the export.
Mute the clone before launch and route any audio checks to a silent sink or capture device.

Write a command to `.minecraft/power-beta-validation.command`. Results append to
`power-beta-validation.log`; wait for `COMMAND DONE` and inspect failures.

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
  `release-151 text` checks the loaded F3, status and chat colours through actual
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

- `world-settings-check`: difficulty transactions, scope notes, hidden cheat rows/tabs,
  command exemptions, input priority and Peaceful behaviour. Run after `new-world`.
  `world-settings-create` creates four disposable saves through the real creation UI,
  checking the initial Creative/Survival mode against the cheats choice, resize, save/reload
  and isolation from the previous world's mode or later cheats edits.
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
  `death-checks inventory` checks original slots, armour, overflow, occupied slots,
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
  reads without chunk generation or cancelled unloads. Run after `new-world`.
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
- `release-111`: cursor/colour defaults, preview, Cancel/Apply and world override groups.
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
- `command-completion`: arrow selection, immediate Tab completion and subsequent
  typing for WorldEdit and singleplayer commands.
- `chat-wrap`: all chat colours, repeated wraps, colour changes, resets, plain text
  and the WorldEdit cheats warning. Run after `new-world`.
- `freecam-culling`: entity/block entity visibility, repeated toggles, player movement,
  setting disable, preserved culling preferences and world exit. Run after `new-world`.
- `creative-borders`: compare catalogue row pixels and the destroy-slot junction
  at narrow and wide GUI sizes; screenshots go under `reports/creative-borders`.
- `creative-drops`: click outside the diamond tab at narrow and wide GUI sizes;
  check whole-stack and single-item drops, damage/NBT, hotbar pickup, UI borders,
  view/category tabs, destroy-slot borders and closure without duplication.
- `creative-paint`: middle-button previews and release in catalogue, inventory,
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
  catalogue and PNGs under `power-beta-data/reports/settings-review/NAME`. Use a
  disposable client window at least 1280 x 840 pixels. Captures include every
  scroll position at 640 x 420 GUI size and the first view at 320 x 240.
  Capture `before` in a world before updating the clone, then `settings-review`
  checks retained IDs, defaults, ranges, row coverage, binding groups and links.
- `new-world`, then `indicators-hud` and `indicators-creative`: live HUD states,
  inclusion/Cancel, eight catalogue rows, hotbar transfers and recipe tooltips.
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
  protection, TNT breaking, normal/fast leaf decay across modes, catalogue
  transfer, entity visibility, full bright and `/spawn`.
- `interaction-150` checks actual catalogue Shift-clicks, stack settings, native
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
  ends the current motion. `creative N` and `creative survival` open catalogue
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

`release-173 fog` checks native OpenGL fog colour/distance underwater with full bright,
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
