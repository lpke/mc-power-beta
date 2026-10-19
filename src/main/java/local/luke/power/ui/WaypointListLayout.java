package local.luke.power.ui;

/** Shared drawing and hit-test bounds, capped to the Options screen's 16:9 content area. */
record WaypointListLayout(int left, int width, int listTop, int listBottom) {
  static WaypointListLayout at(int width, int height) {
    int span = Math.max(1, Math.min(width, height * 16 / 9) - 16);
    return new WaypointListLayout((width - span) / 2, span, 72, Math.max(72, height - 55));
  }

  int right() { return left + width; }
  int sortWidth() { return Math.min(92, Math.max(58, width / 5)); }
  int filterWidth() { return Math.min(102, Math.max(66, width / 4)); }
  int sortX() { return right() - filterWidth() - sortWidth() - 4; }
  int filterX() { return right() - filterWidth(); }
  int searchWidth() { return sortX() - left - 4; }
  int createWidth() { return Math.min(92, Math.max(72, width / 4)); }
  int createX() { return right() - createWidth(); }
  int setWidth() { return width - createWidth() - 4; }
  int actionWidth() { return (width - 12) / 4; }
  int actionX(int index) { return left + index * (actionWidth() + 4); }
  int setRows(int height, int count) { return Math.min(count, Math.max(1, (height - 76) / 20)); }
}
