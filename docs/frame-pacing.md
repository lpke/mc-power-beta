# Capped frame presentation

Beta renders after its early Display.update swap. Its 20 Hz client ticks therefore
land between the preceding frame limiter and the next swap: at a 160 FPS cap,
ordinary tick work lengthens one presented interval and shortens the next.

Power Beta keeps the complete native Display.update at its original location,
including input/event polling, resize bookkeeping and callbacks. Only buffer swaps
requested inside those main-thread run-loop updates wait until after the existing
Post-render limiter. The scope closes before flushing, so the flush cannot defer
itself. Other threads, unscoped display calls, and uncapped rendering retain native
swaps. No preference or new limiter is added.

## Validation and limits

Compare actual Display.swapBuffers entry and return intervals in reversed paired
trials. Check tick-bearing frames and their successors separately to distinguish
periodic presentation variation from longer rendering or simulation work.
Terrain rendering plus client ticks can exceed a frame's budget; changing
presentation order cannot remove that work. Software swap timings do not measure
physical scanout. Keep measured results and driver-specific limits in the owner
review report.

The validation mod provides `presentation-470 legacy|native` comparison controls,
real-swap counters, fullscreen and unscoped-update probes, saved VSync requests,
and bounded `smoothness-470` action profiles. Validate capped and uncapped modes,
GUI typing/drags, focus loss/regain, resize, fullscreen and F7 transitions. Native
polling/resize behavior must stay in place when adapting this code.
