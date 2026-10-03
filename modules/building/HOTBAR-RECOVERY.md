# Hotbar recovery

Every accepted swap first appends a durable snapshot to
`.minecraft/lpketweaks-hotbar-backups/<world-and-player-id>.journal`.
The journal contains native uncompressed NBT, with checksummed record boundaries.
It never overwrites older records. No recovery operation runs automatically over
inventory that may have changed through later gameplay.

If swapping reports a protection failure, stop using the affected world and make
a copy of both its save folder and recovery journal. Keep the originals intact.
The game log explains whether the failure involved invalid stack data, changing
inventory state, a full journal, a write failure or a damaged record.

Use the included read-only utility to list complete records:

```sh
python3 scripts/hotbar_recovery.py path/to/file.journal
python3 scripts/hotbar_recovery.py path/to/file.journal --extract -1 --output recovered.nbt
```

Extraction creates a new file and refuses to overwrite one. `-1` selects the last
complete checksummed record. A damaged final record does not prevent extracting
an earlier valid one. Inspect `MainBefore`, `MainAfter`, `Armor`, `SelectedSlot`,
`World`, `Seed`, `Player` and `Time` with an NBT editor. Restore only the intended
missing stacks into a copied save while Minecraft is closed. Do not replace a
whole inventory blindly: later legitimate item changes may need to be retained.

The mod refuses swaps if NBT serialization changes count, ID, damage or custom NBT,
if slots alias the same stack object, or if the standard inventory layout differs.
Main and armour inventories must be stable, the cursor empty, the player alive,
and the operation on the game thread in a focused singleplayer world.

A complete hotbar swap publishes one new array. Old live arrays are never partly
rearranged. On a detected failure after publication, the transaction restores its
preallocated NBT snapshot if it still owns that array. It does not overwrite an
unrelated replacement array installed by another mod. Either failure preserves
the journal and disables swapping for that process.

These protections cover failures the mod can detect. They cannot guarantee data
survival after disk failure, external save corruption or arbitrary bugs in other
mods. Keep normal world backups as well. The journal is capped at 64 MiB per
world/player; reaching the cap stops swapping until the journal is archived and
the game restarted. No automatic deletion or retention window is used.
