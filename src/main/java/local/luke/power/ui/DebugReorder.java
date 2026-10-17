package local.luke.power.ui;

import java.util.*;
import local.luke.power.config.Catalog;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** A temporary order draft, drawn at native GUI font size inside the preview. */
final class DebugReorder extends UiScreen {
  private static final int ROW = 12;
  private final SettingsDraft draft;
  private final DebugPreview preview;
  private List<String> rows;
  private String dragging;
  private int left, top, span, tall, scroll;
  private long lastScroll;
  private String error = "";
  DebugReorder(SettingsDraft draft) { this.draft = draft; preview = new DebugPreview(draft); }
  boolean active() { return rows != null; }
  boolean changed() { return active() && !draft.get("interface.debugOrder").value.equals(Catalog.JSON.toJsonTree(rows)); }
  void begin() { rows = DebugOrder.complete(preview.order()); scroll = 0; dragging = null; error = ""; }
  void finish() {
    if (!active()) return;
    var chosen = Catalog.JSON.toJsonTree(rows);
    if (!draft.get("interface.debugOrder").value.equals(chosen)) {
      draft.checkpoint(); draft.get("interface.debugOrder").value = chosen;
    }
    rows = null; dragging = null;
  }
  boolean contains(int x, int y) { return active() && inside(x, y, left, top, span, tall); }
  private List<String> shown() { return DebugOrder.visible(rows, preview::enabled); }
  boolean press(int x, int y, int button) {
    if (!contains(x, y)) return false;
    if (button == 0) {
      int index = scroll + (y - top - 5) / ROW;
      var shown = shown();
      if (index >= 0 && index < shown.size() && !shown.get(index).isEmpty()) dragging = shown.get(index);
    }
    return true;
  }
  void cancelDrag() { dragging = null; }
  void wheel(int amount) { if (active()) scroll = Math.max(0, Math.min(DebugOrder.MAX_ROWS - visibleRows(), scroll - Integer.signum(amount) * 3)); }
  private int visibleRows() { return Math.max(1, Math.min(DebugOrder.MAX_ROWS, (tall - 10) / ROW)); }
  private int dropRow(int y) { return Math.max(0, Math.min(DebugOrder.MAX_ROWS - 1, scroll + Math.max(0, y - top - 5) / ROW)); }
  private boolean insert(int y) {
    int at = Math.floorMod(y - top - 5, ROW);
    var shown = shown(); int row = dropRow(y);
    return row < shown.size() && !shown.get(row).isEmpty() || at < 3 || at >= ROW - 3;
  }
  void release(int x, int y, int button) {
    if (button != 0 || dragging == null) return;
    if (contains(x, y)) {
      int at = Math.floorMod(y - top - 5, ROW);
      int target = dropRow(y) + (insert(y) && at >= ROW - 3 ? 1 : 0);
      try { rows = DebugOrder.move(rows, dragging, target, insert(y), preview::enabled); error = ""; }
      catch (IllegalArgumentException e) { error = e.getMessage(); }
    }
    dragging = null;
  }
  void draw(int previewLeft, int previewTop, int previewWidth, int previewHeight, int mx, int my) {
    if (!active()) return;
    int desired = Arrays.stream(DebugLines.values()).filter(line -> preview.enabled(line.id))
        .mapToInt(line -> textRenderer.getWidth(preview.text(line.id))).max().orElse(160) + 22;
    left = previewLeft + 8; top = previewTop + 8;
    span = Math.max(20, Math.min(previewWidth - 16, desired)); tall = Math.max(24, previewHeight - 16);
    if (!Display.isActive()) cancelDrag();
    if (dragging != null && !Mouse.isButtonDown(0)) cancelDrag();
    long now = System.nanoTime();
    if (dragging != null && now - lastScroll > 100_000_000L) {
      if (my >= top + tall - 14) scroll = Math.min(DebugOrder.MAX_ROWS - visibleRows(), scroll + 1);
      else if (my <= top + 14) scroll = Math.max(0, scroll - 1);
      lastScroll = now;
    }
    rectangle(left - 2, top - 2, left + span + 2, top + tall + 2, 0xff268bff);
    rectangle(left, top, left + span, top + tall, 0xf012171e);
    var visible = shown();
    clip(left, top, span, tall);
    try {
      for (int index = scroll; index < Math.min(DebugOrder.MAX_ROWS, scroll + visibleRows()); index++) {
        int y = top + 5 + (index - scroll) * ROW;
        String id = index < visible.size() ? visible.get(index) : "";
        if (inside(mx, my, left + 2, y - 2, span - 4, ROW)) rectangle(left + 2, y - 2, left + span - 2, y + ROW - 2, 0x403c83c8);
        if (id.isEmpty()) for (int x = left + 5; x < left + span - 6; x += 6) rectangle(x, y + 7, Math.min(left + span - 5, x + 3), y + 8, 0xff424b55);
        else {
          text("=", left + 4, y, 0x8b9daf);
          text(fit(preview.text(id), span - 24), left + 16, y, id.equals(dragging) ? 0x6688aa : 0xffffff);
        }
      }
      if (dragging != null && contains(mx, my)) {
        int row = dropRow(my), y = top + 5 + (row - scroll) * ROW;
        if (insert(my)) {
          if (Math.floorMod(my - top - 5, ROW) >= ROW - 3) y += ROW;
          rectangle(left + 2, y - 3, left + span - 2, y - 1, 0xff52b5ff);
        }
        else {
          rectangle(left + 2, y - 2, left + span - 2, y + ROW - 2, 0x8052b5ff);
        }
        rectangle(left + 10, my - 5, left + span - 4, my + 7, 0xee182b40);
        text(fit(preview.text(dragging), span - 26), left + 14, my - 3, 0xffffff);
      }
    } finally { unclip(); }
    if (!error.isEmpty()) tooltip(error, left, top + tall - 12);
  }
}
