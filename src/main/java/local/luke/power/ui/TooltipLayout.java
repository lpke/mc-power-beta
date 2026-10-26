package local.luke.power.ui;

/** Text origin and outer tooltip bounds with equal four-pixel padding. */
record TooltipLayout(int x, int y, int width, int height) {
  static TooltipLayout at(int screenWidth, int screenHeight, int textWidth, int lines, int mx, int my) {
    int height = Math.max(1, lines) * 11 + 5;
    int x = Math.max(5, Math.min(mx + 10, screenWidth - textWidth - 10));
    int y = Math.max(5, Math.min(my + 12, screenHeight - height - 1));
    return new TooltipLayout(x - 4, y - 4, textWidth + 8, height);
  }
}
