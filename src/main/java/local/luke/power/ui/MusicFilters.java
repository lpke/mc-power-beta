package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Catalog tabs and actions share a row when their actual bounds fit. */
public final class MusicFilters {
  public record Tab(String id, String label, int x, int y, int width) {}
  public record Layout(List<Tab> tabs, int foldersX, int reloadX, int actionsY, int height) {}

  public static Layout layout(int left, int top, int width, String selector) {
    String[] ids = {"library", "playlists", "favorites"};
    String[] labels = {"Library", "Playlists", "Favorites"};
    int[] sizes = {50, 60, 66};
    List<Tab> tabs = new ArrayList<>();
    int x = left;
    for (int i = 0; i < ids.length; i++) {
      tabs.add(new Tab(ids[i], labels[i], x, top, sizes[i]));
      x += sizes[i] + 2;
    }
    int foldersX = left + width - 104;
    int actionsY = foldersX >= x + 6 ? top : top + 22;
    int height = selector.equals("playlists") ? 18 : actionsY - top + 18;
    return new Layout(List.copyOf(tabs), foldersX, left + width - 48, actionsY, height);
  }
}
