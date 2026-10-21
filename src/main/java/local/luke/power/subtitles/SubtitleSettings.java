package local.luke.power.subtitles;

public final class SubtitleSettings {
  public boolean enabled;
  public int position = 8;
  public int offsetX = -1, offsetY = -30;
  public double textSize = 1;
  public int backgroundOpacity = 80;
  public double displaySeconds = 3;
  public boolean hideWithDebug;
  public boolean showInInventory = true;
  public void validate() {
    if (position < 0 || position > 8 || Math.abs((long)offsetX) > 4096 || Math.abs((long)offsetY) > 4096
        || !Double.isFinite(textSize) || textSize < .5 || textSize > 3 || backgroundOpacity < 0 || backgroundOpacity > 100
        || !Double.isFinite(displaySeconds) || displaySeconds < 1 || displaySeconds > 15)
      throw new IllegalArgumentException("Invalid subtitle position, size, background or duration");
  }
}
