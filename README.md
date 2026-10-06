# Power Beta

Minecraft Beta 1.7.3 with one searchable Options menu for building tools, creative
and spectator modes, WorldEdit, inventory controls, camera tweaks, textures and
music. Most gameplay changes start off. Fixes, performance improvements and small
quality-of-life features start on.

Built for singleplayer. Each world has a **Cheats enabled** switch under
**General > Game**, with further controls for commands and game modes.

## Install

1. Import `Power-Beta-1.0.0-Prism.zip` through Prism Launcher's **Add Instance >
   Import**.
2. Select **Java 21** for the instance, then launch.
3. Open **Options** to customize it. Hover settings for explanations.

For an existing installation, use a **Beta 1.7.3 Babric** instance. Copy **all seven
JARs** from the export's `.minecraft/mods` directory into its `.minecraft/mods`
directory. Power Beta contains its utility modules, but is **not a standalone
JAR**. The tested external dependencies are:

- StationAPI 2.0.0-alpha.6.4
- Glass Networking 1.0.7
- RetroAuth 1.3.1+mcb1.7.3
- SmoothBeta 1.1.8
- EntityCulling 1.7.0-candidate.1
- stapi-fast-intro 2.0.0

The export pins the launcher components and these dependencies. Use its versions;
do not add separate copies of the utility mods integrated into Power Beta, or Mod
Menu. Back up an existing instance before replacing its mods. Installing or
updating the JARs does not replace worlds or preferences. Extra mods are untested.

## Settings and music

Settings and key bindings live in `config/power-beta.json`. Options remembers your
page, search and scroll during the session. Apply saves without closing; Cancel
undoes unsaved edits, including previews. Right-click Apply to enable auto-apply.
Right-click a slider to enter a precise value. Bindings support Ctrl, Shift and Alt.

**Interface > Active tweaks** shows temporary toggles such as fake sneak and fast
placement on the HUD. Choose which appear, their position, colour and opacity.

Audio includes bundled Minecraft soundtracks, custom folders, playlists, presets,
per-track volumes and a queue. Presets capture their available tracks when saved:
adding folders later does not silently expand them. Include new tracks explicitly
or use **Include all**.

Menu music defaults to **Mix both**. This and **World soundtrack** keep the current
song playing when entering or leaving a world. **Menu music controls** can show
playback controls on the pause and title menus, with an optional scrub bar.

OGG, WAV and MUS play directly. Optional **ffmpeg**, available on PATH when the game
starts, enables **Convert MP3**. Converted WAVs go into
`power-beta-data/music-cache`; original files stay untouched.

## Source

Build the importable pack with Java 21, Python 3 and `JAVA_HOME` set:

```sh
python3 tools/build_pack.py
```

The output is `dist/Power-Beta-1.0.0-Prism.zip`. See [AGENTS.md](AGENTS.md) for
development and update instructions, [upstreams](docs/upstreams.md) for dependencies
and references, and [recovery](docs/recovery.md) for inventory journals.

Source code uses [AGPL-3.0-only](LICENSE). Minecraft music, textures and separately
licensed dependencies retain their own terms. This is not an official Minecraft
product.
