package local.luke.power.chat;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

/** Chat editing state. Cursor and anchor are offsets between characters. */
public final class ChatBuffer {
  private String text = "", draft = "";
  private int cursor, anchor, history, viewStart;
  private boolean browsing;
  private final int limit;

  public ChatBuffer(int limit) { this.limit = limit; }
  public String text() { return text; }
  public int cursor() { return cursor; }
  public int start() { return Math.min(cursor, anchor); }
  public int end() { return Math.max(cursor, anchor); }
  public boolean selected() { return cursor != anchor; }
  public boolean browsing() { return browsing; }
  public String selection() { return text.substring(start(), end()); }

  public void set(String value) {
    text = value.substring(0, Math.min(limit, value.length()));
    cursor = anchor = text.length();
    viewStart = 0;
  }
  public void sync(String value) { if (!text.equals(value)) set(value); }
  public void reset(String value) { set(value); history = 0; browsing = false; draft = ""; }
  public void position(int value, boolean select) {
    cursor = Math.max(0, Math.min(text.length(), value));
    if (!select) anchor = cursor;
  }
  public void all() { anchor = 0; cursor = text.length(); browsing = false; }
  public void move(int direction, boolean select, boolean word) {
    if (!select && selected() && !word) { position(direction < 0 ? start() : end(), false); return; }
    int next = cursor;
    if (word) {
      if (direction < 0) {
        while (next > 0 && Character.isWhitespace(text.charAt(next - 1))) next--;
        while (next > 0 && !Character.isWhitespace(text.charAt(next - 1))) next--;
      } else {
        while (next < text.length() && !Character.isWhitespace(text.charAt(next))) next++;
        while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
      }
    } else next += direction;
    position(next, select);
  }
  public void write(String value, IntPredicate allowed) {
    StringBuilder accepted = new StringBuilder();
    int room = limit - text.length() + end() - start();
    for (int i = 0; i < value.length() && accepted.length() < room; i++)
      if (allowed.test(value.charAt(i))) accepted.append(value.charAt(i));
    // An unsupported key/control character must not delete a selection.
    if (accepted.isEmpty()) return;
    replace(accepted.toString());
  }
  private void replace(String value) {
    int from = start();
    text = text.substring(0, from) + value + text.substring(end());
    cursor = anchor = from + value.length();
    browsing = false; history = 0;
  }
  public void delete(int direction, boolean word) {
    if (!selected()) move(direction, true, word);
    if (selected()) replace("");
  }
  public void history(List<String> entries, boolean up) {
    if (!browsing) { draft = text; history = 0; }
    browsing = true;
    history = Math.max(0, Math.min(entries.size(), history + (up ? 1 : -1)));
    set(history == 0 ? draft : entries.get(entries.size() - history));
  }
  public void complete(String suffix) {
    if (selected()) return;
    int to = cursor;
    while (to < text.length() && !Character.isWhitespace(text.charAt(to))) to++;
    anchor = to;
    int room = limit - text.length() + end() - start();
    replace(suffix.substring(0, Math.min(room, suffix.length())));
    browsing = false; history = 0;
  }
  /** The visible range always contains the cursor, including at either end. */
  public View view(int width, ToIntFunction<String> measure) {
    int from = Math.min(viewStart, cursor);
    while (from < cursor && measure.applyAsInt(text.substring(from, cursor)) > width - 1) from++;
    int to = from;
    while (to < text.length() && measure.applyAsInt(text.substring(from, to + 1)) <= width - 1) to++;
    viewStart = from;
    return new View(from, to);
  }
  public record View(int start, int end) {
    public int hit(String text, int x, ToIntFunction<String> measure) {
      for (int i = start; i < end; i++) {
        int left = measure.applyAsInt(text.substring(start, i));
        int right = measure.applyAsInt(text.substring(start, i + 1));
        if (x < (left + right) / 2) return i;
      }
      return end;
    }
  }
}
