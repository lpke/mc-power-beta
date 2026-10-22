package local.luke.power.input;

import java.util.function.BooleanSupplier;

/** Read-only target policy shared across Minecraft mappings. */
public final class SpectatorTarget {
  private static BooleanSupplier policy = () -> false;
  private SpectatorTarget() {}
  public static void register(BooleanSupplier value) { policy = value; }
  public static boolean interactable() { return policy.getAsBoolean(); }
}
