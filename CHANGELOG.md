# Changelog

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
