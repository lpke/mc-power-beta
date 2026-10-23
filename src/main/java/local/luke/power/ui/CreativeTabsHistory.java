package local.luke.power.ui;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded editor snapshots. Documents and selections never share mutable draft state. */
final class CreativeTabsHistory {
  private static final int LIMIT = 32, BYTES = 8 * 1024 * 1024;
  record State(JsonObject document, int selected, int listScroll, int gridScroll, Set<Integer> cells, int anchor) {
    State { document = document.deepCopy(); cells = Set.copyOf(cells); }
    State copy() { return new State(document, selected, listScroll, gridScroll, cells, anchor); }
    int bytes() { return document.toString().getBytes(StandardCharsets.UTF_8).length; }
  }
  private final Deque<State> states = new ArrayDeque<>();
  private int bytes;
  boolean available() { return !states.isEmpty(); }
  void record(State before, JsonObject after) {
    if (before.document().equals(after)) return;
    State copy = before.copy(); states.addLast(copy); bytes += copy.bytes();
    while (states.size() > LIMIT || bytes > BYTES && states.size() > 1) bytes -= states.removeFirst().bytes();
  }
  State undo() {
    if (states.isEmpty()) return null;
    State state = states.removeLast(); bytes -= state.bytes(); return state.copy();
  }
}
