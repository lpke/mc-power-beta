package local.luke.power.ui;

import java.util.*;
import java.util.function.ToIntFunction;

/** Shared wrapping geometry keeps the preview controls reachable at narrow GUI sizes. */
record HudEditorToolbar(List<Toggle> toggles, int labelX, int labelY, int controlsTop) {
  static final String LABEL = "Toggle in preview:";
  record Toggle(String id, int x, int y, int width) {}
  static HudEditorToolbar at(int width, int selectorWidth, ToIntFunction<String> measure) {
    return at(width, selectorWidth, measure, false);
  }
  static HudEditorToolbar at(int width, int selectorWidth, ToIntFunction<String> measure, boolean compact) {
    var entries = new String[][]{{"f3","F3"},{"background","Bg"},{"toast","Toast"},{"chat","Chat"},{"tweaks","Active tweaks"},{"snap","Snap"}};
    if (compact) {
      List<Toggle> buttons = new ArrayList<>(); int x = 10, y = 54;
      for (var entry : entries) {
        String label = entry[0].equals("tweaks") ? "Tweaks" : entry[1];
        int span = measure.applyAsInt(label) + 14;
        if (x + span > width - 10) { x = 10; y += 22; }
        buttons.add(new Toggle(entry[0], x, y, span)); x += span + 4;
      }
      return new HudEditorToolbar(List.copyOf(buttons), -1, -1, y + 25);
    }
    int total = measure.applyAsInt(LABEL) + 4;
    for (var entry : entries) total += measure.applyAsInt(entry[1] + ": Off") + 16;
    boolean inline = 10 + selectorWidth + 12 + total - 4 <= width - 10;
    int labelX = inline ? 10 + selectorWidth + 12 : 10, y = inline ? 30 : 54;
    List<Toggle> buttons = new ArrayList<>(); int x = labelX + measure.applyAsInt(LABEL) + 4;
    for (var entry : entries) {
      int span = measure.applyAsInt(entry[1] + ": Off") + 12;
      if (x + span > width - 10) { x = 10; y += 22; }
      buttons.add(new Toggle(entry[0], x, y, span)); x += span + 4;
    }
    return new HudEditorToolbar(List.copyOf(buttons), labelX, (inline ? 30 : 54) + 5, y + 25);
  }
}
