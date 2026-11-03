# Boat and minecart attack sounds

The optional Audio > Extra sounds > Boat and minecart hit sounds setting adds
Java 26.3 attack audio to successful singleplayer hits against boats and all three
Beta minecart types: rideable, storage and powered. It defaults on only in the
Power Beta base. Missing preferences are frozen once against the selected base
through the existing backed-up configuration initializer.

## Official reference

The reference is the official Java 26.3 client, SHA-1
`e877b6a07acd633fb3bb475002175cec036e7b87`, recorded by the `waterSource` entry in
[`extra-sounds.json`](../src/main/resources/assets/powerbeta/extra-sounds.json).
That entry pins the official version metadata, resource index, client and
`sounds.json`, SHA-1 `7975165ad4a3c0cb3c05078b914eac0f2c3b5f5f`.

`VehicleEntity.hurtServer` handles damage and Creative discarding. Neither that
method nor its `destroy` methods emit a vehicle break sound. Boats and minecarts
inherit that behavior. The audible hit is produced by `Player.attack`, including
its final hit. There is no second boat or minecart destruction cue to import.

`Player.attack` uses charge above 0.9 for the strong cue and otherwise uses the
weak cue. Charge is `(attackTicks + 0.5) / (20 / attackSpeed)`, clamped to 0–1.
A full-strength sprint attack also emits knockback audio. A full-strength sword
attack uses sweep audio when grounded, not sprinting, and moving horizontally
slower than the player's movement-speed attribute multiplied by 2.5. That sweep
test does not require a living target. Critical hits do require one, so boats and
minecarts cannot produce critical-hit audio. The default attack call gain and
pitch are both 1; variant gains come from `sounds.json`.

The bundled variants are the six strong cues, four weak cues, seven sweep cues
and four knockback cues. Strong variants 1–4 have gain 0.6; strong variants 5–6
and all the other variants have gain 0.7. Each has equal default weight, pitch 1
and no streaming override. The manifest retains every original resource path,
SHA-1, byte size and source frame count. Official Ogg bytes are unchanged.

## Beta integration

Successful native damage produces Survival audio. Creative removal cancels the
native damage call, so its interaction wrapper emits the cue once after actual
removal. Dead targets, disabled settings, Spectator and remote worlds add no cues.
A charged empty-hand Creative hit produces one strong thunk. A final Survival hit
uses the same attack cue as any other successful hit. This covers ordinary native
Beta vehicles, whose damage callbacks accept the hit. A custom vehicle that
rejects native damage adds no optional success cue; this feature does not add
modern rejection audio or claim parity for custom invulnerable entities.

Only sound selection tracks modern attack charge, accepted misses, cancellation
of active native mining and native held-item type changes, including the official
attack speeds for Beta's tool materials. It does not change Beta attack timing, damage, drops, inventory contents or vehicle
physics. Charge updates and the held-item comparison stop while the feature is
disabled, the player is dead, the world is remote or the player is Spectator.
Accepted attack presses check a selected tile for an air miss. Mining hooks reuse
native progress and block reads. There are no terrain scans, render calculations,
extra entity random draws or file writes on attacks.

The pinned server's `ServerGamePacketListenerImpl.handleMovePlayer` passes the
accepted final position minus the pre-packet position to `handlePlayerKnownMovement`;
`ServerPlayer.getKnownMovement` reads that displacement. Power Beta uses its native
last-tick `x - prevX` and `z - prevZ` displacement, including collision response,
instead of the remaining velocity after friction. The ordinary movement-speed
attribute is the same `0.1f`; the exact threshold is `(double) 0.1f * 2.5`, or
0.2500000037252903, with a strict less-than comparison on squared movement.

All cues use Players volume and their individual sound volume. Their registered
sound-pool selections and previews apply the source variant gain before normal
mixing, preserving it through later volume changes. Subtitles use the official
attack caption labels. Resources remain inside the scoped extra-effect decoder.

## Validation

`python3 tools/audit_extra_sounds.py` verifies official event definitions and every
asset byte against pinned Mojang resources. Unit tests cover tool charge,
strong/weak/sweep selection, per-variant gain, registration, previews, Gson
round-trips and defaults for all three bases.

Run `vehicle-sounds-473` in a muted disposable clone on a verified silent output
for native Creative removal and Survival attacks against every Beta vehicle type,
charged and rapid hits, final hits, repeated dead targets, accepted and suppressed
misses, right use, native mining aborts and completion, swaps, sword sweeps,
rejected native damage, sprint cue ordering, exact sweep speed boundaries,
displacement that differs from remaining velocity, disabled sounds, Spectator and
remote ownership. The fixture restores player data, settings, vehicles and drops.
`ui-473` also captures the setting and its help at 854×480 and 320×240, checking
visible preview/toggle bounds without changing the preference.

Run `extra-sound-decoders-432` in the same disposable clone for full registered
`readAll` and streaming playback resources, then compare the exported PCM with:

```sh
python3 tools/verify_extra_sound_pcm.py /path/to/power-beta-validation-pcm --output /tmp/extra-sound-comparison.json
```

The comparison requires every current manifest entry, checks byte hashes and the
source's final Ogg granule, verifies both native frame counts and compares an
independent FFmpeg decode. It reports initial-preroll alignment and tail padding
separately from the maximum sample difference, which must stay within three
16-bit sample units.
