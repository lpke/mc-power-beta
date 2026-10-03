# Beta Auto Walk

A client mod for Minecraft Beta 1.7.3 with Babric. Press **R** to toggle normal
forward walking. Rebind **Auto-walk (toggle)** in Options > Controls.

- Holding the toggle key triggers it only once.
- Opening a screen, losing window focus, or changing world/player stops walking.
- Jumping, strafing and sneaking retain vanilla behavior. Holding W does not
  double movement speed. Pressing the bound forward or backward key cancels
  auto-walk; releasing it does not restart auto-walk.
- Auto-walk starts off each session and does not change worlds or server settings.

## Free look

The companion is the local [Omnilook source build](../omnilook/LOCAL-BUILD.md).
Hold **grave/backtick (`)** to orbit the third-person camera without turning the
player; release it to return to the previous view. Rebind **Free look** in Controls.
Its `config/omnilook.properties` contains `toggleMode=false` for hold mode.

As of 1.0.2, this mod contains no Omnilook compatibility code or translations.
Omnilook owns its in-game settings screen and Controls label. Auto-walk works
independently, without Omnilook or StationAPI.

## Build

Requires a JDK, Java 17 or newer, plus internet access on the first build.
The installed Minecraft set uses Java 21 for its other mods.

```sh
./gradlew --no-daemon build
```

The Gradle 8.6 wrapper downloads Gradle and checks its SHA-256. Babric Loom 1.5
resolves the Minecraft development JARs, Barn mappings `b1.7.3+build.8`, and Babric
Loader `0.15.6-babric.2`. The target instance runs Fabric Loader 0.18.1. Compilation
targets Java 17. JUnit 5.10.2 tests cover toggling, held-key debouncing, screen/focus
cancellation, world reset and movement speed. Build output:

- `build/libs/beta-autowalk-1.0.3.jar`: remapped installable mod.
- `build/libs/beta-autowalk-1.0.3-sources.jar`: source archive.

Copy only the first JAR into an instance's `.minecraft/mods/`. Replacing this mod
requires closing that instance first. There are no extra runtime dependencies.

## Implementation

`WalkToggle` contains input state and movement calculation. `AutoWalk` reads the
rebindable key and checks gameplay/focus. Three Mixins add the key before options
load, tick/reset its state, and add forward input after vanilla keyboard input.
Barn build 8 leaves keyboard-input classes and fields unnamed; those intermediary
names are documented in `KeyboardInputMixin`.

The Gradle setup and wrapper originate from the official
[Babric example](https://github.com/babric/babric-example-mod/tree/b1.7.3).
This repository contains no Minecraft binaries, account files or world saves.
