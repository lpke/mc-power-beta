package local.luke.power.audio;

import java.util.*;
import java.util.function.Function;

/** Selection advances when playback accepts a track, never when its label is rendered. */
public final class TrackSelector {
  private static final String DEFAULT_CONTEXT = "";
  private static final class Cursor {
    String previous = "";
    List<String> order = List.of(), pendingOrder = List.of();
  }
  private final Map<String, Cursor> cursors = new LinkedHashMap<>();
  private String previous = "";

  private Cursor cursor(String context) {
    Cursor cursor = cursors.get(context);
    if (cursor != null) return cursor;
    if (cursors.size() >= 128) cursors.remove(cursors.keySet().iterator().next());
    cursor = new Cursor();
    cursors.put(context, cursor);
    return cursor;
  }

  public <T> T choose(List<T> tracks, boolean shuffle, boolean avoidRepeats, Random random,
      Function<T, String> key) {
    T chosen = reserve(DEFAULT_CONTEXT, tracks, shuffle, avoidRepeats, random, key);
    if (chosen != null) played(DEFAULT_CONTEXT, key.apply(chosen));
    return chosen;
  }

  public <T> T reserve(String context, List<T> tracks, boolean shuffle, boolean avoidRepeats,
      Random random, Function<T, String> key) {
    if (tracks.isEmpty()) return null;
    Cursor cursor = cursor(context);
    List<String> order = tracks.stream().map(key).toList();
    int index = shuffle ? random.nextInt(tracks.size()) : successor(cursor, order);
    if (avoidRepeats && tracks.size() > 1 && order.get(index).equals(previous))
      index = (index + 1 + (shuffle ? random.nextInt(tracks.size() - 1) : 0)) % tracks.size();
    cursor.pendingOrder = order;
    return tracks.get(index);
  }

  private static int successor(Cursor cursor, List<String> order) {
    int index = order.indexOf(cursor.previous);
    if (index >= 0) return (index + 1) % order.size();
    // If the last track disappeared, continue with its next surviving successor.
    int old = cursor.order.indexOf(cursor.previous);
    if (old >= 0)
      for (int step = 1; step <= cursor.order.size(); step++) {
        int surviving = order.indexOf(cursor.order.get((old + step) % cursor.order.size()));
        if (surviving >= 0) return surviving;
      }
    return 0;
  }

  public void played(String context, String id) {
    Cursor cursor = cursor(context);
    cursor.previous = id;
    cursor.order = cursor.pendingOrder;
    previous = id;
  }

  /** Explicit song choices continue from the saved order without consuming a reservation. */
  public boolean align(String context, List<String> order, String id) {
    if (!order.contains(id)) return false;
    Cursor cursor = cursor(context);
    cursor.previous = id;
    cursor.order = List.copyOf(order);
    cursor.pendingOrder = cursor.order;
    previous = id;
    return true;
  }
}
