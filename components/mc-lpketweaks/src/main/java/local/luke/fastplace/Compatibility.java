package local.luke.fastplace;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional public mod APIs, resolved once. No worlds or players are retained. */
final class Compatibility {
  private static Field addonConfig, replacePlants;
  private static Field freecamController;
  private static Method freecamActive;
  private static boolean unknownFreecam;
  private static Method flexibleClick, flexibleActive, slabTarget;
  private static boolean brokenFlexible;

  static {
    try {
      flexibleClick =
          Class.forName("local.luke.flexible.FlexiblePlacement")
              .getMethod(
                  "resolveClick",
                  net.minecraft.world.World.class,
                  net.minecraft.item.ItemStack.class,
                  int.class,
                  int.class,
                  int.class,
                  int.class);
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      brokenFlexible = true;
      FastPlace.LOG.error("Unknown Flexible Placement API; fast placement suspended", e);
    }
    try {
      slabTarget =
          Class.forName("local.luke.slabplacement.SlabMerge")
              .getMethod(
                  "target",
                  net.minecraft.world.World.class,
                  net.minecraft.item.ItemStack.class,
                  int.class,
                  int.class,
                  int.class,
                  int.class);
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      FastPlace.LOG.error("Unknown Slab Placement API", e);
    }
  }

  static boolean flexibleModifiersActive() {
    if (flexibleClick == null) return false;
    try {
      if (flexibleActive == null)
        flexibleActive = flexibleClick.getDeclaringClass().getMethod("modifiersActive");
      return Boolean.TRUE.equals(flexibleActive.invoke(null));
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }

  static int[] flexibleClick(
      net.minecraft.world.World w, net.minecraft.item.ItemStack s, int x, int y, int z, int face) {
    if (brokenFlexible) return new int[0];
    if (flexibleClick == null) return null;
    try {
      return (int[]) flexibleClick.invoke(null, w, s, x, y, z, face);
    } catch (ReflectiveOperationException e) {
      brokenFlexible = true;
      FastPlace.LOG.error("Flexible Placement integration failed; fast placement suspended", e);
      return new int[0];
    }
  }

  static int[] slabTarget(
      net.minecraft.world.World w, net.minecraft.item.ItemStack s, int x, int y, int z, int face) {
    if (slabTarget == null) return null;
    try {
      return (int[]) slabTarget.invoke(null, w, s, x, y, z, face);
    } catch (ReflectiveOperationException e) {
      slabTarget = null;
      FastPlace.LOG.error("Slab Placement integration failed", e);
      return null;
    }
  }

  static {
    try {
      Class<?> c = Class.forName("com.github.telvarost.unitweakstelsaddons.Config");
      addonConfig = c.getField("config");
      Class<?> type = addonConfig.getType();
      replacePlants = type.getField("plantReplacementFixesEnabled");
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      FastPlace.LOG.warn("Could not read optional slab/plant settings", e);
    }
    try {
      Class<?> c = Class.forName("ralf2oo2.freecam.Freecam");
      freecamController = c.getField("freecamController");
      freecamActive = freecamController.getType().getMethod("isActive");
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      unknownFreecam = true;
      FastPlace.LOG.warn("Fast placement suspended: unknown Freecam API", e);
    }
  }

  private Compatibility() {}

  private static boolean addonFlag(Field field) {
    if (addonConfig == null || field == null) return false;
    try {
      return Boolean.TRUE.equals(field.get(addonConfig.get(null)));
    } catch (ReflectiveOperationException | NullPointerException e) {
      return false;
    }
  }

  static boolean replacesPlants() {
    return addonFlag(replacePlants);
  }

  static boolean freecamActive() {
    if (unknownFreecam) return true;
    if (freecamController == null || freecamActive == null) return false;
    try {
      Object controller = freecamController.get(null);
      return controller != null && Boolean.TRUE.equals(freecamActive.invoke(controller));
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }
}
