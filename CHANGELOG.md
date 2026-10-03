# Changelog

## Power Beta 1.0.0, 2026-10-03

- Compiled the gameplay and utility modules from source into one Power Beta JAR
  and Prism export. Kept module IDs for compatibility. Platform/performance
  dependencies remain pinned binaries. Recipe browser, shaders and LAN hosting
  remain disabled optional modules.
- Replaced Mod Menu with a shared Options screen accessible from title and pause
  menus. Added 15 functional pages, item-icon navigation, independent scrolling,
  collapsible groups, global search and adaptive layouts.
- Added forward/backward option cycling, exact numeric entry, keyboard and mouse
  binding capture, conflict indicators, per-setting/page reset, draft save/cancel
  and restart notices. Added texture-pack access under Video.
- Grouped existing configuration by function rather than upstream mod names.
  Included native game settings, creative controls, building, world editing,
  camera, movement, inventory, HUD, recipes, fixes and advanced settings.
- Set conservative defaults: optional building, movement, crafting and world
  editing changes start off. Normal mob spawning and Beta recipes remain intact.
  Creative controls, performance improvements and selected small quality-of-life
  fixes remain available.
- Added master, category, individual-sound and individual-track volumes, integrated
  with existing ambient controls and music timing.
- Added custom world/menu music folders with a folder browser, Add/Replace modes,
  shuffle, repeat prevention, play/pause, next track and reload. Supports OGG, WAV
  and MUS; MP3 is unsupported by the retained sound engine.
- Added bounded background folder scans, malformed-header rejection, missing-folder
  fallback and Unicode filename support. Music files remain in place and are read
  only. Preserved portal stop requests, dimension/biome tags and music debug data.
- Loaded cached audio before legacy network downloads, bounded network timeouts
  and synchronized/deduplicated sound-pool insertion.
- Added transactional settings backups, failed-save rollback and interrupted-save
  recovery before config and mixin initialization. Rejected unsafe linked paths.
- Integrated audio and controls directly into imported utility sources. Removed
  unused optional integrations and obsolete Mod Menu hooks.
- Added Ctrl/Shift/Alt combinations for keyboard and mouse bindings, immediate
  key-down actions, press ownership, conflict markers and safe modifier release.
- Placed Options directly below Achievements/Statistics with normal row spacing,
  restored the Photo Mode icon beside it and retained the Minecraft title logo.
- Flight sprint in Toggle mode now ends when forward movement stops.
- Fixed double WorldEdit command execution, retained completion/history, and added
  coloured command, placement-toggle and error feedback.
- Aligned the creative inventory delete panel with the native bottom border.
- Preserved complete native button borders at compact heights and odd widths.
- Fixed StationAPI world-edit writes so stacked slab metadata and chest contents
  survive set, clipboard and undo without native placement/removal side effects.
- Added mouse-bound inventory closing to both creative and survival tabs.
- Added right-click cycling to the world-creation mode button.
- Added an upstream inventory and development instructions for future update reviews.

## Separate-mod checkpoint

- All multi-option settings cycle forward with left click and backward with right
  click in the three custom mods.
- Creative flight now advances and settles the walking pose smoothly. Flight sprint
  supports Toggle and Hold, defaulting to Toggle.
- Integrated the delete slot visually, limited its tooltip to Shift, removed picker
  help text and preserved the held cursor stack when changing inventory tabs.
- Removed the first-person free-look return animation. Hotbar preview numbers stay
  hidden when number-key swapping is off.
- Committed and pushed LpkeCreative 1.1.0, LpkeTweaks 1.2.1 and WorldEdit Beta 1.2.1.
  Froze the tested Prism instance as `separate mods final` before cloning it for
  Power Beta. The frozen snapshot and standalone repositories remain unchanged.
