# Gameplay permissions and input

- Keep gameplay labels functional, without upstream branding. Use the approved pack
  defaults. World rules are explicit, unprefixed values in world metadata; global
  preferences and the two creation presets use PowerConfig. There is no inheritance
  or Cheats master. Presets copy once at creation; existing worlds keep their own rules.
  Commands, UI, bindings, help and completion share each capability's rule check.
  Preserve read-only subcommands and independent shared controls. New actions must
  choose a permission owner. Creative retains native inventory, flight and protection.
  Revocation exits safely and preserves warps, history, inventories and recovery records.
  Never revoke a carried-container mode until the container can be placed safely.
- Ctrl, Shift and Alt are the standard chord modifiers. Debug actions have a separate
  configurable hold key and own their action presses until release. Keep the mode
  picker independent and preserve plain F3 and performance graph handling. Gameplay acts on key-down.
  Test both release orders, held movement, focus loss and open menus.
  Linux key repairs must update native events and polling together. F20–F24 use
  reserved LWJGL codes 114–118. Inventory movement only admits known screens without
  text input, excludes mouse/Shift, and uses normal container closure for item cleanup.
- Auto-walk retains the player's activation direction independently of freecam's
  manual keys. It survives focus loss by default, including the automatic pause
  screen. Supported inventories retain actual auto-walk movement while unfocused;
  only manual movement needs focus. Test displacement, not just the toggle.
- Difficulty is per-world, independent of access permissions, including creation, world switches,
  dimension copies and commands. Never copy it from global GameOptions. Defaults
  are Normal; manual menu edits take effect on Apply and auto-apply commits them.
  Keep multiplayer server-owned.
- Hide unavailable capability rows and empty tabs using their individual rules.
  Game rules remain editable by the local owner. Main-menu rule editors change only
  creation defaults. Shared freecam controls and safe commands remain independent.
  Hidden bindings must not claim input priority or mouse events. Only world-specific settings need scope
  notes in tooltips and setting or group names; global settings have no scope boilerplate.
- World cycles are per-world rules, independent of access permissions. Keep the
  simulation clock running; daylight
  uses an offset saved in world metadata. Sky rendering, day counters, time commands
  and photo mode use the daylight clock. Sleep must wake players without advancing
  frozen daylight or clearing frozen weather. Test normal and accelerated sleep,
  scheduled updates, pause/resume, save/reload and access permissions being disabled.
- Flight sprint covers all three movement axes in creative, spectator and freecam.
  Opposing inputs cancel; stopping movement resets Toggle sprint.
- WorldEdit selection/query commands use the reported camera position and current-view
  ray in Spectator/freecam. Keep rays loaded-terrain-only and separate from interaction
  targets; explicit WorldEdit commands may edit or navigate with their normal individual permissions while freecam remains active. Physical world/inventory interactions remain blocked.
- Boat placement facing is a global Fixes preference, independent of speed, steering
  and world permissions. Set the new boat's current/previous yaw before native spawn;
  preserve placement eligibility, item use, existing boats and multiplayer authority.
  Initialize a missing preference once from the selected base with a verified backup,
  so later Use as base changes cannot alter it on restart.
- Chat editing shares a buffer across input and completion. History keeps arrow
  ownership until editing or explicit completion; rendering must not change text.
  Wrapped chat keeps the last Beta color code from the preceding line.
  Help and completion share command availability. Group WorldEdit under //help,
  retain aliases, and keep per-command help free of gameplay changes.
  Added, removed and changed commands must stay consistent in /help, individual help and completion.
  Individual help filters subcommands through the same permissions as execution, including bare
  command help, /<command> help and /help <command>. Every new command must appear in /help and completion, with /<command> help
  describing its syntax and permissions without changing gameplay. /gamerule uses
  the same registry, aliases and validation as the UI; confirm writes only after a
  verified durable metadata save. /repair changes only held-item durability and
  preserves identity, count and native NBT; empty, undamageable and repaired items
  receive useful feedback.
  Optional death records are scoped by stable world ID and player. Recovery needs
  allow_death_recovery, plans full capacity first and protects native item NBT and original slots
  with a flushed journal. GUI recovery uses per-stack claims; viewing never changes
  inventory. Occupied original slots cancel recovery; GUI is the only recovery entry.
  Interrupted claims stay blocked; disabling keeps history. Decimal death logs
  start fresh in 1.4.2. Keep inventory is an independent per-world rule; changing
  access permissions must not reset it. Preserve native stacks through death, respawn and dead-player
  saves; kept deaths claim their recorded items without creating drops.
  Kept-save count repair runs only at native load, matches the exact signed-byte projection
  and full verified item data, and retains owner checks and rollback for partial repairs.
  Death snapshots include the four player crafting inputs, never the recipe output.
  Normal deaths drop those inputs once; kept deaths preserve them through respawn.
  Recovery restores empty original crafting slots and rolls back recipe callback failures.
  Ordinary crafting-grid saves also retain full native item data and counts. Keep
  pre-2.0 grid saves readable; changing the storage preference must not erase inputs.
- Natural spawning rules are saved with world properties and copied across
  dimensions. Monsters and animals cover every native species, including squid and
  wolves. Preserve existing mobs and never gate /summon with natural-spawn rules.
  Dungeon spawners and sleep nightmares have independent rules. The sleeping percentage
  applies to instant and accelerated sleep; zero still needs a sleeper, allow_sleep disables sleeping while retaining bed respawn points, and dead players and spectators do not count.
- Optional modern water landings reset fall distance before landing damage, for
  source and flowing water. Match fluid height and hitbox contact, including fast
  movement through water. Preserve multiplayer server authority.
- Optional empty bucket stacks cap at 16; filled buckets remain unstackable.
  Filling and milking exchange one bucket, with inventory insertion or a normal
  overflow drop. Preserve existing stacks when disabled and never split inventories
  while editing settings. Keep creative buckets reusable.
- Leaving, replacing a world, or quitting clears every temporary activation through
  `TweakLifecycle`. Every `TweakIndicators.Tweak` needs an owning reset participant.
  This includes restocking, automatic tool switching, fake sneak, fast placement and
  its restriction, auto-walk and line lock, auto-mine, Hold use, temporary click-mining
  inversion, free look, cinematic camera, freecam and player movement, hidden entities,
  and manual full bright. Activation stays in memory and legacy saved activation is
  inert. Copies and menu drafts retain live transient fields without serializing them.
  Preserve bindings, HUD inclusion, toggle messages, tuning, feature availability,
  saved data and actual Creative/Spectator state and protection. Spectator automatic
  full bright remains a saved preference. Cancel pending native work on its existing
  menu/focus boundaries without resetting unrelated activation or auto-walk policy.
  Inventory resets cancel work through the existing leases and journal ownership;
  never rewrite slots or clear carry state during generic cleanup. Carrying still
  requires safe placement before revocation. Queue changed persistent preferences.
  Flexible placement is always available through its unbound-by-default hold controls.
  Complete slabs retains its saved enablement; only temporary placement operations
  and held modifiers clear. Slab completion has no hotkey,
  toggle messages or Active tweaks entry.
- Preserve free-look offsets through explicit perspective changes and retain the
  chosen perspective when free look ends.
- Creative vehicle placement uses press events only; holding use must not spawn extra
  boats or minecarts. Preserve native item use and other held-button actions.
- Modern water movement is a global player-only singleplayer preference, enabled
  only in Power Beta defaults. Keep native collision, limb interpolation, other
  fluids, mobs, vehicles and flight; see `docs/water-movement.md`. Bound current
  sampling to loaded terrain. Ordinary water keeps normal swimming speed. Modern sprint
  swimming additionally requires allow_sprinting; retain compact/crouched collision until
  there is space to stand, Beta-normalized saved positions and render-scoped player/armor poses.
- Boat riders suppress lateral cape rotations only while rendering, retaining native
  pitch motion and restoring ordinary cape response immediately on dismount.
- Cape limits change only rendered cape angles, leaving player interpolation and movement
  untouched. Lava/cactus item rules affect only dropped items in singleplayer.
  Player and mob trampling use independent world rules, both off in new presets.
  Convert old item protections from frozen evidence, never from changing live settings.
- Torch shovel uses only owned diamond shovels, most worn first, for sand/gravel in
  singleplayer survival. Never swap held slots; native successful breaks charge wear once.
  Vein searches/mining are bounded, tick-owned and loaded-terrain-only. Revalidate blocks,
  tools, mode and ownership before each native break, stop when the tool breaks, and clear
  jobs on menus, focus loss and world exit. Compose timing after other mining adjustments;
  the configured percentage of whole-vein time never beats one block's normal time.
- Fire spread is a per-world rule, default on. Disabling it suppresses spread, block
  consumption and lava ignition while retaining native fire aging, extinguishing and damage.
  Creeper/ghast griefing affects block destruction only; ghast ignition remains separate.
  Explosions destroy items covers TNT, creepers and ghasts without changing block drops
  or other damage. TNT-off prevents new priming and pauses native entities/fuses until
  re-enabled. Work budgets remain global. Player damage rules leave mobs and unrelated
  damage sources unchanged; Peaceful regeneration does not gate food healing.
  Nether portal entry rules must retain return travel. Keep multiplayer server-owned.
  Temporary click mining inverts its global preference with separate unsaved state,
  reports the effective value, and clears through TweakLifecycle.

- Signs use native editing, rendering and text storage; shared text-input widgets do not
  own sign lines. Preserve Escape/Done saving and optional reopening of existing signs.

- Creation starts in Survival for the Survival preset, Creative for Cheats and the
  selected valid mode for Custom. Initial mode selection is silent; later changes keep
  normal messages. Editing an existing world's rules never promotes its game mode.

- Only plain F3 debug plus plain F3/F4 mode-picker bindings defer the debug toggle until
  release. Cycling consumes that toggle. Cancel pending input on menus, focus/world loss or
  binding changes; keep all remapped/chorded bindings immediate and respect input ownership.

- Creative/Spectator use native water breathing to refill oxygen and skip drowning particles.
  Mode transitions and protected-player reloads restore a full breath. Flight must advance
  and settle limb interpolation even while freecam owns movement.
- /fly changes only flight ability. Survival requires allow_flight; Creative retains its own permission and protection; Spectator flight stays independent. Revocation cancels survival flight safely.

- Flight speed percentages share the modern Creative baseline; freecam stores its original speed units and only converts the display. Shared motion maths retain frame-owned freecam timing. Config format 7 converts legacy response rates to 0–5 drift and retires sprint/classic switches.

- Flight acceleration and deceleration use independent 0–5 response controls on all axes.
  Preserve terminal speeds, native modern response at 5 and freecam frame timing.
  Render head markers after translucent terrain with self-occlusion and restored GL state.
- Commands and WorldEdit use the shared reported-position contract in
  [Rendering and maps](rendering-maps.md), including mode-specific Y and literal
  coordinate exceptions. Keep physical destinations and safe placement separate.

- Axes slash leaves changes mining strength only, for leaf material and axes in singleplayer
  Survival. Preserve native drops, per-block durability, tool break cleanup and mining
  penalties; compose with StationAPI and vein timing without applying the speed twice.
- Flight response sliders step by whole numbers; numeric entry and saved values may
  remain fractional. Keep acceleration/deceleration storage IDs and defaults intact.

- Ground sprinting uses the per-world `allow_sprinting` rule. Power Beta creation
  defaults enable it with stamina and combat blocking; Refined Beta and Beta keep it off.
  Existing worlds and missing world rules retain their own values, defaulting off. Speed, activation, stamina, combat and FOV tuning
  stay global. Retain legacy `sprint.enabled` data inert; never use it to grant a
  world permission. Missing rules default off. Revocation immediately cancels ground
  sprint and its FOV, preserving flight and freecam sprint preferences and state.
  Preserve native land collision, fluids, vehicles, flight and multiplayer authority.
  Modern defaults use a 1.3 speed multiplier, 0.2 horizontal jump impulse and native
  terrain dust with modern particle movement. Optional stamina/combat rules persist
  in player NBT without changing the world rule. Ground, Creative and Spectator share sprint FOV enablement/amount; freecam retains its independent preference. Creative and every flight mode bypass stamina without resetting its saved state. Survival stamina uses nine-pixel, black-outlined icons filled from the right, with slightly darker depleted restart reserves only while exhausted. Gameplay and HUD previews share the same glyphs, palette and armor-aware placement. Combined layouts include stamina; separate layouts retain independent visibility, anchors and offsets. Keep it visible while paused and in the player inventory alongside native status icons. Each movement mode publishes
  sprint state; render code never polls keys.
  Boat interaction consumes a held boat's placement fallback without skipping native boarding.

- Waypoint abbreviations are optional stable per-waypoint data; preserve empty sets and legacy warp NBT. Shared destination color/opacity applies to maps, edge arrows and world labels. Color pickers confirm dirty exits and retain isolated RGB/opacity/swatches. Pause shortcuts initialize missing base values once with verified backups. Extra sounds use verified official assets and successful native action events only.

- Saved local chat messages are user data, separate from ordinary transcript limits.
  Never persist server transcripts or send restored notes to the network. Commands
  remain in sent-message recall; restored notes appear only in scrollback.

- Open chat uses immutable original messages and character offsets for selection, reflow
  and copying. Insert clipboard newlines only between source messages. Preserve selection
  and the visible scroll anchor through incoming messages and resize, clamp removed sources,
  and test real pointer drags. History and typing-row selection share translucent paint that
  restores blend enables, factors and current color before drawing glyphs. Keep original
  message storage bounded by chat retention even while chat is closed. UI keyboard shortcuts
  use queued modifier state during keyboard dispatch; mouse selection uses physical polling.
  Test fast Ctrl+C with either release order and overlapping left/right modifiers. Drag
  autoscroll runs from the chat screen tick so paused singleplayer chat keeps working.
- Standalone dark title/footer bars span the window; constrained content stays centered.
  Embedded map settings chrome stays within its panel. Footer geometry includes measured
  labels and padding and is shared by rendering and input. Map coordinate dialogs retain
  the current world-map screen and own all modal input. The normal screenshot binding
  captures map terrain without controls, once per press.

- Hold use and Auto-walk line lock are temporary native actions. Dispatch their hotkeys
  on key-down and clear through TweakLifecycle. Hold use supplies one initial native
  press before held repeats. Line lock adds block-lane side walls without rotating the
  player, and exempts real and invisible collisions from stopping ground sprint.
- Modern crawling and modern water movement remain independent global preferences.
  Reuse collision-safe compact/crouched poses, native feet, camera interpolation and
  render-scoped player/armor animation. Never force an unsafe standing pose when disabled.
  Ladder sprint uses normal ground permission, stamina and combat ownership.
- Spectator crosshair checks use loaded-only read-only rays and Creative reach. Detect
  native use overrides without invoking them or changing the player's interaction target.
- Optional crouch camera height affects open-space sneaking only with Modern crawling. Keep
  Beta's native sneak movement when off, the deeper modern drop when on, collision-safe
  low-ceiling cameras and both native perspectives consistent. Retaining native sneak/step
  smoothing during pose resizing must preserve collision-box feet and saved positions.
