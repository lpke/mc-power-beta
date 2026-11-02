# Music assets and playback

The committed catalog contains 107 tracks: 92 official game assets and 15
album-only songs converted from the owner's purchased C418 MP3s. Game assets were
checked through 26.3 and 26.4-snapshot-2. This is the pinned catalog, not a claim
that future releases contain no more music.

The Library groups bundled and imported songs by source. Playback uses an explicit
saved playlist; the protected Beta 1.7.3 playlist contains exactly the twelve
original tracks. Creative, Nether, End, record and credits songs are available for
custom playlists. Volume Beta is C418's 2013 album, not Minecraft's Beta era.

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

Menu music defaults to Same as world. World and menu playback read their selected
saved playlists directly. Eligible playing or paused streams survive world/menu
transitions; changing to a playlist that excludes the background song ends that
stream without interrupting a preview. Explicit pause, gaps and queues remain
intact.

Saved playlists contain ordered track IDs. Selecting one changes its ID without
copying or saving an editor draft. Editing a playlist changes only that draft
until Save; row playback selects the saved playlist without publishing draft
membership. New folders and tracks enter the Library but never expand playlists
automatically. Favorites has its own ordered membership. Group and track volumes
belong to the Library and apply in every playlist. Missing track IDs and queued
entries stay saved so returning files retain their membership and order.

Playlist, folder, group-order, gain and queue data use music format 4 in
`power-beta/music.json`; playback preferences stay in `config/power-beta.json`.
Both paths join the Options transaction; queue edits merge independently of
playlist drafts. Catalog display order is independent of playlist playback order.

Runtime MP3 conversion writes stereo 44.1 kHz, 16-bit PCM WAV files to
`power-beta-data/music-cache/<source-fingerprint>.wav`. The fingerprint includes
path, size and modification time. The optional FFmpeg executable is probed once at
startup; conversion requires unconverted MP3 tracks. Each click converts up to the
MP3 conversion batch limit, which defaults to 500 and accepts 1 through 2048 in
Audio settings. Each source is limited to 256 MiB; each conversion is limited to
three minutes and 1 GiB of output. Originals remain unchanged. Never delete a cache
while it is playing.

The scanner records converted songs in
`power-beta-data/music-cache/index.json`. Its entries retain stable track IDs,
source identity, original name, size, modification time and cache filename.
Indexed converted songs remain playable when their imported source folder is
missing. A changed source fingerprint requires conversion again. Removing the
folder hides its cached songs from the Library without deleting cache files or
playlist entries. Invalid indexes stay untouched and produce a warning; ordinary
source scanning continues.

A missing-source warning offers Relink source path. The same action is available
from an imported folder's context menu, including folders that still exist.
Relinking changes the folder location while preserving its original identity root,
so track IDs, playlist membership, favorites, track gains and queues stay unchanged.
Group order and gain follow the new folder location. Availability checks run during
the session's scan and explicit Reload, never while rendering.

Older WAV caches have no index until their source tracks are scanned by this
version. If a source folder disappeared before that scan, cache filenames alone
cannot recover its song names and identities. Restore the original source path and
reload to index those conversions. Relinking to a new location preserves saved IDs
but may require converting again. Existing WAV files remain untouched.
