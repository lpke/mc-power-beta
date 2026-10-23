package local.luke.power.audio;

import java.net.URL;

/** Limit Ogg page repairs to bundled extra effects, preserving music and native codecs. */
public final class ExtraSoundResources {
  private ExtraSoundResources() {}
  public static boolean owns(URL resource) {
    if (resource == null) return false;
    String path = resource.getPath();
    return path.contains("/assets/power_beta/stationapi/sounds/sound/")
        || path.contains("/assets/power_controls/stationapi/sounds/sound/");
  }
}
