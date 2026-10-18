package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import java.util.function.Consumer;
import local.luke.power.config.Catalog;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Selection, grouped dragging and presets live inside a separate F3 draft. */
final class DebugReorder extends UiScreen {
  private static final int ROW = 12;
  private final SettingsDraft owner;
  private final Consumer<String> select;
  private SettingsDraft draft;
  private DebugPreview preview;
  private int selectedSide = -1, anchor = -1, first = -1, last = -1;
  private boolean pressed, dragging, rangePress;
  private int pressedRow, presetScroll;
  private int pressX, pressY, left, top, span, tall, toolbarTop, toolbarWidth;
  private final int[] scroll = new int[2];
  private long lastScroll;
  private String error = "";
  private int presetIndex;
  private boolean choosing, naming, reset, remove;
  private final TextInput name = new TextInput("", 64);
  DebugReorder(SettingsDraft owner, Consumer<String> select) { this.owner = owner; this.select = select; }
  boolean active() { return draft != null; }
  boolean changed() { return active() && draft.changed(); }
  SettingsDraft draft() { return active() ? draft : owner; }
  void begin() {
    draft = new SettingsDraft(owner.settings(), s -> true); preview = new DebugPreview(draft);
    Arrays.fill(scroll, 0); clearSelection(); error = "";
  }
  void finish(boolean save) {
    if (!active()) return;
    if (save && changed()) { owner.checkpoint(); owner.restore(draft.snapshot()); }
    draft = null; choosing = naming = reset = remove = false; clearSelection();
  }
  private DebugColumns rows() { return new DebugColumns(preview.order(), preview.rightOrder()).complete(); }
  private void store(DebugColumns rows) {
    draft.get("interface.debugOrder").value = Catalog.JSON.toJsonTree(rows.left());
    draft.get("interface.debugRightOrder").value = Catalog.JSON.toJsonTree(rows.right());
  }
  void clearSelection() { selectedSide = anchor = first = last = -1; cancelDrag(); select.accept(""); }
  private int x(int side) { return left + side * (span + 12); }
  private int side(int mx, int my) {
    for (int side = 0; side < 2; side++) if (inside(mx, my, x(side), top, span, tall)) return side;
    return -1;
  }
  boolean contains(int mx, int my) { return active() && side(mx, my) >= 0; }
  private List<String> shown(int side) { return rows().column(side); }
  String help(int mx, int my) {
    if (active() && inside(mx, my, left, toolbarTop, toolbarWidth, 18)) {
      int at = Math.min(3, (mx - left) / (toolbarButtonWidth() + 4));
      return switch (at) { case 0 -> "Load a saved F3 layout and line visibility. Scroll the list for more presets.";
        case 1 -> "Save this F3 layout and visibility as a named preset.";
        case 2 -> "Remove the selected saved preset."; default -> "Restore the F3 layout for your Defaults base. Visibility is kept."; };
    }
    if (!contains(mx, my) || dragging) return "";
    int side = side(mx, my), index = scroll[side] + Math.floorDiv(my - top - 5, ROW);
    var visible = shown(side);
    return index >= 0 && index < visible.size() && !visible.get(index).isEmpty()
        ? "Click to find this setting. Shift-click selects a range; drag to move it." : "Drop a line here to leave a gap.";
  }
  boolean press(int mx, int my, int button) {
    if (!active()) return false;
    if (prompt()) return promptPress(mx, my, button);
    if (toolbarPress(mx, my, button)) return true;
    if (!contains(mx, my)) return false;
    if (button == 0) {
      int side = side(mx, my), index = scroll[side] + Math.floorDiv(my - top - 5, ROW);
      var visible = shown(side);
      if (index < 0 || index >= visible.size()) { clearSelection(); return true; }
      boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
      if (shift && selectedSide == side && anchor >= 0) {
        first = Math.min(anchor, index); last = Math.max(anchor, index); select.accept("");
      } else if (selectedSide != side || index < first || index > last) {
        if (visible.get(index).isEmpty()) { clearSelection(); return true; }
        selectedSide = side; anchor = first = last = index; select.accept(visible.get(index));
      } else if (first == last) select.accept(visible.get(index));
      pressedRow = index; rangePress = shift;
      pressed = true; dragging = false; pressX = mx; pressY = my;
    }
    return true;
  }
  void cancelDrag() { pressed = dragging = false; }
  void wheel(int amount, int mx, int my) {
    if (choosing) { presetScroll = Math.max(0, Math.min(Math.max(0, presets().size() - popupRows()), presetScroll - Integer.signum(amount))); return; }
    int side = side(mx, my);
    if (active() && !prompt() && side >= 0) scroll[side] = Math.max(0, Math.min(DebugOrder.MAX_ROWS - visibleRows(), scroll[side] - Integer.signum(amount) * 3));
  }
  private int visibleRows() { return Math.max(1, Math.min(DebugOrder.MAX_ROWS, (tall - 10) / ROW)); }
  private int dropRow(int side, int my) { return Math.max(0, Math.min(DebugOrder.MAX_ROWS - 1, scroll[side] + Math.max(0, my - top - 5) / ROW)); }
  private boolean insert(int side, int my) { return DebugColumns.insertion(shown(side), dropRow(side, my), Math.floorMod(my - top - 5, ROW), ROW); }
  void release(int mx, int my, int button) {
    if (button != 0 || !pressed) return;
    dragging |= Math.abs(mx - pressX) + Math.abs(my - pressY) >= 3;
    if (dragging && contains(mx, my) && selectedSide >= 0) {
      int side = side(mx, my), at = Math.floorMod(my - top - 5, ROW);
      boolean insert = insert(side, my);
      int target = dropRow(side, my) + (insert && at >= ROW - 3 ? 1 : 0);
      try {
        DebugColumns before = rows(), after = before.moveRange(selectedSide, first, last, side, target, insert);
        if (!before.equals(after)) { draft.checkpoint(); store(after); clearSelection(); }
        error = "";
      } catch (IllegalArgumentException e) { error = e.getMessage(); }
    }
    if (!dragging && !rangePress && first != last && selectedSide >= 0) {
      anchor = first = last = pressedRow;
      select.accept(shown(selectedSide).get(first));
    }
    cancelDrag();
  }
  boolean prompt() { return naming || reset || remove; }
  boolean key(char c, int key) {
    if (!prompt() && !choosing) return false;
    if (key == Keyboard.KEY_ESCAPE) { naming = reset = remove = choosing = false; return true; }
    if (naming) {
      if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) savePreset();
      else name.key(c, key);
    }
    return true;
  }
  private JsonArray presets() { return draft.get("interface.debugPresets").value.getAsJsonArray(); }
  private int popupRows() { return Math.max(1, Math.min(8, (height - toolbarTop - 52) / 18)); }
  private int toolbarButtonWidth() { return Math.max(20, (toolbarWidth - 12) / 4); }
  private String selectedPreset() { return presets().size() == 0 ? "No saved presets" : presets().get(Math.min(presetIndex, presets().size() - 1)).getAsJsonObject().get("name").getAsString(); }
  private boolean toolbarPress(int mx, int my, int button) {
    if (choosing) {
      if (button == 0 && inside(mx, my, left, toolbarTop + 19, toolbarWidth, Math.min(popupRows(), presets().size()) * 18)) {
        presetIndex = presetScroll + (my - toolbarTop - 19) / 18; click();
        DebugPresets.load(presets().get(presetIndex).getAsJsonObject(), draft); clearSelection();
      }
      choosing = false; return true;
    }
    if (!inside(mx, my, left, toolbarTop, toolbarWidth, 18)) return false;
    if (button == 0) {
      int at = Math.min(3, (mx - left) / (toolbarButtonWidth() + 4));
      if (at == 0 && presets().size() > 0) { click(); choosing = true; presetScroll = Math.max(0, presetIndex - popupRows() + 1); }
      else if (at == 1) { click(); naming = true; name.setText(""); name.focused = true; }
      else if (at == 2 && presets().size() > 0) { click(); remove = true; }
      else if (at == 3) { click(); reset = true; }
    }
    return true;
  }
  private void savePreset() {
    try {
      JsonArray saved = presets().deepCopy(); saved.add(DebugPresets.capture(name.text(), draft));
      DebugPresets.checked(saved); draft.checkpoint(); draft.get("interface.debugPresets").value = saved;
      presetIndex = saved.size() - 1; naming = false; error = ""; click();
    } catch (RuntimeException e) { error = e.getMessage(); }
  }
  private boolean promptPress(int mx, int my, int button) {
    if (naming && inside(mx, my, width / 2 - 110, height / 2 - 10, 220, 20)) { name.click(mx, button); return true; }
    if (button == 0 && inside(mx, my, width / 2 - 102, height / 2 + 20, 100, 20)) {
      if (naming) savePreset();
      else {
        click(); draft.checkpoint();
        if (remove) { JsonArray saved = presets().deepCopy(); saved.remove(Math.min(presetIndex, saved.size() - 1)); draft.get("interface.debugPresets").value = saved; presetIndex = 0; }
        else { draft.get("interface.debugOrder").reset(); draft.get("interface.debugRightOrder").reset(); clearSelection(); }
        reset = remove = false;
      }
    } else if (button == 0 && inside(mx, my, width / 2 + 2, height / 2 + 20, 100, 20)) { click(); naming = reset = remove = false; }
    return true;
  }
  void drawPrompt(int mx, int my) {
    if (!prompt()) return;
    rectangle(0, 0, width, height, 0xc0000000);
    centeredText(naming ? "Save F3 preset" : remove ? "Remove saved preset?" : "Reset F3 layout to defaults?", height / 2 - 32, 0xffffff);
    if (naming) input(name, width / 2 - 110, height / 2 - 10, 220, mx, my, "Preset name");
    button(naming ? "Save" : remove ? "Remove" : "Reset", width / 2 - 102, height / 2 + 20, 100, 20, mx, my, true);
    button("Cancel", width / 2 + 2, height / 2 + 20, 100, 20, mx, my, true);
    if (!error.isEmpty()) centeredText(fit(error, width - 24), height / 2 + 48, 0xff8888);
  }
  void draw(int previewLeft, int previewTop, int previewWidth, int previewHeight, int mx, int my) {
    if (!active()) return;
    left = previewLeft + 8; toolbarTop = previewTop + 5; toolbarWidth = previewWidth - 16;
    top = previewTop + 43; span = Math.max(20, (previewWidth - 28) / 2); tall = Math.max(24, previewHeight - 51);
    if (!Display.isActive()) cancelDrag();
    if (pressed && Math.abs(mx - pressX) + Math.abs(my - pressY) >= 3) dragging = true;
    int targetSide = side(mx, my); long now = System.nanoTime();
    if (dragging && targetSide >= 0 && now - lastScroll > 100_000_000L) {
      if (my >= top + tall - 14) scroll[targetSide] = Math.min(DebugOrder.MAX_ROWS - visibleRows(), scroll[targetSide] + 1);
      else if (my <= top + 14) scroll[targetSide] = Math.max(0, scroll[targetSide] - 1);
      lastScroll = now;
    }
    for (int side = 0; side < 2; side++) drawColumn(side, mx, my);
    int w = toolbarButtonWidth();
    String[] labels = {"Preset: " + selectedPreset(), "Save as...", "Remove", "Reset layout..."};
    for (int i = 0; i < labels.length; i++) button(fit(labels[i], w - 8), left + i * (w + 4), toolbarTop, w, 18, mx, my, i != 0 && i != 2 || presets().size() > 0);
    if (choosing) for (int i = presetScroll; i < Math.min(presets().size(), presetScroll + popupRows()); i++) button(fit(presets().get(i).getAsJsonObject().get("name").getAsString(), toolbarWidth - 8), left, toolbarTop + 19 + (i - presetScroll) * 18, toolbarWidth, 18, mx, my, true);
    if (!error.isEmpty() && !prompt()) tooltip(error, left, top + tall - 12);
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
        if (selectedSide == side && index >= first && index <= last) rectangle(left + 2, y - 2, left + span - 2, y + ROW - 2, 0x8052b5ff);
        else if (inside(mx, my, left + 2, y - 2, span - 4, ROW)) rectangle(left + 2, y - 2, left + span - 2, y + ROW - 2, 0x403c83c8);
        if (id.isEmpty()) for (int x = left + 5; x < left + span - 6; x += 6) rectangle(x, y + 7, Math.min(left + span - 5, x + 3), y + 8, 0xff424b55);
        else { text("=", left + 4, y, 0x8b9daf); String label = preview.text(id); text(fit(label.isEmpty() ? id : label, span - 24), left + 16, y, preview.enabled(id) ? 0xffffff : 0x888888); }
      }
      if (dragging && side(mx, my) == side) {
        int row = dropRow(side, my), y = top + 5 + (row - scroll[side]) * ROW;
        if (insert(side, my)) {
          if (Math.floorMod(my - top - 5, ROW) >= ROW - 3) y += ROW;
          rectangle(left + 2, y - 3, left + span - 2, y - 1, 0xff52b5ff);
        } else rectangle(left + 2, y - 2, left + span - 2, y + ROW * (last - first + 1) - 2, 0x8052b5ff);
      }
    } finally { unclip(); }
  }
}
