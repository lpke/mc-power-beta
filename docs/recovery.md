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

## Settings

`power-beta-data/settings-backups` stores settings transactions. Pending recovery
runs before settings load. Keep pending records and originals when investigating
a failed save. This archive is not a world backup. Existing backups from previous
versions remain on disk for manual recovery.
