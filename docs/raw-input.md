# Raw mouse input

Raw input polls JInput on a worker and applies relative movement on the render
thread. `RawMouseMotion` transfers both signed axes together with one atomic
exchange. A device sample belongs to exactly one frame. Cursor grabbing and GUI
entry discard pending movement through the same accumulator.

`RawInputWorker` owns at most one device poll. Disabling raw input stops the worker
and interrupts its sleep; disabled bootstrap and world changes do not start it or
scan controllers. An immediate re-enable either keeps the existing worker or
starts its successor after the old exit decision. Device discovery publishes one
immutable list, so polling never sees a half-populated scan. Hardware polling
errors still terminate that worker without an automatic restart loop.
Disabling raw input or grabbing the cursor retires the current accumulator.
Each poll captures its destination before reading the device, so a late sample
finishes in the retired accumulator and cannot move the new input session.

Do not replace this with separate reads and resets, even with volatile counters.
A producer can read the old count, pause while rendering drains it, then write
that old count plus its new sample back. The next frame repeats movement already
applied. Two separate atomic axes would also allow one diagonal sample to be split
across frames. Keep the pair atomic and do not add input smoothing or change
sensitivity to hide a broken handoff.

The official Java 26.3 reference is its
[client JAR](https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar),
SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`. `MouseHandler` accumulates mouse
movement on its event path and consumes it before rendering. `Entity.turn` and
Beta's native turn method both advance current and previous view angles together.
Camera position interpolation remains separate from mouse input and simulation.

For regressions, run the controls unit tests through the sequential utility build.
The concurrency check verifies exact counts and paired axes while producers race
frame drains. In a muted disposable native client, `camera-jitter-470 raw` exercises
the loaded mouse helper under contention. This is a race test, not a measurement
of how often it occurs with a physical mouse.

`camera-jitter-470 stage`, `orbit`, `stop`, `restore` prepare a loaded log pillar,
use native Survival movement and turning, and capture the rendered GL camera.
The scene disables bobbing to isolate interpolation and restores its entry state.
Repeat with bobbing enabled when investigating visible movement. Compare block
outline placement with the rendered block, and report projection error separately
from frame duration. An automated exact-aim orbit does not reproduce every detail
of human mouse tracking or a particular device's polling schedule.
