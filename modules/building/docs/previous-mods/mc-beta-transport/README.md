# Beta Transport

BTA-style boats and faster minecarts for singleplayer Minecraft Beta 1.7.3 on
Babric. This is a personal port for Luke's existing StationAPI mod set.

Boats use forward/backward for thrust and left/right for steering. Looking around
does not steer the boat. Steering slows from 5 to 3 degrees per tick as speed
increases; reverse thrust brakes more strongly. The total horizontal speed cap is
0.8 blocks per tick, and boats survive collisions.

Minecarts take two movement steps each tick, allowing 16 blocks per second on
straight track while retaining the smaller movement steps needed for corners.
Riders can nudge a stationary cart using movement input. Furnace carts follow
their direction of travel after rail transitions and reversals.

The implementation targets the movement code in the official BTA 8.0.1 release.
The same main boat equations and two-step cart movement were checked in 7.3_04.

- [BTA 8.0.1 release files](https://downloads.betterthanadventure.net/bta-client/release/v8.0.1/)
- [BTA 7.3_04 release files](https://downloads.betterthanadventure.net/bta-client/release/v7.3_04/)

## Build

Use JDK 21 with the included Gradle wrapper:

```sh
./gradlew --no-daemon build
```

The installable JAR is `build/libs/beta-transport-1.0.0.jar`. The sources JAR and
the separate validation JAR are not gameplay mods to install alongside it.

The build uses Babric Loom 1.5-SNAPSHOT, Barn build 8 and the Babric loader development
dependency. The installed instance uses Fabric Loader 0.18.1. No StationAPI or
extra library is required by this mod itself.

## Scope and compatibility

The mod retains vanilla boat and minecart classes, IDs, inventory data and NBT
formats. It does not add blocks, items, recipes, world generation or saved custom
entity fields. Removing it restores vanilla movement for existing vehicles.

All movement changes run only in local worlds. Multiplayer is not supported.
BetaLAN is removed from the test instance; no other installed mod requires it.

The test instance disables UniTweaks' `oldfeatures.yml` setting
`minecartBoosters`. This mod controls the corresponding cart collision behaviour
to match BTA. Keep `boatElevators: false`. UniTweaks' boat durability, boat item
drops, dismount fix and item/minecart collision fix can remain enabled.

The test clone also sets EntityCulling's `disableBlockEntityCulling: true`.
Testing exposed a renderer `ConcurrentModificationException`: the culling worker
can populate the block-entity renderer map while the game thread iterates it
during a world switch. Disabling that setting removes those background lookups.
Normal entity culling remains enabled. No culling mod code was patched.

No background threads, tick queues, global entity caches or periodic file writes
are added. Each boat has one small reusable movement object. The additional cart
step runs synchronously with a bounded re-entry guard, preserving render
interpolation and the normal damage-animation timer.

## Tests

See [validation results](VALIDATION.md) for the completed game checks, measured
load and memory results, save/reload verification and remaining test limits.

`./gradlew test` runs motion properties, long control sequences, invalid-number
checks and 100,000 comparisons against the boat methods extracted from the BTA
release. `BtaBoatReference.java` is a test oracle, not code shipped in the mod.

`validation-mod/` builds a separate game-test mod:

```sh
./gradlew -p validation-mod --no-daemon build
```

Only run it in a disposable instance. It creates a world called
`Transport Laboratory` with a unique suffix, modifies its terrain into test courses and executes the
integration checks. It writes `transport-validation.log` in that instance. The
finished user-facing test instance must not contain this validation JAR.
