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

## Publish

Publishing requires the owner's explicit permission. A request to edit code or
push a commit does not grant permission to publish a release.

Install Java 21, Python 3, GitHub CLI and Vercel CLI. Sign in with `gh auth login`
and `vercel login`. From this repository run:

```sh
python3 tools/release.py --publish --site /path/to/power-beta-website
```

The command builds modules sequentially, runs tests, checks the seven-JAR export
and creates a draft GitHub release tagged `vX.Y.Z` at the reviewed commit. It
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
Never overwrite published ZIPs or move existing release tags. Fixes get a new version.

The two repositories are configured in `tools/publish_config.json`. Public release
metadata is copied to the website's `public/release.json`; `tools/sync_release.py`
in that repository renders the page and fallback metadata. No service token is
needed to serve the website. When a private repository needs Blob, the publisher
uses a temporary, private file for its credentials and removes it when finished.

## Verify and install

Check the GitHub release, website version, changelog, download and checksum.
For an idle existing instance with the same platform dependencies:

```sh
python3 tools/update_artifacts.py /path/to/prism/instance
```

The updater verifies a backup, replaces the pack JAR, removes the old version and
checks that settings and worlds remain byte-identical. A failed copy restores the
previous artifacts. Keep the backup until verification finishes.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
versioned modpack notes such as [Fabulously Optimized](https://github.com/Fabulously-Optimized/fabulously-optimized/blob/main/CHANGELOG.md).

The website reads published GitHub release notes and ZIP assets. While the repo
is private, the publisher saves a verified GitHub snapshot for anonymous visitors.
Once public, the site checks GitHub automatically, with a five-minute cache.
Keep changelog bullets limited to modpack features and fixes. Website and release
process changes belong in commits and development documentation.
