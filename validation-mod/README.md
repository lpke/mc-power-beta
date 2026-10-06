# Runtime checks

Build after the root JAR with `./gradlew -p validation-mod --no-daemon build`.
Install the resulting validation JAR only in a disposable Power Beta clone.
Fixtures can replace settings and edit worlds. They are never part of the export.

Write a command to `.minecraft/power-beta-validation.command`. Results append to
`power-beta-validation.log`; wait for `COMMAND DONE` and inspect failures.

- `audit`, `roundtrip`, `screens`: schema, save/restore and multi-resolution checks.
- `movement-keys`, then `new-world`, `movement-setup`, `movement-checks`: key mapping,
  container reach and item conservation. `movement-inventory`, `movement-chest` and
  `movement-state` support physical-input checks in the disposable client.
- `audio-maintenance`: preset-pool isolation, folder browsing, draft confirmation,
  duplicate imports, single-group expansion and FFmpeg detection. This fixture
  expects FFmpeg on PATH; the unit suite covers an unavailable executable.
- `audio-presets-setup`, `audio-presets-ui`, `audio-presets-finish`: preset editing.
  Playback steps in `AudioPresetChecks` need separate ticks between commands.
- `audio-groups-setup`, `audio-groups-ui`, `audio-groups-menu`,
  `audio-groups-finish`: group gains, layout and pause controls.
- `new-world`: create an isolated test world; `options PAGE`, `pause`, `title`
  open screens for inspection. `quit` closes the test client normally.

See the dispatch in `Validation.java` for focused inventory, commands, navigation
and input fixtures. Some older checks assume a particular setup; inspect their
preconditions before running them. Physical input tests require focused game
input and both modifier release orders.
