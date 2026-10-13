package local.luke.power.ui;

/** Shared drawing and hit bounds. Standalone tracks reserve equal space on both sides. */
record MusicScrubLayout(int x, int width) {
  static MusicScrubLayout at(int left, int width, boolean label) {
    int reserved = label ? Math.min(160, width / 2) : width;
    return new MusicScrubLayout(left + width - reserved + (label ? 0 : 40),
        reserved - (label ? 40 : 80));
  }
}
