# Changelog

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
