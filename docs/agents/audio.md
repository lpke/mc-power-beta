# Audio imports, playback and decoder validation

- `AudioController` owns all music sources, pauses, previews and timing.
  World/menu playback uses only the selected saved playlist. Menus default to
  Same as world; transitions preserve eligible streams, explicit pause, gaps and
  queue state. A changed selection drops an ineligible background stream without
  interrupting an audition. Preview restoration must not revive old jukeboxes.
  Synchronize sound-source cleanup with decoder reads on the sound threads.
  Never scan folders or decode audio from a render loop. Playlist editor drafts
  stay separate from saved playlists until Save. Playback reads the selected saved
  playlist directly; selecting one changes its ID without making a working copy. The
  reserved `beta` playlist contains exactly the twelve original Beta 1.7.3 tracks and
  cannot be edited or removed. Ordered positive track IDs prevent imported folders
  silently expanding playlists. Global music folders supply the Library; playlist
  membership belongs to each saved playlist. Group and track gains belong to the
  Library and apply in every playlist. Imported group gains use their configured
  source-root identity, matching Library headings. The reserved `favorites` playlist has ordered membership;
  it can be selected but not renamed or removed. Library
  group order is presentation data, separate from scan priority and playlist order.
  Imported sources have no On/Off state. Music data format 4 lives in `power-beta/music.json`, while playback
  preferences stay in the shared config; both paths join the Options transaction.
  Queue snapshots merge independently of editor drafts.
  Keep missing track IDs, queues and user music files intact.
  Publish an immutable queue list only on load or edit; reads retain its identity.
  A queued song keeps its row until a new native `BgMusic` source with the requested
  URL is playing. The previous source, including the same song, cannot acknowledge
  an asynchronous replacement. Each attempt also retains a distinct URL object through
  native `FilenameURL`, so an older unpublished same-song start cannot claim it.
  Preserve the URL text and handler when copying that token. Admission owns the exact queue snapshot and library
  revision; edits or rescans invalidate it. Pause and mute retain pending admission
  without expiring its startup grace. Manual replacement, Previous, Quiet, seeking
  and auditions cancel it. Failed starts keep the request and suppress retries until
  the queue changes or a new scan completes. Publish immutable source identity and
  URL observations from the sound command thread; do not read its mutable source map
  or acquire decoder locks from the render thread. File availability belongs to the
  scanner, and source startup confirms playback without per-frame filesystem probes.
  Previous during a between-track gap replays the last played track without
  moving back in history or consuming queued tracks. Paused tracks remain current.
  Previous restarts the visible song after more than ten seconds, including an
  audition, while keeping its background snapshot, history, queue and ordered
  playlist cursor. At ten seconds or earlier, keep the existing previous-song
  behavior, ending an audition before navigating background history.
  Master or native Music at zero pauses background music and auditions without
  stopping or replacing their sources. Muted status takes precedence over playback
  labels. Unmuting, including Options preview rollback, leaves both paused until
  manual resume. Seek completion and preview restoration must honor that pause.
  Native Music's zero-volume callback must pause rather than stop the stream.
  Accepted paused seeks retain their published decoder handoff until that source
  first plays. Cancelling a pending seek, starting an audition or seeking another
  source must not clear that handoff. Source replacement retires only its own
  obsolete handoffs and preserves its incoming token. Claim and disposal transfer
  cleanup ownership under one lock; unclaimed decoder cleanup runs on the seek
  worker, while claimed streams use the native stream cleanup lock.
  Pause commands for owned music streams skip stopped native sources on the sound
  command thread. Marking an unstarted seek source paused makes Beta skip its first
  preload. Preserve command ordering so a queued Play followed by Pause still pauses
  the initialized stream; leave jukeboxes and other sources unchanged.
  Keep the advertised next track reserved through settings previews while it remains
  eligible; volume, gap and menu-control edits must not consume the selector.
  Explicit playback of a saved playlist member aligns its cursor to the song's
  position in the saved order, including pause/resume and queued acceptance. Keep
  outside songs and auditions from moving that cursor. Playlist row playback selects
  the saved world playlist without saving editor drafts. Now-playing navigation
  opens Library for auditions or songs outside the world playlist; other songs open
  their actual playlist. Paused background songs remain gold; playing and paused
  auditions are blue and their row transport toggles the audition without selecting
  another world playlist. Between-song links use the reserved next song and its
  world/menu origin without consuming a request or advancing the selector.
  Starting/resuming music, changing tracks and starting/stopping previews produce
  no UI click. Actual Pause and Quiet actions retain their click. Preview buttons
  and Stop preview context actions remove the audition completely, preserving
  background-source identity, pause state, position, history and queue; they never
  pause/resume the audition. Keep the retained background song gold while another
  song is previewed, and use the same blue for the preview title and active speaker.
  Add songs removal uses the current editable playlist draft from both row X and
  context menu. Included rows retain a red X through hover, offer active Remove
  from playlist, and never modify source files, the Library or saved playlists
  before Save. Read-only playlists expose no membership edits.
  Catalog order is persisted separately from each playlist's song order. Rename,
  duplicate, reorder and Play use saved objects and never save isolated editor drafts.
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

- Music source relinking keeps a canonical original-root identity per configured folder.
  Hash the original root plus relative filename for stable track IDs. Move only source
  group order/gain keys; never rebuild playlists, favorites or queues. Keep those aliases
  in the format-4 music document and include them in the Options transaction.
  The scanner maintains a bounded derived cache index for converted MP3 files. Missing
  sources retain indexed cache playback and report Unavailable. Read sources only during
  startup, explicit reload, conversion completion or folder edits. Old unindexed caches
  cannot recover a missing original's filenames; relink the folder to restore discovery.
  Queue every missing source path from the immutable scanner snapshot, showing each
  once per session. Compact Library headings retain the gray folder name, red
  Source missing status, square relink magnifier, delete and volume controls with
  shared hit bounds. The magnifier and context action open the same single-folder
  chooser, displaying the exact old path and basename. Add folder keeps its
  independent multi-selection. Rendering never checks source availability on disk.
- Music fades apply to song openings and endings, using accumulated native playback time
  and asynchronously read duration. Zero skips fade work and duration discovery for this
  feature. Pause and mute remain immediate; explicit transport keeps its existing timing.
  Unknown durations can fade in but cannot schedule an ending fade.
  The default is 1000ms, with a 0–10000ms range and 10ms pointer steps. Existing
  saved values, including zero, stay intact; exact entry accepts every whole
  millisecond. MP3 batch counts likewise drag by 10 while exact entry accepts
  every whole count within the existing range.

- Optional boat and minecart attack audio follows the pinned modern `Player.attack`
  events, with native successful damage/removal as the trigger. There is no separate
  modern vehicle break cue. Keep charge and held-item tracking limited to sound
  selection, preserve per-variant gains through live mixing and previews, and leave
  native damage, drops and attack timing intact. See [vehicle sounds](../vehicle-sounds.md).
