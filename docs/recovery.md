# Inventory recovery

Keep normal world backups. Recovery records protect detected failures; they cannot
recover data lost with the disk itself.

## Hotbar swaps

Each accepted swap appends native NBT to
`.minecraft/power-beta-data/hotbar-backups/<world-and-player-id>.journal` before
publishing the new inventory array. Records are checksummed and never pruned.
At 64 MiB, swapping stops until the journal is archived and the game restarted.

If protection fails, close the world and copy both the save and journal. Inspect
complete records without changing either original:

```sh
python3 modules/building/scripts/hotbar_recovery.py FILE.journal
python3 modules/building/scripts/hotbar_recovery.py FILE.journal --extract -1 --output recovered.nbt
```

Extraction refuses to overwrite files. `-1` selects the last complete valid
record. Inspect `MainBefore`, `MainAfter`, `Armor`, `World` and `Player` with an NBT
editor. Restore only missing stacks into a copied save while Minecraft is closed;
replacing the whole inventory could erase later gameplay.

## Carried containers

Carry journals live in the world's `data` directory. They preserve original block
NBT, removal/placement intent and completed-transfer archives. Never delete a held
or failed transaction record. Each chest half has its own inventory. A replay mismatch must stop and preserve evidence rather
than overwrite a changed block or inventory.

## Death records

Optional death records live in `.minecraft/power-beta-data/deaths`, separated by
world ID and player name. Disabling the feature or cheats preserves the journal.
Records include native item NBT, full counts, damage, location and cause.

`/death recover [id]` copies a saved inventory into the main inventory once,
without replacing current items or equipment. It requires creative mode and
cheats, and cancels before changing slots if every saved item cannot fit. Dropped
items remain in the world.

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
