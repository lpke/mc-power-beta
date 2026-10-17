# Changelog

## [Unreleased]

## [2.4.0] - 2026-10-17

- Reorder individual F3 information lines by dragging them in the HUD preview, leave blank rows between them, control each line's visibility and change the text size.
- HUD previews share the in-game info-cluster formatting, show chat with its native cursor, and mark hidden clusters only while selected. Background previews start off and include a clear daytime forest scene captured at 12 chunks.
- Find HUD settings under their editor sections in search, edit individual results directly, and open the matching editor section from its heading.
- Reorganise Interface settings into General, Title screen and Screenshots, with a Customise button beside Retro titles. Editor controls share inline editing, individual resets and native click sounds.
- Show the current Power Beta version beside Minecraft's title-screen version, with an option to hide it, and remove blocking settings saves when cycling logos.
- Rename worlds inline with verified metadata backups, show each world's name in its editor, and keep panorama world selections translucent.
- Enlarge the achievement map on larger screens and return to the pause menu with Done or Escape.
- Show panoramas across main-menu screens by default, use the current Power Beta version as the default custom version text, reset weather after sleep only while raining, and limit TNT work by default.

## [2.3.0] - 2026-10-17

- Adapt the HUD editor to wide and portrait screens, combine normal and underwater previews, and keep crosshair, achievement and controller options under General.
- Add a forest preview background, outlines for hidden info clusters, individual resets and delayed tooltips. Keep nested colour swatches in the editor draft until Done or Save.

- Share collapsible Options groups with the HUD editor. Reset all HUD settings, all positions or only the selected element, with Undo for every reset.
- Preview selected item names only when enabled, using their shared message position. Bring item-name controls, crosshair, chat, controller prompts and achievement toasts into the HUD editor.

- Preserve the cursor position between main-menu screens and restore clickable title-screen music controls alongside logo cycling.
- Simplify the HUD editor to one top selector, group related settings and rename custom info clusters. Select preview elements before dragging, and reset positioning separately from visibility, styling and content.
- Use the Game sounds setting name in category and individual-sound volume tooltips.
- Use greyscale colours in the HUD element dropdown and add individual default-reset buttons for title rotation and appearance controls.
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

- Set bed respawn points during the day with sleeping enabled, while retaining native sleeping and multiplayer behaviour.
- Add Edit game rules to the saved-world Edit menu, with verified metadata backups and checks for changed or unsupported data.
- Combine panorama controls into one choice with the original dirt background as the default. Show one option per distinct scene while preserving saved selections from older versions.
- Preview panoramas live in the picker. Apply selections immediately, retain the selection marker and leave with Back or Escape.
- Use the clean release 1.0 images for the Beta 1.8.1 panorama, removing its purple seam while keeping its name and saved selections. Centre the panorama picker buttons and search field.
- Adjust panorama blur directly in the picker, sharing the same setting and preview as the main settings list.
- Return to Select world when pressing Escape in the world Edit menu.
- Centre Beta, 2017 and 2020 title lettering and their Java Edition subtitles using the visible texture bounds. Keep the already-centred modern style and custom retro title.
- Remove the panorama picker's search field and give its choices more space.
- Group Active tweaks HUD inclusion switches below its display controls, and move Full bright messages below Brightness.

## [2.1.0] - 2026-10-16

- Restore native walking, jumping, falling and bobbing whenever flight is off. Entering Creative silently restores its flight ability, and flight tooltips explain their speed and drift baselines.
- Apply title panorama choices immediately when clicked, with a single Cancel button.
- Add Title style choices for the official Beta, 2017 Java Edition, 2020 Java Edition and modern logos. Keep customisable retro lettering as the default and preserve its customisation between styles.
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
- Default lightning fire off, tick the world during accelerated sleep and allow clicking to wake. Default stacked chests, sign dye colours and vignette off.
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
- Add four custom HUD info clusters in Interface, with independent content, anchors, offsets, text size, colour, opacity and F3 visibility. Choose coordinates, facing, biome, time, day count, chunk coordinates and frame rate.
- Restore the inventory cursor to its exact starting position after right-click looking, with both native and raw mouse input.
- Match the section collapse button to the search-clear button and separate its inward chevrons.
- Preserve quick inventory-look gestures without losing their cursor origin or camera movement.
- Add an unbound hotkey for toggling all custom HUD clusters together while preserving their individual settings and F3 visibility.
- Show HUD coordinates as comma-separated values, with the facing axis and compass direction alongside them when enabled.
- Keep background movement-key events and focus transitions from cancelling auto-walk when focus-loss cancellation is off.
- Keep auto-walk moving while an inventory is open and the game loses focus.
- Preserve cursor position and camera movement through consecutive fast inventory drags, including movement immediately after release.
- Restore custom skins when the first skin provider returns an incomplete profile, and prevent bundled fallback skins from crashing the texture downloader.
- Restore photo-mode mouse panning and rotation, and preserve quick drags, slider changes and time restoration through resizing.
- Centre the menu music scrub track above and below the menu.
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
- Shift-click creative catalogue items to transfer one full stack into the inventory, respecting item and stacking limits.
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

- Restore text buttons and show the death number and timestamp in the saved inventory view, without the held label or armour-panel black bar.
- Save and recover the four player crafting inputs in their original positions, and drop them once on normal deaths.
- Prevent crashes after long-distance WorldEdit jumps by keeping background culling out of chunk generation and fixing chunk sorting.

## [1.4.2] - 2026-10-11

- Simplify the saved death inventory view and cancel recovery when original slots are occupied.
- Recover death items through the inventory view only, and list deaths from oldest to newest with numeric IDs. Existing death logs start fresh.
- Add Keep inventory on death as a per-world cheat rule, off by default, with kept deaths marked recovered and items preserved through respawn.

## [1.4.1] - 2026-10-11

- Default lost-item chat notices to off and allow death recovery in survival with cheats.
- Save original inventory and armour positions, restore them when available, and show deaths in an inventory view locked without cheats.
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

- Preserve chat colours when messages wrap onto another line.
- Show entities and block entities during freecam, then resume normal culling when freecam ends.

## [1.2.0] - 2026-10-10

- Add an optional Inventory in portals setting, off by default, with normal item cleanup before dimension travel.
- Add optional Shift-click transfers into crafting tables, off by default.
- Keep auto-walk moving the player while freecam controls the camera.
- Add an optional Low fire texture setting that lowers on-screen flames, off by default.
- Add separate camera shake controls for all damage and fire damage, both on by default.
- Add Auto-mine with an unbound toggle hotkey to hold the normal attack and block-breaking action.
- Fix dropping carried items outside the creative catalogue, preserving whole-stack and single-item drops.

## [1.1.1] - 2026-10-09

- Keep the chat cursor visible by default, with optional blinking.
- Add an F3 text colour setting, defaulting to white, and prevent overlapping debug rows.
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

- Fix isometric capture bounds and centring with custom render distances.

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
