# Water sounds

Audio > Extra sounds has independent controls for body splashes, swimming,
submerging, surfacing, underwater ambience and three groups of occasional
underwater effects. Power Beta defaults enable them. The other defaults bases
keep them off. Existing explicit choices stay unchanged.

The reference is the verified Mojang Java 26.3 client and resource index.
`assets/powerbeta/extra-sounds.json` pins its metadata and every asset hash.
`python3 tools/audit_extra_sounds.py` checks those bytes against Mojang.

| Effect | When it plays | Modern sound gain before user volume controls |
| --- | --- | --- |
| Body splash | The player's body first touches water, excluding the first world tick. Remaining in water does not repeat it. | Weighted movement speed multiplied by 0.2, capped at 1. A gain of 0.25 or higher selects the heavy splash. |
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

The long ambience file streams on the sound threads. Sound creation and decoding
never run in rendering. With all water additions off, they do no terrain sampling
or sound work. Master, Game sounds, Ambient/Players and individual sound volume
controls continue to apply. Previews use the same official variant gains.

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
