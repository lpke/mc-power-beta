package local.luke.power.ui;

/** Visit-local split position; resize never removes the controls or the preview. */
final class EditorSplit {
  private double fraction = .45;
  private boolean dragging;
  int bottom(int top, int previewBottom) {
    int available = Math.max(0, previewBottom - top - 12);
    int minimum = Math.min(23, available), maximum = Math.max(minimum, available - 48);
    return top + Math.max(minimum, Math.min(maximum, (int)Math.round(available * fraction)));
  }
  void start(int y, int top, int previewBottom) { dragging = true; move(y, top, previewBottom); }
  void move(int y, int top, int previewBottom) {
    int available = Math.max(1, previewBottom - top - 12);
    fraction = Math.max(0, Math.min(1, (y - 5d - top) / available));
  }
  void stop() { dragging = false; }
  boolean dragging() { return dragging; }
}
