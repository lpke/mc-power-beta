package local.luke.power.debug;

/** Restricts the compact resource overlay to an explicit Debug reload. */
public final class DebugReload {
  private static volatile boolean requested;
  private DebugReload() {}
  public static void begin() { requested = true; }
  public static boolean requested() { return requested; }
  public static void finish() { requested = false; }
}
