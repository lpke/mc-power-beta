# Development validation utility

Use only in a disposable Prism clone. This JAR creates laboratory worlds, changes
blocks, inventory, settings and key bindings, and writes `building-validation.log`.
It is never included in the gameplay release.

Build LpkeTweaks and the sibling `mc-worldedit-beta` project first, then run:

```sh
./gradlew -p validation-mod --no-daemon build
```

Install both gameplay JARs and this validation JAR alongside the tested mod pack.
The 72 integration checks run automatically, including native save/reload.
Write one command into `.minecraft/building-validation.command`:

- `config-flex`, `config-fake`, `config-slab`: open LpkeTweaks settings.
- `config-we`: open WorldEdit Beta settings.
- `we //command`: execute a WorldEdit command through its runtime entry point.
- `extended`: rerun the 20 combined slab, WorldEdit, mining and camera checks.
- `status`: record settings, input state and player position.
- `showcase`: reset the platform and inventory.
- `soak`: run 2,400 ticks of native piston placement and movement checks.
- `large-edit`: fill a 65,536-block air region, then undo and compare its original hash.
- `slab-input CONTINUOUS|DOUBLE|MATCH_FIRST`: prepare a reachable target for physical right-click input.
- `slab-result`: verify the slab layers after holding right-click.
- `quit`: save and request normal shutdown.
- `features`: run 17 checks covering creative/spectator, reach, access, navigation,
  hotbar NBT safety, nested item-use failures and camera defaults. Requires the
  sibling LpkeCreative gameplay JAR.
- `config-creative`: open LpkeCreative settings.
- `mode CREATIVE|SURVIVAL|SPECTATOR`, `feature-state`: prepare and inspect mode input.
- `flight-input 0|5`: prepare airborne physical flight checks at the chosen glide.
- `hotbar-input`, `hotbar-mouse`, `hotbar-state`: prepare row swaps and inspect
  counts, metadata and the selected slot. These commands replace test inventory.

The physical input test needs game-window focus and a real mouse-button event.
Use an isolated display for automation. Restore the user's configuration and key
bindings after testing and remove this JAR before normal gameplay.

Revision checks (`revision`) cover slab restrictions/completion, native chat,
entity removal and free look. `camera FIRST_PERSON` / `camera THIRD_PERSON`
prepare manual input checks; `revision-state` records camera and chat state.
Without BHCreative, the utility runs a separate minimal-pack smoke test for
vanilla rendering and chat without optional dependencies. Test worlds and
settings are disposable. Never install this utility in the main instance.
