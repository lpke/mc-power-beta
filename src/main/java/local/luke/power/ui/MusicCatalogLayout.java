package local.luke.power.ui;

/** Catalog render and pointer bounds, including the compact two-line layout. */
record MusicCatalogLayout(int left, int right, boolean inline, int rowHeight, int useWidth, int duplicateWidth) {
  static final int NAME_INSET = 8;
  static MusicCatalogLayout at(int left, int width, int useWidth, int duplicateWidth) {
    int use = Math.max(32, Math.min(48, useWidth));
    int duplicate = Math.max(58, Math.min(80, duplicateWidth));
    boolean inline = width >= NAME_INSET + 140 + 4 + use + 4 + duplicate + 4 + 20;
    return new MusicCatalogLayout(left, left + width, inline, inline ? 28 : 48, use, duplicate);
  }
  int activeX() { return left + 2; }
  int nameX() { return left + NAME_INSET; }
  int nameWidth() { return (inline ? useX() - 4 : right) - nameX(); }
  int useX() { return duplicateX() - 4 - useWidth; }
  int duplicateX() { return deleteX() - 4 - duplicateWidth; }
  int deleteX() { return right - 20; }
  int controlsY(int rowY) { return rowY + (inline ? 5 : 25); }
}
