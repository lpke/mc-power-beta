package local.luke.power.chat;

import java.util.function.ToIntFunction;

/** Selection follows the native, already wrapped and scrolled chat draw calls. */
public final class ChatSelection {
  public static final ChatSelection history = new ChatSelection();
  private static final int LIMIT = 20;
  private final Row[] rows = new Row[LIMIT];
  private Object screen;
  private int count, anchorRow = -1, anchor, cursorRow = -1, cursor;
  private boolean dragging;

  public ChatSelection() { for (int i = 0; i < LIMIT; i++) rows[i] = new Row(); }

  public void context(Object current, boolean focused) {
    if (!focused || current != screen) { clear(); count = 0; screen = null; }
  }

  public void begin(Object current) {
    if (current != screen) clear();
    screen = current; count = 0;
  }

  /** Reuses line metrics until the native line or its font changes. Rows arrive newest first. */
  public int line(String text, Object font, int x, int y, ToIntFunction<String> measure) {
    if (screen == null || count == LIMIT) return -1;
    int index = count++;
    Row row = rows[index];
    if (row.raw != text || row.font != font || row.x != x || row.y != y) {
      if (anchorRow >= 0) clear();
      row.set(text, font, x, y, measure);
    }
    return index;
  }

  public void end() {
    if (anchorRow >= count || cursorRow >= count) clear();
  }

  public boolean click(Object current, int x, int y) {
    if (current != screen) return false;
    for (int i = 0; i < count; i++) {
      Row row = rows[i];
      if (y >= row.y - 1 && y < row.y + 8 && x >= row.x && x <= row.x + row.width()) {
        anchorRow = cursorRow = i; anchor = cursor = row.hit(x); dragging = true;
        return true;
      }
    }
    clear(); return false;
  }

  public void move(Object current, int x, int y, boolean release) {
    if (current != screen || !dragging || count == 0) return;
    int nearest = 0, distance = Integer.MAX_VALUE;
    for (int i = 0; i < count; i++) {
      int d = Math.abs(y - (rows[i].y + 3));
      if (d < distance) { nearest = i; distance = d; }
    }
    cursorRow = nearest; cursor = rows[nearest].hit(x);
    if (release) dragging = false;
  }

  public boolean dragging() { return dragging; }
  public boolean selected() { return anchorRow >= 0 && (anchorRow != cursorRow || anchor != cursor); }
  public void clear() { anchorRow = cursorRow = -1; dragging = false; }

  private boolean before() { return anchorRow > cursorRow || anchorRow == cursorRow && anchor <= cursor; }
  public int start(int row) {
    if (!selected() || row < Math.min(anchorRow, cursorRow) || row > Math.max(anchorRow, cursorRow)) return -1;
    int first = Math.max(anchorRow, cursorRow);
    return row == first ? before() ? anchor : cursor : 0;
  }
  public int end(int row) {
    if (start(row) < 0) return -1;
    int last = Math.min(anchorRow, cursorRow);
    return row == last ? before() ? cursor : anchor : rows[row].plain.length();
  }
  public int x(int row, int offset) { return rows[row].x + rows[row].positions[offset]; }

  public String text() {
    if (!selected()) return "";
    StringBuilder result = new StringBuilder();
    for (int i = Math.max(anchorRow, cursorRow); i >= Math.min(anchorRow, cursorRow); i--) {
      if (i != Math.max(anchorRow, cursorRow)) result.append('\n');
      result.append(rows[i].plain, start(i), end(i));
    }
    return result.toString();
  }

  private static final class Row {
    String raw, plain = "";
    Object font;
    int x, y;
    int[] positions = {0};

    void set(String text, Object font, int x, int y, ToIntFunction<String> measure) {
      raw = text; this.font = font; this.x = x; this.y = y;
      StringBuilder visible = new StringBuilder();
      int[] widths = new int[text.length() + 1];
      for (int i = 0; i < text.length(); i++) {
        if (text.charAt(i) == '\u00a7' && i + 1 < text.length()) { i++; continue; }
        visible.append(text.charAt(i)); widths[visible.length()] = measure.applyAsInt(text.substring(0, i + 1));
      }
      plain = visible.toString(); positions = java.util.Arrays.copyOf(widths, plain.length() + 1);
    }
    int width() { return positions[positions.length - 1]; }
    int hit(int mouseX) {
      int relative = mouseX - x;
      for (int i = 0; i < positions.length - 1; i++)
        if (relative < (positions[i] + positions[i + 1]) / 2) return i;
      return plain.length();
    }
  }
}
