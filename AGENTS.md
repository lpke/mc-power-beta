# Power Beta development

## Working agreement

The usual request lists changes and a release version. That authorizes the full job:
implement, test, commit, push, publish that version through the documented publisher,
and update the primary Prism instance, **Beta 1.7.3 babric**. Preserve preferences,
existing worlds, player data, music and recovery records. Apply required migrations
with the shared backed-up converters. Other requests keep Git read-only unless the
user explicitly authorizes Git changes. Never publish without release authorization.

Use plain, short progress updates and state the files/approach before substantial
edits. Finish authorized work without repeatedly asking for permission. Report facts
and test limits. Do not commit personal paths, credentials, logs or runtime reports.

Subagents are optional when they save meaningful time. Use only **GPT-6.1 Sol with
high reasoning**, or **GPT-6 Astra with xhigh reasoning**. Astra requires the user's
explicit request. Sol is encouraged for focused audits, research and checking how
new features interact with existing settings. Give agents separate ownership and
review their changes. Never run Loom builds concurrently, including in subagents.

## Repository and safety

- Java 21 and Python 3. `python3 tools/build_pack.py --offline` builds sequentially,
  runs tests and exports Prism. Omit `--offline` only when dependencies need fetching.
- `src/` owns integration, settings/UI, audio and overlays. `input-api/` bridges
  modules. Creative, building and WorldEdit have independent builds; other modules
  use `source-build/modules.json`. Do not build from their upstream Gradle files.
- PowerConfig is the sole settings writer, at `config/power-beta.json`. Keep stable
  IDs and the `gcapi3` binary identity. Gameplay snapshots queue immutable saves;
  input/render paths never wait for disk. Keep unsupported data protected.
- Test only in disposable clones outside the normal instance list. Never launch or
  edit an active instance. Use silent audio routing and stop only verified processes
  you started. Preserve all user data and verified backups.
- Plan toggleable features around their actual consumers. Disabled or currently
  irrelevant features stop their calculations, scans and preparation, even for
  small costs. Retain background work only when its player benefit justifies it,
  such as recording explored terrain for a later map visit.
- Inventory writes retain leases, complete native item data, durable journals,
  callback validation and owned rollback. UI drafts never edit gameplay inventory.
- Read the relevant instructions below before changing that area. Reuse existing
  controls, pickers, selectors, dropdowns, text input, geometry and isolated drafts.
  Keep rendering/input bounds identical and verify small GUI layouts.

## Task instructions

| When working on | Read |
| --- | --- |
| Builds, disposable runtime tests, cleanup or instance updates | [Builds and testing](docs/agents/build-testing.md) |
| Settings, defaults, migrations or UI | [Settings and shared UI](docs/agents/settings-ui.md) |
| Inventory, crafting, sorting or recovery | [Inventory and recovery](docs/agents/inventory-recovery.md) |
| Any new/imported sound or audio behavior | [Audio and decoder validation](docs/agents/audio.md) |
| Rendering, water lighting, HUDs or maps | [Rendering and maps](docs/agents/rendering-maps.md) |
| Gameplay rules, commands or input | [Gameplay and input](docs/agents/gameplay-input.md) |
| Preparing, publishing or installing a release or re-release | [Release delivery](docs/agents/releasing.md), then [the publisher procedure](docs/releases.md) |
| Starting or completing a requested release or re-release | [Owner review HTML](docs/agents/release-report.md), using the `$html` skill |

The website keeps its normal GitHub-derived changelog. Detailed owner review belongs
in the separate HTML checklist, not in an extra website release-notes section.
Re-releases must update both changelog destinations and the existing review HTML,
with re-released items clearly marked.
Keep lasting instructions in these documents rather than appending session history.
