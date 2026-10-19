# Modern water movement

The Gameplay setting affects the local singleplayer player. It has no world-rule
permission requirement. Power Beta defaults enable it; Refined Beta and Beta
defaults disable it. Changing the reset base never changes an applied value.

The reference is the unenchanted player in Java Edition 1.21.9. Mojang's
[client](https://piston-data.mojang.com/v1/objects/ce92fd8d1b2460c41ceda07ae7b3fe863a80d045/client.jar)
and [official mappings](https://piston-data.mojang.com/v1/objects/3641ccb54eac2153c7e8274823c5a8e046beaba0/client.txt)
were verified against their SHA-1 hashes in the version manifest. The relevant
methods are `Entity.updateFluidHeightAndDoFluidPushing`,
`LivingEntity.travelInFluid`, `LivingEntity.getFluidFallingAdjustedMovement`
and their callers.

Normal water movement retains 0.02 input acceleration and 0.8 horizontal drag.
Vertical drag remains 0.8. Water gravity is 0.005 per tick, compared with Beta's
0.02. Sneak adds 0.04 downward velocity per tick. Currents average contacted
water cells without normalizing the player's combined flow. Shallow contact
scales the current. The push factor is 0.014 and the minimum nonzero push from
rest is 0.0045. Uniform deep currents can retain their original strength;
shallow and mixed currents can be easier to resist.

Beta keeps its upright player model, eye height and collision size. This setting
does not add sprint-swimming, look-directed swimming, crawling, enchantments,
status effects, land sprinting or new fluids.
Native travel still owns collision response, stepping out of water, fall-distance
handling and limb interpolation. Native water blocks still supply flow vectors.
The scan checks loaded terrain, including the neighbor margin needed by those
flow calculations, and visits at most 64 cells. It never loads chunks.

Flight, Spectator, mounted players, other entities and multiplayer retain their
existing physics. No new binding or input priority is added.

The `water-253` validation fixture checks displacement and velocity in a disposable
world, including currents, full ticks, collisions, fluids, modes and live changes.
Keep its reference calculations independent of the implementation helpers.
