# Bundled music

Power Beta bundles **107 tracks** through **26.3**: 92 official game assets and
15 album-only songs converted from the owner's purchased Volume Alpha/Beta MP3s.
The checked snapshot, **26.4-snapshot-2**, has the same game soundtrack.
There are 56 unique overworld background tracks, 27 menu/record/credits tracks,
nine Nether/End background and boss tracks, and 15 album extras.

The files stream directly from the mod JAR. Playback needs no download or extraction.
Bundled OGG audio totals 358,368,039 bytes.

| World music | Tracks | Selection |
| --- | ---: | --- |
| Vanilla | 12 | Original Beta 1.7.3 background music |
| Alpha and Beta survival | 12 | Alpha/Beta songs actually used for survival background music |
| Alpha and Beta all | 50 | Alpha/Beta survival, creative, menu, records, credits and album extras |
| Minecraft survival | 50 | All survival overworld/underwater/biome background songs |
| Minecraft all | 98 | All non-dimensional groups, including album extras |

Nether and End background/boss tracks appear in Everything and presets, but never
in these five World music selections. Records such as Pigstep remain eligible
because records can play in any dimension. The credits song Alpha stays eligible.

Overworld groups:

| Library group | Tracks |
| --- | ---: |
| Alpha | 12 |
| Beta / Creative | 6 |
| Update Aquatic | 3 |
| Caves & Cliffs | 8 |
| The Wild Update | 4 |
| Trails & Tales | 4 |
| Tricky Trials | 9 |
| Chase the Skies | 5 |
| Chaos Cubed | 5 |

The original 12 cover Minecraft Beta 1.7.3's ordinary background music.
"Beta / Creative" means C418's Volume Beta album, released in 2013. Its six
creative tracks arrived after Minecraft's Beta development era. Tracks shared
between survival and creative appear once in their release group. Only the six
creative-exclusive tracks get the separate Creative group.

## Album-only tracks

The owner supplied purchased MP3s on 2026-10-05. All 15 album-only songs are bundled
under **Alpha / Album extras** and **Beta / Album extras**:

- Alpha: Door, Death, Moog City, Équinoxe, Chris, Excuse, Dog, Beginning,
  Droopy Likes Ricochet, Droopy Likes Your Face.
- Beta: Ki, Flake, Kyoto, Eleven, Intro. Album Eleven is distinct from record 11.

`tools/import_album_extras.py <album-folder>` imports only these missing songs.
FFmpeg converts them to Vorbis quality 8 at the original 44.1 kHz sample rate and
channel count. Source MP3 bitrates average about 207–260 kbit/s. Original MP3s are
never edited. The manifest records source filenames, SHA-256 hashes, codec,
sample rate, bitrate and conversion settings. Game versions of existing songs
keep their original identifiers and bytes; the albums do not create duplicate entries.
Both World music choices ending in **all** include the album extras.

Every group, including custom folders, has an inclusion button and included/total
count. Clicking a fully included group excludes every member; clicking a mixed
or excluded group includes every member. Search does not narrow a group edit.
The button updates the existing individual track settings together, with the
usual live preview, Apply, auto-apply and Cancel behavior. It does not change
track volumes, queue requests, folders or files.

## Sources and reproducibility

- [Official version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json).
- Per-version asset-index URLs and sound-definition hashes are pinned in the
  committed soundtrack manifest.
- [C418: Minecraft Volume Alpha](https://c418.bandcamp.com/album/minecraft-volume-alpha).
- [C418: Minecraft Volume Beta](https://c418.bandcamp.com/album/minecraft-volume-beta).
- [Caves & Cliffs music announcement](https://feedback.minecraft.net/hc/en-us/articles/4411754678157-Minecraft-Java-Edition-Snapshot-21w42a).
- [Chase the Skies music announcement](https://www.minecraft.net/en-us/article/craftable-saddles-and-fresh-music).
- [Chaos Cubed music announcement](https://www.minecraft.net/en-us/article/minecraft-26-2-snapshot-7).

`src/main/resources/assets/powerbeta/music/manifest.json` records filenames,
titles, eras, creative-exclusive classification, source event references,
original asset paths, playback roles, SHA-1 hashes and byte sizes. Existing 21
filenames and
encoded assets remain unchanged so saved volumes, exclusions and queue entries
still work. The game catalog uses official assets; purchased album sources are recorded separately. Renamed modern files map
back to their historical setting IDs; remasters do not duplicate songs.

`python3 tools/audit_music.py` resolves game/creative/underwater/overworld sound
events, menu/credits events and records recursively across the pinned source
versions, compares their union with the bundled catalog, checks the creative-only subset and validates provenance. Dimension events are included
in the asset audit, while unit tests ensure they never enter a World music pool.
Historical aliases prevent duplicate entries after official filename changes.

`python3 tools/fetch_music.py` retrieves missing/mismatched game files from Mojang's
content-addressed asset service, checking size and SHA-1 before replacement.
Unit tests verify every bundled file. Runtime validation plays each file through
Beta's actual streaming decoder in a disposable instance.

Beta's JOrbis decoder treats short compressed-resource reads as EOF.
`AudioResource` fills each requested read, buffering only a small amount of
data. It preserves direct JAR streaming and prevents tracks from cutting off
after their first OGG page. `tools/VerifyMusicStreams.java` decodes each entire
file both from disk and through the packaged resource adapter, comparing the
PCM length and SHA-256. Run with the Beta client JAR and `build/classes/java/main`
on the Java source-launcher classpath, passing the built pack JAR and source
asset directory as arguments.

## Preference migration

Migration backs up the complete configuration before writing. Original custom
Add/Replace modes become separate Custom music preferences. Disabled background
music becomes music volume zero. Old minimum ticks and random extra ticks become
minimum and maximum seconds. Portal-stop switches become one dimension-change
policy. Menu-folder preference becomes one selection control. Track volumes,
exclusions, folder switches, queue order and unrelated settings remain intact.
Migration is versioned and idempotent. It never edits user music files.

## Presets, favourites and playback

Presets use stable IDs and independent exclusion snapshots. Creating, editing or
renaming one never changes live exclusions. Loading one replaces live exclusions;
None restores the ordinary soundtrack/folder controls without clearing exclusions.
Deleting an active preset returns to None and keeps current exclusions. Presets,
favourites, exclusions and volume controls share the Options transaction and the
single Power Beta configuration file. Missing custom-track IDs stay in saved
presets so returning files recover their selections.

A selected preset makes every available bundled/custom track a candidate, with
its copied exclusions deciding what plays. It overrides World music and Custom
music handling. The Active view retains excluded rows so they can be re-enabled.
The Presets filter browses snapshots without loading them. Include all resets
exclusions in the current library or editor scope only.

The countdown reserves its next song. Next plays that reservation or the head of
the explicit queue. Pause and resume preserve stream identity and position,
including temporary previews. Preview completion restores the interrupted track.
Sound previews do not play an extra UI click.

Scrubbing prepares a positioned decoder on one background worker using bounded
PCM buffers. It hands the decoder to Beta's sound system without restarting the
song from zero. Repeated OpenAL initialization keeps the prepared position;
cancelled seeks close their decoder. Metadata reads and decoding never run in
the menu render loop. Unknown durations disable scrubbing without blocking playback.
`MusicSeekingTest` checks the entire remaining PCM against a direct decode after
seeking, including the repeated-initialization path.
