package local.luke.power.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** A Music visit retains each tab without turning tab changes into Back routes. */
final class MusicTabNavigation<V> {
  private final Map<String, V> views = new HashMap<>();
  private final Map<String, Deque<V>> histories = new HashMap<>();

  Deque<V> history(String tab) { return histories.computeIfAbsent(tab, ignored -> new ArrayDeque<>()); }
  V view(String tab) { return views.get(tab); }
  void leave(String tab, V view) { views.put(tab, view); }
  void remember(String tab, V view) {
    var history = history(tab);
    if (history.size() == 32) history.removeLast();
    history.push(view);
  }
  void reset(String tab) { views.remove(tab); histories.remove(tab); }
  void clear() { views.clear(); histories.clear(); }
}
