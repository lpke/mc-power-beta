# Changelog

## Sign editor, mining, toggle messages and WorldEdit

- Escape saves the current sign text and closes the editor, like Done.
- Add an Obsidian breaking speed slider under Building > Mining. Off preserves
  vanilla mining; maximum gives diamond pickaxes the modern Efficiency V netherite
  and Haste II speed. Applies only to obsidian in singleplayer and defaults to off.
- Add individual chat-message switches for all eight Active tweaks features,
  independent of HUD visibility. All default to off, including the existing edge
  protection and slab completion switches. Previews, focus loss and world loading
  do not announce state changes.
- WorldEdit accepts `hand` in block patterns and masks, using the held block and
  its variant. The held stack is preserved, and the block is fixed when the command starts.

## Settings usability and world cycles

- Show duration controls in seconds, including weather, leaf decay, double-tap,
  auto-walk hold time and HUD fades. Stored values and timing stay unchanged.
- Show numeric limits in tooltips and separate notices with a blank line.
- Use click-to-edit number fields for item IDs, capture dimensions, large counts
  and long durations. Reject unregistered item and block IDs before accepting edits.
- Add separate per-world Daylight cycle and Weather cycle switches below Cheats.
  Move Autosave interval below Difficulty. Hidden cycle drafts reset when cheats
  are turned off; saved preferences remain available when cheats return.
- Keep the simulation clock and scheduled updates running while daylight is frozen.
  Day counters, time commands and photo mode use a separate daylight clock.
  Frozen weather survives sleep; normal and accelerated sleep respect frozen daylight.
- Replace feather sign editing with Allow editing signs, off by default.
  Right-click edits without consuming an item; Shift-right-click does not edit.
- Fix photo mode preserving the day count when adjusting the time slider.

## Current configuration and inventory spacing

- Seed one current Power Beta JSON document. Remove old config importers, renamed
  field aliases, migration markers and unused standalone settings writers/screens.
  Keep atomic saves, interrupted-save recovery and inventory journals.
- Remove superseded skin/authentication and Controls screen implementations; the
  bundled account service and unified Options screen continue to provide them.
- Require explicit music preset pools and group volumes. Remove obsolete music
  playback bridging and filename-based custom-track gain fallback.
- Fix nonempty fast-placement filter reloads with the game's Gson 2.8.9.
- Increase the creative catalogue-to-hotbar gap by two GUI pixels to match survival.
  Keep the hotbar and survival slots fixed.
- Leaving spectator keeps the current position, including inside solid blocks.

## Settings organisation

- Group key bindings by action and make those groups collapsible. Reorder settings
  so feature switches precede their details, with explicit tab and group ordering.
- Collect texture, cloud, debug, inventory, vehicle, sleep, recipe and capture
  controls in their relevant sections. Put shared flight/freecam sprint settings
  in Camera and retain their related-key links.
- Remove the ineffective legacy Controls menu selector and unused resource URL
  controls. Hide legacy skin controls while the bundled account service replaces
  them. Their saved values remain intact.
- Consolidate the inverse focus-loss switches into Pause on lost focus, with a
  verified backup and migration that preserves whether the game pauses.
- Clarify inventory gestures, mob-drop item choices, shared sprint settings and hotbar
  preview help. Rename the cloud fix previously labelled as a hotbar fix.
- Mark startup-only display, controller, applet and resource-download
  settings as requiring restart. Link the fog binding to its configurable sequence.

## Active tweaks and menu music

- Add an active-tweaks HUD list, on by default, with per-tweak inclusion,
  nine positions, offsets, colour and opacity. Slab completion is opt-in.
- Default menu music to Mix both. Mix both and World soundtrack preserve playing
  and paused music across world/menu transitions, including previews.
- Rename Pause menu music to Menu music controls and add Show in main menu,
  on by default when music controls are enabled. Keep controls clear of the
  bottom menu buttons at small GUI sizes, including with the scrub bar visible.
- Show eight creative catalogue rows instead of seven, expanding upward while
  keeping the hotbar, delete slot and survival inventory unchanged.
- Describe recipe ingredients, arrangements and outputs in tooltips. Remove
  recipe details from setting names.

## Inventory movement and keyboard fixes

- Add Inventory while moving, off by default. Movement and auto-walk work in
  singleplayer inventories without sneaking; containers close beyond reach through
  their normal item-cleanup path. Other menus and focus loss stop movement.
- Make the light overlay's scan budget adjustable from 256 to 16384 positions per
  tick. The default remains 2048; disabled overlays do no scanning.
- Fix Linux backslash and extend function-key support through F24, including held
  keys and modifiers. Unknown keys no longer clear an existing binding.
- Make fullscreen follow its configured binding in menus as well as gameplay.
  Unbinding F11 disables it; capturing a binding does not toggle fullscreen.
  Holding the key toggles once, including after Linux recreates the window.

## Music playback fix

- Start music streams once to prevent the opening audio repeating during buffering.
  Use the same startup for normal playback, previews and seeking.
- Keep playback controls and the scrub bar available while creating or editing
  music presets. Leaving a draft through the current-track link prompts to save.

## Preset reset fix

- Loading None clears all music exclusions and the preset track pool while
  preserving saved presets, volumes and soundtrack settings.

## 2026-10-05 maintenance

- License source under AGPL-3.0-only and include LICENSE in the main JAR.
- Freeze preset track pools so added music folders do not expand saved selections.
  Migrate existing presets once with a configuration backup.
- Add folder multi-selection, shallow child-folder imports, duplicate checks,
  remembered browse location and Save/Discard/Cancel for folder drafts.
- Detect optional FFmpeg at startup before offering MP3 conversion.
- Restore gaps after audio sliders and expand single-section library views.
- Add an optional pause-menu scrubber, off by default.
- Stop building and shipping disabled mods in the standard pack.
- Consolidate documentation; remove obsolete standalone instructions and test diaries.

## Current pack

- One searchable Options menu with persistent navigation, live previews, precise
  inline values, colour pickers, sticky sections and modifier-aware key bindings.
- Creative and spectator modes, building tools, safe hotbar swaps, container carrying,
  free look/freecam, light overlays and configurable inventory improvements.
- Singleplayer commands and WorldEdit with per-world cheats gating and separate
  command access rules.
- Bundled texture packs and independent weather/block texture overrides.
- Music library, queue, favourites, isolated presets, group/track volumes, custom
  folders, seeking and optional pause-menu controls.
- Unified configuration with transactional saves and recovery. Owned utility code
  builds here; platform and performance binaries remain pinned dependencies.

Earlier changes and standalone-mod history are available in Git history and
the references in [docs/upstreams.md](docs/upstreams.md).
