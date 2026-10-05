package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Tabs wrap with their selector, which stays beside the active tab. */
public final class MusicFilters {
  public record Tab(String id, String label, int x, int y, int width) {}

  public record Layout(List<Tab> tabs, int folderX, int folderY, int folderWidth, int height) {}

  public static Layout layout(int left, int top, int width, String selector) {
    String[] ids = {"active", "", "favourites", "presets", "custom", "folder"};
    String[] labels = {"Active", "Everything", "Favourites", "Presets", "All folders", "Folders"};
    int[] sizes = {42, 66, 66, 48, 68, 50};
    List<Tab> tabs = new ArrayList<>();
    int x = left, y = top, selectorX = left, selectorY = top, selectorWidth = 0;
    for (int i = 0; i < ids.length; i++) {
      boolean selected = !selector.isEmpty() && ids[i].equals(selector);
      int extra = selected ? Math.min(120, width - sizes[i] - 2) + 2 : 0;
      if (x + sizes[i] + extra > left + width) {
        x = left;
        y += 22;
      }
      tabs.add(new Tab(ids[i], labels[i], x, y, sizes[i]));
      x += sizes[i] + 2;
      if (selected) {
        selectorX = x;
        selectorY = y;
        selectorWidth = extra - 2;
        x += extra;
      }
    }
    return new Layout(List.copyOf(tabs), selectorX, selectorY, selectorWidth, y - top + 18);
  }
}
