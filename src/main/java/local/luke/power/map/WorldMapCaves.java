package local.luke.power.map;

/** A map visit follows live cave detection and height until the owner changes its view. */
public final class WorldMapCaves {
  private Boolean caves;
  private Integer selectedY;

  public boolean shown(boolean automatic) { return caves == null ? automatic : caves; }
  public void toggle(boolean automatic) { caves = !shown(automatic); }
  public void select(int y) { selectedY = Math.max(0, Math.min(127, y)); caves = true; }
  public void reset() { selectedY = null; }
  public void center() { caves = null; reset(); }
  public boolean following() { return selectedY == null; }
  public CaveWindow window(MapSettings settings, double currentY) {
    return CaveWindow.at(selectedY == null ? currentY : selectedY, settings.caveAbove, settings.caveBelow);
  }
}
