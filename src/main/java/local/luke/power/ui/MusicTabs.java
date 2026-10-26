package local.luke.power.ui;

/** The selected tab opens onto the ordinary content area; inactive tabs stay dark. */
final class MusicTabs {
  private MusicTabs() {}
  static void render(UiScreen screen, MusicFilters.Layout layout, String selected, int end, int mx, int my) {
    var active = layout.tabs().stream().filter(tab -> tab.id().equals(selected)).findFirst().orElseThrow();
    ConnectedTabs.baseline(screen, layout.tabs().get(0).x(), end, active.y(), active.x(), active.width());
    for (var tab : layout.tabs()) ConnectedTabs.tab(screen, tab.label(), tab.x(), tab.y(), tab.width(), mx, my, tab.id().equals(selected));
  }
}
