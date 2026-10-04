# Changelog

## Light overlay and equipment swapping, 2026-10-04

- Add an F7 light overlay toggle, rebindable with keyboard, mouse and modifiers.
  Show red/green light numbers flat on nearby exposed block tops. Off by default.
- Configure the colour threshold, both RGB colours, range, number size, block or
  combined light, and full-block or spawnable-surface filtering under Building.
  Default block-light colours split at 8 for Beta, ignoring daytime sunlight.
- Bound scans to 2,048 positions per tick and cache at most 2,048 labels. Skip
  unloaded chunks, stop all scans while disabled and retain normal depth testing.
- Add default-on Swap tools and armour under Inventory. Clicking matching
  damageable equipment with different durability uses the native slot swap,
  preserving the original stacks and slot restrictions. Singleplayer only.

## Chest splitting, search navigation and previews, 2026-10-04

- Pick up only the targeted half of a double chest. Preserve the other half and
  both inventories; retain recovery for double chests carried by older versions.
- Fix crashes after opening related settings, controls or conflicts from typed
  search results. Keep text and cursor updates together across input fields.
- Search section headings open that section and clear the filter. Back restores
  the search results and scroll position.
- Keep state toggles and placement modifiers visible while off. Hide only actions
  with disabled feature switches; repair Creative picker and flight links.
- Preview built-in and custom music with speaker buttons, pause and resume the
  existing song, and show newly scanned tracks without reopening Options.
- Open Minecraft's native texture-pack picker from the existing Video button.
  Support custom ZIP packs alongside bundled Alpha, 1.14 and Faithful 32 packs;
  preserve menu drafts and support Cancel for pack previews.

## Chest joining and related-settings filters, 2026-10-04

- Allow a carried single chest to join an existing single chest on any horizontal
  side. Preserve both inventories, including their full item data.
- Continue blocking triple chests, occupied spaces and joins whose neighboring
  chunks are unavailable. Whole double chests still require two separate spaces.
- Filter controls-to-settings links to the related feature settings. Keep links
  symmetric, restore the previous view with Back, and limit Reset listed to
  the filtered settings.

## Container carrying and settings navigation, 2026-10-04

- Highlight setting rows only while hovered, with a lighter background. Restore the
  Controls reset label to R.
- Add keyboard-icon links from settings to related controls, including disabled
  features. Back or Escape restores the previous page, scroll, search, collapsed
  sections and conflict filter without discarding edits.
- Add the BTA carry pose, walking bob and centered first-person container rendering.
  Render double chests with joined Beta textures.
- Move double chests as one unit, preserving both inventories and their slot order.
  Their placement axis follows the player; both spaces must be clear, loaded and
  separate from other chests. Require releasing Use after pickup or placement so
  holding the button cannot immediately move the container again.
- Extend durable carry journals to both halves and recover interrupted pickup or
  placement without overwriting unrelated blocks. Keep old single-container journals
  readable and retain completed transfers as recovery copies.

## Unified settings and interaction update, 2026-10-04

- Remember the last Options page, scroll position and collapsed groups during a session.
- Play Next/Previous music immediately while Options is open, including a paused world.
  Retain bounded track history and clear it when playlists or world/menu contexts change.
- Add speaker-icon previews to individual sound controls. Filter cached audio assets
  against actual Beta sounds plus sounds explicitly supplied by the pack.
- Put master volume first and Music and ambience above Individual sounds. Split extra
  eating, burping, shearing, tool-break, armor-break and chest sounds into separate toggles.
- Replace raw-input advice with an explanation of device input and troubleshooting.
- Preview all Video changes immediately. Fix render-distance rounding and renderer
  updates; always expose the full controls. F cycles Beta fog levels and reports the result.
- Rename culling controls positively and invert their backing values correctly. Both
  entity and block-entity culling default on.
- Darken hovered rows; add click sounds to buttons without sounding sliders or headings.
- Flatten Controls, hide disabled-feature bindings by default, add Show Disabled,
  related-settings links and circular reset icons. Clicking a conflict indicator opens
  a stable conflict list, with the source binding first. Fix narrow-layout hit areas.
- Move Mouse and GUI scale to General and explain each bound action in its tooltip.
- Add optional slash-to-chat, off by default and on in Configured. Enable the assigned
  auto-walk action in Configured; verify movement and focus/menu cancellation.
- Default slab fast placement to Double slabs. Separate boat steering, higher speed
  and collision protection into independent switches.
- Replace the approximate light display with actual block light at the player feet.
  Render debug additions once per frame and keep the setting optional.
- Colour WorldEdit help commands separately from arguments and headings. Verify single
  command execution, history and double-slash completion.
- Add optional container carrying with full NBT preservation, occupied-target checks,
  double-chest restrictions, durable recovery journals and retained recovery archives.
  Support Sneak-use while creative flying.
- Add held-key container contents previews, including ordered double chests and storage
  minecarts, without opening inventories or moving items.
- Add freecam sprint using creative flight bindings, speed multiplier and Hold/Toggle
  mode. Forward release clears toggle sprint. Add independent door/trapdoor collisions.
- Fix Create New World title placement at large GUI heights.
- Move owned code and runtime IDs into Power Beta modules. Consolidate native options,
  module settings, bindings and saved camera positions into config/power-beta.json.
  Import old preferences with verified backups; retain old names only for migration,
  platform compatibility and provenance. Move backups/reports out of live configuration.
- Remove duplicate inventory, fog-control availability, boat-break, chat-shortcut,
  bit-depth, death-score, Quit-button and download-URL switches and redundant handlers.
- Build the configuration API from source against unified storage; keep permitted
  platform/performance binaries separate. Remove the obsolete title-credit toggle.
- Add migration-aware profile creation and backed-up updates that refuse running games.

## Menu and texture update, 2026-10-04

- Fixed GUI scale waiting for Done and ignoring mouse-held resize requests.
- Added live previews for scale, all audio controls, sensitivity, field of view,
  camera distance, texture packs and individual texture overrides. Discard
  restores unsaved previews. Volume changes no longer rescan music folders.
- Added Apply without navigation, numerical sliders with exact-value editors,
  and a clickable unsaved-changes filter beside Reset page.
- Added concise explanations for every setting, removed repeated names and
  usage boilerplate, and listed shared bindings in conflict tooltips.
- Darkened the active page and centered menus within a 16:9 maximum width.
- Simplified reset wording and the creative Shift tooltip to Clear inventory.
- Default perspective cycling now skips front view. Added adjustable camera
  distance while retaining vanilla four-block distance and collision handling.
- Included Alpha, 1.14 and Faithful 32 texture packs. Added independent soft
  rain, soft snow, old cobblestone and old brick overrides, including HD support.
- Added fresh Defaults and Configured profile generation. Preserved existing
  instances, running sessions and the frozen separate-mod checkpoint.

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
