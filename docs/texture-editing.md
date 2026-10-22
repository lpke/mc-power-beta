# Editing textures

Edit the source PNGs directly, then use a preview texture pack to see changes in
the chosen Prism instance. No mod rebuild or game restart is needed.

From the repository directory, run:

```sh
python3 tools/preview_textures.py --instance "/path/to/Prism instance" --watch
```

All editable game textures live in [textures/](../textures/), grouped by feature.
The script packages them using the same runtime-path manifest as the build.
Keep image dimensions and sprite positions unchanged unless the drawing code is
being updated too. Save as PNG with transparency.

1. In Options > Texture pack, select **Power Beta Texture Preview.zip**.
2. Edit and save the source PNGs. Watch mode updates the ZIP after each save.
3. Select **Default**, then **Power Beta Texture Preview.zip** to reload the images.
4. Press Ctrl+C in the terminal when finished.

Omit `--watch` to refresh the ZIP once. The script writes only its preview ZIP;
it leaves instance settings, other texture packs, worlds and JARs alone. Incomplete
saves retain the previous preview until the image is ready.

To preview a smaller selection, add `--feature offhand --feature inventory-buttons`.
Each feature selects a folder inside `textures/`; repeat it to combine folders.
For external artwork, `--source` can still package an existing resource root
instead. Conflicting images at the same runtime path are rejected.

The source PNGs remain the files to build and commit. When finished, tell the agent
what to build, update, commit or publish. There is no need to import the preview
ZIP. Older separate editing folders are no longer the working copies.
