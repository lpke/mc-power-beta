# Changelog

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
