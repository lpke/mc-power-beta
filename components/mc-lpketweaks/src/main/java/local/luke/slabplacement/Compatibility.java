package local.luke.slabplacement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional APIs resolved once. No world or player references are retained. */
public final class Compatibility {
  private static Field freecam;
  private static Method active;
  private static boolean unknownFreecam;

  static {
    try {
      freecam = Class.forName("ralf2oo2.freecam.Freecam").getField("freecamController");
      active = freecam.getType().getMethod("isActive");
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      unknownFreecam = true;
    }
  }

  private Compatibility() {}

  public static boolean freecam() {
    if (unknownFreecam) return true;
    if (freecam == null || active == null) return false;
    try {
      Object controller = freecam.get(null);
      return controller != null && Boolean.TRUE.equals(active.invoke(controller));
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }
}
