package local.luke.power.audio;

import java.lang.reflect.Field;
import local.luke.power.PowerBeta;

/** Keeps the existing debug HUD's music line connected to the shared player. */
final class MusicDisplay {
  private static Field current;
  private static boolean resolved;
  private static String previous;

  static void update(String name) {
    if (!resolved) {
      resolved = true;
      try {
        current =
            Class.forName("local.luke.power.environment.ModHelper$ModHelperFields")
                .getField("currentBGM");
      } catch (ReflectiveOperationException e) {
        PowerBeta.LOG.warn("Music debug display is unavailable", e);
      }
    }
    if (current == null || name.equals(previous)) return;
    try {
      current.set(null, name);
      previous = name;
    } catch (IllegalAccessException e) {
      current = null;
      PowerBeta.LOG.warn("Could not update the music debug display", e);
    }
  }
}
