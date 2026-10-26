package local.luke.power.ui;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.tools.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Edits encoded preset copies. No gameplay inventory is read or changed. */
public final class SavedHotbarsScreen extends UiScreen {
  private final Screen parent;
  private final SavedCreativeHotbarsData original;
  private SavedCreativeHotbarsData draft;
  private final ItemStack[][] preview = new ItemStack[9][9];
  private final String[][] names = new String[9][9];
  private final ItemRenderer renderer = new ItemRenderer();
  private final DraftExitPrompt exit = new DraftExitPrompt();
  private final TextInput count = new TextInput("", 7);
  private int row, slot;
  private String error = "";
  public SavedHotbarsScreen(Screen parent) {
    this.parent = parent; original = SavedCreativeHotbars.editorSnapshot(); draft = original; refresh();
  }
  private int left() { return (width - 300) / 2; }
  private int top() { return Math.max(32, (height - 192) / 2); }
  private int controls() { return left() + 178; }
  private boolean changed() { return !draft.json().equals(original.json()); }
  private boolean doneEnabled() {
    return changed() || count.focused && preview[row][slot] != null
        && !count.text().equals(Integer.toString(preview[row][slot].count));
  }
  private void refresh() {
    for (int r = 0; r < 9; r++) {
      String[] stored = draft.bar(r);
      for (int s = 0; s < 9; s++) {
        try {
          preview[r][s] = stored[s] == null ? null : DeathItems.decode(stored[s]);
          names[r][s] = preview[r][s] == null ? "" : ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, Integer.toString(preview[r][s].itemId)).name();
        } catch (IOException | RuntimeException missing) { preview[r][s] = null; names[r][s] = ""; }
      }
    }
    count.focused = false; count.setText(preview[row][slot] == null ? "" : Integer.toString(preview[row][slot].count));
  }
  private void set(String value) {
    String[] items = draft.bar(row); items[slot] = value; draft = draft.with(row, items); refresh(); error = "";
  }
  private void choose() {
    minecraft.setScreen(new ItemPickerScreen(this, "Hotbar " + (row + 1) + ", slot " + (slot + 1),
        ItemCatalogue.Domain.FILTER, List.of(), false, values -> {
          try { set(DeathItems.encode(ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, values.get(0)).icon())); }
          catch (IOException | RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Cannot save this item"); }
        }));
  }
  private void applyCount() {
    if (preview[row][slot] == null) return;
    try {
      ItemStack item = DeathItems.decode(draft.bar(row)[slot]);
      int value = Integer.parseInt(count.text());
      if (value < 1 || value > Math.min(64, item.getMaxCount())) throw new IllegalArgumentException("Use 1 to " + Math.min(64, item.getMaxCount()) + " items.");
      item.count = value; set(DeathItems.encode(item));
    } catch (IOException | RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Invalid item count"); }
  }
  private void done() {
    if (!doneEnabled()) return;
    try {
      if (count.focused) { applyCount(); if (!error.isEmpty()) return; }
      if (changed()) SavedCreativeHotbars.acceptEdit(minecraft, original, draft);
      minecraft.setScreen(parent);
    } catch (IOException | RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Saved hotbars could not be saved"); }
  }
  private void discard() { minecraft.setScreen(parent); }
  @Override protected void keyPressed(char c, int key) {
    if (exit.key(key)) return;
    if (count.focused) {
      if (key == Keyboard.KEY_ESCAPE) refresh();
      else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); applyCount(); }
      else count.key(c, key);
      return;
    }
    if (key == Keyboard.KEY_ESCAPE) exit.leave(changed(), this::discard);
    else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) done();
  }
  @Override protected void mouseClicked(int x, int y, int button) {
    if (exit.click(this, x, y, button, this::done, this::discard)) return;
    if (button == 1 && inside(x, y, controls() + 34, top() + 66, 58, 18) && preview[row][slot] != null) {
      count.focused = true; count.click(x, button); return;
    }
    if (button != 0) return;
    if (inside(x, y, left(), top(), 162, 162)) {
      row = (y - top()) / 18; slot = (x - left()) / 18; click(); refresh(); return;
    }
    int cx = controls(), ty = top();
    if (inside(x, y, cx, ty + 18, 122, 18)) { click(); choose(); }
    else if (inside(x, y, cx, ty + 40, 122, 18)) { click(); set(null); }
    else if (inside(x, y, cx + 34, ty + 66, 58, 18) && preview[row][slot] != null) { count.focused = true; count.click(x, button); }
    else if (inside(x, y, cx + 96, ty + 66, 26, 18) && preview[row][slot] != null) { click(); applyCount(); }
    else if (inside(x, y, cx, ty + 94, 122, 18)) { click(); draft = draft.with(row, new String[9]); refresh(); }
    else if (doneEnabled() && inside(x, y, width / 2 - 80, height - 28, 76, 20)) { click(); done(); }
    else if (inside(x, y, width / 2 + 4, height - 28, 76, 20)) { click(); discard(); }
  }
  @Override public void render(int mx, int my, float delta) {
    int pointerX = mx, pointerY = my;
    if (exit.active()) { mx = -1; my = -1; }
    renderBackground(); titleBar("Edit saved hotbars");
    String tooltip = "";
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      for (int r = 0; r < 9; r++) {
        text(Integer.toString(r + 1), left() - 8, top() + r * 18 + 5, 0xaaaaaa);
        for (int s = 0; s < 9; s++) {
          int x = left() + s * 18, y = top() + r * 18;
          fill(x, y, x + 18, y + 18, r == row && s == slot ? 0xffaaaaaa : 0xff555555);
          fill(x + 1, y + 1, x + 17, y + 17, 0xff202020);
        }
      }
      for (int r = 0; r < 9; r++) for (int s = 0; s < 9; s++) {
        ItemStack item = preview[r][s]; int x = left() + s * 18 + 1, y = top() + r * 18 + 1;
        GuiItems.draw(renderer, minecraft, item, x, y, true);
      }
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      for (int r = 0; r < 9; r++) for (int s = 0; s < 9; s++) {
        int x = left() + s * 18, y = top() + r * 18;
        if (preview[r][s] == null && draft.bar(r)[s] != null) {
          text("?", x + 5, y + 5, 0xffbb66);
          if (inside(mx, my, x, y, 18, 18)) tooltip = "Unavailable item. Saved data is retained until you replace or clear this slot.";
        } else if (preview[r][s] != null && inside(mx, my, x, y, 18, 18)) {
          tooltip = "Hotbar " + (r + 1) + ", slot " + (s + 1) + "\n" + names[r][s];
        }
      }
      int cx = controls(), ty = top();
      text("Hotbar " + (row + 1) + ", slot " + (slot + 1), cx, ty + 4, 0xffffff);
      button("Choose item", cx, ty + 18, 122, 18, mx, my, true);
      button("Clear slot", cx, ty + 40, 122, 18, mx, my, draft.bar(row)[slot] != null);
      text("Count", cx, ty + 71, 0xcccccc);
      input(count, cx + 34, ty + 66, 58, mx, my, "");
      button("OK", cx + 96, ty + 66, 26, 18, mx, my, preview[row][slot] != null);
      button("Clear hotbar", cx, ty + 94, 122, 18, mx, my, true);
      button("Done", width / 2 - 80, height - 28, 76, 20, mx, my, doneEnabled());
      button("Discard", width / 2 + 4, height - 28, 76, 20, mx, my, true);
      if (!error.isEmpty()) drawCenteredTextWithShadow(textRenderer, fit(error, width - 20), width / 2, height - 41, 0xffaaaa);
      if (!tooltip.isEmpty()) foregroundTooltip(tooltip, mx, my);
    } finally { GL11.glPopAttrib(); }
    exit.render(this, "Save hotbar changes?", pointerX, pointerY);
  }
}
