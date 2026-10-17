# Power Beta development

## Build and layout

- Java 21 and Python 3. Run `python3 tools/build_pack.py` from this directory.
  It builds sequentially, runs unit tests and exports a clean Prism ZIP. Do not run
  Loom builds concurrently; their mapping caches are shared.
- `src/` owns the Options UI, config adapters, audio, overlays and integration.
  `modules/creative`, `building` and `worldedit` have their own builds.
  Other modules use `source-build/modules.json`; their original Gradle files are
  upstream reference, not the pack build. `input-api/` is the shared input bridge.
- Utility JARs are compiled here and nested inside Power Beta. Six platform,
  authentication and performance JARs remain external. Disabled vendor artifacts
  and the recipe-browser source are retained for reference, not shipped.
  Review pinned dependency manifests before changing platform binaries.
- Root tests: `./gradlew --no-daemon test`. Utility build:
  `python3 tools/build_utilities.py [MODULE ...]`. For a pack already built,
  `python3 tools/build_pack.py --skip-build` only assembles the export.

## Changes that need care

- Keep gameplay labels functional, without upstream branding. Use the approved pack
  defaults. World rules are explicit, unprefixed values in world metadata; global
  preferences and the two creation presets use PowerConfig. There is no inheritance
  or Cheats master. Presets copy once at creation; existing worlds keep their own rules.
  Commands, UI, bindings, help and completion share each capability's rule check.
  Preserve read-only subcommands and independent shared controls. New actions must
  choose a permission owner. Creative retains native inventory, flight and protection.
  Revocation exits safely and preserves warps, history, inventories and recovery records.
  Never revoke a carried-container mode until the container can be placed safely.
- `PowerConfig` is the only settings writer: `config/power-beta.json`.
  Rule groups identify world scope without repeated row labels.
  Defaults and interrupted-save recovery run from the language adapter **before**
  mixin plugins and configuration entrypoints. Preserve stable setting, world,
  track and preset IDs. Global schema 7 supports conversion from schemas 3, 4, 5 and 6 through
  `input-api/.../storage/ConfigVersions` and `ConfigCleanup`. World-rule format 3
  supports formats 1 and 2 and pre-rule metadata through `rules/LegacyWorldRules`, `WorldRuleUpgrade210` and `WorldRuleData`.
  Preserve frozen legacy evidence for worlds converted later. Use these helpers when
  updating an instance; never widen conflicting permissions or reset unsupported data.
  Validate converted values and verify durable backups before replacing originals.
  Unsupported/future formats block writes or world opening with a useful recovery UI.
  Fresh installs copy `defaults/power-beta.json`; keep unrelated historical importers out.
  Document current/supported formats in release notes whenever either format changes.
  Gameplay hotkeys queue settings snapshots through `PowerConfig`; never flush files
  on the input or render path. Flush queued saves before menu backups and on shutdown.
  The config API retains its `gcapi3` binary identity for external dependencies.
- Saved-world rule edits use the shared registry and editor without loading a world. Back up and verify metadata, reject concurrent changes and unsupported data, and preserve player data, game mode, the previous metadata file.
- The game uses Gson 2.8.9. Do not rely on record deserialization without an explicit
  adapter. Test persisted data with that version, including rollback and restart.
- Options edits use `ConfigSession`. Auto-apply defaults on; manual Apply commits and
  Cancel restores previews. Dirty exits offer Save, Discard and Keep editing. Creation
  edits affect only the future-world draft. Main-menu templates, creation and loaded
  world rules share search and editor behavior. Preserve Custom drafts when cycling
  presets. Custom starts in Survival; block the Cheats preset visibly if its
  Creative starting mode lacks permission.
  Update linked values together. Preserve search, scroll, related-view history and
  modifier ownership when rebuilding rows. Share render and hit-test geometry.
- `settings-layout.json` owns tab/group order; catalogue `order` values arrange
  settings inside groups. Presentation metadata applies to every adapter without
  changing storage IDs. Keep the flight sprint binding shared; Creative, Spectator and freecam own separate sprint preferences.
  The focus-loss pause preference is `power_controls:general.pauseOnLostFocus`.
- Ctrl, Shift and Alt are the only chord modifiers. Gameplay acts on key-down.
  Test both release orders, held movement, focus loss and open menus.
  Linux key repairs must update native events and polling together. F20–F24 use
  reserved LWJGL codes 114–118. Inventory movement only admits known screens without
  text input, excludes mouse/Shift, and uses normal container closure for item cleanup.
- Auto-walk retains the player's activation direction independently of freecam's
  manual keys. It survives focus loss by default, including the automatic pause
  screen. Supported inventories retain actual auto-walk movement while unfocused;
  only manual movement needs focus. Test displacement, not just the toggle.
- Inventory looking shares eligible screens and focus rules with inventory movement.
  Check the originating press against every GUI panel, creative tab and destroy slot.
  Use native sensitivity/rotation. Capture and hide the pointer, GUI hover and cursor
  stack until release; hidden hover coordinates must never drive the player model.
  Restore the originating position inside the native grab transition, without a
  second warp. Restore on release, lost focus, screen/world changes or lost eligibility;
  holding the button must not restart a cancelled drag. Leave events and polling to
  unrelated screens, including photo mode. Right-click looking must not drop items.
  Dispatch complete mouse batches before changing capture. Preserve each drag's
  press coordinates/deltas, send final motion through the native camera renderer,
  and remap later clicks to the restored cursor. Both native and raw helpers supply
  GUI cursor deltas; raw device deltas remain gameplay-only. Linux capture retains
  the event baseline because XQueryPointer can lead queued events. Complete quick
  drags need a hidden native cursor transition on Xwayland. Test consecutive fast
  drags, both helpers and real pointer movement immediately after release.
- `AudioController` owns all music sources, pauses, previews and timing.
  World/menu transitions preserve streams and pause state in WORLD/MIX modes;
  only CUSTOM resets playback. Preview restoration must not revive old jukeboxes.
  Synchronize sound-source cleanup with decoder reads on the sound threads.
  Never scan folders or decode audio from a render loop. Preset drafts and saved
  snapshots must stay separate from live library edits; only Load copies a preset
  into live state. Track pools prevent new folders silently expanding presets.
  Keep missing track IDs, queues and user music files intact.
  Previous during a between-track gap replays the last played track without
  moving back in history or consuming queued tracks. Paused tracks remain current.
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
- Numeric display units do not change stored values. Duration editors use seconds;
  processing budgets remain per tick. Validate item/filter IDs before accepting
  drafts, and keep temporary errors outside row layout.
- Shift-double-click transfers matching native item data from the clicked inventory,
  leaving the held cursor stack untouched. Use normal item/slot limits and native
  multiplayer transfers. Exclude outputs, hidden catalogue and read-only slots. Cancel
  pending gestures on cursor/container/focus changes and keep gathering, dragging,
  wheel transfers and crafting from claiming the same gesture.
  Measure double-click intervals using native event timestamps, not frame processing
  time. Mouse batches precede queued keyboard events; Shift key-down must not cancel
  its own gesture. Ordinary drags must revalidate cursor and visited slot data before
  redistribution or rollback, and preserve existing oversized stacks.
- Item transfers are transactions, including full counts, damage and NBT. Preserve
  journal flushes, rollback, ownership checks and failure shutdown. Menu edits must
  never touch inventory slots. WorldEdit and carrying suppress StationAPI block
  callbacks only inside their scoped transaction to avoid drops and slab merging.
  Keep old carry-journal formats recoverable. See `docs/recovery.md`.
- Compact buttons must retain all four texture borders. Check narrow GUI sizes,
  sticky headings, clipping and click targets, not only a wide screenshot.
  Creative catalogue rows grow upward; hotbar and survival slots never move.
  The final catalogue row and hotbar use the same 22-pixel origin spacing as survival.
  Leaving spectator keeps the current position, including inside solid blocks.
- Creative middle-click dragging previews compatible, writable slots and commits
  together on release. Use normal item and slot limits; preserve cursor damage/NBT.
  Never paint hidden catalogue slots, survival, spectator or multiplayer inventories.
  Cancel on focus loss, screen/container changes, lost Creative access or cursor changes.
- `TweakIndicators` exposes live module state across mappings. HUD inclusion is
  separate from gameplay settings. Do not infer active auto-walk/free look from
  feature availability or poll action keys while rendering.
  Per-feature toggle messages default off and observe transitions on ticks,
  independently of HUD inclusion. Menus, focus changes and world loading stay silent.

- Flight sprint covers all three movement axes in creative, spectator and freecam.
  Opposing inputs cancel; stopping movement resets Toggle sprint.
- Chat editing shares a buffer across input and completion. History keeps arrow
  ownership until editing or explicit completion; rendering must not change text.
  Wrapped chat keeps the last Beta colour code from the preceding line.
  Help and completion share command availability. Group WorldEdit under //help,
  retain aliases, and keep per-command help free of gameplay changes.
  Every new command must appear in /help and completion, with /<command> help
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
  Death snapshots include the four player crafting inputs, never the recipe output.
  Normal deaths drop those inputs once; kept deaths preserve them through respawn.
  Recovery restores empty original crafting slots and rolls back recipe callback failures.
  Ordinary crafting-grid saves also retain full native item data and counts. Keep
  pre-2.0 grid saves readable; changing the storage preference must not erase inputs.
- Freecam bypasses entity and block entity culling independently of player movement.
  Background culling only reads loaded chunks and uses its own vectors and boxes;
  never run chunk loading or touch Beta's shared geometry pools from that worker.
  Keep culling results current and preserve preferences so normal culling resumes on exit.
  Entity render-distance checks use the detached camera position; simulation stays player-owned.
  Enter from the rendered camera pose, including interpolation, eye height and bobbing.
  Keep the first detached frame still, reset movement timing and reject old-world poses.
  Integrate detached motion before each rendered frame, once for both stereo eyes.
  Damping must give the same displacement across uneven frame intervals.
  Hide the HUD on entry only when requested, default off. Allow manual F1 changes
  during the visit and restore its entry state on every exit. Freecam and Spectator
  never use front view, regardless of perspective style or HUD visibility. Proximity must never hide the player's body.
  Container previews use a separate interpolated, occluded ray and configured
  Creative block/entity reach in every mode, including with the HUD hidden. Reject
  unloaded terrain before raycasting; never load chunks, open inventories or change
  the player's interaction target. Keep multiplayer previews disabled.
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
- WorldEdit named states map only to supported Beta metadata. Reject unsupported
  properties before starting a transaction; commas inside [] are state separators.

## Verification and delivery

- Test in a disposable clone outside the user's normal instance list. Never launch
  or edit an active instance. Stop only the exact process you started, verified by
  PID and game directory. Preserve worlds, recovery journals and original music.
- Clean up generated files when their task is finished: obsolete build JARs/ZIPs,
  accumulated Loom remap copies, disposable test clones, temporary exports and captures.
  Check disk usage during long build/test runs. Before removing anything, verify that
  no active process or pending check needs it; do not clean shared mapping caches
  during builds. Keep current artifacts, source, user files, worlds, recovery journals,
  original music and required backups. Do not delete unrelated application caches.
- Mute every test instance before launch. Route audio tests to a silent output or
  capture device so they never play through the user's speakers or headphones.
  Never mute or change the audio routing of the user's own instances.
- `validation-mod/` contains runtime fixtures, including `audio-maintenance`.
  Build with `./gradlew -p validation-mod --no-daemon build` after the root JAR.
  Install only in the disposable clone. Its README explains command dispatch.
  Fixtures can change settings and worlds; never ship them.
- For an existing Power Beta instance on matching dependencies, use
  `python3 tools/update_artifacts.py /path/to/instance`. It refuses running clients,
  verifies a backup and checks that settings and worlds remain byte-identical.
  Configuration changes are separate, backed-up operations through `PowerConfig`.
- Verify the final export contains seven enabled JARs and no test fixtures, accounts,
  worlds, logs or disabled mods. Keep README human-facing; add lasting implementation
  constraints here rather than another session diary.
- Commit and push only when the current user request authorizes it.

## Releases

- This project does not accept external contributions. Keep that policy in the
  README and encourage forks under the AGPL; do not add a contribution guide.
- `gradle.properties` owns the pack version. Use semantic versions and tags `vX.Y.Z`.
  Module/API versions are independent; preserve them unless that module needs a change.
- Keep `CHANGELOG.md` newest first, with `## [Unreleased]` followed by dated version
  headings and plain bullets describing modpack features and fixes. Exclude website,
  release-management and development-process changes. Move Unreleased entries
  into the next version when preparing a release. Never invent past release versions.
- Follow `docs/releases.md`. Publishing requires explicit user permission in the
  current task. A code push alone does not authorize publishing a release.
- `python3 tools/release.py --check` validates release notes and versioning.
  The publish command builds and tests sequentially, verifies the export, publishes
  GitHub release assets and notes, and updates/deploys the website from the same data.
  Do not hand-edit generated website release details or overwrite published artifacts.
- GitHub hosts every release ZIP and checksum. Public repositories use GitHub
  downloads and retain no Blob mirrors. Private repositories mirror only the
  latest ZIP; older website entries keep their notes without a download link.
  Prune obsolete pack mirrors after verifying the deployed snapshot. Keep the
  previous live ZIP during deployment and enforce the 900 MB store budget before
  upload, including that temporary overlap. Never delete unrelated Blob files.
  After quota cleanup, check the store and account block state; an empty store
  does not prove that Vercel has lifted a suspension.
- Never commit runtime reports, logs, instance backups, account files, personal paths,
  `.env` files or Git bundles. Runtime fixture source stays in `validation-mod/`.
- Keep the top-level AGPL license and the README Inspired by links. Source modules
  have no separate license files or attribution metadata. Preserve binary dependency
  hashes and runtime IDs, especially `gcapi3`.
- Optional redstone textures use four-pixel wires with shaded edges and six-pixel
  junctions; the power number panel occupies one quarter of a block.
- Isometric captures use the actual renderer chunk grid and Beta's 128-block
  height. Centre both horizontal axes on the camera's chunk; never use camera Y
  for Z or infer capture size from the obsolete four-step distance option.

- Entity visibility changes only rendering. Full bright overrides float shading only
  on the render thread, never saved or simulated light. Manual full bright is independent
  of permissions. Spectator overrides last for one visit; leaving restores manual state, and
  lost Spectator access clears only that override.
- Item merging preserves native NBT, damage, normal stack limits and total counts.
  TNT limits retain native entities and saved fuses, bound work each tick and stay
  available from the main menu for recovery. Both leave multiplayer server-owned.

- Item/TNT collision indexing must cover native and StationAPI flattened chunks,
  including insertion, section changes and removal while the fix is off. Filter
  only exact native classes with null collision boxes; retain custom subclasses,
  vehicles and the original multiplayer query. The Fixes toggle takes effect live.
- Merge sweeps use terrain render distance for their threshold, resume within a
  tick budget and back off when idle. Revalidate native item data between batches
  and exclude removed entities. TNT interpolation changes render coordinates only.

- Leaving a world or quitting clears active tweaks through `TweakLifecycle`; preserve
  feature preferences, bindings and saved user data. Queue changed toggle snapshots.
  Flexible placement is always available through its unbound-by-default hold controls.
  Complete slabs retains its saved enablement; only temporary placement operations
  and held modifiers clear. Slab completion has no hotkey,
  toggle messages or Active tweaks entry.
- Reject incomplete skin-provider responses so the next provider can supply the
  texture. Keep bundled fallback skin paths out of the HTTP downloader.
- Custom HUD clusters keep stable IDs and independent content, anchors, offsets,
  colour, size, opacity and F3 visibility. Sample world data on ticks and use the
  daylight clock. Native HUD previews and Cancel update positions immediately.
  F3 and custom HUD location data follow freecam while detached, including facing,
  biome, chunk and block light. HUD queries must not load chunks or move the player.
  F3 providers share debug text layout/colour and capture only that block, never chat
  or other HUD text. F3 and custom HUD share the eye/feet preference; freecam and
  Spectator always report the native head base. Freecam collision uses that same
  lower bound. Third-person ghost heads use the current skin dimensions, draw only
  head/hat and support free look without moving the player. Spectator third-person
  cameras bypass terrain clipping; their head marker stays visible inside blocks.
- Preserve free-look offsets through explicit perspective changes and retain the
  chosen perspective when free look ends.
- Photo-mode drags own the button from its originating press through release. Widget
  drags never become camera drags; lost focus cancels them. Clamp tilt before setting its
  target and preserve the original daylight snapshot through GUI resizing.
- Cape limits change only rendered cape angles, leaving player interpolation and movement
  untouched. Lava/cactus item rules affect only dropped items in singleplayer.
  Player and mob trampling use independent world rules, both off in new presets.
  Convert old item protections from frozen evidence, never from changing live settings.
- Music-library collapse/expand-all acts on groups in the current view, including search,
  without changing inclusion, volume, presets or queues. Menu scrub tracks reserve equal
  space on either side so the track itself stays centred with the menu.

- Hotbar swaps flush immutable recovery bytes on a worker before publishing on a later
  game tick. Keep native NBT access on the game thread, bound queued requests, and cancel
  stale plans on inventory/owner, menu, focus or world changes. Never skip the durable flush.
- Text inputs share editing and pointer selection through the input bridge. UI cursor
  blinking is separate from chat. Cancel selection on focus/screen changes; keep native
  limits, validation and gcapi3 widget methods. Search-only cluster suffixes never change IDs.
  Search group chevrons toggle silently; collapsed titles expand and expanded titles navigate.
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

- Item and block settings use the shared graphical picker. Keep each editor draft local
  until Done, validate custom IDs against loaded registries, retain metadata variants
  where the stored format supports them, and never touch gameplay inventories.

- Signs use native editing, rendering and text storage; shared text-input widgets do not
  own sign lines. Preserve Escape/Done saving and optional reopening of existing signs.

- Block-rule and item-picker exits confirm unsaved drafts, including nested tool selections.
  Modal input must not reach the editor behind it. Underwater overlay overrides affect
  only rendering; full bright temporarily hides that texture without changing its preference.
  Full bright uses normal terrain fog underwater, preserving fog-distance controls and
  native water physics, lava fog and cloud fog. Disabling it restores native water fog.
  The water-material override is scoped to fog rendering on its owning thread, including
  StationAPI's colour hooks; it must not affect other fluid queries.

- Creation starts in Survival for the Survival preset, Creative for Cheats and the
  selected valid mode for Custom. Initial mode selection is silent; later changes keep
  normal messages. Editing an existing world's rules never promotes its game mode.

- Underwater fog density is 0–100 percent, default 100. Zero and full bright use normal
  terrain fog; other values scale native water fog without changing lighting or other fluids.
  Preview/Cancel restore it immediately, and full bright never changes the saved percentage.
- Only plain F3 debug plus plain F3/F4 mode-picker bindings defer the debug toggle until
  release. Cycling consumes that toggle. Cancel pending input on menus, focus/world loss or
  binding changes; keep all remapped/chorded bindings immediate and respect input ownership.

- Creative/Spectator use native water breathing to refill oxygen and skip drowning particles.
  Mode transitions and protected-player reloads restore a full breath. Flight must advance
  and settle limb interpolation even while freecam owns movement.
- Underwater darkness adjusts render-only daylight shading with bounded, frame-local column
  samples. Preserve roofs, night lighting, emission and opaque AO. Never change stored light,
  water opacity, physics, or fog preferences; keep loaded-chunk and render-thread guards.

- Water fog and FOV use the current rendered camera pose, including third person, free look
  and freecam. Resolve perspective after capturing that pose; preserve photo projection,
  stereo and zoom. Inside/outside water darkness affects rendering only. Terrain retains native
  and water-clear vertex colours in one 32-byte vertex, with one shader uniform choosing
  brightness. After the first opt-in prepares existing meshes, surface crossings and
  slider previews must never queue terrain rebuilds. Day/night updates must also include
  compensated deep water whose native skylight is zero.
  Roof and water-column edits must invalidate dependent meshes below native light's reach,
  using the existing bounded render grid and update queue without loading terrain.
  Calculate the second colour only for affected sections; bound scratch memory and preserve
  geometry, normals, alpha, native update scheduling and world light arrays.
  Detached flight admits auto-walk independently of manual camera keys and keeps animation
  interpolation current when the player is stationary.

- Plain F4 toggles the custom HUD. A valid mode-picker modifier gesture owns its cycle
  key instead; test both release orders and remapped/chorded bindings.

- Official title backgrounds use verified Mojang client and resource-index assets. Resource-index panoramas override fallback client textures. Keep version IDs stable, deduplicate identical scenes, and render unblurred panoramas at window resolution.
- /fly changes only flight ability. Survival requires allow_flight; Creative retains its own permission and protection; Spectator flight stays independent. Revocation cancels survival flight safely.

- The panorama picker groups variants of the same captured scene. Preserve legacy version IDs and texture paths. Its single selection also updates the retained enable flag, and its live preview shares the title renderer.

- In-group dividers use catalogue `separatorAfter` on the preceding setting; they share row geometry and never act as headings or input targets.
- Flight speed percentages share the modern Creative baseline; freecam stores its original speed units and only converts the display. Shared motion maths retain frame-owned freecam timing. Config format 7 converts legacy response rates to 0–5 drift and retires sprint/classic switches.

- Flight acceleration and deceleration use independent 0–5 response controls on all axes.
  Preserve terminal speeds, native modern response at 5 and freecam frame timing.
  Render head markers after translucent terrain with self-occlusion and restored GL state.
- Creative reports the same feet/eye Y as Survival, including while falling or flying.
  Only Spectator and freecam use head-base Y. Share this
  through HudCoordinates and ReportedPosition across HUDs, commands and WorldEdit.

- Axes slash leaves changes mining strength only, for leaf material and axes in singleplayer
  Survival. Preserve native drops, per-block durability, tool break cleanup and mining
  penalties; compose with StationAPI and vein timing without applying the speed twice.
- Flight response sliders step by whole numbers; numeric entry and saved values may
  remain fractional. Keep acceleration/deceleration storage IDs and defaults intact.

- HUD and title editors own isolated drafts until Done. Preserve existing setting IDs, combined/separate layouts and title preset IDs; nested pickers and modal exits must not leak changes. HUD previews share native positioning maths and the active font/item textures.
- Light-overlay rendering batches native digit geometry with the current font texture and advances. Keep camera-relative precision, depth occlusion and restored GL state. Scans obey both the configured check limit and a short time budget, stay tick-owned and never load terrain.

- Recovery-status checks run every game tick and must never acquire the settings writer's disk lock. Publish the status independently while retaining locked durable writes and explicit menu/shutdown flushes. Toggle regression checks must include the following full tick and HUD draw, with the writer stalled.

- Plain double-click gathering consumes player inventory before container sources, partial
  stacks before full stacks within each inventory. Use native clicks, preserve oversized
  stacks and full native data, and revalidate callback results without overwriting changes.

- Title, panorama picker and other main-menu backgrounds share one animation clock.
  Native scrolling lists keep dirt header/footer textures over the panorama. Title clicks
  cycle a retained retro draft, stable preset IDs and official styles through ConfigSession.
  Brush pickers may opt into filtered instant selection; other pickers retain draft exits.
  HUD/title inline edits and underline tabs use shared controls. HUD divider drags are
  visit-local, cancel on focus loss and always retain controls and preview space.
- Title music and logo clicks share one input handler, with music controls taking priority.
  Releasing an already-free pointer must not warp it during menu navigation; gameplay
  capture requires an open world. HUD editor
  selection presses never start a drag; position-only resets use explicit position fields
  and preserve layout mode, visibility, information, styling and unrelated elements.

- HUD editor groups share the Options heading renderer, collapse actions and sticky
  geometry. Reset scopes cover all editor-owned fields or the selected view, and
  remain undoable as one edit. Preserve the selected info-cluster ID during global
  resets. Item-name previews use the shared native message position only when enabled.

- HUD editor layout shares one geometry model for controls, scrollbars, dividers and
  preview input. Preview coordinates retain the window aspect ratio in both split
  orientations. Hidden info clusters stay selectable without enabling them. Preview
  backgrounds are cached captures, never a second live world. Nested colour swatches
  stay in the HUD draft and share its Undo, Done and Discard boundary.
