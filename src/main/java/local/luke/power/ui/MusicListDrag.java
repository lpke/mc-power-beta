package local.luke.power.ui;

import java.util.*;
import org.lwjgl.opengl.Display;

/** One pointer-owned reorder gesture; callers validate its immutable source before commit. */
final class MusicListDrag {
  enum Kind { GROUPS, PLAYLIST, QUEUE }
  record Slot(String id, int y, int height) {}
  private Kind kind;
  private Object owner;
  private List<String> original = List.of();
  private List<Slot> slots = List.of();
  private int index, pressX, pressY;
  private boolean moved;
  private long lastScroll;
  boolean active() { return kind != null; }
  boolean moved() { return moved; }
  Kind kind() { return kind; }
  Object owner() { return owner; }
  int index() { return index; }
  List<String> original() { return original; }
  void begin(Kind kind, Object owner, List<String> original, List<Slot> slots, int index, int x, int y) {
    this.kind = kind; this.owner = owner; this.original = List.copyOf(original); this.slots = List.copyOf(slots);
    this.index = index; pressX = x; pressY = y; moved = false; lastScroll = 0;
  }
  void cancel() { kind = null; owner = null; original = List.of(); slots = List.of(); moved = false; }
  void update(int x, int y) {
    if (!Display.isActive()) { cancel(); return; }
    if (active()) moved |= Math.abs(x - pressX) + Math.abs(y - pressY) >= 3;
  }
  int slot(int documentY) {
    int next = 0;
    for (Slot row : slots) { if (documentY < row.y + row.height / 2) break; next++; }
    return next;
  }
  int lineY(int slot) {
    if (slots.isEmpty()) return 0;
    return slot < slots.size() ? slots.get(slot).y : slots.get(slots.size() - 1).y + slots.get(slots.size() - 1).height;
  }
  boolean scrollDue() {
    long now = System.nanoTime();
    if (!moved || now - lastScroll < 100_000_000L) return false;
    lastScroll = now; return true;
  }
}
