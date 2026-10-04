# Bundled music

Power Beta pins the overworld background tracks in Minecraft Java 1.14.4.
The 21 OGG files are streamed directly from the mod JAR. No download, extraction,
or directory scan runs during playback. The package adds 77.2 MB of audio.

| World music | Tracks | Library groups |
| --- | ---: | --- |
| Vanilla | 12 | Alpha |
| Alpha and Beta | 18 | Alpha, Beta |
| All Minecraft | 21 | Alpha, Beta, Update Aquatic |

"Beta" here refers to C418's Volume Beta album, released in 2013. Its six
creative background tracks arrived after Minecraft's Beta development era.
The original 12 tracks cover Beta 1.7.3's ordinary background music. The
additional underwater tracks arrived in 1.13. The expanded pools are available
in survival and creative; they do not require a matching mode or underwater
location. No Nether, End, menu-only tracks or music discs enter these pools.
The unfinished Nether-music request was explicitly withdrawn.

## Sources and reproducibility

- [Official version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json).
- [Official 1.14 asset index](https://piston-meta.mojang.com/v1/packages/43b2f3021fe9f7d768378de95538e22da3ee8301/1.14.json).
- [C418: Minecraft Volume Alpha](https://c418.bandcamp.com/album/minecraft-volume-alpha).
- [C418: Minecraft Volume Beta](https://c418.bandcamp.com/album/minecraft-volume-beta).
- [Official Update Aquatic release notes](https://feedback.minecraft.net/hc/en-us/articles/360007323492-Minecraft-Java-Edition-1-13-Update-Aquatic).

`src/main/resources/assets/powerbeta/music/manifest.json` records every filename,
song title, era, original asset path, SHA-1 and byte size. `tools/fetch_music.py`
verifies existing files and retrieves missing/mismatched assets from Mojang's
content-addressed game asset service. It verifies size and SHA-1 before replacing
any file. Unit tests verify every bundled file against the manifest.

## Preference migration

Migration backs up the complete configuration before writing. Original custom
Add/Replace modes become separate Custom music preferences. Disabled background
music becomes music volume zero. Old minimum ticks and random extra ticks become
minimum and maximum seconds. Portal-stop switches become one dimension-change
policy. Menu-folder preference becomes one selection control. Track volumes,
exclusions, folder switches, queue order and unrelated settings remain intact.
Migration is versioned and idempotent. It never edits user music files.
