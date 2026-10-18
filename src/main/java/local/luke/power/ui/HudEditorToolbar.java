package local.luke.power.ui;

import java.util.*;
import java.util.function.ToIntFunction;

/** Shared wrapping geometry keeps the preview controls reachable at narrow GUI sizes. */
record HudEditorToolbar(List<Toggle> toggles, int selectorWidth, boolean compact, int labelX, int labelY, int controlsTop) {
  static final String LABEL = "Toggle in preview:";
  record Toggle(String id, int x, int y, int width) {}
  static HudEditorToolbar at(int width, int selectorWidth, ToIntFunction<String> measure) {
    return at(width, selectorWidth, measure, false);
  }
  static HudEditorToolbar at(int width, int selectorWidth, ToIntFunction<String> measure, boolean panelButtons) {
    var entries = new String[][]{{"f3","F3"},{"background","Bg"},{"toast","Toast"},{"chat","Chat"},{"tweaks","Active tweaks"},{"snap","Snap"}};
    int limit = width - 10 - (panelButtons ? 44 : 0);
    int total = measure.applyAsInt(LABEL) + 4;
    for (var entry : entries) total += measure.applyAsInt(entry[1] + ": Off") + 16;
    boolean compact = panelButtons || 10 + selectorWidth + 12 + total - 4 > limit;
    if (compact) {
      int compactWidth = 0;
      for (var entry : entries) compactWidth += measure.applyAsInt(entry[0].equals("tweaks") ? "Tweaks" : entry[1]) + 18;
      int inlineSelector = Math.min(selectorWidth, limit - 10 - 12 - compactWidth + 4);
      boolean inline = inlineSelector >= 120;
      selectorWidth = inline ? inlineSelector : Math.min(selectorWidth, limit - 10);
      List<Toggle> buttons = new ArrayList<>(); int x = inline ? 10 + selectorWidth + 12 : 10, y = inline ? 30 : 54;
      for (var entry : entries) {
        String label = entry[0].equals("tweaks") ? "Tweaks" : entry[1];
        int span = measure.applyAsInt(label) + 14;
        if (x + span > (y == 30 ? limit : width - 10)) { x = 10; y += 22; }
        buttons.add(new Toggle(entry[0], x, y, span)); x += span + 4;
      }
      return new HudEditorToolbar(List.copyOf(buttons), selectorWidth, true, -1, -1, y + 25);
    }
    int labelX = 10 + selectorWidth + 12, y = 30;
    List<Toggle> buttons = new ArrayList<>(); int x = labelX + measure.applyAsInt(LABEL) + 4;
    for (var entry : entries) {
      int span = measure.applyAsInt(entry[1] + ": Off") + 12;
      if (x + span > width - 10) { x = 10; y += 22; }
      buttons.add(new Toggle(entry[0], x, y, span)); x += span + 4;
    }
    return new HudEditorToolbar(List.copyOf(buttons), selectorWidth, false, labelX, 35, y + 25);
  }
}
