package local.luke.power.input;

/** Mapping-independent entry to the local read-only container viewer. */
public final class SpectatorContainers {
  private static Runnable opener = () -> {};
  private SpectatorContainers() {}
  public static void register(Runnable action) { opener = java.util.Objects.requireNonNull(action); }
  public static void open() { opener.run(); }
}
