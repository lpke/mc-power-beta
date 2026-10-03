# Beta Fake Sneak

Full-speed edge protection for singleplayer Minecraft Beta 1.7.3 on Babric.

Open **Mods > Beta Fake Sneak > Configure > Hotkeys**, or **Options > Controls**,
and bind **Fake sneak (toggle)**. Press once to enable and again to disable.
The mod starts off and its key starts unbound. The optional chat message reports
each toggle. The enabled state is saved between launches.

The mod clips movement at unsupported edges without setting the sneak flag,
lowering the camera or reducing normal walking input. It includes diagonal
corner protection missing from Beta's native sneak routine and uses block
collision shapes for slabs, stairs and other partial blocks.

It affects grounded local-player movement. Jumping, airborne movement, ladders,
creative flight, riding, noclip, Freecam and multiplayer keep their native
behaviour. This is edge protection while walking, not fall-damage immunity.

**Done** saves settings; **Cancel** or Escape discards edits. Configuration lives
in `config/beta-fake-sneak.properties`; the key uses native `options.txt`.
No world data is added. Removing the JAR needs no conversion.

The implementation follows Tweakeroo's separation of edge backoff from actual
sneaking. See [research](RESEARCH.md) and [validation](VALIDATION.md).

## Build

Use JDK 21:

```sh
./gradlew --no-daemon build
```

Install `build/libs/beta-fake-sneak-1.0.0.jar`. Runtime requires Java 17+ and
MixinExtras 0.5.0+, already included in this instance's Fabric Loader 0.18.1.
Mod Menu is optional. No other custom mod is required.
