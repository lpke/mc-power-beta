package local.luke.power.ui;

/** Exact-entry input and check button share the original control's outer bounds. */
record InlineValueGeometry(int x, int y, int width) {
  static final int HEIGHT = 18, CHECK_WIDTH = 18, GAP = 2;
  int checkWidth() { return Math.min(CHECK_WIDTH, Math.max(0, width)); }
  int checkX() { return x + width - checkWidth(); }
  int inputWidth() { return Math.max(0, width - checkWidth() - GAP - 2); }
  boolean contains(int mx, int my, int clipTop, int clipBottom) {
    return mx >= x && mx < x + width && my >= Math.max(y, clipTop) && my < Math.min(y + HEIGHT, clipBottom);
  }
  boolean checkContains(int mx, int my, int clipTop, int clipBottom) {
    return contains(mx, my, clipTop, clipBottom) && mx >= checkX();
  }
}
