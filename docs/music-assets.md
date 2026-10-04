# Bundled music

Power Beta bundles **83 game-supplied tracks**, including all **56 unique
overworld background tracks**, through **26.3**. The latest
available snapshot, **26.4-snapshot-2**, has the same music. The collection
includes survival, creative, underwater and biome music. Expanded pools can play
in any Beta biome or mode. The remaining 27 tracks are four menu tracks,
22 records and the credits song.
They have separate Menu, Records and Credits groups within their release eras.
Nether and End background/boss music stays excluded. Records such as Pigstep
remain included because they can play in any dimension; Alpha is the credits
song, not End background music.

The OGG files stream directly from the mod JAR. No download, extraction or
folder scan runs during playback. Bundled audio totals 266,250,343 bytes.

| World music | Tracks | Library groups |
| --- | ---: | --- |
| Vanilla | 12 | Alpha |
| Alpha and Beta | 35 | Available Alpha/Beta background, creative, menu, record and credits music |
| All Minecraft | 83 | All bundled groups |

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

The complete commercial albums contain 15 more songs that do not exist in
Mojang's Java game-asset service. These files are not bundled yet.
Local album copies are needed to add these:

- Volume Alpha: Door, Death, Moog City, Équinoxe, Chris, Excuse, Dog, Beginning,
  Droopy Likes Ricochet, Droopy Likes Your Face.
- Volume Beta: Ki, Flake, Kyoto, Eleven, Intro. The album's Eleven is different
  from the game's record 11.

The existing music-folder support can play local OGG/WAV files and convert MP3
to its playback cache. Put album extras in their own folder to give them a
separate library group with the same group/individual inclusion controls.
Original files are never modified.

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
still work. The 62 additions use official 26.3 assets. Renamed modern files map
back to their historical setting IDs; remasters do not duplicate songs.

`python3 tools/audit_music.py` resolves game/creative/underwater/overworld sound
events, menu/credits events and records recursively across the pinned source
versions, compares their union with the bundled catalog, checks the creative-only
subset and validates provenance. It catches missing tracks, accidental dimension
additions and
duplicates caused by official filename changes.

`python3 tools/fetch_music.py` retrieves missing/mismatched files from Mojang's
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
