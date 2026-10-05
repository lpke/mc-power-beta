# Changelog

## Audio presets and complete soundtrack library

- Added independent music preset creation, editing, renaming, deletion and explicit loading, with unsaved-change prompts and an Include all action. Active presets override ordinary soundtrack/custom-music choices.
- Added persistent favourites and preset browsing. Renamed library filters to Active, Everything, All folders, Folders, Favourites and Presets.
- Added survival/all soundtrack choices. Both all choices include 15 purchased album extras. Added all nine Nether/End tracks for browsing, manual playback and presets only; the complete library has 107 tracks.
- Added inline track rows, wider volume sliders, responsive compact layouts, initial section collapsing and custom-folder groups first.
- Added a minimal seek bar, reserved upcoming-track countdowns in minutes, and silent sound-preview buttons.
- Fixed pause/resume losing the currently playing identity, preserved preview playback through global Pause/Play, and handled Beta's repeated decoder initialization during seeking.


## Complete overworld soundtrack and group controls, 2026-10-05

- Expand All Minecraft from 21 to 56 tracks through Java 26.3, also checked
  against 26.4-snapshot-2. Add Caves & Cliffs, The Wild Update, Trails & Tales,
  Tricky Trials, Chase the Skies and Chaos Cubed background music.
- Also bundle all four menu tracks, 22 records and the credits song in separate
  groups, for 83 game-supplied tracks total. Alpha and Beta includes its 35
  available game tracks. Vanilla keeps the original 12. Preserve every existing
  track ID, volume, exclusion and queue reference. Album-only files still need
  local copies; document the 15 missing album extras.
- Separate dedicated creative music into Beta / Creative. Keep shared
  survival/creative tracks in their release groups without duplicates.
- Add group inclusion buttons with included/total counts and mixed-state
  highlighting. Apply to the whole era or folder even when searched/collapsed;
  batch changes through the normal preview, Apply, Cancel and auto-apply flow.
- Fix Beta OGG decoding from compressed JAR resources: complete short reads
  before passing data to the old decoder, preventing silent/one-buffer playback.
  Keep bounded streaming without file extraction or whole-song buffering.
- Add a reproducible official sound-event audit for soundtrack completeness,
  creative classification and asset hashes, plus full PCM stream comparisons.

## Soundtrack library and music gaps, 2026-10-05

- Bundle all 21 overworld tracks available through Minecraft 1.14: the original
  12, six Volume Beta creative tracks, and three Update Aquatic tracks. Add
  Alpha and Beta and All Minecraft soundtrack choices.
- Separate custom-track mixing from the built-in soundtrack selection. Merge
  menu playback choices and portal music behavior into one control each. Remove
  redundant background-music disable switches; music volume zero mutes music.
- Group all delay controls under Music gaps, immediately after Music library.
  Show the countdown and minimum/maximum gaps in seconds. Fix the former maximum
  setting, which was actually a random addition to the minimum.
- Preserve existing preferences through a backed-up, one-time migration. Remove
  unused music mixins and the second config-save timer controller.
- Group library tracks under collapsible era and folder headings. Show filenames
  with song titles only when different. Replace Included/Excluded text with a
  music-note icon and red strike-through. Keep queue volume and actions inline.
- Remove individual music sliders from the base Audio page. Keep track volume
  and rotation controls together in the library. Move music debug display
  controls to Interface.
- Prevent blank Playing labels during asynchronous audio transitions.
- Make Nether portal transfers immediate in creative and spectator while
  preserving survival warm-up, portal cooldowns and StationAPI destination setup.
- Include the setting name in related-controls search placeholders. Colour
  unbound keys grey and conflicting keys red. Add a second space before the
  world-list Cheats suffix and colour Cheats bright blue.

## Audio controls and effective settings, 2026-10-04

- Show Off or Disabled for cheats-locked settings while retaining the stored
  preferences. Keep the lock explanation available on hover.
- Replace the playback toolbar's Play/Pause text with icons. Put Back beside
  library search or the queue title. Add Quiet after Next to end a track without
  pausing automatic music.
- Make the playing-track status open All tracks and scroll to that track.
  Add queue volume sliders and exact-value controls, sharing the track's library
  volume without changing queue order or files. Reject clicks against queue rows
  that changed since they were drawn, including volume edits.
- Replace the library filter cycle with Active tracks, All tracks, Custom tracks
  and Folder tracks tabs. The folder button cycles forward or backward through
  configured/discovered folders. Preserve folder choice when switching tabs.
- Add Wait between tracks, on by default. Off starts tracks back to back.
  Add Wait before queued tracks, off by default to preserve immediate queue
  playback. Both use the existing delay settings, with the master wait switch
  taking priority. Manual Next/Prev/Play bypass the delay.
- Use one automatic music scheduler for normal, custom, menu and queued music.
  Keep Pause persistent, retain unavailable requests with an error, and show the
  remaining wait instead of world/menu library counts when idle.
- Show the twelve built-in music filenames with their published song titles,
  such as calm1.ogg (Minecraft). Keep persistence keys unchanged.
- Reorder General into Game, Interface and Input with difficulty and cheats
  first, followed by autosave. Group menu/chat preferences under Interface and
  mouse/controller preferences under Input. Rename Sensitivity to Mouse sensitivity.
- Draw restart asterisks in soft red independently of the setting-name colour.

## Cheats access, commands and audio polish, 2026-10-04

- Add the per-world Cheats enabled switch beside Difficulty. New worlds default
  to survival with cheats off; replace the creation mode button with this switch.
- Show `[Cheats]` after enabled world names and omit the former mode suffix.
- Gate commands, mode switching and WorldEdit through the saved world flag.
  Remove creative-only restrictions; retain global and per-world allow/block
  rules, including separate creative/spectator entry controls.
- Keep locked options visible with explanatory tooltips. Separate cheat and
  non-cheat command groups. Leave freecam and QoL controls independent.
- Return safely to survival when disabling cheats. Preserve inventories, saved
  warps and access preferences, and roll back interrupted editor operations.
- Fix target-first singleplayer teleport; support aliases, selectors, relative
  and local coordinates, rotation and facing. Modernize give, clear, time,
  weather, kill, summon and ride, with validation before mutations.
- Add seed, difficulty, personal respawn and world-spawn commands. Preserve
  saved warps through respawn; keep chat clearing under `/clearchat`.
- Colour command help and show complete lists with scrolling chat enabled.
  Keep explicit pagination and omit WorldEdit help prefixes.
- Remove the empty Queue count. Toggle Queue/Library closed when clicked again;
  Back, Escape and Audio return to settings. Add skip icons and the Prev label.
- Make setting names slightly greyer without changing headings or values.

## Audio navigation, 2026-10-04

- Keep Queue visible with its live request count; grey out Queue (0).
- Order the right toolbar group as Queue, Library, Reload. Library always opens
  tracks, with Back to Settings beside playback controls in either music view.
- Remove the separate Tracks/Queue tabs. Preserve each view's scroll, track
  search and folder filter when switching or returning to settings.
- Wrap toolbar groups at narrow widths and compact track controls into two lines.
  Keep the full Back to Settings label, Clear queue and folder filter usable.

## Audio layout and folder controls, 2026-10-04

- Keep Play, Previous and Next on the left; place compact Queue, Reload and
  Library/Settings controls on the right. Queue appears only when requests exist
  and opens the queue directly.
- Put queue names and controls on one line, with a compact Clear queue button
  anchored right. Keep protection against editing a queue that changed after rendering.
- Add On/Off switches beside music folders. Preserve paths, files, per-track
  volumes and exclusions. World and menu folders have independent switches.
- Default the library to Active tracks, using the same soundtrack and location
  rules as automatic playback. Keep individually excluded tracks visible for
  re-enabling. Retain All, Custom and individual-folder filters.
- Keep disabled-folder tracks available for manual preview and explicit queue
  requests. Fall back to the built-in soundtrack when no replacement folder has
  playable tracks, subject to the existing built-in music switch.

## Command access and embedded music library, 2026-10-04

- Add Commands with a master switch, per-command global access rules and per-world
  overrides. Master Off always wins; world Allow/Block takes priority over the
  global rule. Cheat commands default to creative-only, except `/gamemode` and
  `/gm`, so survival players can enter creative. Information/help and WorldEdit
  keep their existing access rules. Saved warps and command data remain intact.
- Enforce access in chat dispatch and completion, including `/gm`. Suspend god-mode
  protection while its command is blocked. Store policies in `power-beta.json`,
  keyed by a stable ID saved in world metadata and shared between dimensions.
- Remember the creative inventory's selected tab and creative/survival view for
  the game session, without moving cursor items or changing inventory contents.
- Remove successful container pickup/placement chat messages. Keep warnings,
  errors and the existing transaction/recovery protections.
- Show a colour swatch beside hex values. Dim setting labels slightly, support
  dragging content/sidebar/folder scrollbars, and scroll an active sidebar page
  to the top when clicked again.
- Embed the music library in Audio. Remember its Tracks/Queue tab, folder filter,
  query and separate list scroll positions when reopening Options.
- Put Play/Pause, Previous, Next, Reload and Music library on one toolbar. Show
  the current playback action, highlight active preview speakers yellow, and
  let a second preview click stop both music and short effects.
- Mute and pause late-starting background sources when previewing or pausing music,
  preventing overlap after rapid Next/Preview/Pause clicks.
- Add All/custom/individual-folder library filters, clearer tabs and compact
  queue-row move arrows. Remove redundant queued labels. Reject stale queue
  edits when playback has changed the list since its last render.
- Replace the fog-cycle JSON field with an ordered distance editor: labelled
  sliders, exact values, add/remove, reorder and Defaults. Keep edits local until
  Done, reject duplicates/out-of-range values, and preserve the parent menu position.

## Menu, video and music controls, 2026-10-04

- Use ranked fuzzy settings search with typo, abbreviation and omitted-space matching.
  Remember the search, filters, page, scroll and Back history when reopening Options.
- Match search button heights to the input and hide Clear when no filter is active.
- Right-click Apply to enable persistent auto-apply. Sliders preview immediately
  and save on release, avoiding a configuration write for every mouse movement.
- Add optional direct-to-Options pausing under General / Game and input. The Menu
  button returns to the normal pause menu and handles unsaved changes safely.
- Add a colour picker for RGB hex settings: channel sliders, exact hex input,
  clipboard Copy/Paste and up to 16 saved swatches.
- Add editable fog-key cycling distances. Defaults remain [12, 8, 4, 2]; Shift+F
  cycles backward. Keep immediate render-distance changes and chat feedback.
- Move Framerate limit to the top of Rendering, support actual caps through 1000
  FPS and Unlimited above 1000, and migrate existing caps without changing their
  displayed value.
- Replace Fancy graphics with independent Quality controls for transparent leaves,
  grass sides, 3D clouds, layered transparency, weather detail and entity shadows.
  Move the existing vignette switch into Quality with positive On/Off wording.
- Add Hide options / Show options in Video to preview the world without closing.
- Keep brightness chunk rebuilds: Beta compiles light colours into chunk geometry.
  Avoid rebuilding a second time when Apply saves the already-previewed value.
- Changing World music now pauses the current song without advancing it. Resume
  continues the same track while the menu remains open.
- Add a music library combining configured folders, searchable tracks, individual
  rotation switches and volumes, previews, Play now, and a persistent queue with
  reorder, remove, clear and playback controls. Equal filenames have distinct IDs.
- Add explicit background MP3 conversion through FFmpeg into an instance-local WAV
  cache. Original files remain untouched; conversion can be cancelled and is bounded
  by time, size and track-count limits. No additional Java dependency is introduced.
- Auto-walk supports short-tap toggling and hold-to-walk, enabled by default with a
  configurable 350 ms threshold. Menus, lost focus and manual stops cancel it safely.
- Default freecam door collisions to Off and place the main collision setting above
  it. Slash-open-chat now seeds the visible enhanced chat input with `/`.

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
