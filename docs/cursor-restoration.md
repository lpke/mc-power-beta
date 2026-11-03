# Native cursor restoration

Power Beta restores the native pointer inside the normal grab transition. It adds
no sleeps, GPU completion waits, presentation round trips or repeated recentering.
Cursor release must preserve both the requested position and deliberate later input.

`CursorRelease` and `InventoryLookMouseMixin` keep the Linux boundary in X event
order. The requested center or inventory-look origin is published before the release
poll. An ordered native warp separates older gameplay motion from GUI input.
Opening or navigating a menu never starts a centering loop.

Inventory-looking releases also preserve GUI input queued before that warp. Baseline
markers before pending events and at the warp let a following quick drag retain its
deltas without treating the warp as movement. Polling still reports the requested
visible position immediately. Gameplay-to-menu releases discard older gameplay
motion. Screen, world and player ownership prevent stale markers from moving a new
screen's logical cursor.

## Compositor ordering

Xwayland forwards its hidden pointer position as a Wayland surface hint. KWin can
leave that hint behind a rendered buffer's GPU fence, then restore an older hint
when the lock is destroyed. LWJGL, XQueryPointer and AWT may all report the requested
center while the composited pointer remains elsewhere. See
[KDE bug 520910](https://bugs.kde.org/show_bug.cgi?id=520910).

This requires a compositor or Xwayland correction. The pack does not replace those
components or infer a fix from their version strings. Finishing GPU work or waiting
for several frames on the game thread is not an acceptable workaround. Those waits
can stall rendering and input on every inventory-look release.

A compositor correction must also preserve deliberate movement after the cursor
becomes visible. Deferring lock destruction until the next client buffer can lose
input at ordinary frame caps if Xwayland sends no surface commit after destruction.
Test the complete compositor/Xwayland pair, including clients that do not repaint
immediately after releasing the pointer.

## Validation

Use a muted disposable instance and an isolated compositor. Observe its actual
pointer independently of the X server, then verify native hover/click destinations.
Exercise chest, inventory, pause and chat transitions, inventory-look origin
restoration, rapid gestures, fullscreen, focus changes and busy GPU frames with both
mouse helpers. Preserve immediate deliberate movement and free menu navigation.

Measure release-call duration, actual swaps and matched deliberate input separately
from visible compositor frames. A frame's scheduled presentation timestamp alone
does not prove that it appeared before the input; record when presentation feedback
arrived too. Include cold releases and ordinary 30/60/high frame caps. Disable verbose
protocol logs and other builds for final timing comparisons.

Java tests cover logical event ordering. They cannot establish compositor behavior
or physical input-to-photon latency. Keep tested stacks, unsupported configurations
and unmeasured claims explicit in the release review.
