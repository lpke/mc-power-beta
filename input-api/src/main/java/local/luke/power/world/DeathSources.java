package local.luke.power.world;

/** Command deaths share a cause across module mappings without changing damage rules. */
public final class DeathSources {
  private record Source(Object victim, String cause) {}
  private static final ThreadLocal<Source> CURRENT = new ThreadLocal<>();
  private DeathSources() {}
  public static String cause(Object victim) {
    Source source = CURRENT.get();
    return source != null && source.victim == victim ? source.cause : "";
  }
  public static void run(Object victim, String cause, Runnable action) {
    Source previous = CURRENT.get(); CURRENT.set(new Source(victim, cause));
    try { action.run(); }
    finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
  }
}
