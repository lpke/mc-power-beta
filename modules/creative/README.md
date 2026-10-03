# LpkeCreative

Creative and spectator modes for singleplayer Minecraft Beta 1.7.3 on Babric.
Forked from BHCreative 0.4.10. Install `lpkecreative-1.1.0.jar` in place of BHCreative.
Existing creative tabs, item IDs, API interfaces and saved creative flags remain compatible.
Do not install both mods together.

## Controls

- Double-tap Jump to toggle creative flight. Jump rises; Sneak descends.
- Press Left Ctrl to toggle sprint while flying. Default horizontal speed doubles.
  Choose Hold activation in settings if preferred.
- Hold F3 and press F4 to open the gamemode picker. Further F4 presses cycle;
  releasing F3 selects. Moving the mouse selects a hovered mode. Escape cancels.
  The initial selection is the previous mode. The debug HUD keeps its prior state.
- Spectators fly through blocks. Scroll up/down changes speed by 0.005 per notch, matching modern Minecraft.
- `/gamemode survival`, `/gamemode creative`, `/gamemode spectator`; `/gm` is an alias.
  Numeric aliases are 0, 1 and 3. The optional target supports your name, @s, @p or @a.
  RetroCommands supplies colouring, completion and help in the tested pack.

## Settings

Open **Mods > LpkeCreative > Configure**. Left/right arrows group flight, sprint,
gamemodes, spectator and creative interaction. Done saves; Cancel/Escape discards.
Sprint, picker and modifier keys also appear in native Controls. Mouse bindings work.
Settings use `config/lpkecreative.properties`; key assignments stay in `options.txt`.

| Setting | Default | Range or behaviour |
| --- | --- | --- |
| Creative flight | On | Double-tap Jump; independently disableable |
| Flight speed | 100% | 25–400% |
| Flight glide | 5 | 0 stops immediately; 1–4 scale residual velocity by 20–80%; 5 uses modern drag |
| Landing stops flight | On | Disable to remain in flight after landing |
| Double-tap window | 7 ticks | 2–20 ticks |
| Flight sprint | On | Default Left Ctrl |
| Sprint activation | Toggle | Toggle or Hold; resets on flight/mode/focus changes |
| Sprint multiplier | 200% | 100–400% horizontal acceleration |
| Gamemode picker | On | Default F3 modifier and F4 cycle |
| Starting spectator speed | 100% | 0–400%; wheel speed is bounded to 0–400% |
| Scroll speed step | 10% of base speed | 1–100%; 10% means the modern 0.005 increment |
| Spectator scroll and speed display | On | Independently configurable |
| Creative block and entity reach | 5 blocks each | Independently adjustable from 3–10 blocks |
| Destroy item slot | On | Left of the creative inventory hotbar |
| Shift-click clears inventory | On | Clears main inventory, armour, crafting grid and cursor |

Glide applies separately to horizontal and vertical input. Releasing horizontal
movement stops horizontal drift at 0 even while rising. Normal flight uses modern
1.21.4 acceleration and drag: 0.05 speed, 0.98 input factor, 0.91 horizontal drag,
0.15 vertical acceleration and 0.6 vertical drag. Beta collision and world height
remain native. Sprint boosts horizontal movement in any direction while active.

Creative block reach changes use StationAPI's reach event, preserving custom item
reach providers. Entity targeting respects intervening blocks even when configured
entity reach exceeds block reach. Survival retains Beta's ordinary reach.

## Spectator and saves

Spectator disables damage, block/item/entity interactions, inventory opening,
dropping, pickups, pushing, the player model, held items and normal hotbar.
Leaving inside solid blocks returns to a clear entry position or a nearby clear
position in the same column. If no safe exit exists, the mode stays spectator.
Gamemode, previous mode, flight and spectator speed save with the player. Native
respawn preserves gamemode. Freecam, menus and focus loss suspend movement input.

This is a local singleplayer implementation. It does not add multiplayer protocols,
modern adventure mode, spectator entity cameras or a spectator teleport menu.
The existing BHCreative inventory, tabs and world-creation mode selector remain.

## Build

Use JDK 21 and `bash ./gradlew --no-daemon build`. Output targets Java 17.
StationAPI is required; Mod Menu and RetroCommands integrate when present.
The tested loader supplies MixinExtras 0.5.0. No other installed mod JAR needs updating.

The build uses Gradle 8.14.3, Fabric Loom 1.11.8 and Babric Loom Extension 1.11.9
so it can compile against the installed Mod Menu API. The original BHCreative
snapshot is the first commit. See [provenance](PROVENANCE.md),
[upstream API notes](docs/BHCREATIVE-UPSTREAM.md) and [validation](VALIDATION.md).

Latest fixes: [validation report](VALIDATION-1.1.0.md). Left-click option
values to increase or cycle forward; right-click to decrease or cycle backward.
