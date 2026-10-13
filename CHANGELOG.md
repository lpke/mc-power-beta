# Changelog

## [Unreleased]

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
