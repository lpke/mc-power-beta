# Rendering, HUDs and maps

- Held-item lighting uses render-only terrain endpoints and the existing 32-byte
  vertex. Never change world light, material opacity or spawning. Bound endpoint
  preparation to loaded render sections; freeze and prepare the physical source
  before synchronous image capture. Freecam movement never moves that source.
  Retain established light strength across terrain rebuilds, and regenerate existing
  endpoints in the source's at-most-eight influence sections separately from bounded
  first-time preparation. Mining must not toggle the light off between meshes.
- Compact buttons must retain all four texture borders. Check narrow GUI sizes,
  sticky headings, clipping and click targets, not only a wide screenshot.
  Creative catalog rows grow upward; hotbar and survival slots never move.
  The final catalog row and hotbar use the same 22-pixel origin spacing as survival.
  Leaving spectator keeps the current position, including inside solid blocks.
- `TweakIndicators` exposes live module state across mappings. HUD inclusion is
  separate from gameplay settings. Do not infer active auto-walk/free look from
  feature availability or poll action keys while rendering.
  Per-feature toggle messages default off and observe transitions on ticks,
  independently of HUD inclusion. Menus, focus changes and world loading stay silent.

- The locally ridden boat or minecart bypasses render frustum/occlusion culling only while
  that player owns the view. Keep other entities, visibility preferences, saved flags
  and physics unchanged; normal culling resumes immediately on dismount.
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
  never use front view, regardless of perspective style or HUD visibility. On freecam entry, hide the stationary player only until the view first leaves its head; after that, proximity never hides it during that visit.
  Container previews use a separate interpolated, occluded ray and independent
  container-preview block/entity reach in every mode, including with the HUD hidden. Reject
  unloaded terrain before raycasting; never load chunks, open inventories or change
  the player's interaction target. Keep multiplayer previews disabled.
- WorldEdit named states map only to supported Beta metadata. Reject unsupported
  properties before starting a transaction; commas inside [] are state separators.
- Entity visibility changes only rendering. Full bright overrides float shading only
  on the render thread, never saved or simulated light. Manual full bright is independent
  of permissions. Spectator overrides last for one visit; leaving restores manual state, and
  lost Spectator access clears only that override.
- Custom HUD clusters keep stable IDs and independent content, anchors, offsets,
  color, size, opacity and F3 visibility. Sample world data on ticks and use the
  daylight clock. Native HUD previews and Cancel update positions immediately.
  F3 and custom HUD location data follow freecam while detached, including facing,
  biome, chunk and block light. HUD queries must not load chunks or move the player.
  F3 providers share debug text layout/color and capture only that block, never chat
  or other HUD text. F3 and custom HUD share the eye/feet preference; freecam and
  Spectator always report the native head base. Freecam collision uses that same
  lower bound. Third-person ghost heads use the current skin dimensions, draw only
  head/hat and support free look without moving the player. Spectator third-person
  cameras bypass terrain clipping; their head marker stays visible inside blocks.
- Photo-mode drags own the button from its originating press through release. Widget
  drags never become camera drags; lost focus cancels them. Clamp tilt before setting its
  target and preserve the original daylight snapshot through GUI resizing.
- Underwater fog density is 0–100 percent. Power Beta defaults to 40; the other bases retain 100. Zero and full bright use normal
  terrain fog; other values scale native water fog without changing lighting or other fluids.
  Preview/Cancel restore it immediately, and full bright never changes the saved percentage.
- Underwater darkness adjusts render-only daylight shading with bounded, frame-local column
  samples. Preserve roofs, night lighting, emission and opaque AO. Never change stored light,
  water opacity, physics, or fog preferences; keep loaded-chunk and render-thread guards.

- Water fog and FOV use the current rendered camera pose, including third person, free look
  and freecam. Resolve perspective after capturing that pose; preserve photo projection,
  stereo and zoom. Inside/outside water darkness affects rendering only. Terrain retains native
  and water-clear vertex colors in one 32-byte vertex, with one shader uniform choosing
  brightness. After the first opt-in prepares existing meshes, surface crossings and
  slider previews must never queue terrain rebuilds. Day/night updates must also include
  compensated deep water whose native skylight is zero.
  Roof and water-column edits must invalidate dependent meshes below native light's reach,
  using the existing bounded render grid and update queue without loading terrain.
  Calculate the second color only for affected sections; bound scratch memory and preserve
  geometry, normals, alpha, native update scheduling and world light arrays.
  Detached flight admits auto-walk independently of manual camera keys and keeps animation
  interpolation current when the player is stationary.

- Plain F4 toggles the custom HUD. A valid mode-picker modifier gesture owns its cycle
  key instead; test both release orders and remapped/chorded bindings.

- Official title backgrounds use verified Mojang client and resource-index assets. Resource-index panoramas override fallback client textures. Keep version IDs stable, deduplicate identical scenes, and render unblurred panoramas at window resolution.
- The panorama picker groups variants of the same captured scene. Preserve legacy version IDs and texture paths. Its single selection also updates the retained enable flag, and its live preview shares the title renderer.

- HUD and title editors own isolated drafts until Done. Preserve existing setting IDs, combined/separate layouts and title preset IDs; nested pickers and modal exits must not leak changes. HUD previews share native positioning maths and the active font/item textures.
- HUD preview controls share the selector row when they fit and wrap otherwise.
  General-element clicks reveal their setting and highlight both its row and the
  clicked preview element until another element, empty preview space or setting
  is clicked, without changing the draft.
- Light-overlay rendering batches native digit geometry with the current font texture and advances. Keep camera-relative precision, depth occlusion and restored GL state. Scans obey both the configured check limit and a short time budget, stay tick-owned and never load terrain.

- Recovery-status checks run every game tick and must never acquire the settings writer's disk lock. Publish the status independently while retaining locked durable writes and explicit menu/shutdown flushes. Toggle regression checks must include the following full tick and HUD draw, with the writer stalled.

- Title, panorama picker and other main-menu backgrounds share one animation clock.
  Native scrolling lists keep dirt header/footer textures over the panorama. Title clicks
  cycle a retained retro draft, stable preset IDs and official styles. Validate the full
  selection and queue its immutable snapshot through PowerConfig; never back up, read
  or flush settings on a logo click. Prepare title textures before interactive cycling.
  Brush pickers may opt into filtered instant selection; other pickers retain draft exits.
  HUD/title inline edits and underline tabs use shared controls. HUD divider drags are
  visit-local, cancel on focus loss and always retain controls and preview space.
- HUD editor groups share the Options heading renderer, collapse actions and sticky
  geometry. Reset scopes cover all editor-owned fields or the selected view, and
  remain undoable as one edit. Preserve the selected info-cluster ID during global
  resets. Item-name previews use the shared native message position only when enabled.

- HUD editor layout shares one geometry model for controls, scrollbars, dividers and
  preview input. Small layouts can hide either pane for that visit, keeping at least
  one visible. Hiding preview cancels its input and gives settings the space; keep its
  scroll and selection, nested F3 drafts and modal ownership intact. Wide layouts
  retain their existing controls. Preview coordinates retain the window aspect ratio in both split
  orientations. Only the selected hidden info cluster has an empty outline and hit
  target; selection never enables it. Preview
  backgrounds are cached captures, never a second live world. Nested color swatches
  stay in the HUD draft and share its Undo, Done and Discard boundary.
- Info-cluster previews use the gameplay formatter, clipping, anchors and renderer.
  F3 rows in both columns use stable IDs, individual visibility and a bounded saved order
  with explicit gap rows. Reorder keeps a temporary draft at GUI font size; Save or
  an outside click accepts it into the HUD draft as one undoable edit. Preserve
  unknown IDs during reordering and frozen legacy offsets. Columns share validation, preview and live placement.
  Drag ownership lasts through the native release event; polling may lead queued releases.
  HUD scene backgrounds default on and use an untouched native-renderer capture at
  12 chunks in clear daytime Forest terrain with a visible mob for the dynamic entity-ID
  tag preview, without loading a second world in menus. Preview toggles never change
  settings, and editor selection/scroll memory stays separate from drafts.

- Map sampling stays on the game thread and reads loaded native/flattened chunks only.
  Minimap and world map share versioned tiles, mip levels and bounded CPU/GPU caches.
  Compression, image decoding, exports and durable writes use immutable requests on
  the bounded map worker. Corrupt/future terrain and waypoint data remain protected.
  Keep world/server identity, dimensions and cave layers separate; terrain edits and
  texture changes invalidate loaded samples. Terrain format 2 reads format 1; verify
  the original tile backup before upgrading. Bake directional relief before reducing
  map levels; reconcile cached neighbor seams without sampling new terrain. Map
  movement is frame-owned; sampling cadence, worker queues and texture uploads stay
  bounded. Buffer padding must not select terrain detail or request extra tiles.
  Map navigation never moves the player
  or loads terrain. Teleport actions use the existing local permission and command.
  Automatic cave roof checks ignore leaf material without changing sampled canopy terrain, solid roofs or cave floors. Nighttime surface shading uses native daylight/weather darkness and sampled block light.
  Full bright bypasses surface and cave-light shading through render styles, preserving saved preferences and terrain data. Map preferences use PowerConfig; minimap presentation belongs to the HUD draft,
  with a link from the Map tab. Initialize a missing minimap enablement once from
  the selected base with a verified backup; later Use as base changes never alter it
  on restart. Preserve waypoints while features are disabled.
  Disabled views perform no sampling, radar, path or cache preparation; finish only
  previously queued durable work before releasing idle caches. Independent world
  waypoints/deathpoints and explicit waypoint actions retain only their own storage.
  World Map detail uses physical window pixels and bounded paired surfaces. Keep
  coarse-to-fine fallback images separate so unknown fine cells cannot retain coarse
  coverage. Publish only completed disk reads within the render budget; sampling,
  parent merges and writes stay tick-owned. Exact cave windows use versioned cache
  contexts; retain legacy cave files and existing surface paths. Both cave ranges use
  0–64 with defaults 10/10. Keep partially sampled columns when reprioritizing.
  Cancel only obsolete queued view-only reads, reserving worker admission for native
  sampling and parent maintenance; durable writes and FIFO flush barriers retain order.
  Minimap live rendering and HUD hit/drag bounds share geometry and cached paired
  offsets. Fixed-to-screen world waypoint projection removes only the native bob transform from the
  actual per-eye matrix; Fixed to world retains it. Preserve hurt, portal, third-person and detached camera poses in both modes.
  Reject behind/near-plane points without culling distant waypoint labels at the terrain
  far plane. Color-wheel/RGBA tabs share one local RGB/opacity draft and retain native
  drag release ownership even when mouse polling observes the release early.
- Minimap frames use the retained reference atlas and seam-aware square/circle UVs through the native texture manager; keep frame mesh allocation off steady-state frames. In-world symbols stay anchored while aimed labels expand. Viewport updates must rotate missing tiles and admit bounded reloads before ready-first painting, without starving initial fill.
- World-map settings reuse the scoped Options session with the map as the active
  screen. Render, input, search, related jumps and reset targets share the same
  full-GUI viewport; nested editors return to that host without publishing drafts.
  Retained map copies use only the map pane's physical pixel rectangle, with
  independent X/Y GUI scaling. Clip terrain as well as markers before drawing
  beside the settings panel.
  Secondary minimap reads retain their own bounded admission without replacing the
  world-map interest or consuming every parent-maintenance slot.
- Entity radar heads are upright orthographic native head models. Minimap icons are hold-only;
  the World Map can also retain heads through its own Mobs preference. Sidebar text editors,
  modals and binding capture retain keyboard ownership. Build at
  most one new icon and resolve at most one source texture per native frame, shared by both
  map views and stereo eyes. Use bounded caches, invalidate on texture reload, and retain
  dot fallback for pending/unsupported entities. Do not read entities on workers or modify
  native renderer models, poses or textures. Disabled radar starts no icon work.
- Entity radar uses loaded chunk sections with bounded discovery and separately capped
  minimap/world-map selections. Refresh tracked entities every tick; entity-count churn
  must not restart discovery or delay publication. Minimap radar respects EntityVisibility;
  only an actually open World Map bypasses that filter. Chunk cursors must not retain
  unloaded terrain. Horizontal range is capped by the terrain render distance.
- Terrain block-ID caches belong to one synchronous mesh and its owning render thread.
  Bound the cache to the section plus one-block border, bypass unknown chunk subclasses
  and nested builds, and invalidate IDs and water opacity when live terrain callbacks edit it.
  Never use the cache to load chunks, change world light or defer native geometry updates.
- Leaf brightness hooks retain StationAPI state emission for ordinary samples and
  fixed block emission for the enabled leaf override. Read native brightness once
  and avoid per-sample cancellable callback allocation.
- Water-light columns remain frame-local and resume scans only to requested heights.
  Maximum-brightness shortcuts must check the complete current brightness table.
  Direct section reads admit only known native implementations; custom chunks and
  sections retain native reads. Map clipping scratch retains exact coordinates and
  texture interpolation. Map parent reduction may share game-thread-owned tiles;
  durable worker writes must still receive immutable snapshots.

- Water-lighting columns use bounded reusable storage and mixed coordinate hashes. Publish only
  completed requested rows; nested reads and live invalidation must never expose unfinished or stale data.
  Culling workers read published renderer capabilities, never the native renderer resolver, which
  mutates its registry even for missing renderers. Native world setup and renderer discovery own
  snapshot publication. Map clipping retains exact native coordinates and texture interpolation.
- Terrain draw-command storage grows with submitted meshes, independently of vertex-pool capacity.
  Retain all pending command words across both command-buffer and vertex-buffer growth. Native
  draw order, indices, vertex format, visibility and GPU buffer ownership stay unchanged.

- Title music and logo clicks share one input handler, with music controls taking priority.
  Releasing an already-free pointer must not warp it during menu navigation; gameplay
  capture requires an open world. HUD editor
  selection presses never start a drag; position-only resets use explicit position fields
  and preserve layout mode, visibility, information, styling and unrelated elements.

- Reject incomplete skin-provider responses so the next provider can supply the
  texture. Keep bundled fallback skin paths out of the HTTP downloader.

- Optional redstone textures use four-pixel wires with shaded edges and six-pixel
  junctions; the power number panel occupies one quarter of a block.

- Isometric captures use the actual renderer chunk grid and Beta's 128-block
  height. Center both horizontal axes on the camera's chunk; never use camera Y
  for Z or infer capture size from the obsolete four-step distance option.


After each release, follow [the owner review report instructions](release-report.md).
Keep the website changelog generated from GitHub; do not add separate detailed notes.
