package local.luke.power.ui;

/** Catalog render and pointer bounds, including the compact two-line layout. */
record MusicCatalogLayout(int left, int right, boolean inline, int rowHeight, int duplicateWidth) {
  static MusicCatalogLayout at(int left, int width, int duplicateWidth) {
    int duplicate = Math.max(58, Math.min(80, duplicateWidth));
    boolean inline = width >= 48 + duplicate + 24 + 140;
    return new MusicCatalogLayout(left, left + width, inline, inline ? 28 : 48, duplicate);
  }
  int useX() { return left + 2; }
  int useWidth() { return 42; }
  int nameX() { return left + 49; }
  int nameWidth() { return (inline ? duplicateX() - 4 : right - 4) - nameX(); }
  int duplicateX() { return deleteX() - 4 - duplicateWidth; }
  int deleteX() { return right - 20; }
  int controlsY(int rowY) { return rowY + (inline ? 5 : 25); }
}
