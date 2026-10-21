package local.luke.power.input;

import java.util.ArrayDeque;

/** A semantic tag around one actual sound call, shared across independently mapped modules. */
public final class SoundCaptionContext {
  private static volatile boolean enabled;
  private static final ThreadLocal<ArrayDeque<Scope>> TAGS = ThreadLocal.withInitial(ArrayDeque::new);
  private static final Scope OFF = new Scope(null);
  private SoundCaptionContext() {}
  public static void enabled(boolean value) { enabled = value; }
  public static boolean enabled() { return enabled; }
  public static String claim() {
    if (!enabled) return null;
    Scope scope = TAGS.get().peek();
    if (scope == null || scope.claimed) return null;
    scope.claimed = true; return scope.key;
  }
  public static Scope push(String key) {
    if (!enabled || key == null) return OFF;
    Scope scope = new Scope(key); TAGS.get().push(scope); return scope;
  }
  public static final class Scope implements AutoCloseable {
    private final String key;
    private boolean closed, claimed;
    private Scope(String key) { this.key = key; }
    public void close() {
      if (key == null || closed) return;
      closed = true; ArrayDeque<Scope> stack = TAGS.get();
      if (stack.poll() != this) { stack.clear(); throw new IllegalStateException("Sound caption scopes closed out of order"); }
      if (stack.isEmpty()) TAGS.remove();
    }
  }
}
