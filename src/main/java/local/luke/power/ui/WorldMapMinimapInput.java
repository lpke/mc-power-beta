package local.luke.power.ui;

import java.util.function.Consumer;
import local.luke.power.map.MapToggle;

/** The settings preview already belongs to an open map; left presses stay local. */
final class WorldMapMinimapInput {
  private WorldMapMinimapInput() {}

  static void press(int button, int middleAction, Runnable resetZoom,
      Consumer<MapToggle> toggleSetting, Runnable toggleHeads) {
    if (button == 1) resetZoom.run();
    else if (button == 2) {
      MapToggle toggle = MapToggle.ALL.get(middleAction);
      if (toggle == MapToggle.HEADS) toggleHeads.run();
      else toggleSetting.accept(toggle);
    }
  }
}
