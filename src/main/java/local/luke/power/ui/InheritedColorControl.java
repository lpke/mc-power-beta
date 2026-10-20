package local.luke.power.ui;

/** Rendering and clicks share the mode button and optional ordinary color-picker bounds. */
record InheritedColorControl(int modeWidth, int colorX, int colorWidth) {
  static InheritedColorControl at(int x, int width, boolean custom) {
    int gap = Math.min(4, Math.max(0, width - 2));
    int mode = custom ? Math.max(1, Math.min(68, (width - gap) / 2)) : width;
    return new InheritedColorControl(mode, x + mode + gap,
        custom ? Math.max(0, width - mode - gap) : 0);
  }

  boolean colorAt(int x) { return colorWidth > 0 && x >= colorX && x < colorX + colorWidth; }
}
