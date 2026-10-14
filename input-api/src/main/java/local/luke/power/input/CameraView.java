package local.luke.power.input;

import java.util.function.*;

/** Render preferences cross mappings without moving the player's simulation pose. */
public final class CameraView {
  private static DoubleSupplier distance = () -> 4;
  private static BooleanSupplier front = () -> false;
  private static DoubleUnaryOperator yaw = value -> value, pitch = value -> value;
  private CameraView() {}
  public static void configure(DoubleSupplier distance, BooleanSupplier front) { CameraView.distance = distance; CameraView.front = front; }
  public static void look(DoubleUnaryOperator yaw, DoubleUnaryOperator pitch) { CameraView.yaw = yaw; CameraView.pitch = pitch; }
  public static double distance() { return Math.max(1, Math.min(12, distance.getAsDouble())); }
  public static boolean front() { return front.getAsBoolean(); }
  public static float yaw(float value) { return (float)yaw.applyAsDouble(value); }
  public static float pitch(float value) { return (float)pitch.applyAsDouble(value); }
}
