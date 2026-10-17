package local.luke.power.ui;

import java.util.*;
import java.util.function.ToIntFunction;

/** Shared wrapping geometry keeps the preview controls reachable at narrow GUI sizes. */
record HudEditorToolbar(List<Toggle> toggles, int controlsTop) {
  record Toggle(String id, int x, int y, int width) {}
  static HudEditorToolbar at(int width, ToIntFunction<String> measure) {
    List<Toggle> buttons = new ArrayList<>(); int x = 14 + measure.applyAsInt("Preview toggles:"), y = 54;
    for (var entry : new String[][]{{"f3","F3"},{"background","Bg"},{"toast","Toast"},{"chat","Chat"},{"tweaks","Active tweaks"},{"snap","Snap"}}) {
      int span = measure.applyAsInt(entry[1] + ": Off") + 12;
      if (x + span > width - 10) { x = 10; y += 22; }
      buttons.add(new Toggle(entry[0], x, y, span)); x += span + 4;
    }
    return new HudEditorToolbar(List.copyOf(buttons), y + 25);
  }
}
