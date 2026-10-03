package local.luke.worldedit.config;

public final class Settings {
  public boolean enabled = true, creativeOnly = true;
  public int wandItem = 271, blockLimit = 65536, blocksPerTick = 2048, historySize = 20;
  public boolean wandEnabled = true, showSelection = true, throughWalls = true;
  public int color = 0, opacity = 80, lineWidth = 2;

  public Settings copy() {
    Settings s = new Settings();
    s.enabled = enabled;
    s.creativeOnly = creativeOnly;
    s.wandItem = wandItem;
    s.blockLimit = blockLimit;
    s.blocksPerTick = blocksPerTick;
    s.historySize = historySize;
    s.wandEnabled = wandEnabled;
    s.showSelection = showSelection;
    s.throughWalls = throughWalls;
    s.color = color;
    s.opacity = opacity;
    s.lineWidth = lineWidth;
    return s;
  }
}
