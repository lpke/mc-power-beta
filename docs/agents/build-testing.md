# Builds and runtime testing

- Java 21 and Python 3. Run `python3 tools/build_pack.py` from this directory.
  It builds sequentially, runs unit tests and exports a clean Prism ZIP. Do not run
  Loom builds concurrently; their mapping caches are shared.
  Add `--offline` when all pinned Gradle dependencies are cached to avoid remote
  metadata checks; the same option is available in `tools/build_utilities.py`.
- `src/` owns the Options UI, config adapters, audio, overlays and integration.
  `modules/creative`, `building` and `worldedit` have their own builds.
  Other modules use `source-build/modules.json`; their original Gradle files are
  upstream reference, not the pack build. `input-api/` is the shared input bridge.
- Utility JARs are compiled here and nested inside Power Beta. Six platform,
  authentication and performance JARs remain external. Disabled vendor artifacts are retained for reference, not shipped.
  Review pinned dependency manifests before changing platform binaries.
- Root tests: `./gradlew --no-daemon test`. Utility build:
  `python3 tools/build_utilities.py [MODULE ...]`. For a pack already built,
  `python3 tools/build_pack.py --skip-build` only assembles the export.
- Texture editing uses source PNGs and `tools/preview_textures.py --instance ...
  [--watch]`; see `docs/texture-editing.md`. Source PNGs are authoritative; preview
  ZIPs are disposable and untracked. A preview needs no build or release.
  Keep all editable game textures in `textures/`, grouped by feature, including
  future additions. Register build mappings in `textures/manifest.json` and preserve
  existing runtime paths. Screenshots and test fixtures stay separate.

- Test in a disposable clone outside the user's normal instance list. Never launch
  or edit an active instance. Stop only the exact process you started, verified by
  PID and game directory. Preserve worlds, recovery journals and original music.
- Clean up generated files when their task is finished: obsolete build JARs/ZIPs,
  accumulated Loom remap copies, disposable test clones, temporary exports and captures.
  Check disk usage during long build/test runs. Before removing anything, verify that
  no active process or pending check needs it; do not clean shared mapping caches
  during builds. Keep current artifacts, source, user files, worlds, recovery journals,
  original music and required backups. Do not delete unrelated application caches.
- Mute every test instance before launch. Route audio tests to a silent output or
  capture device so they never play through the user's speakers or headphones.
  Never mute or change the audio routing of the user's own instances.
- `validation-mod/` contains runtime fixtures, including `audio-maintenance`.
  Build with `./gradlew -p validation-mod --no-daemon build` after the root JAR.
  Install only in the disposable clone. Its README explains command dispatch.
  Fixtures can change settings and worlds; never ship them.
  Validate current-format world metadata with full native item data through
  `world-metadata-450` on disposable copies at the native client's title screen.
  Standalone JVMs lack the game's item-codec mixins; never weaken round-trip guards
  or rewrite player data to make offline validation pass.
- For an existing Power Beta instance on matching dependencies, use
  `python3 tools/update_artifacts.py /path/to/instance`. It refuses running clients,
  verifies a backup and checks that settings and worlds remain byte-identical.
  Configuration changes are separate, backed-up operations through `PowerConfig`.
  When updating a user's instances, also refresh the Survival and Cheats creation
  templates to the latest selected Defaults base using `upgradeInstance -PinstanceRuleDefaults`.
  This operation must verify a durable backup and preserve all other preferences,
  existing world rules, player data and recovery records.
  For releases, finish validation and review first, then install and verify the
  exact final build before any publication. Notify the owner when it is ready.
- Verify the final export contains seven enabled JARs and no test fixtures, accounts,
  worlds, logs or disabled mods. Keep README human-facing; add lasting implementation
  constraints here rather than another session diary.
- Commit and push only when the current user request authorizes it.
