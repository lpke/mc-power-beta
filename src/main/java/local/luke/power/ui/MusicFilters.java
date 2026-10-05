package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Wrap full tab labels at small GUI sizes without clipping the track list. */
public final class MusicFilters {
  public record Tab(String id, String label, int x, int y, int width) {}
  public record Layout(List<Tab> tabs, int folderX, int folderY, int folderWidth, int height) {}
  public static Layout layout(int left, int top, int width, boolean folder) {
    String[] ids = {"active", "", "custom", "folder", "favourites", "presets"};
    String[] labels = {"Active", "Everything", "All folders", "Folders", "Favourites", "Presets"};
    int[] sizes = {42,66,68,50,66,48};
    List<Tab> tabs = new ArrayList<>();
    int x = left, y = top;
    for (int i = 0; i < ids.length; i++) {
      if (x + sizes[i] > left + width) { x = left; y += 22; }
      tabs.add(new Tab(ids[i], labels[i], x, y, sizes[i]));
      x += sizes[i] + 2;
    }
    if (folder && left + width - x < 80) { x = left; y += 22; }
    return new Layout(List.copyOf(tabs), x, y, Math.min(180, left + width - x), y - top + 18);
  }
}
