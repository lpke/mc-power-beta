# Changelog

## 1.1.0

- Restore smoothly interpolated walking animation while flying.
- Add Toggle/Hold flight sprint, defaulting to Toggle; clear the latch on flight,
  focus, world and mode changes.
- Join the destroy slot to the inventory frame; show its tooltip only with Shift.
- Preserve the cursor stack when switching creative/survival inventory tabs.
- Remove help text below the gamemode icons and shorten the picker panel.
- Left-click increases/cycles settings; right-click decreases/reverses them.

## 1.0.0

- Fork BHCreative 0.4.10 as LpkeCreative. The first commit is an exact upstream
  snapshot. Preserve existing creative tabs, compatibility API and saved flags.
- Add modern creative flight acceleration, drag, double-tap Jump and Left Ctrl
  sprint. Expose speed, sprint multiplier, landing behaviour and double-tap timing.
- Add flight glide from 0 to 5. Zero stops motion immediately when the relevant
  movement keys are released; five retains normal modern flight drag.
- Add the F3+F4 gamemode picker, selection on F3 release, mouse selection and
  previous-mode recall. Preserve the debug HUD state.
- Add spectator flight, noclip, wheel speed control, saved speed and safe exit
  from solid blocks. Block damage, interactions, pickups, drops and inventory use.
- Add modern `/gamemode` and `/gm` syntax for survival, creative and spectator,
  including numeric aliases and local selectors. Integrate with RetroCommands.
- Extend creative block and entity reach to five blocks by default, with separate
  settings and intervening-block checks. Preserve survival reach.
- Add a destroy-item slot to the left of the creative hotbar. Shift-click clears
  inventory, armour, crafting grid and cursor; both behaviours are configurable.
- Add five Mod Menu pages, native key bindings and a grass-block icon.
- Preserve gamemode across save/load and respawn. Suspend movement input during
  menus, focus loss and Freecam.
- Replace BHCreative's shared item-use backups with local exception-safe backups
  so nested uses cannot corrupt item counts or durability.
- Guard creative pick-block against entity hits and absent worlds/players.
- Upgrade build tooling for the installed Mod Menu API. Runtime dependencies
  remain unchanged; reject simultaneous installation with BHCreative 0.4.x.
