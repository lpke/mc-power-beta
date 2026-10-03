package local.luke.flexible;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional APIs resolved once. No world or player references are retained. */
public final class Compatibility {
  private static Field freecam;
  private static Method active, slabsEnabled;
  private static Field addonConfig, plantReplacement;
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

  static {
    try {
      addonConfig =
          Class.forName("com.github.telvarost.unitweakstelsaddons.Config").getField("config");
      plantReplacement = addonConfig.getType().getField("plantReplacementFixesEnabled");
    } catch (ReflectiveOperationException ignored) {
    }
    try {
      slabsEnabled = Class.forName("local.luke.slabplacement.SlabPlacement").getMethod("enabled");
    } catch (ReflectiveOperationException ignored) {
    }
  }

  public static boolean replacesPlants() {
    if (addonConfig == null || plantReplacement == null) return false;
    try {
      return Boolean.TRUE.equals(plantReplacement.get(addonConfig.get(null)));
    } catch (ReflectiveOperationException e) {
      return false;
    }
  }

  public static boolean adjacentSlabs() {
    if (slabsEnabled == null) return false;
    try {
      return Boolean.TRUE.equals(slabsEnabled.invoke(null));
    } catch (ReflectiveOperationException e) {
      return false;
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
