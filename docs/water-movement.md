# Modern water movement

The Gameplay setting affects the local singleplayer player. It has no world-rule
permission requirement. Power Beta defaults enable it; Refined Beta and Beta
defaults disable it. Changing the reset base never changes an applied value.

The reference is the unenchanted player in Java Edition 1.21.1. The cached Mojang
[client](https://piston-data.mojang.com/v1/objects/30c73b1c5da787909b2f73340419fdf13b9def88/client.jar)
and [official mappings](https://piston-data.mojang.com/v1/objects/2244b6f072256667bcd9a73df124d6c58de77992/client.txt)
were verified against the version manifest's SHA-1 hashes and the relevant classes
were decompiled. Movement follows `Entity.updateSwimming`,
`Entity.updateFluidHeightAndDoFluidPushing`, `LivingEntity.travel`,
`LivingEntity.getFluidFallingAdjustedMovement`, `Player.travel`,
`Player.updatePlayerPose` and `LocalPlayer.aiStep`. Rendering follows
`LivingEntity.updateSwimAmount`, `HumanoidModel.setupAnim` and
`PlayerRenderer.setupRotations` and `Camera.tick`.

Normal water movement retains 0.02 input acceleration and 0.8 horizontal drag.
Vertical drag remains 0.8. Water gravity is 0.005 per tick, compared with Beta's
0.02. Sneak adds 0.04 downward velocity per tick. Currents average contacted
water cells without normalizing the player's combined flow. Shallow contact
scales the current. The push factor is 0.014 and the minimum nonzero push from
rest is 0.0045. Uniform deep currents can retain their original strength;
shallow and mixed currents can be easier to resist.
Before input and jumping, water velocity components smaller than 0.003 settle to
zero. Grounded water contact no deeper than 0.4 blocks uses the normal 0.42 jump,
with a ten-tick repeat delay while Jump is held. Releasing Jump clears that delay.
Deeper water retains the 0.04 rise. Fluid depth comes
from the same bounded current scan.

With the per-world `allow_sprinting` rule enabled, the ordinary sprint control also
starts swimming while the player's eyes are underwater. Both the global Modern
water movement preference and the world rule are required. Disabling either leaves
the corresponding ordinary water movement available. Sprinting uses the existing
activation, stamina and combat controls. Ground speed tuning does not multiply the
water acceleration.

Sprint swimming retains 0.02 input acceleration and 0.8 vertical drag, changes
horizontal drag to 0.9 and skips water gravity. Before travel, vertical velocity
approaches the look direction's Y component by 0.085 when looking down past 0.2,
or 0.06 otherwise. Looking up at an empty surface does not pull the player out of
the water unless Jump is held. Jump and descend retain their 0.04 impulses;
descending does not apply Beta's horizontal sneak slowdown. Horizontal collisions
do not cancel swimming. Stopping forward input, exhaustion, menus, lost focus,
revoked access and incompatible movement modes cancel its active state.

The swimming collision box is 0.6 by 0.6 blocks, with eyes 0.4 blocks above the
feet. Native collision allows one-block tunnels. On exit, the player stands if
there is room, crouches at 1.5 blocks with 1.27-block eyes if necessary, or keeps
the compact pose until space opens. A retained low pose grants no sprint
permission. Dry crawling uses the crouched input factor. Safe low poses survive
save/reload with a normalized native position and per-player pose metadata.
Death first restores the native coordinate origin while preserving physical feet,
then leaves body shrink, drops and kept inventories to native death handling.
Dead-player saves and loads clear the low-pose state.
Pose checks include the native collision query's loaded-neighbor margin.
The rendered eye approaches the physical eye height halfway each tick and
interpolates between those values each frame. This keeps the modern camera
transition separate from collision and coordinate reporting. Bobbing, third person,
water rendering and freecam entry use the resulting native camera matrix.

The player and both armor models blend the official 26-unit arm stroke, opposing
leg motion, head angle and body rotation over the native limb cycle. Swim blend
changes by 0.09 per tick. Renderer state clears after each player draw, including
before first-person hands and other bipeds. Native entry splashes, bubbles and
drowning effects remain. The reference client has no continuous sprint-swim bubble
trail; underwater sprinting never emits terrain dust.

Beta has no swimming enchantments, swimming status effects or modern fluid types.
Native travel still owns collision response, stepping out of water, fall-distance
handling and limb interpolation. Native water blocks still supply flow vectors.
The scan checks loaded terrain, including the neighbor margin needed by those
flow calculations, and visits at most 64 cells. It never loads chunks.

Flight, Spectator, freecam, mounted players, other entities and multiplayer retain their
existing physics. No new binding or input priority is added.

The `water-253` validation fixture checks ordinary displacement, currents,
collisions, fluids and modes. `swimming-300` compares native full-tick travel with
independent 1.21.1 equations across all four setting/rule combinations, pitch and
vertical controls. It checks real one-block collision, forced crouch, revocation,
reload, compact deaths and kept snapshots, current/mode ownership, surface transitions, player/armor model rendering,
native particles, matrix-derived eye transitions and the loaded-terrain boundary. It also checks land sprint
acceleration, jump timing and the modern eight-degree glancing-collision threshold.
Run both only in the muted disposable test clone.
