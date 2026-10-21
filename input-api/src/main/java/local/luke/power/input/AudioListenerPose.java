package local.luke.power.input;

/** Captions consume the actual audio-listener policy, including detached and muted listeners. */
public final class AudioListenerPose {
  @FunctionalInterface public interface Sink {
    void position(Object world, double x, double y, double z, float forwardX, float forwardZ);
  }
  private static volatile boolean interested;
  private static volatile Sink sink = (world, x, y, z, fx, fz) -> {};
  private AudioListenerPose() {}
  public static void register(Sink value) { sink = java.util.Objects.requireNonNull(value); }
  public static void interested(boolean value) { interested = value; }
  public static boolean interested() { return interested; }
  public static void publish(Object world, double x, double y, double z, float forwardX, float forwardZ) {
    if (interested) sink.position(world, x, y, z, forwardX, forwardZ);
  }
}
