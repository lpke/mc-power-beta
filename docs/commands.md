# Chat and WorldEdit

Use **Up/Down** to browse command history. Once browsing starts, those keys stay
with history until you edit the text or use Tab. When typing a new command,
Up/Down select completion suggestions and **Tab** inserts the selected result.
**Ctrl+Up/Down** always browse history.

Chat supports clicking and dragging to select text, **Shift+arrows**, **Home/End**,
**Ctrl+A**, **Ctrl+C**, **Ctrl+X** and **Ctrl+V**. Ctrl+arrows move by words.
Text stays within Beta's 100-character chat limit.

## Game rules and permissions

`/gamerule` lists the current world's rules. `/gamerule fire_spread` reads one;
`/gamerule fire_spread false` changes it. Familiar aliases such as `doFireTick`
refer to the same value. Use `/gamerule help` or the searchable Game rules tab
for descriptions. Rule changes save with the world and affect every dimension.

The local owner can always edit rules. Separate permission rules control game
modes, item spawning, world editing, entity control, survival assistance, travel,
spawn editing, time, weather and death recovery. `/help`, completion and command
execution use the same permissions. Read-only tools such as `/time query`, warp
listing and count-only `/clear` remain available.

`/repair` fully repairs the held damageable item without changing its custom data
or count. It requires `allow_survival_assistance`, alongside `/heal` and `/god`.
Use `/repair help` for instructions without changing the item.

## World spawn

`/spawn` returns to the Overworld's world spawn, including from the Nether.
It requires `allow_teleportation` and leaves your bed spawn unchanged. Use `/spawn help` for
its instructions; `/help` lists available commands.

## Selection size

- `//contract 1 down` moves the top face down one block.
- `//contract 1 up` moves the bottom face up one block.
- `//contract 1 top` removes one block from the top.
- `//contract 1 bottom` removes one block from the bottom.

Other directions describe the movement of the contracting face, as in modern
WorldEdit. An amount that would invert the selection is rejected.

## Block states

Use `block[property=value]` for named states. Examples:

```text
//set rail[shape=east_west]
//set powered_rail[shape=ascending_north,powered=true]
//set redstone_torch[lit=false]
//set redstone_wall_torch[facing=east,lit=true]
//replace redstone_torch[lit=false] redstone_torch[lit=true]
//set repeater[facing=north,delay=4]
//set oak_stairs[facing=west]
//set furnace[facing=south,lit=true]
//set oak_sign[rotation=8]
```

Tab completes supported properties and values. Rails support straight, ascending
and curved shapes; powered and detector rails cannot curve. Other supported
states include piston facing/extension, ladder and wall-sign facing, button and
pressure-plate power, redstone wire power, crop age and farmland moisture.
Only states available in Beta are accepted. `ID:metadata` remains available.

Patterns can mix states, such as `rail[shape=east_west],stone`. A replacement mask
with named states matches only the properties you specify. Edits retain undo/redo.
Normal game updates can later change power or rail connections.

The syntax follows [WorldEdit block states](https://worldedit.enginehub.org/en/latest/usage/general/patterns/).

## Calculator

`//calc` evaluates arithmetic without changing the world or requiring editing permission.
It supports `+`, `-`, `*`, `/`, `%`, `^`, parentheses, `pi`, `e` and common functions
such as `sqrt`, `abs`, `min`, `max`, `floor`, `ceil`, `round`, `sin`, `cos` and `tan`.
Angles use radians. For example, `//calc 64 * 27` returns `1728`.

`/fly` toggles flight without changing game mode. It requires `allow_flight` outside Creative and does not change Spectator flight. `/fly help` describes the permission and controls.
