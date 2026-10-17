package local.luke.power.ui;

import java.util.*;
import local.luke.power.config.Catalog;
import org.lwjgl.opengl.Display;

/** Native press/release events own a temporary, two-column order draft. */
final class DebugReorder extends UiScreen {
  private static final int ROW = 12;
  private final SettingsDraft draft;
  private final DebugPreview preview;
  private DebugColumns rows;
  private String dragging;
  private int dragSide, left, top, span, tall;
  private final int[] scroll = new int[2];
  private long lastScroll;
  private String error = "";
  DebugReorder(SettingsDraft draft) { this.draft = draft; preview = new DebugPreview(draft); }
  boolean active() { return rows != null; }
  boolean changed() { return active() && (!draft.get("interface.debugOrder").value.equals(Catalog.JSON.toJsonTree(rows.left()))
      || !draft.get("interface.debugRightOrder").value.equals(Catalog.JSON.toJsonTree(rows.right()))); }
  void begin() { rows = new DebugColumns(preview.order(), preview.rightOrder()).complete(); Arrays.fill(scroll, 0); cancelDrag(); error = ""; }
  void finish() {
    if (!active()) return;
    if (changed()) {
      draft.checkpoint();
      draft.get("interface.debugOrder").value = Catalog.JSON.toJsonTree(rows.left());
      draft.get("interface.debugRightOrder").value = Catalog.JSON.toJsonTree(rows.right());
    }
    rows = null; cancelDrag();
  }
  private int x(int side) { return left + side * (span + 12); }
  private int side(int mx, int my) {
    for (int side = 0; side < 2; side++) if (inside(mx, my, x(side), top, span, tall)) return side;
    return -1;
  }
  boolean contains(int mx, int my) { return active() && side(mx, my) >= 0; }
  private List<String> shown(int side) { return DebugOrder.visible(rows.column(side), preview::enabled); }
  String help(int mx, int my) {
    if (!contains(mx, my) || dragging != null) return "";
    int side = side(mx, my), index = scroll[side] + Math.floorDiv(my - top - 5, ROW);
    var visible = shown(side);
    return index >= 0 && index < visible.size() && !visible.get(index).isEmpty() ? preview.text(visible.get(index)) : "Drop a line here to leave a gap.";
  }
  boolean press(int mx, int my, int button) {
    if (!contains(mx, my)) return false;
    if (button == 0) {
      int side = side(mx, my), index = scroll[side] + Math.floorDiv(my - top - 5, ROW);
      var visible = shown(side);
      if (index >= 0 && index < visible.size() && !visible.get(index).isEmpty()) { dragging = visible.get(index); dragSide = side; }
    }
    return true;
  }
  void cancelDrag() { dragging = null; }
  void wheel(int amount, int mx, int my) {
    int side = side(mx, my);
    if (active() && side >= 0) scroll[side] = Math.max(0, Math.min(DebugOrder.MAX_ROWS - visibleRows(), scroll[side] - Integer.signum(amount) * 3));
  }
  private int visibleRows() { return Math.max(1, Math.min(DebugOrder.MAX_ROWS, (tall - 10) / ROW)); }
  private int dropRow(int side, int my) { return Math.max(0, Math.min(DebugOrder.MAX_ROWS - 1, scroll[side] + Math.max(0, my - top - 5) / ROW)); }
  private boolean insert(int side, int my) {
    int at = Math.floorMod(my - top - 5, ROW), row = dropRow(side, my); var visible = shown(side);
    return row < visible.size() && !visible.get(row).isEmpty() || at < 3 || at >= ROW - 3;
  }
  void release(int mx, int my, int button) {
    if (button != 0 || dragging == null) return;
    if (contains(mx, my)) {
      int side = side(mx, my), at = Math.floorMod(my - top - 5, ROW);
      int target = dropRow(side, my) + (insert(side, my) && at >= ROW - 3 ? 1 : 0);
      try { rows = rows.move(dragging, dragSide, side, target, insert(side, my), preview::enabled); error = ""; }
      catch (IllegalArgumentException e) { error = e.getMessage(); }
    }
    cancelDrag();
  }
  void draw(int previewLeft, int previewTop, int previewWidth, int previewHeight, int mx, int my) {
    if (!active()) return;
    left = previewLeft + 8; top = previewTop + 20;
    span = Math.max(20, (previewWidth - 28) / 2); tall = Math.max(24, previewHeight - 28);
    if (!Display.isActive()) cancelDrag();
    // Polling can lead the queued release event. Only that event may finish a drop.
    int targetSide = side(mx, my); long now = System.nanoTime();
    if (dragging != null && targetSide >= 0 && now - lastScroll > 100_000_000L) {
      if (my >= top + tall - 14) scroll[targetSide] = Math.min(DebugOrder.MAX_ROWS - visibleRows(), scroll[targetSide] + 1);
      else if (my <= top + 14) scroll[targetSide] = Math.max(0, scroll[targetSide] - 1);
      lastScroll = now;
    }
    for (int side = 0; side < 2; side++) drawColumn(side, mx, my);
    if (!error.isEmpty()) tooltip(error, left, top + tall - 12);
  }
  private void drawColumn(int side, int mx, int my) {
    int left = x(side);
    text(side == 0 ? "Left" : "Right", left, top - 13, 0xd6e8ff);
    rectangle(left - 2, top - 2, left + span + 2, top + tall + 2, 0xff268bff);
    rectangle(left, top, left + span, top + tall, 0xf012171e);
    var visible = shown(side); clip(left, top, span, tall);
    try {
      for (int index = scroll[side]; index < Math.min(DebugOrder.MAX_ROWS, scroll[side] + visibleRows()); index++) {
        int y = top + 5 + (index - scroll[side]) * ROW;
        String id = index < visible.size() ? visible.get(index) : "";
        if (inside(mx, my, left + 2, y - 2, span - 4, ROW)) rectangle(left + 2, y - 2, left + span - 2, y + ROW - 2, 0x403c83c8);
        if (id.isEmpty()) for (int x = left + 5; x < left + span - 6; x += 6) rectangle(x, y + 7, Math.min(left + span - 5, x + 3), y + 8, 0xff424b55);
        else { text("=", left + 4, y, 0x8b9daf); text(fit(preview.text(id), span - 24), left + 16, y, id.equals(dragging) ? 0x6688aa : 0xffffff); }
      }
      if (dragging != null && side(mx, my) == side) {
        int row = dropRow(side, my), y = top + 5 + (row - scroll[side]) * ROW;
        if (insert(side, my)) {
          if (Math.floorMod(my - top - 5, ROW) >= ROW - 3) y += ROW;
          rectangle(left + 2, y - 3, left + span - 2, y - 1, 0xff52b5ff);
        } else rectangle(left + 2, y - 2, left + span - 2, y + ROW - 2, 0x8052b5ff);
        rectangle(left + 10, my - 5, left + span - 4, my + 7, 0xee182b40);
        text(fit(preview.text(dragging), span - 26), left + 14, my - 3, 0xffffff);
      }
    } finally { unclip(); }
  }
}
