# Inventory recovery

Keep normal world backups. Recovery records protect detected failures; they cannot
recover data lost with the disk itself.

## Hotbar swaps

Each accepted swap appends native NBT to
`.minecraft/power-beta-data/hotbar-backups/<world-and-player-id>.journal` before
publishing the new inventory array. Records are checksummed and never pruned.
New journals use `world-<world-and-player-id>.journal`, keyed by the stable saved
world ID and player. Their NBT records include `OwnershipVersion: 2` and `WorldId`.
Renaming a world keeps its journal; duplicated worlds receive independent records.
Older name-and-seed journals stay unchanged and remain readable by the same recovery
tool. Keep them when investigating older swaps; they cannot always be assigned to
one world when old copies shared a name and seed.

The journal flush runs on a worker. The game continues while it writes, then
commits the swap on a later tick only if the same player still owns the unchanged
inventory. Inventory edits, menus, lost focus and world exits cancel pending swaps.
Rapid requests are processed in order with a bounded queue. A canceled request
can leave a recovery record even though its swap was never applied.
At 64 MiB, swapping stops until the journal is archived and the game restarted.

If protection fails, close the world and copy both the save and journal. Inspect
complete records without changing either original:

```sh
python3 modules/building/scripts/hotbar_recovery.py FILE.journal
python3 modules/building/scripts/hotbar_recovery.py FILE.journal --extract -1 --output recovered.nbt
```

Extraction refuses to overwrite files. `-1` selects the last complete valid
record. Inspect `MainBefore`, `MainAfter`, `Armor`, `World`, `WorldId` when present,
and `Player` with an NBT
editor. Restore only missing stacks into a copied save while Minecraft is closed;
replacing the whole inventory could erase later gameplay.

## Carried containers

Carry journals live in the world's `data` directory. They preserve original block
NBT, removal/placement intent and completed-transfer archives. Never delete a held
or failed transaction record. Each chest half has its own inventory. A replay mismatch must stop and preserve evidence rather
than overwrite a changed block or inventory.

## Inventory plans

Offhand swaps, recipe placement, sorting, restocking, tool changes and saved Creative
hotbar loads use `.minecraft/power-beta-data/inventory-recovery`. Each checksummed
journal is scoped to the stable world ID and player. Prepared records contain full
native before/after item data, protected player slots and external container snapshots.
The worker flushes immutable bytes before a game tick can publish any slot changes.
Canceled preparations can leave records even though no inventory change occurred.

Publication rechecks the player, world, screen, handler, slot ownership and full item
data. A detected callback failure restores only changes still owned by the plan and
stops this action family. Derived crafting output is restored only after all owned
inputs are restored. Foreign changes remain intact. Keep the world and journal if
the game reports a stopped inventory action; inspect their before/after records on
copies rather than replaying a whole snapshot over later gameplay. At 64 MiB, archive
the journal with the game closed and restart before using these actions again.

Offhand ownership uses separate versioned player data. World-rule format 4 prevents
older Power Beta versions from opening these saves and silently losing that slot.
The format upgrade verifies and backs up primary, previous and dimension metadata
without changing gameplay rules. Keep `world-metadata-backups` if an upgrade is
interrupted. A changed file that no longer matches the transaction is preserved and
requires recovery instead of being overwritten.

## Death records

Optional death records live in `.minecraft/power-beta-data/deaths`, separated by
world ID and player name. Disabling recording or recovery permission preserves the journal.
Records include native item NBT, full counts, damage, original slots, location and cause.

`/death items [id]` opens a saved inventory view. Without `allow_death_recovery` its items are
locked. With that rule enabled, click a stack or the check-mark button to recover remaining
items once. Recovery cancels before writing if any selected original slot is
occupied, including by a matching item. It never replaces or merges current items.
Held cursor items use available main inventory space. Dropped items remain in the
world. Individual and bulk GUI recovery share durable per-stack claims.

The Game rules setting Keep inventory on death defaults off for Survival and on
for the Cheat preset. It is independent of recovery permissions. The rule keeps main
inventory, armor, the offhand, the held cursor and player crafting inputs through respawn.
Dead-player NBT includes a complete native snapshot for restart safety. Death
records retain their item snapshots but mark every stack recovered, with the log
label Recovered (kept). The keep rule works even when death records are disabled.

Death IDs are decimal numbers, starting at 1 for each world and player. Release
1.4.2 deliberately wipes earlier death logs rather than importing their IDs or
recovery claims. Instance updates back up the logs before resetting them. A client
opening a previous-format journal also resets it. Current logs retain normal
checksum and interrupted-recovery protection. /death log lists old to new.

Each recovery flushes its complete before/after inventory to the checksummed
journal before changing slots. A detected failure restores the original
inventory and disables recovery until restart. A crash can leave an interrupted
claim, which stays blocked to prevent duplicate recovery. Keep both the world
and journal for inspection; do not remove a claim to force a retry. Restore only
missing items into a copied save. Completed claims do not replace world backups.

## Settings

`power-beta-data/settings-backups` stores settings transactions. Pending recovery
runs before settings load. Keep pending records and originals when investigating
a failed save. This archive is not a world backup. Existing backups from previous
versions remain on disk for manual recovery.
