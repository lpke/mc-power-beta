# Runtime checks

Build after the root JAR with `./gradlew -p validation-mod --no-daemon build`.
Install the resulting validation JAR only in a disposable Power Beta clone.
Fixtures can replace settings and edit worlds. They are never part of the export.
Mute the clone before launch and route any audio checks to a silent sink or capture device.

Write a command to `.minecraft/power-beta-validation.command`. Results append to
`power-beta-validation.log`; wait for `COMMAND DONE` and inspect failures.

- `world-settings-check`: difficulty transactions, scope notes, hidden cheat rows/tabs,
  command exemptions, input priority and Peaceful behaviour. Run after `new-world`.
  `world-settings-create` creates four disposable saves through the real creation UI.
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
- `toggle-messages`: all nine defaults, live providers, HUD independence,
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
