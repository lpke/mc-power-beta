package local.luke.power.ui;

/** Native event timestamps keep queued clicks independent of frame processing time. */
final class WaypointRowClicks {
  private String waypoint = "";
  private long pressed;
  private boolean released;

  boolean press(String id, long eventNanos) {
    long elapsed = eventNanos - pressed;
    boolean edit = released && waypoint.equals(id) && elapsed > 0 && elapsed <= 250_000_000L;
    if (edit) clear();
    else { waypoint = id; pressed = eventNanos; released = false; }
    return edit;
  }

  void release() { released = true; }
  void clear() { waypoint = ""; pressed = 0; released = false; }
}
