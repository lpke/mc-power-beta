package local.luke.power.ui;

import java.util.List;

/** RGB, opacity and swatches share one picker acceptance and discard boundary. */
record ColorPickerSnapshot(int rgb, double opacity, List<String> swatches) {
  ColorPickerSnapshot { swatches = List.copyOf(swatches); }
  boolean changed(int color, double alpha, List<String> colors, boolean invalidText) {
    return invalidText || rgb != color || Double.compare(opacity, alpha) != 0 || !swatches.equals(colors);
  }
}
