# Changelog

## [Unreleased]

## [4.4.2] - 2026-10-24

- Keep the searched tab selected for Controls and This tab searches; simplify Controls result headings. Give container previews independent block and entity reach, both defaulting to five blocks, and hide the Creative tab when Creative access is disabled.
- Make Creative deletion available in native inventory screens through protected inventory transactions. Restore palette cursor deletion, limit wheel transfers to matching held items and allow wheel paging over overflowing tabs.
- Refine Creative tab editing with scrollbars, row removal, selection-first dragging, native double-click editing, drag previews and Undo for every edit including Reset.
- Focus Creative Search on entry, use a light search field, keep movement out of text editing and group ordinary items before unusual unobtainables.
- Keep Shift-drag transfers active through verified asynchronous transfers into the personal crafting grid, independently of crafting-grid storage.
- Remove optional lava burning sounds and retain native lava audio. Right-click an expanded minimap to reset its zoom when expanded wheel zoom is enabled.
- Limit recipe amount scrolling to recipes still placed in the grid. Hide complete recipe ghost hints when native items would overlap them.
- Retire historical configuration and world-rule converters after verifying the primary instance. Current formats remain configuration 7 and world rules 4; older formats must be upgraded with 4.4.1 first. Preserve current recovery records, unsupported-data protection and per-world sorting migration seeds.

## [4.4.1] - 2026-10-24

- Use Alt-click to open recipe variants.
- Shift-right-click fills and crafts the maximum whole recipe batches that fit in the inventory through one protected transaction.
- Scroll over a chosen recipe to adjust its ingredients, including removing the last batch. Hovering untouched recipes keeps page scrolling available.
- Add wheel zoom while holding Enlarge minimap, enabled by default. Share inventory zoom increments and limits while preserving menu, hotbar-row and hidden-map input ownership.

## [4.4.0] - 2026-10-23

- Add editable Creative tabs with custom names, icons, ordered item grids, empty slots, multiple selection and dragging. Keep Search pinned beside six regular tabs, with fuzzy item search and remembered tab scrolling.
- Combine Tools and Weapons, simplify Creative tab titles, darken unselected tabs, hide unused arrows and refine palette spacing. Support advanced tooltips and cursor wheel transfers, protect the combined offhand/destroy frame from outside drops, and keep only inventory-clear confirmations audible.
- Use one All settings / This tab search choice across Options, starting at All. Controls always searches controls and has no scope button.
- Make Fast placement, Fake sneak, Placement restriction and Hide selected entities temporary gameplay actions. Retain their bindings and tuning, add an unassigned placement-restriction toggle, and clear active state on world exit.
- Allow Shift-click and Shift-drag into the personal crafting grid when the hotbar is full, independently of crafting-grid storage. Preserve native item data, callbacks and durable inventory protection.
- Keep offhand equip animation steady during count-only wheel transfers. Batch queued recipe-wheel steps into one protected transaction and fix saving the Allowed tools picker.
- Save sorting layouts and their presets separately for each world. Migrate existing layouts from a retained global seed, preserve world-copy layouts and protect unsupported data.
- Use a circular minimap settings button aligned to the frame. Left-click the minimap in an inventory or Pause to open the World Map; Escape returns to the same screen and container.
- Set Power Beta's default underwater fog density to 40 percent and disable underwater ambience, rare sounds and very rare sounds by default. Preserve existing saved preferences and other defaults bases.
- Split water and lava bucket sounds into separate toggles and previews, preserving the old bucket choice for both. Extend modern splashes to mobs, items and vehicles with their own volumes and categories.
- Add independently toggleable modern lava ambience and burning sounds, retain native cooling and extinguishing audio, and correct lava-pop volume-category handling.

## [4.3.2] - 2026-10-23

- Add independently toggleable submerging, surfacing, underwater ambience, bubbles and water, rare underwater sounds and very rare underwater sounds under Audio > Extra sounds. Match modern head crossings, variant volume, random chances, pause handling and ambience fades.
- Update modern swimming and splash audio to the verified Java 26.3 assets. Match modern body-contact depth, movement cadence, quiet movement exclusions, volume and pitch without changing water physics or native particles.
- Fix bundled extra sounds ending early or retaining padding, including swimming, splashes, planting, milking, bone meal and chests. Keep personal music and other sound resources on their existing decoder path.

## [4.3.1] - 2026-10-23

- Restore Beta's normal crouch camera movement when Lower camera when crouching is off. Enable it for the deeper Modern crawling camera drop in both first and third person; keep low ceilings and crawling collision-safe.
- Move the inventory and Pause quick-settings button farther outside circular minimaps, retaining corner-aware dropdown placement and matching click bounds.

## [4.3.0] - 2026-10-23

- Remove per-sample callback allocation from leaf lighting while retaining the same brightness and native light reads.
- Reduce water-lighting work during terrain rebuilds by scanning only to the requested height, reusing known block IDs and loaded section data, and skipping samples already at maximum brightness. Preserve native shading, geometry, roofs, daylight changes and held-item lighting.
- Reuse minimap and world-map clipping storage instead of allocating temporary points and lists each frame, preserving exact map positions and texture coordinates.
- Avoid repeated full-tile copies when preparing zoomed-out map images. Keep terrain updates on the game thread and retain immutable snapshots for durable worker saves.

## [4.2.3] - 2026-10-23

- Show the minimap and its quick settings in Pause as well as inventories, with mouse-wheel zoom and right-click reset. Keep the button visible after focus loss, use a native compact button and place its dropdown away from the map according to the selected corner.
- Keep in-game waypoint labels visible behind inventory and Pause controls, while hiding them in settings and other editors.
- Add a Cave mode checkbox to minimap quick settings. Disabling it immediately stops obsolete cave sampling and cancels queued cave-view reads while preserving explored terrain and pending saves.
- Set the Power Beta defaults base and fresh-install autosave interval to five minutes. Preserve existing saved intervals and the other defaults bases.
- Add Lower camera when crouching, off by default, for Modern crawling. Enable it to lower both first-person and third-person cameras while sneaking; retain safe camera and collision heights under low ceilings.
- Fix Creative offhand hover changing the destroy-slot highlight and drawing a grey item tooltip.
- Start Options search with All settings on General and This tab elsewhere. Once changed, retain one shared search-scope choice across ordinary tabs for the rest of the session.

## [4.2.1] - 2026-10-23

- Reduce terrain-rebuild CPU work and temporary allocations with reusable water-lighting columns and direct loaded-chunk lookups, preserving native geometry and lighting.
- Reduce minimap allocations and clipping work while retaining exact map coordinates, texture interpolation and saved cave identities.
- Size terrain draw-command storage by submitted meshes instead of vertex capacity, and retain queued draws when the terrain buffer grows.
- Avoid repeated color-pattern compilation while drawing the HUD and light overlay.
- Prevent the background culling worker from changing the block-entity renderer registry during world setup, fixing a world-load crash.

## [4.2.0] - 2026-10-23

- Start ordinary Options tabs with This tab search, retaining All settings on General and remembering each tab's scope. Name the selected defaults base in reset tooltips and add Cancel and Clear to binding capture.
- Add unassigned Hold use and Auto-walk line lock controls. Show their live state in Active tweaks, keep forward and backward Auto-walk within its block lane, and retain sprinting at real and invisible walls while locked. Leave the camera-position save/load binding unassigned by default.
- Add Stop sprinting at walls and optional sprinting up ladders. Allow ground sprint to continue or start in supported inventories while preserving text input, focus, permission, stamina and combat ownership.
- Show Spectator's crosshair over interactable targets and share Creative's configured block and entity reach.
- Add /instakill and /ik under Allow survival assistance. Filter individual waypoint, time, clear and game-mode help through the same permissions as execution and completion.
- Allow double-click gathering from the offhand and fill matching offhand stacks first during item pickups, retaining native item data and normal limits.
- Adjust crafting-grid recipe amounts with the inventory scroll controls, including initial placement and inverted scrolling. Right-click a recipe to craft one batch through a verified durable inventory plan.
- Reveal the minimap over F3 while holding its expand shortcut. Refresh expanded chicken bounds live, allow F3+H in inventories, keep debug holds out of ordinary bindings and repair F3+Escape. Use a fading dark overlay and progress bar for resource reloads.
- Add inventory minimap quick settings for items, mobs, map waypoints and in-game waypoints. Skip unwanted radar tracking and unused head preparation, default vertical radar range to 16, and distinguish the detached camera from the physical player on both maps.
- Add an allowed-tools picker for automatic switching and keep the current hand for instant-break targets. Restore repeated hand swings when automatic mining starts after pointer capture.
- Use available 2x2 crafting storage when a main-inventory Shift-click cannot enter the hotbar. Support player-inventory sorting in furnaces and crafting tables alongside existing chest and dispenser targets.
- Add official modern lava pops, normal and heavy player splashes, and swimming sounds with previews, volume categories and subtitles. Enable them only in Power Beta defaults.
- Add automatic modern crawling with collision-safe standing and crouching, low camera and player/armor poses, reduced dry movement and preserved saved positions. Keep crawling independent of modern water movement and sprint permissions; enable it only in Power Beta defaults.
- Reuse block IDs within one synchronous terrain mesh build and cached water-column opacity to reduce repeated palette reads during directional and chunk-border rebuilds. Preserve loaded-terrain guards, dynamic edit invalidation, geometry, light arrays and native update scheduling.
- Restore oversized kept-inventory counts safely when native dead-player NBT wraps them into signed bytes, verifying full item data and ownership before repair.
- Keep configuration format 7 with support for formats 3–6 and world-rule format 4 with support for formats 1–3 and earlier metadata. Initialize new preferences once from the selected base with verified backups, preserving existing settings, bindings, worlds and recovery data.

## [4.1.0] - 2026-10-22

- Add editable Debug shortcuts behind one enable setting, on by default for Power Beta and Refined Beta. Keep the mode picker independent and prevent debug chords from triggering ordinary gameplay actions.
- Show entity hitboxes, eye lines and facing directions, and Beta-height chunk borders. Copy location and target data, clear chat, toggle focus-loss pausing and open Debug controls with dedicated shortcuts.
- Reload chunks and textures, dump loaded textures, record ten-second Java performance captures, and show frame/tick, network and light-brightness diagnostics. Retain the traditional debug help shortcut and deliberate hold-to-crash handling.
- Remove the disabled legacy recipe-browser source and unused textures. Retain the current recipe book and the Creative inventory atlas used by the destroy slot.

## [4.0.4] - 2026-10-22

- Inspect containers in Spectator without withdrawing, depositing or activating them. Keep your own inventory editable while preventing item drops, including outside clicks and inventory closure.
- Share the recipe book's open state between inventory, crafting and furnace screens. Find recipes with fuzzy item search.
- Use fuzzy command suggestions, including matches within names such as plank for wooden_planks. Keep closer matches nearest the input and replace the full token when completing.
- Support editable texture-pack assets for inventory side slots and compact buttons. Refresh cached mod textures after pack changes.
- Refine offhand and Creative destroy-slot padding, preserve custom frame corners and align the slots with the white inventory border across native containers.

## [4.0.3] - 2026-10-22

- Align the chest offhand frame with the native bottom border, retaining native hotbar item and click positions.
- Open preferred-item selection by double-clicking a sorting slot. Use one Item, Clear and Reset row with balanced spacing and hover guidance for protection.
- Apply hotkey sorting-layout edits immediately, including presets, without Done or discard warnings. Keep menu-opened layouts as drafts until saved.
- Hide the underlying inventory, player model and Creative tabs while sorting and saved-hotbar layers are open. Escape returns to the same inventory and container.
- Lower compact button letters by one pixel and shorten them while keeping a clear row above the bottom bevel.
- Keep movement keys out of recipe searches, and show recipe-book controls in Creative's Survival inventory view with working shifted panels.
- Suggest display names such as wooden_planks alongside plain item IDs in commands. Keep the suggestion list visible down to its final match, including a fully typed item name.
- Allow the minimap mob-heads hold key in supported inventories, with text fields, modal layers and conflicting inventory controls taking priority.
- Return to the pause menu when closing its map or photo-mode view with Escape.

## [4.0.2] - 2026-10-22

- Open the sorting layout editor over your inventory with a new unassigned shortcut. Keep the native container open through layout edits, preferred-item selection and custom IDs, with isolated drafts and saved presets.
- Put Sorting layout first in Sorting settings, followed by item order and button visibility. Remove the redundant sorting and deposit enable switches, keep both actions available through unassigned hotkeys, and link each button setting to its shortcut.
- Clear a slot's preferred item with right-click while retaining its protected state. Keep layout tooltips short and preserve drag painting for protection.
- Make compact inventory button letters lighter, darken their upper and left bevels slightly, and balance dropdown padding and selection highlights across sorting presets, waypoint sets and the HUD editor.
- Accept and suggest plain item names in /give, /clear and /id, and plain block names in WorldEdit. Keep explicit namespaces, numeric IDs, metadata and block states available; report ambiguous item names without silently choosing another item.
- Right-click a placed crafting recipe to return one batch of ingredients to your inventory. Clear the last batch, preserve native item data and decline safely when there is no capacity. Recipes without a placed batch retain their variant chooser.
- Show the offhand beside the hotbar in single and double chests, crafting tables, furnaces and dispensers, including chest minecarts. Keep native slots and shift-transfers separate from the offhand and align its frame, click area and recipe-book spacing.

## [4.0.1] - 2026-10-21

- Open and use your inventory in Spectator. Worlds allowing Creative use the remembered Creative inventory views; other worlds use Survival. Move, craft, remove and drop items while the inventory is open, while world interaction and closed-inventory dropping stay blocked.
- Separate sorting presets from slot controls, choose presets from a dropdown, and accept preferred items with one click.
- Use smaller, lighter inventory action buttons and connect the offhand/destroy extension cleanly to the native panel bevel.
- Hide inventory action buttons independently while keeping their features and hotkeys available. Simplify the Creative hotbar popup.
- Avoid unnecessary subtitle tick bookkeeping and semantic processing while subtitles are off.
- Fix swaps into an empty offhand incorrectly stopping inventory transactions, and give accurate recipe-placement feedback for pending or stopped actions.
- Keep held-item lighting stable while breaking blocks, including with a torch in the offhand.
- Keep chest action buttons above item slots and draw item tooltips over inventory controls.
- Make the sort hotkey target player storage when pointing over your inventory, or the open container everywhere else. Sort buttons retain their existing targets.
- Show crafting recipes immediately when opening the pause-menu book, return to inventory after loading a Creative hotbar, and keep player-model depth out of hotbar previews.

## [4.0.0] - 2026-10-21

- Add a searchable recipe book for crafting and smelting, with recipe categories, craftable filtering, ingredient ghosts and safe recipe placement. Use it in supported inventories or only from the pause menu. Power Beta enables the pause-menu book by default and leaves the inventory button optional.
- Add an optional offhand slot, hand-swap binding, held-item rendering and native item use. Preserve offhand items through saving, death, respawn and permitted recovery.
- Light nearby terrain smoothly while holding a light-emitting block or lava bucket, including in the offhand. This changes rendering without changing world light or spawning.
- Add automatic tool selection and optional hotbar restocking, with durability controls and checks for native item data, mining targets and manual input. Both start off with unbound toggle keys and appear in Active tweaks when enabled.
- Sort native inventories and transfer items matching chest contents with compact controls, protected storage slots, preferred item positions and configurable bindings. Paint protected slots by dragging, save sorting layout presets, and keep hotbars, equipment, crafting inputs and custom inventories protected.
- Save nine Creative hotbars and load them through a menu or configurable held-key shortcuts. Edit saved hotbars from Options in any mode with Creative access. Saved hotbars and sorting layouts remain separate from settings defaults.
- Back up, duplicate and recreate saved worlds from the world menu. Verify copied files before publishing them and give duplicated worlds independent map and recovery identities.
- Add optional directional sound subtitles with configurable duration, visibility and HUD placement. Subtitles start off. Captions remain available when game audio is muted.
- Integrate the new features with searchable Options, selected defaults, shared input ownership and HUD drafts. Power Beta leaves subtitles, automatic tool switching and automatic restocking off; Beta and Refined Beta leave them off.
- Place the offhand beside the hotbar with the Creative destroy slot above it, align inventory extensions and keep compact inventory actions inside the panel. Use modern per-item hand transforms and correct native tool texture orientation.
- Keep the recipe book stable through crafting and inventory changes, align item tooltips, and offer modern or compact inventory buttons. Support wheel paging and native search styling, with pause-menu recipes in a joined crafting or smelting panel.
- Keep minimap zoom responsive while cave tiles load, reset it by right-click to a chosen zoom step, and avoid rebuilding Options lists for each scroll event.
- Start sprint combat restrictions only after taking mob damage, and accept on/off aliases in /gamerule.
- Include Power Beta settings in world backups, with optional maps and historical metadata backups.
- Keep configuration format 7 with support for formats 3–6. World-rule format 4 supports formats 1–3 and earlier metadata; verified upgrades retain existing world rules and prevent older clients from dropping new inventory ownership.

## [3.3.0] - 2026-10-21

- Keep the minimap visible above inventory dimming and below container panels. Scroll over the minimap to zoom smoothly and precisely in inventories, the HUD editor and map settings previews.
- Limit entity radar by horizontal range, capped at render distance. Discover entities through bounded loaded-chunk scans and keep tracked markers moving smoothly during large entity bursts.
- Respect hidden entity categories on the minimap without spending time tracking them. The open World Map retains its independent radar view.
- Search all Options settings or only the current tab with a remembered search-scope toggle. Follow accurate HUD editor paths and open minimap appearance results directly beside the World Map when it fits.
- Open World map appearance directly from Options with live previews and reliable return navigation. Add a Chunks toggle, clearer Spawnable and Path labels, and keep map zoom working while dragging the scale.
- Give search clear, scope and section buttons consistent square bounds and centered icons.

## [3.2.2] - 2026-10-21

- Continue sprinting when leaving flight while moving forward, with normal ground-sprint permissions, stamina and combat checks.
- Keep mob heads visible with a World Map Mobs toggle, and allow the hold key while map settings are open. The minimap retains its hold-only behavior.
- Return directly to Options when leaving the minimap settings sidebar opened from its Edit button.
- Simplify the minimap zoom-step editor with one row of actions and less header text.
- Set Power Beta defaults to 75% mob icons and a brighter red hostile-mob color.

## [3.2.1] - 2026-10-21

- Preview every Extra sounds option, including eating, shearing, breakage and chest sounds. Refresh water and lava bucket sounds from current official Minecraft assets.
- Prevent accumulated footstep sounds from playing rapidly after flight or fast movement. Include active freecam in the Active tweaks HUD by default.
- Open Minimap appearance directly in the World Map settings sidebar when it fits, with the HUD editor available on smaller screens or when the World Map is disabled.
- Animate minimap zoom smoothly and wrap zoom keys through editable, exact zoom levels, including 1.25 by default. Retain terrain while preparing zoom changes.
- Add adjustable mob-head size on both maps, smaller default heads and normalized squid icons. Keep height chevrons closer and brighter, especially while showing heads.
- Update Power Beta radar colors for dropped items, vehicles and other entities. Set minimap waypoint text to 70% and icons to 90%, allow edge arrows down to 5%, and clarify the waypoint-range tooltip.

## [3.2.0] - 2026-10-20

- Select and copy chat history with the same translucent blue highlight as the typing row. Preserve selection through wrapping, window resizing, scrolling and new messages; scroll while dragging beyond the visible rows.
- Extend dark menu headers and the Options footer across the window while keeping controls within their content width. Give Auto apply enough padding in narrow map settings panels.
- Hold the radar icon binding to replace entity dots with upright, outlined mob heads on both maps. Cache native head models with bounded work and restore dots on release.
- Customize radar colors by entity category and individual mob, with inherited colors by default. Add minimap waypoint text/icon size, arrow opacity and direction text color controls to the shared HUD editor and live map settings.
- Pause in map menus in Power Beta defaults. Start with a 90-pixel minimap at zoom 1.25, dropped items visible, smaller waypoint arrows and direction text, and biome names hidden. Collapse map performance controls by default.
- Show the recorded death cause when hovering new deathpoints. Keep older deathpoints readable without inventing a cause.
- Enter Go to coordinates in a dialog over the live world map. Group map controls into two rows with Light, Slime and Recent path toggles, and use the normal screenshot key for map captures.
- Align waypoint footer actions to the content edges, open editing on double-click and center visibility icons. Delete named sets with confirmation while moving their waypoints safely to Default.
- Preview extra sounds directly beside their toggles using the same controls as individual sounds.
- Keep configuration format 7, world-rule format 3 and waypoint format 1 with their existing supported conversions. Existing settings, worlds and recovery data are preserved.

## [3.1.0] - 2026-10-20

- Edit map settings beside the live world map, with a minimap preview, shared appearance controls, scoped search and related-control navigation. Keep the previous cave view visible while a new level is prepared. Preserve terrain alignment and clip it to the map pane when panning with settings open.
- Keep sidebar scrollbar drags active through mouse movement and fit Reset page beside the other footer actions at narrower widths.
- Place minimap abbreviations closer to their markers and show death times without dates. Open Minimap appearance without a preselected setting highlight.
- Add the modern XYZ debug crosshair, including its HUD editor preview. Enable it in Power Beta defaults.
- Hide the stationary player on freecam entry only until the camera first leaves the head. Keep it visible for the rest of that visit.
- Play the retro title entrance only when opening a fresh menu with that logo selected.
- Review changed new-world defaults with a detailed comparison, Reset and Keep current. Untouched templates follow Defaults base changes automatically; customized and incompatible templates retain their review. Keep existing worlds unchanged. Enable sprinting in all Cheats defaults and Power Beta Survival defaults.
- Save your submitted chat messages across sessions, with a separate history limit from other chat. Both limits support 1–5000 messages. Use a familiar blue text-selection highlight.
- Keep configuration format 7 and world-rule format 3, with their existing supported conversions.

## [3.0.0] - 2026-10-20

- Add a minimap and explored world map with saved surface and cave terrain, texture colors, relief shading, entity radar, waypoints and deathpoints.
- Keep map movement smooth between terrain updates. Prioritize visible terrain and nearby gaps, retain the previous view while loading more detail, and choose zoom detail from the game window's physical resolution.
- Add map performance controls, continued updates while the world map is open, and bounded caches and background work. Disabled map features stop their unneeded work.
- Customize the live minimap in the HUD editor, including position, size, zoom, frame, compass, text, markers and edge arrows. Start with a circular map at zoom 1 and recent paths off.
- Shade surface maps at night and caves by their light levels, with full bright bypassing both. Ignore outdoor tree cover when detecting caves. Add adjustable cave search ranges, vertical radar dimming, subtle height chevrons and vehicle markers.
- Toggle red spawn-light shading from the world map with its own survey radius. Add map screenshots, a movable comparison scale, coordinate and distance readouts, and compact navigation controls.
- Organize waypoints into named sets with abbreviations, fuzzy search, visibility toggles and context actions. Add shared destination colors, shaded diamond markers and skull deathpoints with timestamps. World markers support both view-bobbing modes and default to Fixed to world.
- Add map and waypoint bindings, plus configurable photo, map and waypoint shortcuts in the pause menu. Related controls and settings jump to their highlighted row with a Back action.
- Add synchronized color-wheel and RGBA editors with combined color/opacity controls, a larger preview and confirmation before discarding edits. Use consistent American English and hide tooltips while dragging sliders.
- Add modern ground sprinting through the per-world Sprinting rule. Power Beta defaults enable sprinting with stamina and combat blocking; Refined Beta and Beta defaults keep it off. Speed, activation, stamina, combat and FOV preferences remain global.
- Carry sprinting smoothly into flight, with optional dynamic FOV and adjustable strength. Creative and all flight modes bypass stamina. Add modern sprint swimming with faster look-directed movement, one-block collision and swimming animations.
- Show Survival stamina as ten gold lightning icons with half steps and an exhausted-only restart reserve. Customize its position in the HUD editor and keep it visible in chat, pause and inventory screens. Set a 30% restart threshold and separate standing/moving recovery rates.
- Replace warp commands with `/waypoint`, `/way` and `/wp`, including creation, removal, listing, destinations and permitted teleportation. Support detached-camera coordinates and permitted WorldEdit commands in freecam. Add case-insensitive completion and selectable chat history.
- Add modern planting, hoe, bucket, milking and bone-meal sounds in Power Beta defaults, and native clicks for death-item recovery controls.
- Fix standalone game-rule editor crashes, Creative rapid-click misses and continuous-mining pauses across air gaps. Preserve native mining damage, held Creative repeat timing and multiplayer behavior. Prevent duplicate boat placement when boarding with a boat item.
- Preserve existing settings, worlds and recovery data. Terrain format 2 reads format 1; configuration format 7 supports formats 3–6; world-rule format 3 supports formats 1, 2 and earlier world metadata.

## [2.5.3] - 2026-10-19

- Place one boat or minecart per right-click in Creative, without repeated placement while holding the button.
- Keep ridden boats visible through frustum and occlusion culling, with original or improved boat movement.
- Keep capes aligned behind boat riders instead of twisting sideways during travel.
- Add Modern water movement for singleplayer players, with gentler currents and sinking, and Sneak-to-descend at normal swimming speed. Enabled in Power Beta defaults; disabled in Refined Beta and Beta 1.7.3 defaults.

## [2.5.2] - 2026-10-19

- Use WorldEdit selection and query commands from Spectator or freecam, including current-view block targeting and camera-relative selection directions. Existing access rules still protect world changes.
- Keep the ridden minecart visible when looking backwards, with normal or faster minecart movement.
- Shift-drag across armor pieces to equip or remove them, preserving native item data and slot limits.

## [2.5.1] - 2026-10-19

- Place boats facing the direction you look, with original or improved steering and either speed setting. Add Boat placement direction fix under Fixes, enabled in Power Beta and Refined Beta defaults and disabled in Beta 1.7.3 defaults. Singleplayer only.

## [2.5.0] - 2026-10-18

- Choose Power Beta, Refined Beta 1.7.3 or Beta 1.7.3 defaults on first launch or from the new Defaults tab. Load a setup or change the reset base without changing current values. Power Beta enables ore vein mining, torch shovel, faster vehicles, clearer water and a blurred Beta 1.8 panorama, with standard controls, full volume and modern Creative/Spectator flight response. Previous Full and Lite selections remain readable.
- Start every defaults base at GUI scale 4x, keeping the welcome selection from switching to Auto scale.
- Enable panorama blur by default in every defaults base while retaining each base's selected background.
- Keep welcome and Defaults tooltips above buttons, use the normal Options tooltip delay and hover backgrounds, and place the welcome reminder below the choices. Defaults rows match ordinary setting sizes and spacing, and Options tab labels sit centrally.
- Move the HUD editor to General and add F3 row selection, range dragging, saved layouts and a separate Save/Discard boundary. Small screens get compact preview toggles and a settings-panel toggle, with more room for F3 reordering.
- Keep title-editor reset confirmations above the paint grid, including while playing a world.
- Keep music labels steady when skipping forward or back between tracks.
- Bring game-rule names and controls closer together in creation and saved-world editors.
- Keep HUD preview toggles beside the selector at more screen sizes, add a preview visibility button on small screens, reduce footer spacing and restore the blue selection marker. Hide the HUD editor launcher's reset button and use its normal search group.
- Keep Defaults actions close to preset names, explain reset bases in plain language and allow the explanation and choices to scroll on small screens.
- Show percentages in slider values instead of setting names. Default flight sprint to Toggle in every base and set Power Beta freecam speed to 110%.

## [2.4.2] - 2026-10-18

- Group general Interface settings below the HUD editor without an extra divider.
- Keep Toggle in preview controls beside the HUD selector when space allows. Clicking the crosshair, achievement toast or controller prompts highlights its setting and preview outline until another element, empty preview space or setting is selected.
- Center standalone game-rule editors without a sidebar, preserving the settings area's width.
- Keep the next music track stable while adjusting settings before playback starts.

## [2.4.1] - 2026-10-18

- Fix F3 drag releases and save both information columns, with cross-column moves, gap rows and Undo shared by the preview and live HUD.
- Start HUD previews with the forest background enabled, add independent toast, chat and active-tweaks preview toggles, and remember the editor section and scroll position.
- Preview F3 entity ID tags over a mob in the forest scene, and expose Show F3 entity id tags with an On default.
- Place general Interface settings above the groups, fix the HUD editor search heading, restore black world selections and hide Rename beneath its inline input.
- Add Clear logo with Undo, compact the lettering editor's utility buttons and open photo mode slightly closer.
- Prepare hotbar recovery snapshots while selecting a row, so ready swaps apply on release with durable backup and inventory validation retained.
- Preserve music pause through dimension and menu transitions, including custom menu music.

## [2.4.0] - 2026-10-17

- Reorder individual F3 information lines by dragging them in the HUD preview, leave blank rows between them, control each line's visibility and change the text size.
- HUD previews share the in-game info-cluster formatting, show chat with its native cursor, and mark hidden clusters only while selected. Background previews start off and include a clear daytime forest scene captured at 12 chunks.
- Find HUD settings under their editor sections in search, edit individual results directly, and open the matching editor section from its heading.
- Reorganize Interface settings into General, Title screen and Screenshots, with a Customize button beside Retro titles. Editor controls share inline editing, individual resets and native click sounds.
- Show the current Power Beta version beside Minecraft's title-screen version, with an option to hide it, and remove blocking settings saves when cycling logos.
- Rename worlds inline with verified metadata backups, show each world's name in its editor, and keep panorama world selections translucent.
- Enlarge the achievement map on larger screens and return to the pause menu with Done or Escape.
- Show panoramas across main-menu screens by default, use the current Power Beta version as the default custom version text, reset weather after sleep only while raining, and limit TNT work by default.

## [2.3.0] - 2026-10-17

- Adapt the HUD editor to wide and portrait screens, combine normal and underwater previews, and keep crosshair, achievement and controller options under General.
- Add a forest preview background, outlines for hidden info clusters, individual resets and delayed tooltips. Keep nested color swatches in the editor draft until Done or Save.

- Share collapsible Options groups with the HUD editor. Reset all HUD settings, all positions or only the selected element, with Undo for every reset.
- Preview selected item names only when enabled, using their shared message position. Bring item-name controls, crosshair, chat, controller prompts and achievement toasts into the HUD editor.

- Preserve the cursor position between main-menu screens and restore clickable title-screen music controls alongside logo cycling.
- Simplify the HUD editor to one top selector, group related settings and rename custom info clusters. Select preview elements before dragging, and reset positioning separately from visibility, styling and content.
- Use the Game sounds setting name in category and individual-sound volume tooltips.
- Use grayscale colors in the HUD element dropdown and add individual default-reset buttons for title rotation and appearance controls.
- Keep panorama motion continuous across title and main-menu screens, and restore native dirt headers and footers on scrolling lists.
- Refine title painting with left-click paint, right-click erase, filtered one-click brush selection, block previews and saved swatches. Add live rotation sliders, inline editing, animation replay and taller controls when space allows.
- Click the title logo to cycle retro lettering, saved presets and official styles; right-click cycles backwards while preserving the custom retro draft.
- Share clearer underline tabs between music views and the HUD/title editors. Move HUD element selection above settings, improve its menu and replace Expand with a draggable divider.
- Remove the game-sounds divider while retaining indented category controls.
- Add an interactive HUD editor with draggable previews, screen anchors, precise offsets, snapping and undo. Bring native HUD positioning, four custom info clusters, active tweaks, version labels and F3 settings into one editor using the active texture pack.
- Paint retro title lettering on a tile grid, choose blocks per tile and preview the actual 3D logo. Keep rotation, shadow and animation controls together, with saved presets, undo and a default Minecraft logo reset. Done accepts edits; unsaved exits offer Save, Discard or Cancel.
- Add Panorama only on the title screen, enabled by default. Turn it off to show panoramas across main-menu screens, with darker backgrounds behind settings and readable native world and texture-pack lists.
- Fix a regression where every game tick could wait for background settings writes. Restore smooth fake-sneak, fast-placement, light-overlay and other gameplay toggles, with the Active tweaks HUD shown or hidden.
- Batch light-overlay numbers into one font draw and limit scanning time per tick to reduce toggle and refresh stutters. Preserve native font textures, light values, depth occlusion and deferred settings saves.
- Double-click gathering combines partial stacks before full stacks and uses player inventory before container slots. Preserve native item data, stack limits and multiplayer transfers.
- Indent detailed game-sound volume controls. Clarify that scrolling menu backgrounds do not affect panoramas.

## [2.2.2] - 2026-10-17

- Add Axes slash leaves, off by default. Axes cut all leaf variants quickly according to their material, with diamond taking about the same time as a diamond shovel on sand. Keep vanilla leaf drops and normal tool durability.
- Flight and freecam acceleration and deceleration sliders move in whole-number steps. Right-click input and saved preferences retain fractional values consistently in every flight mode.

## [2.2.1] - 2026-10-16

- Creative and Survival report consistent feet or eye Y coordinates when walking, falling and flying. Only Spectator and freecam use head-base Y, shared by F3, the custom HUD, commands and WorldEdit.
- Add independent 0–5 acceleration controls for Creative, Spectator and freecam, including vertical movement and direction changes. Keep modern acceleration at 5 and give freecam a quicker default of 3.5, similar to its earlier response.
- Rename flight drift to deceleration while preserving saved values. Acceleration and deceleration retain the configured flight speed and freecam frame timing.
- Keep Spectator and freecam head markers intact inside terrain, with correct depth and rendering after translucent terrain. Preserve the solid player marker and translucent detached-camera marker.
- Dropped items protected by the lava-destruction rule sink naturally instead of bouncing on lava. Keep native damage and bouncing when the rule is enabled, including multiplayer authority.

## [2.2.0] - 2026-10-16

- Unify flight speed percentages, sprint controls and drift tuning across Creative, Spectator and freecam. Sprint is always available, and freecam starts at 110% speed with drift 3. Its frame timing remains unchanged.
- Freecam ignores collisions in Creative and Spectator while retaining the Survival collision preference. Spectator shows a solid head at the player's position alongside the translucent detached-camera marker.
- WorldEdit selection and coordinate-based commands follow the reported location, including the feet-coordinate preference and detached camera.
- Move Advanced OpenGL to Video performance and 3D anaglyph to the initially collapsed Advanced miscellaneous group. Clean up percentage labels and keep music tracks in library search only.
- Separate game-sound volume from its category sliders with an in-group divider.
- Configuration format 7 supports formats 3 through 6, preserves saved freecam speed, converts its old drag value once and retires the flight-sprint switches and classic freecam movement. World-rule format remains 3, with formats 1 and 2 still supported.

## [2.1.1] - 2026-10-16

- Set bed respawn points during the day with sleeping enabled, while retaining native sleeping and multiplayer behavior.
- Add Edit game rules to the saved-world Edit menu, with verified metadata backups and checks for changed or unsupported data.
- Combine panorama controls into one choice with the original dirt background as the default. Show one option per distinct scene while preserving saved selections from older versions.
- Preview panoramas live in the picker. Apply selections immediately, retain the selection marker and leave with Back or Escape.
- Use the clean release 1.0 images for the Beta 1.8.1 panorama, removing its purple seam while keeping its name and saved selections. Center the panorama picker buttons and search field.
- Adjust panorama blur directly in the picker, sharing the same setting and preview as the main settings list.
- Return to Select world when pressing Escape in the world Edit menu.
- Center Beta, 2017 and 2020 title lettering and their Java Edition subtitles using the visible texture bounds. Keep the already-centered modern style and custom retro title.
- Remove the panorama picker's search field and give its choices more space.
- Group Active tweaks HUD inclusion switches below its display controls, and move Full bright messages below Brightness.

## [2.1.0] - 2026-10-16

- Restore native walking, jumping, falling and bobbing whenever flight is off. Entering Creative silently restores its flight ability, and flight tooltips explain their speed and drift baselines.
- Apply title panorama choices immediately when clicked, with a single Cancel button.
- Add Title style choices for the official Beta, 2017 Java Edition, 2020 Java Edition and modern logos. Keep customizable retro lettering as the default and preserve its customization between styles.
- Add allow_sleep while retaining bed respawn points when sleeping is disabled. Use a 0–100% sleeping slider and rename sleep_ambushes to sleep_nightmares, retaining the old command aliases.
- Add world rules for ghast explosion fire, lightning strike fire and explosion block drop chance. Default ghast fire on, lightning fire off and block drops to 100%. Remove the global rain-disabling setting; weather uses its world cycle rule.
- Add /fly and allow_flight for flight outside Creative. Creative can toggle its flight ability independently of its inventory and protection; Spectator keeps its own flight.
- Give Creative, Spectator and freecam separate flight sprint activation and speed preferences. Group each mode's flight controls together.
- Render title panoramas at window resolution with optional blur, and add a searchable picker for original backgrounds from Beta through Minecraft 26.3.
- Reorder Interface, Video and Audio settings, group item and TNT performance controls together, and clarify Game sounds, fast-placement filters and Active tweaks HUD inclusion labels.
- Hide inactive combined or separate HUD position groups. Start secondary Interface groups, Gameplay, Fixes and selected detailed groups collapsed, preserving later manual expansion.
- Move field of view into General, place Creative sapling drops below reach controls, and enable fast leaf decay by default.
- Configuration format 6 converts formats 3, 4 and 5 with verified backups. World-rule format 3 converts formats 1 and 2 and older world metadata, preserving existing sleep, fire, drops and rule choices. Renamed nightmare rules retain their values; old sleeping percentages above 100 disable sleeping explicitly, and old rain suppression freezes the weather cycle.

## [2.0.2] - 2026-10-16

- Smooth freecam movement across uneven frame times, including ascent and descent. Freecam now defaults to speed 12 and drag 10.
- Enable Beta 1.8 leaf rendering by default.
- Add space below the world seed field and simplify Custom world creation to start in Survival.
- Mark saved worlds with [Creative] when their rules allow Creative mode.
- Put Creative and Spectator permissions together and group related game rules in every editor.

## [2.0.1] - 2026-10-16

- Keep world-creation buttons directly below the name and seed fields. Unavailable game-mode icons now appear in grayscale.
- Show menu music controls by default, use Enter for chat and F4 for the custom HUD, and start with only coordinates and facing in the top-left HUD cluster. F3+F4 keeps its mode-switching action.
- Combine chest sounds into one Off, Modern or Door choice, defaulting to Off. Remove burping, Better burning, punch-to-ignite TNT and the Spectator block-outline option.
- Keep flexible placement available through its hold controls, with every placement key unbound by default. Rename edge protection to Fake sneak.
- Add separate player and mob crop-trampling rules, both off in new-world presets. Move lava and cactus damage to dropped items into world rules with vanilla defaults.
- Show explosion block drops as a 0–100% slider, defaulting to 100%, and craft six stairs at a time.
- Enable flint-and-steel TNT priming, punch-to-defuse TNT, original far-distance fog, larger chicken hitboxes, grass/fire recipes, gold equipment abilities, the gold-block sponge ritual, sponge absorption, trapdoor glue, entity ignition and returned-fireball ghast kills by default.
- Default lightning fire off, tick the world during accelerated sleep and allow clicking to wake. Default stacked chests, sign dye colors and vignette off.
- Keep freecam and Spectator out of front view regardless of HUD visibility. Freecam leaves the HUD visible by default.
- Configuration format 5 converts formats 3 and 4 with verified backups. World-rule format 2 converts format 1 and older world settings, preserving existing rule values and item protection. Conditional footwear protection requires an explicit trampling choice before converting its world.

## [2.0.0] - 2026-10-15

- Replace the Cheats switch and per-command overrides with 31 explicit world game rules and independent permissions for Creative, Spectator, editing, items, travel and recovery.
- Add /gamerule with familiar aliases, searchable Game rules settings, editable Survival and Cheat defaults, and a Custom world-creation editor with a starting-mode choice.
- Add world rules for player fall, fire and drowning damage, Peaceful regeneration, mob drops, sleeping percentage, dungeon spawners, sleep ambushes and Nether portal entry.
- Separate creeper and ghast block destruction, fire spread and natural spawning. Protect dropped items from every explosion with explosions_destroy_items; disabling TNT pauses existing fuses and prevents new priming.
- Add /repair for the held item, preserving its count and custom data. Keep read-only command tools available independently of permissions.
- Add default-on Shift-double-click matching-item transfers while preserving the held stack, native item data, source ownership and destination limits. Keep quick gestures reliable across slow frames, and stop drags safely if their items change.
- Simplify shortcut settings, remove redundant recipe and tool masters, and replace fixed command/debug-graph switches with configurable bindings. Remove drag highlighting and the falling-pipe sound; bulk crafting always stops when its result changes.
- Revise defaults for inventory movement and looking, container carrying, crafting storage, modern recipes, tool fixes, slab completion and faster obsidian mining. Keep rendering and performance settings grouped with their related controls.
- Preserve Spectator's unclipped third-person camera with front-view mode enabled, and keep vanilla servers authoritative over bucket, door and sign stack limits.
- Preserve custom item data and full counts when saving player crafting inputs. Keep older crafting-grid saves readable.
- Open numeric item settings in the shared graphical picker, with drafts kept local until Done.
- Use global configuration format 4 with supported conversion from format 3, and world-rule format 1 with conversion from earlier world settings. Verify backups before conversion and preserve unsupported or conflicting data with recovery guidance.

## [1.7.6] - 2026-10-15

- Keep auto-walk moving the player during Creative and Spectator flight in freecam.
- Base water fog and underwater field of view on the rendered camera, including third person and free look.
- Add an option to disable Beta's underwater field-of-view narrowing.
- Add a separate water-darkness slider for viewing water from outside, defaulting to Beta lighting.
- Switch water darkness instantly when crossing the surface or adjusting its sliders, without rebuilding terrain.

## [1.7.5] - 2026-10-15

- Refill oxygen in Creative and Spectator and prevent drowning bubbles, including when returning to Survival.
- Add an underwater darkness percentage, independent of fog, that restores daylight through water without changing world light levels.
- Smooth the frozen player body when entering freecam during Creative flight.

## [1.7.4] - 2026-10-15

- Add an underwater fog density slider from clear water at 0% to vanilla fog at 100%, with full bright overriding it while active.
- Start new Creative worlds silently while keeping normal game-mode change messages.
- Defer the debug toggle until F3 is released for the default F3/F4 game-mode controls, avoiding flicker when switching modes. Remapped controls keep their normal timing.

## [1.7.3] - 2026-10-15

- Silence water-entry splashes in spectator mode.
- Add an option to remove the underwater texture overlay, and hide it automatically while full bright is active.
- Let full bright see through underwater fog, using the configured terrain fog distance.
- Start new worlds in Creative when Cheats is on; keep Survival when it is off.
- Warn before discarding unsaved block rules and item or tool selections.
- Draw the creative Clear inventory tooltip above inventory items.

## [1.7.2] - 2026-10-15

- Hide target block outlines in spectator by default, with an option to show them.
- Remove the saved freecam camera model and its setting.
- Find items and blocks with fuzzy search, including abbreviations and small typos.
- Remove clipboard shortcut hints from value editors.
- Restore the native sign editor and sign text rendering and storage.

## [1.7.1] - 2026-10-15

- Keep hotbar recovery writes off the game thread and reuse inventory snapshots to reduce swap stutter.
- Align freecam floor collisions with the head-base Y coordinate shown by F3 and the custom HUD.
- Keep spectator third-person cameras and ghost heads visible inside solid blocks.
- Keep the player's body visible when freecam moves close to it.
- Add an option to keep the HUD visible when entering freecam.
- Extinguish fire before breaking the block behind it in creative mode.
- Edit vein-mining blocks and tools as individual rules, with a shared searchable icon picker for item and block settings, metadata variants, ID tooltips and validated custom IDs.

## [1.7.0] - 2026-10-14

- Add optional vein mining with configurable blocks, required tools, crouch activation, mining time, durability and bounded search/mining budgets.
- Add optional torch mining for sand and gravel using the most worn diamond shovel in your inventory, including normal tool wear.
- Add a temporary click-mining toggle hotkey and optional Active tweaks indicator, resetting when leaving a world.
- Add a global fire-spread setting, enabled by default, covering fire spread and lava ignition.
- Share eye/feet Y-coordinate selection between F3 and custom HUD clusters. Freecam and spectator report the base of the head.
- Show a translucent player-skin head in third-person freecam and spectator, including free look.
- Allow manual full bright without cheats.
- Standardise text-input cursor movement, selection, clipboard shortcuts and mouse dragging, with a separate UI cursor-blinking preference.
- Identify HUD clusters in search-result labels and let group chevrons expand or collapse search results silently.

## [1.6.2] - 2026-10-14

- Use configured creative reach for container previews in every game mode, including survival and freecam.
- Show freecam coordinates, facing, biome and local world information in F3 and custom HUD clusters while detached.
- Move Complete slabs through adjacent faces into Placement rules beside Shift placing, and remove its toggle messages and Active tweaks entry.

## [1.6.1] - 2026-10-13

- Preview container contents from the detached freecam, including storage minecarts, without opening or changing inventories.
- Hide the HUD on entering freecam and restore its previous visibility on exit. F1 can show it during freecam, and held container previews remain visible with the HUD hidden.
- Preserve Flexible placement and Complete slabs settings when leaving worlds or quitting, while clearing temporary placement modes.
- Remove the slab-completion hotkey; change the feature through its setting.

## [1.6.0] - 2026-10-13

- Turn off active movement, building, camera and render tweaks when leaving a world or quitting the game.
- Add a button beside search to collapse or expand every section in the current settings view.
- Add optional lava protection for dropped items. Off by default.
- Draw flexible placement grids and previews clearly through water and lava.
- Keep auto-walk active when focus is lost, with an optional setting to cancel it instead.
- Fix HUD positioning previews, screen-edge placement, visibility and food-preview alignment. Show which positioning controls apply to each mode.
- Add an optional 12-hour daylight clock to F3, including frozen daylight. Off by default.
- Add four custom HUD info clusters in Interface, with independent content, anchors, offsets, text size, color, opacity and F3 visibility. Choose coordinates, facing, biome, time, day count, chunk coordinates and frame rate.
- Restore the inventory cursor to its exact starting position after right-click looking, with both native and raw mouse input.
- Match the section collapse button to the search-clear button and separate its inward chevrons.
- Preserve quick inventory-look gestures without losing their cursor origin or camera movement.
- Add an unbound hotkey for toggling all custom HUD clusters together while preserving their individual settings and F3 visibility.
- Show HUD coordinates as comma-separated values, with the facing axis and compass direction alongside them when enabled.
- Keep background movement-key events and focus transitions from canceling auto-walk when focus-loss cancellation is off.
- Keep auto-walk moving while an inventory is open and the game loses focus.
- Preserve cursor position and camera movement through consecutive fast inventory drags, including movement immediately after release.
- Restore custom skins when the first skin provider returns an incomplete profile, and prevent bundled fallback skins from crashing the texture downloader.
- Restore photo-mode mouse panning and rotation, and preserve quick drags, slider changes and time restoration through resizing.
- Center the menu music scrub track above and below the menu.
- Keep the scroll position when accepting a settings search destination by clicking its current tab.
- Remove padding before positive facing axes in custom HUD coordinates.
- Add optional cactus protection for dropped items. Off by default.
- Extend collapse/expand-all to the groups in the current music-library view and search results.
- Keep the inventory character model steady while right-click looking hides the pointer.
- Limit excessive cape rotation during fast movement, with a default-on option in Fixes.
- Preserve free-look direction when switching between first and third person while holding free look.

## [1.5.2] - 2026-10-13

- Activate auto-walk while walking backwards to keep walking backwards. Forward and back still cancel it; freecam and free look remain independent.
- Add optional camera movement in inventories. Start holding right-click outside the GUI to look around when Movement in inventory is enabled. Keep looking across the GUI with the cursor hidden until release. Off by default.
- Keep inventory camera movement working with Raw Input enabled, while preserving raw mouse movement during gameplay.
- Make Previous replay the last track during a break between songs. During playback, it still goes to the track before the current one.

## [1.5.1] - 2026-10-12

- Preserve ambient occlusion and smooth corner shading with full bright.
- Add optional right-click TNT ignition with flint and steel, including reusable ignition in creative. Off by default.
- Add Fast item and TNT collisions to Fixes, on by default and switchable without restarting. Avoid redundant collision scans in crowded item piles while preserving block and vehicle collisions.
- Spread item merging across short, resumable sweeps, cover the terrain render distance and reduce idle checks. Preserve item counts, damage and native item data.
- Smooth the displayed movement of TNT while its simulation is limited, without advancing queued fuses or changing physics.

## [1.5.0] - 2026-10-12

- Add an unbound entity visibility hotkey, selectable entity types, HUD status and optional toggle messages. Dropped items are selected by default.
- Add optional merging of nearby dropped items above a configurable threshold, preserving stack limits, damage and item data.
- Add an option to protect dropped items from explosions.
- Add optional TNT work limits to spread large chains across ticks and help reload struggling worlds. Pending TNT keeps its saved fuse.
- Stop creative TNT breaking from dropping blocks, and add an option to suppress saplings while leaves decay in creative.
- Shift-click creative catalog items to transfer one full stack into the inventory, respecting item and stacking limits.
- Enable Auto apply by default for new settings profiles, and move death-record settings into Gameplay.
- Make Next end music previews and resume the previous track without consuming the queue. Right-click the song label to open the queue, or the top of Audio when the queue is empty.
- Prevent rapid music-preview cancellation from stopping the sound streaming thread, and keep first-time audio previews from saving settings.
- Add /spawn with cheat gating, shared help and command completion.
- Add an unbound full-bright hotkey and optional automatic spectator lighting. Both affect rendering only and turn off when cheats are disabled.

## [1.4.4] - 2026-10-12

- Show nearby entities and block entities based on the freecam camera position, including dropped items away from the player.
- Target pillars accurately with /j and /jumpto, including narrow crossings at the top block, without loading distant terrain.
- Make Hide excluded hide individual tracks inside mixed music groups in the library and preset editor.

## [1.4.3] - 2026-10-11

- Restore text buttons and show the death number and timestamp in the saved inventory view, without the held label or armor-panel black bar.
- Save and recover the four player crafting inputs in their original positions, and drop them once on normal deaths.
- Prevent crashes after long-distance WorldEdit jumps by keeping background culling out of chunk generation and fixing chunk sorting.

## [1.4.2] - 2026-10-11

- Simplify the saved death inventory view and cancel recovery when original slots are occupied.
- Recover death items through the inventory view only, and list deaths from oldest to newest with numeric IDs. Existing death logs start fresh.
- Add Keep inventory on death as a per-world cheat rule, off by default, with kept deaths marked recovered and items preserved through respawn.

## [1.4.1] - 2026-10-11

- Default lost-item chat notices to off and allow death recovery in survival with cheats.
- Save original inventory and armor positions, restore them when available, and show deaths in an inventory view locked without cheats.
- Allow one-time recovery of individual saved stacks with cheats, sharing recovery claims with /death recover.

## [1.4.0] - 2026-10-11

- Remove the player-model flicker when entering freecam while walking.
- Add optional death records with locations, causes, lost items and creative-only recovery, off by default.
- Show available commands and aliases in /help, group WorldEdit under //help, and add per-command help.

## [1.3.2] - 2026-10-11

- Middle-click drag a held item in creative inventories and containers to fill compatible slots to their normal stack limits.

## [1.3.1] - 2026-10-11

- Add optional empty bucket stacks of 16, off by default, with safe fluid pickup and milking.
- Start freecam from the current rendered perspective while walking, preserving camera interpolation and view bobbing at entry.

## [1.3.0] - 2026-10-11

- Add Water negates fall damage, off by default, for modern-style shallow and flowing water landings and water bucket clutches.

## [1.2.2] - 2026-10-10

- Remove frame stutters when toggling fast placement, edge protection, slab completion, light overlay and other gameplay settings.

## [1.2.1] - 2026-10-10

- Preserve chat colors when messages wrap onto another line.
- Show entities and block entities during freecam, then resume normal culling when freecam ends.

## [1.2.0] - 2026-10-10

- Add an optional Inventory in portals setting, off by default, with normal item cleanup before dimension travel.
- Add optional Shift-click transfers into crafting tables, off by default.
- Keep auto-walk moving the player while freecam controls the camera.
- Add an optional Low fire texture setting that lowers on-screen flames, off by default.
- Add separate camera shake controls for all damage and fire damage, both on by default.
- Add Auto-mine with an unbound toggle hotkey to hold the normal attack and block-breaking action.
- Fix dropping carried items outside the creative catalog, preserving whole-stack and single-item drops.

## [1.1.1] - 2026-10-09

- Keep the chat cursor visible by default, with optional blinking.
- Add an F3 text color setting, defaulting to white, and prevent overlapping debug rows.
- Group world cycle and spawning switches under World overrides.
- Apply flight sprint in every direction, including sideways and vertical movement, in creative, spectator and freecam.

## [1.1.0] - 2026-10-08

- Keep command history navigation separate from completion suggestions.
- Add a chat caret, text selection and copy, cut and paste shortcuts.
- Add an optional facing direction to the F3 debug overlay.
- Match WorldEdit contraction directions and add top and bottom aliases.
- Add //calc and named block states for rails, torches and other directional blocks.
- Fix redstone pick-block items and allow backwards flight sprinting and freecam sprinting.
- Add per-world hostile and passive natural spawning controls while preserving existing mobs and /summon.

## [1.0.1] - 2026-10-08

- Fix isometric capture bounds and centering with custom render distances.

## [1.0.0] - 2026-10-08

- Combine the pack's settings into one searchable Options menu with Apply, Cancel and live previews.
- Preserve vanilla Beta mechanics by default, with optional gameplay changes and performance improvements.
- Add creative and spectator modes, a mode switcher, an expanded inventory and instant vehicle removal in creative.
- Add WorldEdit, modern singleplayer commands, held-block arguments and immediate command completion.
- Add flexible key bindings, inventory movement, free look, free camera, auto-walk and building controls.
- Add container carrying, inventory previews, light levels and an Active tweaks display with optional toggle messages.
- Add custom music, track previews, playlists, independent volume controls and saved presets.
- Add per-world cheats, difficulty, daylight and weather controls while preserving scheduled updates.
- Add faster vehicles and obsidian mining, configurable recipes and gold tool abilities.
- Add optional sign editing with Escape to save and close.
- Add old cobblestone and brick textures, redstone power numbers and optional redstone placement sounds.
- Add isometric screenshots and detailed camera, graphics and sound controls.
- Fix Linux key handling, inventory spacing, menu alignment, music transitions and settings persistence.
