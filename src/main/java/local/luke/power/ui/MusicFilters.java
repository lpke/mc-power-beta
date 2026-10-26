package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** The music editor keeps the same three tabs at every supported GUI size. */
public final class MusicFilters {
  public record Tab(String id, String label, int x, int y, int width) {}
  public record Layout(List<Tab> tabs, int folderX, int folderY, int folderWidth, int height) {}

  public static Layout layout(int left, int top, int width, String selector) {
    String[] ids = {"library", "playlists", "settings"};
    String[] labels = {"Library", "Playlists", "Settings"};
    int[] sizes = {50, 60, 56};
    List<Tab> tabs = new ArrayList<>();
    int x = left;
    for (int i = 0; i < ids.length; i++) {
      tabs.add(new Tab(ids[i], labels[i], x, top, sizes[i]));
      x += sizes[i] + 2;
    }
    return new Layout(List.copyOf(tabs), 0, 0, 0, 18);
  }
}
