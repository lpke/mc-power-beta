# Releases

The pack uses semantic versions. Use a patch version for fixes and packaging
changes, a minor version for compatible features, and a major version for changes
that require users to change their setup. Minecraft stays at Beta 1.7.3.

`gradle.properties` is the pack version source. Module versions are separate.
`CHANGELOG.md` contains an Unreleased section and dated release headings, newest
first. Each change is a short bullet explaining what users gain or what was fixed.
The original unversioned work is collected under 1.0.0.

## Prepare

1. Update code and add user-visible changes under Unreleased.
2. When a release is requested, move those bullets into `## [X.Y.Z] - YYYY-MM-DD`
   and set `mod_version` in `gradle.properties` to the same version.
3. Run `python3 tools/release.py --check` and the relevant tests. Test runtime
   changes in a disposable Prism clone. Review the ZIP for private data.
4. With permission, commit and push the reviewed changes to `main`. The website
   checkout must also be clean, on `main`, pushed, and linked to the correct Vercel
   project. Keep credentials in the CLIs, never in Git.
5. Build the exact clean commit with `python3 tools/build_pack.py --offline`.
   Complete runtime validation and final review before installing it.
6. Attempt the primary instance update using the instructions below before starting
   publication. If idle, update and verify it, then immediately tell the owner it is
   ready so they can test while uploads and deployment run. If running or otherwise
   unavailable, leave it untouched, report the update as pending and continue
   publication without waiting. If artifacts change, validate the final build and
   attempt its installation again before publishing.

## Try the primary update first

For an idle existing instance with the same platform dependencies:

```sh
python3 tools/update_artifacts.py /path/to/prism/instance
```

The updater verifies a backup, replaces the pack JAR, removes the old version and
checks that settings and worlds remain byte-identical. A failed copy restores the
previous artifacts. Keep the backup until verification finishes. Apply required
migrations with the shared backed-up converters, and refresh creation templates
as described in [Builds and runtime testing](agents/build-testing.md).
Verify the installed JAR matches the validated release candidate. If the primary
is running or otherwise unavailable, defer its update and proceed with authorized
publication. Never edit a running instance. Record a deferred update as pending
until the exact validated artifacts are installed and verified while idle.

## Publish

Publishing requires the owner's explicit permission. A request to edit code or
push a commit does not grant permission to publish a release.

Install Java 21, Python 3, GitHub CLI and Vercel CLI. Sign in with `gh auth login`
and `vercel login`. From this repository run:

```sh
python3 tools/release.py --publish --skip-build --site /path/to/mc-power-beta-website
```

With `--skip-build`, the command verifies the already-built candidate and its
seven-JAR export, then creates a draft GitHub release tagged `vX.Y.Z` at the reviewed
commit. It
uploads the ZIP and SHA-256 file, verifies the public download, publishes the
release notes, then commits and deploys the website's generated release details.
The website's version, download, checksum and changelog all come from that release.
GitHub stores every release ZIP and checksum itself. Public repositories use
GitHub downloads directly and retain no Blob mirrors. Private repositories use
a verified public Blob mirror for the latest ZIP only. Older releases keep their
notes on the website, without a ZIP link; their original assets remain on GitHub.

The publisher prunes obsolete pack mirrors before a private upload, keeping the
current production ZIP available until the new website snapshot is deployed.
It checks total store size, including both ZIPs and any unrelated files, against
a 900 MB budget before uploading. If both ZIPs would exceed that budget, it stops
instead of risking the Hobby quota. After verifying the deployed snapshot, it
removes the previous mirror. A failed deployment keeps the previous live ZIP;
retry the publisher to finish cleanup. Public releases remove all pack mirrors
after deployment. Cleanup matches only Power Beta release paths in the configured
store and leaves unrelated files alone.

Blob storage and download traffic have separate quotas. GitHub downloads consume
neither. Keeping one private-repository mirror limits storage growth, but visitor
downloads can still exhaust Blob's transfer allowance. Check
[Vercel's current limits](https://vercel.com/docs/vercel-blob/usage-and-pricing).
If a quota has already suspended Blob, deleting files does not necessarily lift
the restriction. Verify both `vercel blob list-stores --all --json` and the account's
Blob block state; use Vercel's reported expiry or contact support if it persists.

Use `--skip-build` only after `python3 tools/build_pack.py` succeeds on the exact
clean commit being released. The command verifies a build receipt and archive
hash before accepting it. It refuses changed assets, changed release notes,
incorrect project links, unpushed commits and attempts to replace newer versions.
After an interruption, inspect the draft and local Git status before retrying.
Ordinary fixes get a new version. Replacing a published version requires the
owner's explicit re-release request. Save the existing release metadata,
tag commit, ZIP and checksum in a private
backup. Verify both asset hashes and the archive before removing anything.
After validation and attempting the primary update, remove only that release and tag with
`gh release delete vX.Y.Z --cleanup-tag`,
then run the documented publisher on the reviewed clean commit. Retain the verified
backup until the replacement download, notes and website are checked. Update the
existing version's public changelog and GitHub notes as one tidy release, merging
and rewording overlapping bullets. Do not label public changes as re-released or
mention replacement history, earlier builds or original versions. Never keep public
"original", "before" or archived copies of superseded releases/tags. Keep verified
backups private. Only the existing private review HTML marks revised items as
Re-released and records the replacement history.

The two repositories are configured in `tools/publish_config.json`. Public release
metadata is copied to the website's `public/release.json`; `tools/sync_release.py`
in that repository renders the page and fallback metadata. No service token is
needed to serve the website. When a private repository needs Blob, the publisher
uses a temporary, private file for its credentials and removes it when finished.

## Verify publication

Check the GitHub release, website version, changelog, download and checksum.
Report whether the primary was installed and verified or its update remains pending.
An unavailable primary does not block authorized publication.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
versioned modpack notes such as [Fabulously Optimized](https://github.com/Fabulously-Optimized/fabulously-optimized/blob/main/CHANGELOG.md).

The website reads published GitHub release notes and ZIP assets. While the repo
is private, the publisher saves a verified GitHub snapshot for anonymous visitors.
Once public, the site checks GitHub automatically, with a five-minute cache.
Keep changelog bullets limited to modpack features and fixes. Website and release
process changes belong in commits and development documentation.

After replacing artifacts, migrate idle instances with the same backed-up converters used
by the client, including every `level.dat` and `level.dat_old` under `saves`:

```sh
./gradlew --no-daemon upgradeInstance -PinstanceGame=/path/to/instance/.minecraft
```

Add `-PinstanceDefaults` only when resetting preferences to pack defaults. It retains
FOV, GUI scale and the frame limit. Version 4.6.0 accepts configuration schema 9,
music-data format 3 and world-rule format 4. The client refuses earlier music
formats instead of silently converting them. Convert earlier personal configuration
with verified backups before installation; the client does not import legacy presets.

For a format-2 music document, first verify the instance is idle. Run the built
input API or pack JAR with Gson 2.8.9 on the Java classpath:

```sh
java -cp /path/to/power-beta-input-1.0.0.jar:/path/to/gson-2.8.9.jar \
  local.luke.power.storage.MusicDataUpgrade --idle-instance \
  /path/to/.minecraft/power-beta/music.json
```

The converter verifies a durable backup, preserves list order and queue data, and
removes retired source Off flags. Verify the converted document and unrelated
settings before installing. The world-upgrade task refuses running clients,
validates every metadata file before writing worlds, and restores its transaction
if any required update fails.
