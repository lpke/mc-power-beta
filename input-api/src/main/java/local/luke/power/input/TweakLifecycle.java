package local.luke.power.input;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Owners clear live actions and queue their own settings snapshots on a session exit. */
public final class TweakLifecycle {
  private static final Map<String, Runnable> resets = new LinkedHashMap<>();
  private TweakLifecycle() {}
  public static void register(String owner, Runnable reset) { resets.put(owner, Objects.requireNonNull(reset)); }
  public static void reset() {
    RuntimeException failure = null;
    for (Runnable reset : resets.values()) try { reset.run(); }
    catch (RuntimeException e) {
      if (failure == null) failure = new IllegalStateException("Could not reset all active tweaks");
      failure.addSuppressed(e);
    }
    if (failure != null) throw failure;
  }
}
