# Inventory ownership and recovery

- Inventory looking shares eligible screens and focus rules with inventory movement.
  Check the originating press against every GUI panel, creative tab and destroy slot.
  Use native sensitivity/rotation. Capture and hide the pointer, GUI hover and cursor
  stack until release; hidden hover coordinates must never drive the player model.
  Restore the originating position inside the native grab transition, without a
  second warp. Restore on release, lost focus, screen/world changes or lost eligibility;
  holding the button must not restart a canceled drag. Leave events and polling to
  unrelated screens, including photo mode. Right-click looking must not drop items.
  Dispatch complete mouse batches before changing capture. Preserve each drag's
  press coordinates/deltas, send final motion through the native camera renderer,
  and remap later clicks to the restored cursor. Both native and raw helpers supply
  GUI cursor deltas; raw device deltas remain gameplay-only. Linux capture retains
  the event baseline because XQueryPointer can lead queued events. Complete quick
  drags need a hidden native cursor transition on Xwayland. Test consecutive fast
  drags, both helpers and real pointer movement immediately after release.
- Shift-double-click transfers matching native item data from the clicked inventory,
  leaving the held cursor stack untouched. Use normal item/slot limits and native
  multiplayer transfers. Exclude outputs, hidden catalog and read-only slots. Cancel
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
- Offhand keeps the native 36 main and four armor slots unchanged; its logical
  recovery slot is 45 after cursor and crafting inputs. Disabled offhand retains
  contents for extraction. Format-4 metadata must be durably verified across primary,
  previous and dimension files before any new inventory ownership is enabled.
  Wrap the complete world-load path: the capture module replaces native loading
  with a cancellable HEAD hook, so native TAIL injections alone are insufficient.
  Inventory side slots share drawing and hit geometry through InventorySideSlots.
  Reserve their width beside an open recipe book; gameplay slots are not modal
  panel bounds. Scope native outside-click redirection to the originating slot,
  player and world, retaining the inventory gesture dispatch path.
  Every native inventory-bearing container appends its own offhand transport slot;
  recovery still uses logical slot 45. Native transfer ranges exclude that appended
  slot, and frame drawing and input derive their Y position from the actual hotbar.
- Recipe placement, sorting, refills, automatic tools and saved Creative hotbars
  share inventory leases and immutable worker-flushed plans. Validate the whole
  protected inventory and external native owners before and after callbacks. Roll
  back only owned bytes; restore derived recipe output only after every input was
  restored. Fuel is never a recipe ingredient. Unknown recipe subclasses need an
  explicit display provider. Recipe availability publishes completed display snapshots;
  cached craftability never authorizes an inventory write. Saved Creative hotbars and
  sorting layout presets remain user data outside defaults and instance-default resets.
  Fast placement waits for queued partial-stack refills as well as empty hands,
  including the handoff to a durable inventory plan.
  Sorting-layout overlays keep the native screen and container open through nested
  settings pages and queue immutable settings/preset changes immediately. Menu-opened
  editors retain their isolated drafts and explicit saves. Neither edits inventory.
  Modal sorting and saved-hotbar layers skip native inventory/model/tab rendering;
  Escape restores the same host. Slot double-clicks use native event timestamps and
  cancel on painting, focus loss or navigation. Sorting and matching deposit ignore their retired enable
  fields; button visibility never gates an action or its unassigned-by-default key.
  Chest offhand frames use the actual texture height, one below its declared GUI
  height, while item positions remain tied to native hotbar slots.
  Recipe removal returns one input per occupied ingredient through the same durable
  plan and output guard as placement. Reject stale/manual grids and preflight capacity.
- Creative middle-click dragging previews compatible, writable slots and commits
  together on release. Use normal item and slot limits; preserve cursor damage/NBT.
  Never paint hidden catalog slots, survival, spectator or multiplayer inventories.
  Cancel on focus loss, screen/container changes, lost Creative access or cursor changes.
- Spectator inventory capabilities apply only to the local player's exact native
  inventory screen and owned player container. Creative permission selects its
  inventory views without changing actual mode. Keep crafting, gestures and manual
  inventory tools available there; world interaction, background automation and
  all item dropping remain blocked. Spectator container inspection uses immutable
  display slots without installing or activating a world-owned handler. Closing
  the personal inventory retains crafting inputs and the cursor; persist the latter
  with full native item data so save/reload cannot lose it. Scope the inventory player model to
  its renderer so opening inventory never reveals the Spectator body in the world.
- Armor Shift-drags share the inventory gesture cancellation boundary and native
  clicks, keep cursor/NBT/count ownership, visit each source once and exclude custom
  containers, catalog/output and read-only slots.
- A durable personal-grid transfer acknowledges only its verified changed slots to
  the active drag guard. Keep unrelated visited-slot and cursor checks intact so
  a publication cannot cancel its own Shift-drag or admit a foreign mutation.
  Revalidate the drag before preparing each queued baseline and immediately before
  publication; do not apply the old baseline check between the plan's own writes.
- Creative delete slots share the native container's hotbar geometry, offhand frame
  and pointer bounds. Delete through the flushed inventory plan, including cursor,
  equipment and personal crafting inputs when clearing all. Never clear chest,
  dispenser or furnace contents. Capture those external owners, retain permission
  and exact-screen checks, and restore derived crafting output only after owned
  input rollback. Creative deletion authorizes null replacements only.
- Dropping an incompatible cursor onto the Creative palette deletes that cursor
  through the same protected plan. Palette amount scrolling needs a matching,
  nonempty cursor; zero needs another click. Tab-bar wheel pages regular tabs.
  Search autofocus owns the shared text-input bridge until focus leaves that editor.
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

- Hotbar swaps flush immutable recovery bytes on a worker before publication on the
  game thread. Prepare selected preview rows while held; only a validated, durable
  plan can publish on release. Previewing alone never changes inventory. Keep native NBT access on the game thread, bound queued requests, and cancel
  stale plans on inventory/owner, menu, focus or world changes. Never skip the durable flush.
- Plain double-click gathering consumes player inventory before container sources, partial
  stacks before full stacks within each inventory. Use native clicks, preserve oversized
  stacks and full native data, and revalidate callback results without overwriting changes.

- Inventory minimaps render after screen dimming and before container panels. Pointer
  wheel zoom only owns terrain inside the visible minimap and outside every InventoryGui
  panel/control. HUD preview wheel edits remain in its draft; sidebar edits reuse the
  Options session; gameplay snapshots use the queued PowerConfig writer.
  Expanded-map gameplay zoom must consume the native wheel before Spectator speed
  and camera zoom handlers, while retaining held hotbar-row priority. Hidden maps,
  menus and focus loss leave their existing wheel ownership intact.
- Recipe craft-one exchanges authorize exactly one native recipe batch and its container
  returns. Shift-right-click maximizes one grid and plans complete batches up to native
  ingredient limits and output capacity, capped at 64. Publish the complete turn once
  after its immutable journal is flushed. Run each native callback against its isolated
  grid and check all protected inventory and external owners before and after each
  callback. Enlist external crafting owners even when the final inputs are unchanged.
  Preserve output ownership and owned rollback; a later callback failure cancels the turn.
- Alt-click opens recipe variants. Shift-right-click crafts maximum in crafting grids;
  furnace recipes only fill ingredients and never bypass smelting. Untouched recipe
  icons keep wheel page scrolling. Amount changes require an explicit recipe choice
  during that book visit; removing the last batch releases wheel ownership until a new click places it.
- Minimap quick controls share native inventory and pause hosts, remain visible without focus,
  and open inward from the selected corner. World waypoints render below their GUI and minimap,
  never over settings editors. Disabling cave mode retires only obsolete view work; retain saves.

- Sorting layouts and named layout presets belong to stable world IDs in PowerConfig,
  separately from global tool/restock preferences. Freeze the old global layout as
  the migration seed; never overwrite an existing world entry. Preserve unknown
  presets and block unsafe sorting when data is unsupported. World copies and backups
  retain the scoped layout, including copies made before their first load.
