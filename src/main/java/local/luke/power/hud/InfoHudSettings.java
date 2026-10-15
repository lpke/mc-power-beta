package local.luke.power.hud;

import local.luke.power.light.LightSettings;

/** Fixed IDs keep cluster drafts and saved positions stable across edits. */
public final class InfoHudSettings {
  public static final int CLUSTERS = 4;
  public boolean enabled;
  public int selectedCluster;
  public Cluster[] clusters = {new Cluster(0), new Cluster(2), new Cluster(6), new Cluster(8)};

  public InfoHudSettings() {
    clusters[0].enabled = true;
    clusters[0].coordinates = clusters[0].direction = true;
  }

  public static final class Cluster {
    public boolean enabled;
    public int position;
    public int offsetX, offsetY;
    public double textSize = 1;
    public String textColor = "#FFFFFF";
    public int opacity = 100;
    public boolean hideWithDebug = true;
    public boolean coordinates, direction, biome, timeOfDay, day, chunk, fps;
    public Cluster() {}
    public Cluster(int position) { this.position = position; }
    public void validate() {
      if (position < 0 || position > 8 || Math.abs((long)offsetX) > 4096 || Math.abs((long)offsetY) > 4096
          || !Double.isFinite(textSize) || textSize < .5 || textSize > 3 || opacity < 0 || opacity > 100)
        throw new IllegalArgumentException("Invalid HUD cluster position, size or opacity");
      LightSettings.rgb(textColor);
    }
    public int argb() { return Math.round(opacity * 255 / 100f) << 24 | LightSettings.rgb(textColor); }
  }

  public void validate() {
    if (selectedCluster < 0 || selectedCluster >= CLUSTERS || clusters == null || clusters.length != CLUSTERS)
      throw new IllegalArgumentException("Invalid HUD clusters");
    for (Cluster c : clusters) {
      if (c == null) throw new IllegalArgumentException("Missing HUD cluster");
      c.validate();
    }
  }
}
