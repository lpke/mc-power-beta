package local.luke.power.input;

import java.util.function.BooleanSupplier;

/** Live movement ownership across the camera and building modules' mappings. */
public final class MovementOwnership {
  private static BooleanSupplier camera = () -> false;
  private static BooleanSupplier autoWalk = () -> false;
  private MovementOwnership() {}

  public static void registerCamera(BooleanSupplier value) { camera = value; }
  public static void registerAutoWalk(BooleanSupplier value) { autoWalk = value; }
  public static boolean cameraControlsMovement() { return camera.getAsBoolean(); }
  public static boolean autoWalking() { return autoWalk.getAsBoolean(); }
}
