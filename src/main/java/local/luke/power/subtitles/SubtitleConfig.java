package local.luke.power.subtitles;

import local.luke.power.config.Catalog;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;

public final class SubtitleConfig {
  private static SubtitleSettings current = load();
  private SubtitleConfig() {}
  private static SubtitleSettings load() {
    SubtitleSettings value;
    try { value = PowerConfig.read("subtitles", SubtitleSettings.class); value.validate(); }
    catch (RuntimeException invalid) {
      local.luke.power.PowerBeta.LOG.error("Could not load subtitle settings; original settings retained", invalid);
      value = new SubtitleSettings();
    }
    SoundCaptionContext.enabled(value.enabled); AudioListenerPose.interested(value.enabled); return value;
  }
  public static SubtitleSettings current() { return current; }
  public static SubtitleSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), SubtitleSettings.class); }
  public static void preview(SubtitleSettings value) {
    value.validate(); current = Catalog.JSON.fromJson(Catalog.JSON.toJson(value), SubtitleSettings.class);
    SoundCaptionContext.enabled(value.enabled); AudioListenerPose.interested(value.enabled);
    if (!value.enabled) Subtitles.clear();
  }
  public static void save(SubtitleSettings value) throws java.io.IOException { value.validate(); PowerConfig.save("subtitles", value); preview(value); }
}
