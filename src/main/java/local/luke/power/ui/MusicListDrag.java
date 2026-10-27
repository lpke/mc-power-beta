package local.luke.power.ui;

import java.util.*;
import org.lwjgl.opengl.Display;

/** One pointer-owned reorder gesture; callers validate its immutable source before commit. */
final class MusicListDrag {
  private static final int THRESHOLD = 8;
  enum Kind { GROUPS, PLAYLIST, QUEUE, CATALOG }
  record Slot(String id, int y, int height) {}
  private Kind kind;
  private Object owner;
  private List<String> original = List.of();
  private List<Slot> slots = List.of();
  private int index, pressX, pressY;
  private boolean moved;
  private long lastScroll;
  private String ghostLabel, ghostText;
  private int ghostSpan, ghostWidth;
  /** Pointer ownership includes the press before it becomes a drag. */
  boolean active() { return kind != null; }
  boolean dragging() { return active() && moved; }
  boolean moved() { return moved; }
  Kind kind() { return kind; }
  Object owner() { return owner; }
  int index() { return index; }
  List<String> original() { return original; }
  void begin(Kind kind, Object owner, List<String> original, List<Slot> slots, int index, int x, int y) {
    this.kind = kind; this.owner = owner; this.original = List.copyOf(original); this.slots = List.copyOf(slots);
    this.index = index; pressX = x; pressY = y; moved = false; lastScroll = 0; ghostLabel = null;
  }
  void cancel() { kind = null; owner = null; original = List.of(); slots = List.of(); moved = false; ghostLabel = null; }
  void update(int x, int y) { if (active()) update(x, y, Display.isActive()); }
  void update(int x, int y, boolean focused) {
    if (!focused) { cancel(); return; }
    if (!active() || moved) return;
    long dx = Math.abs((long)x - pressX), dy = Math.abs((long)y - pressY);
    moved = dx >= THRESHOLD || dy >= THRESHOLD || dx * dx + dy * dy >= THRESHOLD * THRESHOLD;
  }
  int slot(int documentY) {
    int low = 0, high = slots.size();
    while (low < high) {
      int middle = (low + high) >>> 1;
      Slot row = slots.get(middle);
      if (documentY < row.y + row.height / 2) high = middle;
      else low = middle + 1;
    }
    return low;
  }
  int lineY(int slot) {
    if (slots.isEmpty()) return 0;
    return slot < slots.size() ? slots.get(slot).y : slots.get(slots.size() - 1).y + slots.get(slots.size() - 1).height;
  }
  boolean scrollDue() {
    if (!dragging()) return false;
    long now = System.nanoTime();
    if (now - lastScroll < 100_000_000L) return false;
    lastScroll = now; return true;
  }
  void renderLine(UiScreen screen, int left, int right, int top, int bottom, int y) {
    if (!dragging() || right <= left || bottom <= top) return;
    int height = Math.min(2, bottom - top);
    int line = Math.max(top, Math.min(bottom - height, y));
    screen.rectangle(left, line, right, line + height, PopupStyle.ACTIVE);
  }
  /** Render after the list's clip is removed, using the unsuppressed pointer coordinates. */
  void renderGhost(UiScreen screen, String label, int x, int y) {
    if (!dragging() || label.isEmpty()) return;
    int span = Math.min(320, screen.width - 24);
    if (span <= 0) return;
    if (!label.equals(ghostLabel) || span != ghostSpan) {
      ghostLabel = label; ghostSpan = span;
      ghostText = screen.fit(label, span); ghostWidth = screen.textWidth(ghostText) + 16;
    }
    int left = Math.max(4, Math.min(screen.width - ghostWidth - 4, x + 12));
    int top = Math.max(4, Math.min(screen.height - 22, y + 12));
    screen.rectangle(left, top, left + ghostWidth, top + 18, PopupStyle.BACKGROUND);
    screen.rectangle(left, top, left + 2, top + 18, PopupStyle.ACTIVE);
    screen.text(ghostText, left + 8, top + 5, PopupStyle.BRIGHT_TEXT);
  }
}
