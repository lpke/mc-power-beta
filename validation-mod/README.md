# Runtime validation fixture

Install only in a disposable Power Beta instance. Never package this JAR.
The separate building-validation fixture creates its own laboratory world and
checks the creative, building, free-look and inventory features.

Write one command to `power-beta-validation.command` in the game's directory:

- `audit`: validate every setting's schema, current value, default and cycle direction.
- `roundtrip`: save, reread and restore settings across every backend.
- `screens`: render every settings page at four resolutions and check OpenGL errors.
- `audio`: verify loaded sound/music resources and runtime volume/mute behavior.
- `options PAGE`, `pause`, `title`: open screens for physical input/screenshots.
- `dump`: export the runtime settings catalog.
- `defaults`: apply the pack defaults in the disposable instance.

Results are appended to `power-beta-validation.log`. Tests can change settings.
