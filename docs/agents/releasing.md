# Release delivery

Read [the release procedure](../releases.md) before preparing or publishing a release.
A user request listing changes and a release version authorizes implementation, testing,
commit, push, publishing and the instance update described in AGENTS.md. A code-only
request does not authorize publication.

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
  immutable build version, tag, assets and checksums, and show the original number
  beside a different display version. Both numbers may resolve to the same download.
- `python3 tools/release.py --check` validates release notes and versioning.
  The publish command builds and tests sequentially, verifies the export, publishes
  GitHub release assets and notes, and updates/deploys the website from the same data.
  Do not hand-edit generated website release details or overwrite published artifacts.
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
