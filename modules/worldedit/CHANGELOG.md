# Changelog

## 1.2.1

- Left-click increases/cycles settings; right-click decreases/reverses them,
  including the per-world override, colours, opacity, width and edit limits.

## 1.2.0

- Add an enabled-by-default master switch for commands, wand and selection display.
- Require creative mode by default when BHCreative or LpkeCreative is installed.
  Keep ordinary singleplayer access when neither is installed.
- Add a per-world Inherit/Enable/Disable override, saved in `level.dat` and copied
  across dimensions. Priority is master switch, world override, then integration.
- Add an Access page to Mod Menu. Disabling access rolls back unfinished edits.
- Add `//up`, `/ascend`, `/descend`, `/ceil`, `/unstuck`, `/thru` and `/jumpto`,
  including aliases, double-slash variants, help and completion.
- Support creative flight or an undoable glass platform for up/ceil, with `-f`
  and `-g` flags. Respect the creative flight setting.
- Check loaded terrain, headroom, dangerous blocks, world height and mounts
  before navigation. Preserve existing multiplayer and Freecam restrictions.
