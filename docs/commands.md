# Chat and WorldEdit

Use **Up/Down** to browse command history. Once browsing starts, those keys stay
with history until you edit the text or use Tab. When typing a new command,
Up/Down select completion suggestions and **Tab** inserts the selected result.
**Ctrl+Up/Down** always browse history.

Chat supports clicking and dragging to select text, **Shift+arrows**, **Home/End**,
**Ctrl+A**, **Ctrl+C**, **Ctrl+X** and **Ctrl+V**. Ctrl+arrows move by words.
Text stays within Beta's 100-character chat limit.

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

`//calc` evaluates arithmetic without changing the world or requiring cheats.
It supports `+`, `-`, `*`, `/`, `%`, `^`, parentheses, `pi`, `e` and common functions
such as `sqrt`, `abs`, `min`, `max`, `floor`, `ceil`, `round`, `sin`, `cos` and `tan`.
Angles use radians. For example, `//calc 64 * 27` returns `1728`.
