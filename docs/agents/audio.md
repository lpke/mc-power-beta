# Audio imports, playback and decoder validation

- `AudioController` owns all music sources, pauses, previews and timing.
  World/menu transitions preserve streams and pause state in WORLD/MIX modes;
  only CUSTOM resets playback. Preview restoration must not revive old jukeboxes.
  Synchronize sound-source cleanup with decoder reads on the sound threads.
  Never scan folders or decode audio from a render loop. Playlist editor drafts
  stay separate from saved playlists until Save. Playback reads the selected saved
  playlist directly; selecting one changes its ID without making a working copy. The
  reserved `beta` playlist contains exactly the twelve original Beta 1.7.3 tracks and
  cannot be edited or removed. Ordered positive track IDs prevent imported folders
  silently expanding playlists. Global music folders supply the Library; playlist
  membership and group gains belong to each saved playlist. Track gains and favorites
  remain global. Music data format 2 lives in `power-beta/music.json`, while playback
  preferences stay in the shared config; both paths join the Options transaction.
  Queue snapshots merge independently of editor drafts.
  Keep missing track IDs, queues and user music files intact.
  Previous during a between-track gap replays the last played track without
  moving back in history or consuming queued tracks. Paused tracks remain current.
  Keep the advertised next track reserved through settings previews while it remains
  eligible; volume, gap and menu-control edits must not consume the selector.
- New and imported Ogg sounds must retain complete playback through Beta's decoder.
  Native `CodecJOrbis` mistakes `SyncState.pageout() == 0` for EOF when a buffered
  page needs more input, and can stop before draining buffered pages. It can also
  retain samples beyond the final Ogg granule position. Valid asset hashes and an
  EOF result do not prove that a sound played completely.
  `ExtraSoundDecoderMixin` fixes page refill/draining, keeps native PCM conversion
  and trims final padding to the declared source frame count. Its scope is cached
  at initialization through `ExtraSoundResources.owns`, currently covering the
  `power_beta` and `power_controls` extra-effect resource paths. Keep personal music,
  native sounds and other mods outside that scope. New sound namespaces must extend
  the scope and its tests; verify the actual registered sound-pool URL reaches it.
  Preserve `AudioResource.streaming` for short compressed-resource reads; that
  wrapper alone does not repair the Ogg page parser. Never alter official audio
  bytes to hide decoder faults. Stream long ambience on the sound threads.
- When adding sounds from another version, verify its official client behavior,
  resource index and `sounds.json`; pin asset hashes and retain per-variant gain,
  pitch, probability/weight and streaming metadata. Extend `extra-sounds.json`,
  `tools/audit_extra_sounds.py` and the runtime decoder fixture for every added
  variant. Run `extra-sound-decoders-432` and relevant behavior fixtures in a muted
  disposable clone on a verified silent output. Check both registered `readAll`
  playback resources and streaming reads against the final Ogg granule frame count,
  and compare PCM with an independent FFmpeg decode, explaining rounding/padding
  differences. Comparing two instances of the same broken decoder is insufficient.
- Subtitles use the same position and yaw basis as the active sound listener,
  including freecam. Muted audio still admits registered captions without starting
  the sound engine, reading terrain or consuming sound-pool randomness. UI and music
  never create guessed captions; semantic scopes apply once and remain thread-local.
- Music-library collapse/expand-all acts on groups in the current view, including search,
  without changing inclusion, volume, playlists or queues. Menu scrub tracks reserve equal
  space on either side so the track itself stays centered with the menu.

- Optional fluid sounds follow verified official assets and native movement/display events.
  Keep splashes singleplayer for every native entity; swimming stays local-player-only.
  Lava pops/ambience use exposed-surface display events and Ambient volume.
  Keep native lava damage and item fizz unchanged. Preserve particles, per-entity
  categories and source categories through live volume edits.
  Audio randomness must not advance native entity/display random generators.
  See `docs/water-sounds.md` for the pinned modern source and Beta differences.
