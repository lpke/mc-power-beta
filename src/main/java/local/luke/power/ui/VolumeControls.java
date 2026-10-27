package local.luke.power.ui;

import local.luke.power.config.Setting;

/** Volume columns keep a common width; adjacent actions never consume their space. */
public final class VolumeControls {
  public static final int WIDTH = 150, MIN_MUSIC_WIDTH = 54, CHILD_INSET = 16;
  private VolumeControls() {}
  public static int width(int available) { return Math.max(8, Math.min(WIDTH, available)); }
  public static int musicWidth(int contentWidth) { return Math.max(MIN_MUSIC_WIDTH, width(contentWidth / 4)); }
  public static boolean accepts(Setting setting) {
    String id = setting.id;
    return setting.slider() && (id.equals("native.music") || id.equals("native.sound")
        || id.equals("audio.master") || id.startsWith("audio.category.") || id.startsWith("audio.sound.")
        || id.startsWith("power_environment:config.MUSIC_CONFIG.volume"));
  }
}
