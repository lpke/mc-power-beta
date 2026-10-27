package local.luke.power.ui;

/** Catalog render and pointer bounds, including the compact two-line layout. */
record MusicCatalogLayout(int left, int right, boolean inline, int rowHeight, int duplicateWidth) {
  static final int PADDING = 8, RADIO_WIDTH = 18;
  static MusicCatalogLayout at(int left, int width, int duplicateWidth) {
    int duplicate = Math.max(58, Math.min(80, duplicateWidth));
    boolean inline = width >= PADDING * 2 + RADIO_WIDTH + 8 + 140 + 4 + duplicate + 4 + 20;
    return new MusicCatalogLayout(left, left + width, inline, inline ? 28 : 48, duplicate);
  }
  int useX() { return left + PADDING; }
  int useWidth() { return RADIO_WIDTH; }
  int nameX() { return useX() + useWidth() + 8; }
  int nameWidth() { return (inline ? duplicateX() - 4 : right - PADDING) - nameX(); }
  int duplicateX() { return deleteX() - 4 - duplicateWidth; }
  int deleteX() { return right - PADDING - 20; }
  int controlsY(int rowY) { return rowY + (inline ? 5 : 25); }
}
