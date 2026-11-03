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
- Check disk usage during long builds/tests and follow the mandatory release cleanup
  below when the work finishes. Cache and temporary-directory cleanup is part of
  delivery, not an optional follow-up.
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
  For releases, finish validation and review first, then attempt to install and
  verify the exact final build before publication. Notify the owner when it is
  ready. If the primary is running or otherwise unavailable, leave it untouched,
  report the update as pending and continue authorized publication without waiting.
- Verify the final export contains seven enabled JARs and no test fixtures, accounts,
  worlds, logs or disabled mods. Keep README human-facing; add lasting implementation
  constraints here rather than another session diary.
- Commit and push only when the current user request authorizes it.

## Mandatory release cleanup

A release or re-release is not finished until its disposable development residue
has been removed. After final validation, publication and live verification, audit
and clean leftovers from earlier cycles as well as the current one. Repeat this
after completing a deferred instance update. Do the cleanup before the final
response; do not merely list files or offer to remove them later.

1. Measure allocated disk use before cleanup. Confirm no build, test, renderer,
   browser or other active process needs each candidate, including open files and
   working directories. Never clean Loom caches while a build is running or remove
   files needed by a pending check. Verify the current release and required backups
   before removing superseded copies.
2. Inspect every build, including the root, `validation-mod`, `source-build`,
   `input-api` and independent modules. Remove accumulated `build/loom-cache/`
   remapping intermediates, abandoned `zipfstmp*.tmp` files and project-local
   `.gradle/loom-cache/remapped_mods/` derivatives after their last use. Check
   `build/devlibs/`, `build/libs/` and module output directories for obsolete
   development, source and release JARs. Do not leave dozens of full-pack remaps
   merely because Gradle calls them a cache; they are rebuildable output.
3. Remove obsolete exports/staging copies, agent-created disposable Prism clones,
   temporary browser profiles, screenshots already saved with the private owner
   report, logs, crash dumps, extracted research assets and one-off helper/compile
   directories. Inspect task-owned paths outside the repository, including system
   temporary directories. Identify ownership from their contents and creation
   context, not from a broad filename wildcard. Do not follow symlinks into shared
   libraries or the owner's instance.
4. Keep the exact current release JAR, ZIP, checksum, build receipt and manifest,
   current module/API artifacts needed by the build, pinned vendor/dependency inputs,
   source assets and committed review evidence. Preserve all owner worlds, player
   data, preferences, music, recovery journals and required verified backups.
   Historical release/rollback backups need a separate retention decision; they
   are not ordinary temporary output. Leave unrelated application/global dependency
   caches alone.
5. Measure actual allocated space reclaimed and remaining free space. Verify the
   retained release hashes and that only intended generated paths were removed.
   Record the totals and any retained development residue with its concrete reason
   in the private owner report and final response. If an active process or pending
   check prevents removal, name the path and keep cleanup explicitly pending.
