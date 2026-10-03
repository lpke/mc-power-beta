package local.luke.worldedit;

import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

/** BHCreative's compatibility interface is also provided by LpkeCreative. */
public final class CreativeAccess {
  private static final boolean INSTALLED =
      FabricLoader.getInstance().isModLoaded("bhcreative")
          || FabricLoader.getInstance().isModLoaded("lpkecreative");
  private static final Method CREATIVE = method("creative_isCreative");
  private static final Method FLYING = method("creative_setFlying", boolean.class);
  private static final Method CAN_FLY = flightPermission();

  private static Method flightPermission() {
    if (!FabricLoader.getInstance().isModLoaded("lpkecreative")) return null;
    try {
      return Class.forName("local.luke.creative.Modes").getMethod("canEnableFlight", Object.class);
    } catch (ReflectiveOperationException | LinkageError e) {
      return null;
    }
  }

  public static boolean flightAvailable(Object player) {
    if (!creative(player) || FLYING == null) return false;
    if (CAN_FLY == null) return true;
    try {
      return Boolean.TRUE.equals(CAN_FLY.invoke(null, player));
    } catch (ReflectiveOperationException | RuntimeException e) {
      return false;
    }
  }

  private CreativeAccess() {}

  private static Method method(String name, Class<?>... args) {
    if (!INSTALLED) return null;
    try {
      return Class.forName("paulevs.bhcreative.interfaces.CreativePlayer").getMethod(name, args);
    } catch (ReflectiveOperationException | LinkageError e) {
      WorldEditBeta.LOG.warn("Creative integration unavailable", e);
      return null;
    }
  }

  public static boolean installed() {
    return INSTALLED;
  }

  public static boolean creative(Object player) {
    if (player == null || CREATIVE == null) return false;
    try {
      return Boolean.TRUE.equals(CREATIVE.invoke(player));
    } catch (ReflectiveOperationException | RuntimeException e) {
      return false;
    }
  }

  public static boolean fly(Object player) {
    if (!flightAvailable(player)) return false;
    try {
      FLYING.invoke(player, true);
      return true;
    } catch (ReflectiveOperationException | RuntimeException e) {
      return false;
    }
  }
}
