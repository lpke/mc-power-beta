package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Music navigation keeps a single row and moves excess tabs and actions into one menu. */
public final class MusicFilters {
  static final int CONTENT_GAP = 8;
  public record Tab(String id, String label, int x, int y, int width) {}
  public record Layout(List<Tab> tabs, List<Tab> hiddenTabs, int addFolderX, int reloadX,
      int actionsY, int height, boolean overflow) {}

  public static Layout layout(int left, int top, int width, String selector) {
    return layout(left, top, width, selector, "Beta 1.7.3", label -> label.length() * 6);
  }

  public static Layout compact(int left, int top, int width, String selector, ToIntFunction<String> measure) {
    return layout(left, top, width, selector, "Beta 1.7.3", measure);
  }

  public static Layout layout(int left, int top, int width, String selector, String activeName,
      ToIntFunction<String> measure) {
    String[] ids = {"active", "library", "playlists", "favorites"};
    String[] labels = {activeName, "Library", "Playlists", "Favorites"};
    int[] sizes = {Math.max(56, Math.min(110, measure.applyAsInt(activeName) + 12)), 54, 64, 70};
    int tabsWidth = sizes[0] + sizes[1] + sizes[2] + sizes[3] + 6;
    boolean overflow = tabsWidth + 126 > width;
    boolean hideTabs = tabsWidth + 26 > width;
    List<Tab> tabs = new ArrayList<>(), hidden = new ArrayList<>();
    int secondary = selector.equals("playlists") ? 2 : selector.equals("favorites") ? 3 : 1;
    if (hideTabs) {
      sizes[0] = Math.min(sizes[0], Math.max(56, width - 102));
      // A fixed secondary slot prevents its active edge shifting when the label changes.
      sizes[1] = sizes[2] = sizes[3] = Math.max(36, Math.min(70, width - sizes[0] - 30));
    }
    int x = left;
    for (int i = 0; i < ids.length; i++) {
      Tab tab = new Tab(ids[i], labels[i], x, top, sizes[i]);
      if (!hideTabs || i == 0 || i == secondary) {
        tabs.add(tab);
        x += sizes[i] + 2;
      } else hidden.add(tab);
    }
    return new Layout(List.copyOf(tabs), List.copyOf(hidden), left + width - (overflow ? 20 : 120),
        left + width - (overflow ? 20 : 48), top, 18, overflow);
  }
}
