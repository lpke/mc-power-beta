# Water sounds

Audio > Extra sounds has independent controls for body splashes, swimming,
submerging, surfacing, underwater ambience and three groups of occasional
underwater effects. Power Beta defaults enable splash, swimming, head transitions
and bubbles/water additions. Underwater ambience, rare and very rare additions
default off in every base. The other bases keep all these additions off.
Existing explicit choices stay unchanged.

The reference is Mojang Java 26.3, pinned by its
[version metadata](https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json),
[client JAR](https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar)
and [resource index](https://piston-meta.mojang.com/v1/packages/0c8f43b28f837869c1e54a6aeb22c396a1548946/34.json).
The client bytes match SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`.
The checked methods are `Entity.updateFluidInteraction`, `Entity.doWaterSplashEffect`,
`EntityFluidInteraction.update`, `Player.doWaterSplashEffect`, `Monster` splash
overrides and `AbstractBoat.modifyPassengerFluidInteractionBox`.
`assets/powerbeta/extra-sounds.json` pins its metadata and every asset hash.
`python3 tools/audit_extra_sounds.py` checks those bytes against Mojang.

| Effect | When it plays | Modern sound gain before user volume controls |
| --- | --- | --- |
| Body splash | An entity's body first touches water, excluding its first tick. Remaining in water does not repeat it. | Weighted movement speed multiplied by 0.2, capped at 1. A controlled boat uses its player's motion multiplied by 0.9. Players select the heavy splash at 0.25 or higher. |
| Swimming | Actual horizontal movement crosses the next stride threshold while in water. Climbing also counts vertical movement. Walking on the bottom, riding, flight and grounded sneaking retain their native exclusions. | Weighted movement speed multiplied by 0.35, capped at 1. |
| Submerging | The player's physical eyes enter water. | 0.5, pitch 1. |
| Surfacing | The player's physical eyes leave water. | 0.3, pitch 1. |
| Underwater ambience | Starts on submersion, fades in over 40 ticks and fades out over 20 ticks after surfacing. | 0.65 at full fade. |
| Bubbles and water | An exclusive random result from 0.001 inclusive to 0.01 exclusive each underwater tick. | 1. |
| Rare underwater sounds | A result from 0.0001 inclusive to 0.001 exclusive. | The official variant gain, between 0.45 and 1. |
| Very rare underwater sounds | A result below 0.0001. | The official variant gain, between 0.7 and 1. |

Weighted speed is `sqrt(0.2 * vx² + vy² + 0.2 * vz²)`. Body splash and swim
pitch is `1 + (random1 - random2) * 0.4`. Stride distance uses modern's float
rounding and skips missed integer thresholds after large movements. Quiet
movement modes preserve the previous stride instead of restarting it.

Head sounds use the player's physical eye height and water level. Changing
perspective or moving freecam does not create a crossing. Source water under a
solid ceiling and water stacked above water use the full column height.
Head transitions use the preceding native fluid snapshot, retaining modern's
tick order instead of firing from render-frame camera interpolation.
Rapid re-entry resumes the existing fading ambience stream. It avoids stacking
duplicate loops, increasing the volume and taking channels away from music.
Occasional sounds stop immediately on surfacing. Disabling one probability
group does not make the remaining groups more frequent. These effects have no
biome, depth, roof, weather or movement requirement beyond submerged eyes.

Spectator updates its underwater state but skips entry/exit cues and new ambience
loops, matching modern. Modern's occasional underwater handler still admits
Spectator, and a loop already playing before a mode change may finish normally.
Creative admits head sounds independently of its flight ability. Pausing freezes
the water audio timers. Removing the player, leaving the world, disabling an
effect or muting its category releases the corresponding owned sources.

These additions remain local to singleplayer. They do not add bubble-column
sounds because Beta has no bubble-column blocks. Native flowing-water sounds
remain native. Body contact for modern splash/swim audio is a separate loaded-only
read and never changes Beta's water contact, currents, damage or particles.
It covers players, all native creatures, dropped items, projectiles and vehicles.
A boat above water shields the part of its passenger below the boat top; a
submerged boat stops shielding. Empty boats and minecarts use their own motion.
Generic and hostile splashes use the official two normal variants, even at high
speed. Each event keeps its entity category, including after live volume edits.
Sampling covers loaded boxes up to 16 blocks on each axis, including native giants.
Larger custom entities retain no additional sound rather than causing an unbounded scan.
Added audio uses separate random generators so native particles, damage and
movement retain their random sequence.

The long ambience file streams on the sound threads. Sound creation and decoding
never run in rendering. With all water additions off, they do no terrain sampling
or sound work. Master, Game sounds, Ambient/Players and individual sound volume
controls continue to apply. Previews use the same official variant gains.

## Buckets and lava

Water bucket sounds and lava bucket sounds have separate controls and previews.
The stable `bucketSounds` preference now controls water and cow milking.
Filling plays at the player with Players volume; emptying plays at the target
block with Blocks volume. Cow milking uses Passive creatures volume.
A missing `lavaBucketSounds` copies the previous explicit bucket choice once
through the verified settings backup; existing explicit split choices remain intact.

The lava audit checked `LavaFluid.animateTick`, `LiquidBlock.fizz` and
`LevelEventHandler` event 1501 in the same verified client.

| Effect | Conditions and gain | Category |
| --- | --- | --- |
| Pop | Native 1/100 exposed-surface particle event, gain 0.2 to 0.4, pitch 0.9 to 1.05. | Ambient |
| Lava ambience | Independent 1/200 exposed-surface display event, same gain and pitch range. | Ambient |
| Cooling/extinguishing | Native water/lava contact, unchanged `random.fizz`, gain 0.5 and pitch `2.6 + (random1 - random2) * 0.8`. | Blocks |

Lava ambience has its own toggle, enabled in the Power Beta base and disabled
in the other bases. Missing preferences initialize from the selected base after
a verified backup. The pop category matches modern Ambient.
Native lava damage, item bounce sounds and their timing remain unchanged.

Cooling already has matching native behavior and bytes, so it needs no extra asset
or toggle. Modern lava adds no general lava entry, swimming or exit sound.
Bubble-column effects and modern-only creatures have no Beta counterpart.
Native Beta fluid physics, damage cadence and boat geometry remain unchanged;
the audio follows their native event times. These are the remaining differences
from running a modern client.

## Player fire extinguish

Audio > Extra sounds > Player fire extinguish adds the missing player water
extinguish cue. It defaults on only in Power Beta. Missing preferences initialize
once from the selected base with a verified backup. Explicit choices stay intact.
The stable setting ID is `audio.playerFireExtinguish`.

The same pinned 26.3 client was checked at `WaterFluid.entityInside`,
`InsideBlockEffectType.EXTINGUISH`, `Entity.applyEffectsFromBlocks`,
`Entity.playEntityOnFireExtinguishedSound` and `Entity.isOnFire`.
Water queues the extinguish effect during movement. The native block-effects
pass plays once when a burning entity stops burning. The server owns the sound.
The event is `entity.generic.extinguish_fire`, uses the entity's sound category,
and plays at gain 0.7 with pitch `1.6 + (random1 - random2) * 0.4`.
Its sole variant is the official `random/fizz.ogg`, SHA-1
`bd76df5c882dc4b65925a8b25c0e7dbebef9bb2e`, unchanged at 15,222 bytes,
59,919 mono frames at 48 kHz. It has no variant gain, pitch, weight or streaming
overrides. The subtitle is "Fire extinguishes".

Beta instead clears fire silently in its native water-contact branch in
`Entity.baseTick`, after splash particles. The addition plays at that fire-clear
point when a non-immune player has a positive fire timer. It does not require
submerged eyes, a new body crossing or a minimum fire duration. It does not repeat
while remaining extinguished. Enabling or disabling modern water movement keeps
that setting's native water contact and extinguish timing. The sound does not
change fire, damage, fluids, particles or native random sequences.

If Beta's existing movement fizz extinguishes an already-wet player first, the
addition reuses that one event with its native gain and pitch. Dry rain fizz,
creatures, item/lava fizz, fire expiry and multiplayer remain native. Modern also
supports other causes, including powder snow, which Beta does not have. This
player-only option adds no effects for those cases.

The bundled `power_beta:entity.generic.extinguish_fire` event uses Players volume,
including individual sound volume and later live volume changes. Its preview uses
gain 0.7 and the central pitch 1.6. Its registered resource stays within the scoped
extra-effect decoder and the full registered/streaming PCM audit.

## Complete decoding

Beta's Ogg decoder can treat an incomplete buffered page as the end of a file.
It can also stop before draining pages already in memory or leave final padding.
Power Beta repairs page reading and trims final padding for its two bundled
extra-effect namespaces. Native sound conversion still uses Beta's JOrbis decoder.
Other resources, personal music and other mods retain their existing decoder path.

The disposable runtime fixtures compare every bundled extra effect with the
unchanged decoder on the same bytes, verify its full source sample count through
both registered playback resources and streaming reads, and export PCM for an
independent FFmpeg comparison. No derived audio replaces the official assets.
