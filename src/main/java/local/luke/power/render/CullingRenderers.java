package local.luke.power.render;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** Main-thread renderer discovery published as immutable capability data for the culling worker. */
public final class CullingRenderers {
  private volatile Map<Class<?>, Boolean> known;

  public void publish(Map<Class<?>, ?> renderers) {
    var next = new IdentityHashMap<Class<?>, Boolean>();
    renderers.forEach((type, renderer) -> next.put(type, renderer != null));
    known = Collections.unmodifiableMap(next);
  }

  public void resolved(Class<?> type, boolean renderer) {
    var before = known;
    if (before != null && Boolean.valueOf(renderer).equals(before.get(type))) return;
    var next = new IdentityHashMap<Class<?>, Boolean>();
    if (before != null) next.putAll(before);
    next.put(type, renderer);
    known = Collections.unmodifiableMap(next);
  }

  public boolean hasRenderer(Class<?> type) {
    var snapshot = known;
    // Before native setup, extra culling work is safer than touching its mutable registry.
    if (snapshot == null) return true;
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      Boolean renderer = snapshot.get(current);
      if (renderer != null) return renderer;
    }
    return false;
  }
}
