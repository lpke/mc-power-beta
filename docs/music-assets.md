# Music assets and playback

The committed catalog contains 107 tracks: 92 official game assets and 15
album-only songs converted from the owner's purchased C418 MP3s. Game assets were
checked through 26.3 and 26.4-snapshot-2. This is the pinned catalog, not a claim
that future releases contain no more music.

| World music | Tracks |
| --- | ---: |
| Vanilla | 12 |
| Alpha and Beta survival | 12 |
| Alpha and Beta all | 50 |
| Minecraft survival | 50 |
| Minecraft all | 98 |

Nether/End music is available in Everything and presets, never the five World
music sets. Records and credits remain eligible. The six creative-exclusive songs
have their own group. Volume Beta is C418's 2013 album, not Minecraft's Beta era.

## Updating assets

`src/main/resources/assets/powerbeta/music/manifest.json` records source versions,
event references, titles, roles, hashes and byte sizes. Keep historical track IDs
stable, including aliases for renamed modern files.

- `python3 tools/audit_music.py`: compare the catalog with the pinned official
  sound-event trees, including overworld, dimensions, menu, credits and records.
- `python3 tools/fetch_music.py`: fetch missing or mismatched game assets, verifying
  size and SHA-1.
- `python3 tools/import_album_extras.py ALBUM_FOLDER`: import the 15 album-only
  tracks from supplied MP3s, using FFmpeg Vorbis quality 8. Originals are read-only.
  Source hashes and conversion settings remain in the manifest.
- Root tests verify bundled files and soundtrack membership.
  `tools/VerifyMusicStreams.java` compares full PCM output from disk and the JAR.
  Use the Beta client JAR and `build/classes/java/main` as its classpath; pass the
  pack JAR and source music directory as arguments.

Official metadata comes from [Mojang's version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json).
Album references: [Volume Alpha](https://c418.bandcamp.com/album/minecraft-volume-alpha)
and [Volume Beta](https://c418.bandcamp.com/album/minecraft-volume-beta).
These audio assets are not covered by the source-code AGPL license.

## Runtime constraints

Audio streams from the JAR without extraction. Beta's JOrbis decoder treats a
short compressed-resource read as EOF; `AudioResource` fills reads with bounded
buffering. Keep that adapter when changing resource loading.

Seek preparation and custom-folder scanning run on workers. Seeking uses bounded
PCM buffers and retains position across OpenAL initialization. Unknown durations
disable seeking, not playback. Pausing any source must preserve its track identity;
temporary previews restore the interrupted song. The countdown reserves the song
that Next will play.

Saved presets contain a track-pool snapshot, exclusions and group volumes.
Existing presets acquire their pool once during configuration migration, with a
backup. New folders and new tracks stay outside saved pools until explicitly
included. Editing a preset cannot change the live pool. Loading copies its state;
library edits then affect that copy. Include all admits every currently known track.
Missing IDs remain saved so returning files retain their selection.

Runtime MP3 conversion writes PCM WAV files to
`power-beta-data/music-cache/<source-fingerprint>.wav`. The fingerprint includes
path, size and modification time. The optional FFmpeg executable is probed once at
startup; the conversion button also requires unconverted tracks. Conversion is
bounded to 256 files per batch, three minutes and 1 GiB per file. Never modify
original music or delete a cache while it is playing.
