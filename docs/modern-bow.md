# Modern bow handling

`Gameplay > Equipment > Modern bow handling` starts off in all Defaults bases.
The retired bow-durability field remains historical configuration data and has no
runtime consumer. The new preference applies only to local singleplayer bows.
Multiplayer keeps the server's Beta item use and projectile authority.

## Official reference

The reference is Mojang Java 26.3, pinned by its
[version metadata](https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json),
[client JAR](https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar)
and [resource index](https://piston-meta.mojang.com/v1/packages/0c8f43b28f837869c1e54a6aeb22c396a1548946/34.json).
The client SHA-1 is `e877b6a07acd633fb3bb475002175cec036e7b87`.
Checked methods include `BowItem.use`, `releaseUsing`, `getPowerForTime`,
`ProjectileWeaponItem.shoot`, `Projectile.getMovementToShoot`, `shootFromRotation`,
`AbstractArrow.tick`, `onHitEntity`, `HumanoidModel` bow poses and
`FirstPersonHandsAndItemsRenderer` bow transforms and `CritParticle`/`Particle`/
`SingleQuadParticle` critical trail behavior. `assets/minecraft/items/bow.json`
defines the pull-frame thresholds. Bow release and break use Players volume; arrow impacts use Neutral, mapped to
Friendly creatures. Individual-sound previews use the same categories. Official
sounds and their variants are pinned
in `assets/powerbeta/extra-sounds.json`.

For a draw of t game ticks, power is min(1, ((t/20)^2+2t/20)/3).
Draws below 0.1 power do not fire, so three ticks is the first firing duration.
Full draw takes 20 ticks. Launch speed is three times power, with triangular
uncertainty 0.0172275 per axis and the shooter's horizontal velocity, plus vertical
velocity when airborne. The normal bow holds for at most 72000 ticks.
Pull textures change at ticks 13 and 18. First-person stretch, shake, mirrored
hand transforms, movement slowdown and the 15% full-draw field-of-view reduction
follow the official formulas. Third-person poses include both arms and native bob.

Air inertia is the exact float 0.99 promoted to double. Water inertia is the exact
float 0.6 promoted to double and applies before collision damage. The current tick's
movement uses the velocity captured before that water multiplication, as in the
reference client. Gravity subtracts 0.05 each airborne tick. Damage is
ceil(float(length(velocity))*2), with full draw adding a uniform integer from zero
through floor(damage/2)+1. A full-speed unenchanted critical hit therefore deals
6 through 10 health points before the target's normal damage handling.
Bows break on the 384th successful shot. Canceled and undercharged uses consume
nothing. Creative needs no ammunition and does not wear its bow; its arrows do
not become survival pickup ammunition.

## Integration and limits

Main-hand use keeps its native block/entity interaction priority. Once a bow starts
drawing, it owns that use and prevents an offhand fallback. Ammunition lookup prefers
the offhand, then main hand, then ordinary inventory storage. Either hand can draw.
The exact hand, selected slot, inventory owner and manual-swap revision remain
bound to the draw. Menus, focus loss, a replaced stack, death, detached cameras,
world changes and disabling cancel the draw. Rendering reads tick-owned state.

Successful survival releases use the shared inventory plan. Complete native bow
and arrow data are copied on the game thread, the immutable recovery journal
flushes on its worker, and publication validates every protected inventory slot
before and after callbacks. An invalidated plan fires nothing. A failed owned
world spawn rolls back the input changes and removes its arrow. This can defer a
shot until the next game tick while the journal finishes; the release position,
rotation and inherited velocity remain the ones captured on release.

Beta block rays mutate their starting vector, so the adapter rebuilds both ray
endpoints before scanning entity intersections. Block impacts back off 0.05 per moving axis
and zero embedded velocity, matching 26.3. Damage probes stay below the
native 128-block entity-query limit and lie before the final crossed ray cell.

Beta remains responsible for target health, armor, ordinary damage invulnerability,
block collision shapes, world height and supported entities. Its single-source
damage API receives the shooter for native loot and retaliation attribution.
Modern enchantments, shields, tipped/spectral arrows, bubble columns, piercing,
modern entity deflection and modern combat armor rules do not exist in Beta and
are outside this bow preference. Beta's fixed-function renderer uses the official
pull sprites and transforms on its native player model, without modern renderer
view-lag fields. Skeleton and dispenser arrows remain native. Marked player arrows
retain their mode and critical flag in native NBT. Switching the preference off
returns their simulation to Beta while retaining that data for a later re-enable.

The full-draw bow intentionally fills much of its side of the first-person view.
An independent projection of the official opaque sprite, generated-model geometry,
draw/display transforms and 70-degree hand field of view gives normalized main-hand
bounds x 0.6932 through 1.2998 and y -0.0180 through 1.3974 at aspect ratio 1.8.
The top, bottom and right are therefore clipped. The native 1800x1000 capture starts
at approximately x1250, matching the projected x1248 edge. Repeating the full
projection with the official left-hand display and draw transforms gives offhand
bounds x -0.2998 through 0.3068 and the same y bounds. Its expected right edge is
x552, consistent with the approximately x549 edge in the native capture.
These bounds check the complete transform chain, including centering and scale.

The disabled paths return before drawing input polls, ammunition lookup, trajectory
math, texture preparation or pose calculation. Native immediate Beta firing and
its original sound/durability behavior remain active. Saved bow damage stays intact
but hidden, so turning the preference off cannot destroy a worn bow.

## Validation

Run root unit tests and the full pack build sequentially. In a focused, muted,
verified disposable singleplayer clone, run `modern-bow-470`. The fixture restores
configuration, inventory and owned terrain/entities. It covers both hands at zero,
two, three, ten, twenty and forty ticks; full native item preservation before
release; one-shot release; disabled Beta behavior; stack/toggle cancellation;
384-use break; Creative; stale journal publication; 25-tick flight against a
closed-form reference; water ordering; block containment/collision; speed damage
and critical damage bounds. The existing `offhand-actions-400 native` remains a
regression check for the default disabled mode. Decoder validation includes every
registered bow sound variant.

Check normal frames manually in both perspectives with the bow in either hand:
all three pull frames, full-draw shake, left/right aim, crouch, armor, movement,
release, cancellation, bow break and continuous Hold use. Listen only through the
verified silent capture route and compare the recorded release/impact/break event
timing with the official client. No sound plays merely for starting or holding a draw.

Texture SHA-1 pins from the official client:

| Source in `textures/bow` | SHA-1 |
| --- | --- |
| `pulling_0.png` | `88f43d74eba59eddf54c3c889167ded99d95d34d` |
| `pulling_1.png` | `b2e395b5059fc06afae2424559148db2acc089cd` |
| `pulling_2.png` | `095c7e23b9ff2b5915ba09250d6158801c30c15f` |
| `crit.png` | `b71adbdb66260b1d7ae2c979f0663e67f9ba2b9f` |

`crit.png` is `assets/minecraft/textures/particle/critical_hit.png` in the
reference client. Its SHA-256 is
`4238f9aa94da3ae811f06614f82ef2863dc77191f6bb4f1a0029480e8eac9300`.
