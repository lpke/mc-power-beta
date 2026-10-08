# Contributing

Read [AGENTS.md](AGENTS.md) before changing the pack. It describes settings,
input, audio, world data and inventory constraints that tests must preserve.

Use Java 21 and Python 3. `python3 tools/build_pack.py` builds the modules in order,
runs their tests and creates the Prism ZIP. Never run Loom builds concurrently.
Test game changes in a disposable instance, with no personal worlds or accounts
in reports or screenshots. Validation fixtures must never enter the shipped pack.

Keep changes focused and describe the resulting behaviour and tests in the pull
request. Add user-visible changes to Unreleased in [CHANGELOG.md](CHANGELOG.md).
Follow [the release guide](docs/releases.md) when preparing an approved release.
