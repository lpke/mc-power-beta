package local.luke.flexible.config;

public final class Settings {
  public boolean enabled = true;
  public boolean showOverlay = true;
  public boolean showPreview = true;
  public boolean placeAgainstContainers = false;
  public int overlayColor = 0;
  public int overlayOpacity = 60;

  public Settings copy() {
    Settings s = new Settings();
    s.enabled = enabled;
    s.showOverlay = showOverlay;
    s.showPreview = showPreview;
    s.placeAgainstContainers = placeAgainstContainers;
    s.overlayColor = overlayColor;
    s.overlayOpacity = overlayOpacity;
    return s;
  }
}
