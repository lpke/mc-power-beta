# Changelog

## 1.2.1

- Restore the player view immediately when Free Look is released.
- Hide hotbar row numbers when modifier-plus-number shortcuts are disabled.
- Left-click increases/cycles settings; right-click decreases/reverses them.

## 1.2.0

- Add Tweakeroo-style hotbar preview and direct inventory-row swaps. Bind a
  preview modifier with 1/2/3 shortcuts or three independent row keys.
- Add hold-and-scroll row selection, swapping on release. Expose reverse scroll,
  remembered row, overlay visibility, five alignments and X/Y offsets.
- Add three grouped Mod Menu pages and native Controls bindings. New hotkeys
  start unbound. Keyboard and mouse bindings are supported.
- Protect swaps with native NBT round-trip checks, duplicate-reference checks,
  stable player/container state and an empty cursor requirement.
- Write, sync and read back an append-only recovery snapshot before every swap.
  Publish the new inventory array once; roll back detected commit failures and
  disable further swaps after a protection failure. Never drop or merge stacks.
- Add a read-only recovery extractor and fault tests for failed writes, damaged
  journals, interrupted commits, unexpected mutation and replacement inventories.
- Cancel pending swaps on menus, focus loss and world changes. Prevent swapping
  from triggering held placement/mining or UniTweaks number-key selection.
- Default Free Look to first person, following the existing third-person camera
  when active. Add a setting to disable that follow behaviour.
- Display `Free Look` in native Controls while preserving the existing binding.
- Disable flexible and fast placement during spectator noclip.
