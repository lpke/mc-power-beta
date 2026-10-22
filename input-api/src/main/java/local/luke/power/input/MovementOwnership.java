package local.luke.power.input;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** Live movement ownership across the camera and building modules' mappings. */
public final class MovementOwnership {
  private static BooleanSupplier camera = () -> false;
  private static BooleanSupplier autoWalk = () -> false;
  private static BooleanSupplier lineLock = () -> false;
  private static DoubleSupplier autoWalkInput = () -> autoWalking() ? 1 : 0;
  private MovementOwnership() {}

  public static void registerCamera(BooleanSupplier value) { camera = value; }
  public static void registerAutoWalk(BooleanSupplier value) { autoWalk = value; }
  public static void registerAutoWalkInput(DoubleSupplier value) { autoWalkInput = value; }
  public static void registerAutoWalkLineLock(BooleanSupplier value) { lineLock = value; }
  public static boolean autoWalkLineLocked() { return lineLock.getAsBoolean(); }
  public static float autoWalkInput() { return (float) autoWalkInput.getAsDouble(); }
  public static boolean cameraControlsMovement() { return camera.getAsBoolean(); }
  public static boolean autoWalking() { return autoWalk.getAsBoolean(); }
}
