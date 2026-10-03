# Consolidated sources

- mc-beta-autowalk 5672e53af5b0e74555ee01b66f59f62e5fb79e05
- mc-beta-transport b2458cfc44851db2c268713ec2130685a52e5b50
- mc-beta-fastplace 2f9995375cd272325f3aa78b5adf88bf6386de9b
- mc-beta-flexible-placement 8b39f08590e01f1d6d565da91f18fb8a06655e01
- mc-beta-fake-sneak 55db85d7d8ad58865eab9dfec68f30badd0ec873
- mc-beta-slab-placement 6cb60645a1a76724f57a76d9e5fbdc08965f2fa4
- mc-omnilook 0ac76c94b6a3ce957057dcacd151c832dcc07f91

Click Mining Forever: snowtyler/click-mining-forever, b1.7.3, CC0.
Grass block icon generated for LpkeTweaks, depicting a Minecraft-style grass block.

## Camera integration in 1.1.0

`local.luke.tweaks.camera` replaces the separate `dev.rdh.omnilook` entrypoint,
singleton and config wrappers. Omnilook's input interception and render-only
camera approach remain the basis; `licenses/omnilook.txt` retains its Unlicense.
First-person offsets, interpolation, recentering, lifecycle guards and rendering
compatibility hooks were added for this Beta client.

## Previous histories

`legacy-history/` contains verified Git bundles of all seven replaced local
repositories, including their branches and tags. For example:

```sh
git clone legacy-history/mc-beta-fastplace.bundle restored-fastplace
```

The old documents under `docs/previous-mods/` describe their original releases.
Their relative evidence links refer to files recoverable from these bundles.

## Retirement on 2 October 2026

All seven old local source folders were moved to KDE Trash, including their
`.git` directories, after the replacement repositories were pushed privately.
Each bundle's Git blob hash matched its copy on GitHub before the move.

GitHub rejected the old-repository deletion attempt with HTTP 403 because the
current login lacks the `delete_repo` scope. The seven old remote repositories
remain online. Neither the main Prism instance nor its installed JARs changed.
The replacements are installed in `beta_1.7.3_lpke_dev`.

## Hotbar switching in 1.2.0

Behaviour and settings were checked against
[Tweakeroo LTS/1.21.4](https://github.com/sakura-ryoko/tweakeroo/tree/LTS/1.21.4),
commit `aabde75a0a4de8dd82dc4ea9610ba1060e56c6ed`, especially `Hotkeys`,
`Configs`, `InputHandler`, `Callbacks` and `InventoryUtils.swapHotbarWithInventoryRow`.
The Beta implementation swaps local array references and adds persistence checks,
recovery journals, rollback and singleplayer/container guards. Native Beta keys
replace MaLiLib key-chord settings. Modifier + 1/2/3 is an extra convenience.
