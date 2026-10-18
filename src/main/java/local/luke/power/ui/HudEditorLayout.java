package local.luke.power.ui;

/** One geometry model for rendering, input, scrolling and preview coordinates. */
record HudEditorLayout(boolean horizontal, int controlsLeft, int controlsTop, int controlsWidth,
    int controlsBottom, int previewLeft, int previewTop, int previewWidth, int previewBottom) {
  static boolean horizontal(int width, int height) {
    return width >= 640 && (width >= height * 1.45 || width >= 800 && width >= height * 1.2);
  }
  static boolean compact(int width, int height) {
    return width < 800 || height < 400;
  }
  static HudEditorLayout at(int width, int height, int split) {
    return at(width, height, split, 55);
  }
  static HudEditorLayout at(int width, int height, int split, int top) {
    return at(width, height, split, top, false);
  }
  static HudEditorLayout at(int width, int height, int split, int top, boolean hidden) {
    return at(width, height, split, top, hidden, false, height - 53);
  }
  static HudEditorLayout at(int width, int height, int split, int top, boolean hidden, boolean previewHidden, int bottom) {
    int left = 10, right = width - 10;
    if (previewHidden && compact(width, height))
      return new HudEditorLayout(horizontal(width, height), left, top, right - left, bottom, left, bottom, right - left, bottom);
    if (hidden && compact(width, height))
      return new HudEditorLayout(horizontal(width, height), left, top, right - left, top, left, top, right - left, bottom);
    if (horizontal(width, height)) {
      int settings = Math.max(280, Math.min(Math.min(440, width - 312), split));
      return new HudEditorLayout(true, left, top, settings, bottom,
          left + settings + 12, top, right - left - settings - 12, bottom);
    }
    int settingsBottom = Math.max(top + 47, Math.min(bottom - 60, split));
    return new HudEditorLayout(false, left, top, right - left, settingsBottom,
        left, settingsBottom + 12, right - left, bottom);
  }
  double scale(int screenWidth, int screenHeight) {
    return Math.min((previewWidth - 4d) / screenWidth,
        Math.max(1, previewBottom - previewTop - 4d) / screenHeight);
  }
  double canvasX(int screenWidth, int screenHeight) {
    return previewLeft + (previewWidth - screenWidth * scale(screenWidth, screenHeight)) / 2;
  }
  double canvasY(int screenWidth, int screenHeight) {
    return previewTop + (previewBottom - previewTop - screenHeight * scale(screenWidth, screenHeight)) / 2;
  }
}
