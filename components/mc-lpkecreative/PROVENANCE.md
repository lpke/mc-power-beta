# Sources and assets

The initial commit `f4a0962` is an exact file snapshot of
[paulevsGitch/BHCreative](https://github.com/paulevsGitch/BHCreative), branch
`stapi-2.0`, commit `c2afd49358d1610df5508dba430e182d7b6a7e97`.
Both initial Git trees are `cd99edc0153887394dba7ab3b8ac7c721db38e82`.
The initial snapshot was pushed privately before development changes.
BHCreative's MIT licence and attribution remain in `LICENSE`.

Movement constants and the picker behaviour were checked against Mojang's official
Minecraft 1.21.4 client and mappings from
[the version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json).
The creative picker uses a grass block. The mod icon reuses LpkeTweaks' grass-block image.

[Tweakeroo](https://github.com/sakura-ryoko/tweakeroo/tree/LTS/1.21.4), commit
`aabde75a0a4de8dd82dc4ea9610ba1060e56c6ed`, provided the behaviour reference for
custom fly deceleration. The new flight implementation uses Beta movement and
independent physics code. It does not embed Tweakeroo classes or depend on MaLiLib.

The picker background, slot, selected-slot, ender-eye and creative inventory
textures under `assets/lpkecreative/textures` come from Minecraft 1.21.4.
These Minecraft assets belong to Mojang/Microsoft and are not covered by this
repository's MIT source licence. This repository and its release are private.

The `bhcreative` compatibility ID, packages, entrypoint names, assets and saved
fields intentionally remain. They support existing tab providers and other mods.
