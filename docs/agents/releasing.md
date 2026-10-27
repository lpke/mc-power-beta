# Release delivery

Read [the release procedure](../releases.md) before preparing or publishing a release.
A user request listing changes and a release version authorizes implementation, testing,
commit, push, publishing and the instance update described in AGENTS.md. A code-only
request does not authorize publication.

Complete validation and review, then attempt the primary Prism instance update
before publication. When idle, install and verify the exact final artifacts and
required backed-up migrations, then immediately tell the owner it is ready.
If the primary is running or otherwise unavailable, leave it untouched, report the
update as pending and continue authorized publication and website deployment.
Do not wait for it to close or ask again for release permission. A deferred update
still requires an idle instance and the exact validated artifacts when performed.

For a re-release, merge changes into that version's existing `CHANGELOG.md` entry,
GitHub notes and website changelog, rewording overlapping bullets into one tidy
release. Public notes must not label changes as re-released or mention earlier
builds, replacements or original versions. Keep one canonical GitHub release and
tag; replace it completely after verified private backups. Never create public
"original", "before" or archived replacement releases/tags. Verify the final public
notes and release list. Replacement history belongs only in the existing private
owner review HTML, following [Owner review HTML](release-report.md).

- This project does not accept external contributions. Keep that policy in the
  README and encourage forks under the AGPL; do not add a contribution guide.
- `gradle.properties` owns the pack version. Use semantic versions and tags `vX.Y.Z`.
  Module/API versions are independent; preserve them unless that module needs a change.
- Keep `CHANGELOG.md` newest first, with `## [Unreleased]` followed by dated version
  headings and plain bullets describing modpack features and fixes. Exclude website,
  release-management and development-process changes. Move Unreleased entries
  into the next version when preparing a release. Never invent past release versions.
- Follow `docs/releases.md`. Publishing requires explicit user permission in the
  current task. A code push alone does not authorize publishing a release.
- Owner-requested display renumbering uses the GitHub release title. Retain the
  package identity, tag, assets and checksums so download links stay valid. Show
  the approved display version without an original-version notice. Both numbers
  may resolve to the same download.
- `python3 tools/release.py --check` validates release notes and versioning.
  The publish command builds and tests sequentially, verifies the export, publishes
  GitHub release assets and notes, and updates/deploys the website from the same data.
  Do not hand-edit generated website release details. Replace published artifacts
  only for an explicitly requested re-release, following the backed-up procedure.
- GitHub hosts every release ZIP and checksum. Public repositories use GitHub
  downloads and retain no Blob mirrors. Private repositories mirror only the
  latest ZIP; older website entries keep their notes without a download link.
  Prune obsolete pack mirrors after verifying the deployed snapshot. Keep the
  previous live ZIP during deployment and enforce the 900 MB store budget before
  upload, including that temporary overlap. Never delete unrelated Blob files.
  After quota cleanup, check the store and account block state; an empty store
  does not prove that Vercel has lifted a suspension.
- Never commit runtime reports, logs, instance backups, account files, personal paths,
  `.env` files or Git bundles. Runtime fixture source stays in `validation-mod/`.
- Keep the top-level AGPL license and the README Inspired by links. Source modules
  have no separate license files or attribution metadata. Preserve binary dependency
  hashes and runtime IDs, especially `gcapi3`.
