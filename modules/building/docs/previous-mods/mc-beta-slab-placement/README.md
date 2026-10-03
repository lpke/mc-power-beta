# Beta Slab Placement

Complete matching half slabs from adjacent block faces in singleplayer Minecraft
Beta 1.7.3 on Babric.

Hold the same slab variant and right-click an adjacent block face whose ordinary
placement destination is the existing half slab. It becomes a double slab. Direct
top-face completion works too. All four Beta slab variants are supported.
Different variants never merge. Occupying the resulting full block's space
prevents completion. Survival consumes one slab; BHCreative preserves the stack.

The mod starts enabled. Open **Mods > Beta Slab Placement > Configure** to change
it, or bind **Slab completion (toggle)** in that screen's Hotkeys page or native
Controls. The key starts unbound; toggle messages can be disabled. Done saves,
Cancel or Escape discards edits.

Configuration is in `config/beta-slab-placement.properties`; the binding uses
native `options.txt`. Multiplayer and Freecam are excluded. Beta has no upper
half slabs, so this mod adds no upper-slab block state or new block IDs.

UniTweaks Tels Addons' slab and plant fixes remain enabled and unchanged. The mod
wraps item use, preserving BHCreative's stack handling. Beta Fast Place 1.1.0
recognises these merges and has a separate Keep half-slab layer option for held
placement. Neither custom mod requires the other.

See [validation](VALIDATION.md). The config and key-binding pattern comes from
[Beta Fast Place](https://github.com/lpke/mc-beta-fastplace). The slab merge is an
independent Beta implementation, not a modern block-state backport.

## Build

Use JDK 21:

```sh
./gradlew --no-daemon build
```

Install `build/libs/beta-slab-placement-1.0.0.jar`. Java 17+ is required at runtime;
Mod Menu is optional. Removing the JAR needs no world conversion because double
slabs are native Beta blocks.
