package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Catalog tabs and actions share a row when their actual bounds fit. */
public final class MusicFilters {
  static final int CONTENT_GAP = 8;
  public record Tab(String id, String label, int x, int y, int width) {}
  public record Layout(List<Tab> tabs, int addFolderX, int reloadX, int actionsY, int height) {}

  public static Layout layout(int left, int top, int width, String selector) {
    String[] ids = {"library", "playlists", "favorites"};
    String[] labels = {"Library", "Playlists", "Favorites"};
    int extra = Math.min(4, Math.max(0, (width - 180) / 3));
    int[] sizes = {50 + extra, 60 + extra, 66 + extra};
    List<Tab> tabs = new ArrayList<>();
    int x = left;
    for (int i = 0; i < ids.length; i++) {
      tabs.add(new Tab(ids[i], labels[i], x, top, sizes[i]));
      x += sizes[i] + 2;
    }
    int addFolderX = left + width - 120;
    int firstActionX = selector.equals("favorites") ? left + width - 48 : addFolderX;
    int actionsY = firstActionX >= x + 6 ? top : top + 18 + CONTENT_GAP;
    int height = selector.equals("playlists") ? 18 : actionsY - top + 18;
    return new Layout(List.copyOf(tabs), addFolderX, left + width - 48, actionsY, height);
  }

  /** Keep complete tab labels and one library action slot on short, narrow screens. */
  public static Layout compact(int left, int top, int width, String selector, ToIntFunction<String> measure) {
    Layout normal = layout(left, top, width, selector);
    if (width >= 380) return normal;
    List<Tab> tabs = new ArrayList<>();
    int x = left;
    for (Tab tab : normal.tabs()) {
      int span = measure.applyAsInt(tab.label()) + 12;
      tabs.add(new Tab(tab.id(), tab.label(), x, top, span));
      x += span + 2;
    }
    int reload = left + width - 20;
    int add = reload - 22;
    if (add < x + 4) add = reload;
    int actionsY = add >= x + 4 ? top : top + 18 + CONTENT_GAP;
    int height = selector.equals("playlists") ? 18 : actionsY - top + 18;
    return new Layout(List.copyOf(tabs), add, reload, actionsY, height);
  }
}
