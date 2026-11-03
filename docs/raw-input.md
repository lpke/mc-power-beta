# Raw mouse input

Raw input polls JInput on a worker and applies relative movement on the render
thread. `RawMouseMotion` transfers both signed axes together with one atomic
exchange. A device sample belongs to exactly one frame. Cursor grabbing and GUI
entry discard pending movement through the same accumulator.

The game thread publishes raw capture only for the current world/player with an
active window, grabbed cursor and no GUI. The worker never queries Minecraft or
LWJGL state. It continues draining devices while capture is suspended, retaining
its existing 1 ms pacing. Each newly owned session or replacement device list
drains its first polling batch before recording new counts, so buffered desktop
movement cannot become the first camera sample. No extra wait or sleep is added.
Grab, screen, world and focus boundaries retire the sample destination before
publishing another. The Linux focus adapter observes each accepted native focus
edge, including an out/in pair in one polling batch. Other platforms retain the
main-frame focus check. GUI looking continues through the native LWJGL queue.
Accepted Linux focus remains authoritative in legacy fullscreen, whose native
active predicate can stay true after focus loss. Window creation and destruction
reset this state, and accepted edges are recorded while raw input is disabled.

`RawInputWorker` owns at most one device poll. Disabling raw input stops the worker
and interrupts its sleep; disabled bootstrap and world changes do not start it or
scan controllers. An immediate re-enable either keeps the existing worker or
starts its successor after the old exit decision. Device discovery publishes one
immutable list, so polling never sees a half-populated scan. Hardware polling
errors still terminate that worker without an automatic restart loop.
First JInput initialization and controller discovery run on a separate daemon,
never on the game thread or the active 1 ms polling worker. The native mouse
helper remains active while a first request is pending. The game thread accepts
only the current enable/discovery generation and handles notifications or the
no-device/error fallback there. Disabling invalidates late results without joining
the discovery thread. Successful immutable device snapshots are reused for world
changes and re-enabling; an explicit rescan runs separately while current raw
sampling continues. Switching helpers retires counts and drains the new device
session before accepting motion, so native and raw deltas are not both applied.
Falling back from the raw helper discards unread LWJGL relative counters once,
without polling or consuming its queued button events. Already-native input keeps
its current relative movement.
Disabling raw input or changing cursor ownership retires the current accumulator.
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

`raw-ownership-473` uses synthetic paired counts in a muted, windowed disposable
Linux world. It stops that instance's physical-device worker, checks the real raw
helper and native grab transitions, dispatches synthetic accepted Linux focus
notifications, races a late producer against regrab, and checks native GUI polling
without consuming a queued click. It restores the worker, cursor, screen and
camera. The report labels its synthetic source; this does not measure physical
mouse latency or replace real window-manager focus and compositor tests.

The saved Raw input preference still controls startup. The runtime toggle remains
temporary. While discovery is pending, another toggle cancels that request and
leaves the native helper active. Discovery never writes settings.

`camera-jitter-470 stage`, `orbit`, `stop`, `restore` prepare a loaded log pillar,
use native Survival movement and turning, and capture the rendered GL camera.
The scene disables bobbing to isolate interpolation and restores its entry state.
Repeat with bobbing enabled when investigating visible movement. Compare block
outline placement with the rendered block, and report projection error separately
from frame duration. An automated exact-aim orbit does not reproduce every detail
of human mouse tracking or a particular device's polling schedule.
