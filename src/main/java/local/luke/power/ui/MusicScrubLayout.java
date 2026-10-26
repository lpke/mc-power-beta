package local.luke.power.ui;

/** Shared drawing and hit bounds. Standalone tracks reserve equal space on both sides. */
record MusicScrubLayout(int x, int width) {
  static final int THUMB_WIDTH = 2;

  int thumbLeft(double fraction) {
    return x + (int) Math.round(Math.max(0, Math.min(1, fraction)) * (width - THUMB_WIDTH));
  }

  double fraction(int pointerX) {
    return Math.max(0, Math.min(1,
        (pointerX - x - THUMB_WIDTH / 2.0) / Math.max(1, width - THUMB_WIDTH)));
  }

  static MusicScrubLayout at(int left, int width, boolean label) {
    int reserved = label ? Math.min(160, width / 2) : width;
    return new MusicScrubLayout(left + width - reserved + (label ? 0 : 40),
        reserved - (label ? 40 : 80));
  }
}
