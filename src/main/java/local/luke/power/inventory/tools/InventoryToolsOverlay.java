package local.luke.power.inventory.tools;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.ui.UiScreen;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.*;
import org.lwjgl.opengl.*;

/** A modal stays inside its native container, preserving crafting and cursor cleanup ownership. */
public final class InventoryToolsOverlay extends UiScreen {
  private Screen host;
  private int panelWidth, panelHeight, ownedButtons;
  private Header sort, second;
  private boolean modal;
  private final ItemStack[][] preview = new ItemStack[9][9];
  private final boolean[][] missing = new boolean[9][9];
  private final String[] rowErrors = new String[9];
  private final ItemRenderer items = new ItemRenderer();
  public boolean modal() { return modal; }
  public void attach(Minecraft mc, Screen host, int width, int height, int panelWidth, int panelHeight, List<ButtonWidget> buttons) {
    init(mc, width, height); this.host = host; this.panelWidth = panelWidth; this.panelHeight = panelHeight;
    sort = new Header(195400, 14); second = new Header(195401, 14); buttons.add(sort); buttons.add(second); prepare();
  }
  private int offset() { return InventoryPanels.offset(host); }
  private boolean hotbars() { return host.getClass() == class_585.class && InventoryToolsConfig.current().creativeHotbars
      && minecraft.player instanceof PlayerMode mode && mode.power$creative(); }
  public void prepare() {
    if (sort == null) return;
    boolean base = SortingSlots.screen(host) && minecraft.world != null && !minecraft.world.isRemote
        && !(host instanceof InventoryGui gui && gui.power$usesCatalogWheel()) && !InventoryPanels.modalOther(host, SavedCreativeHotbars.MODAL_OWNER);
    boolean storage = host.getClass() != class_585.class;
    boolean showSecond = base && (storage ? InventoryToolsConfig.current().depositMatching : hotbars());
    sort.visible = base && InventoryToolsConfig.current().sorting && !modal;
    second.visible = showSecond && !modal; second.text = storage ? "D" : "H";
    place(sort, InventoryToolsLayout.sort(width, height, panelWidth, panelHeight, offset(), showSecond));
    place(second, InventoryToolsLayout.second(width, height, panelWidth, panelHeight, offset()));
    // Keep header appearance stable while a carried stack temporarily blocks an action.
    sort.active = true; second.active = true;
    if (modal && (!Display.isActive() || !SavedCreativeHotbars.eligible(minecraft))) close();
    if (modal) {
      InventoryPanels.extra(host, SavedCreativeHotbars.MODAL_OWNER, List.of(new InventoryPanels.Rect(0, 0, width, height)));
      InventoryPanels.modal(host, SavedCreativeHotbars.MODAL_OWNER, true);
    }
  }
  public InventoryToolsLayout.Rect recipeButtonBounds() {
    boolean second = host != null && host.getClass() != class_585.class
        ? InventoryToolsConfig.current().depositMatching : host != null && hotbars();
    var anchor = InventoryToolsLayout.sort(width, height, panelWidth, panelHeight, offset(), second);
    return new InventoryToolsLayout.Rect(anchor.x() - 16, anchor.y(), 14, 14);
  }
  private void place(Header button, InventoryToolsLayout.Rect rect) { button.x = rect.x(); button.y = rect.y(); button.rect = rect; }
  public boolean press(int x, int y, int mouseButton) {
    boolean wasModal = modal;
    if (wasModal && mouseButton >= 0 && mouseButton < 16) ownedButtons |= 1 << mouseButton;
    prepare();
    if (wasModal && !modal) return true;
    if (modal) {
      if (mouseButton >= 0 && mouseButton < 16) ownedButtons |= 1 << mouseButton;
      if (mouseButton != 0) return true;
      var box = InventoryToolsLayout.modal(width, height, offset());
      if (new InventoryToolsLayout.Rect(box.x() + 8, box.y() + 202, box.width() - 16, 18).contains(x, y)) { click(); close(); return true; }
      int row = (y - box.y() - 32) / 18;
      if (y >= box.y() + 32 && row >= 0 && row < 9) {
        if (new InventoryToolsLayout.Rect(box.x() + box.width() - 82, box.y() + 32 + row * 18, 36, 16).contains(x, y)) {
          click(); if (SavedCreativeHotbars.save(minecraft, row)) refresh();
        } else if (new InventoryToolsLayout.Rect(box.x() + box.width() - 42, box.y() + 32 + row * 18, 36, 16).contains(x, y) && rowErrors[row].isEmpty()) {
          click(); SavedCreativeHotbars.load(minecraft, row);
        }
      }
      return true;
    }
    Header button = sort.visible && sort.rect.contains(x, y) ? sort : second.visible && second.rect.contains(x, y) ? second : null;
    if (button == null) return false;
    if (mouseButton >= 0 && mouseButton < 16) ownedButtons |= 1 << mouseButton;
    if (mouseButton != 0 || !button.active) return true;
    click();
    if (button == sort) SortingActions.sort(minecraft, local.luke.power.ui.TextEditing.shift());
    else if (host.getClass() == class_585.class) {
      if (!SavedCreativeHotbars.eligible(minecraft)) return true;
      NativeInventoryPlan.cancel();
      if (host instanceof InventoryDrag drag) drag.power$cancelItemDrag();
      if (host instanceof CreativePaint paint) paint.power$cancelCreativePaint();
      modal = true; InventoryPanels.modal(host, SavedCreativeHotbars.MODAL_OWNER, true); refresh(); prepare();
    }
    else SortingActions.depositMatching(minecraft);
    return true;
  }
  public boolean release(int mouseButton) {
    boolean owned = mouseButton >= 0 && mouseButton < 16 && (ownedButtons & 1 << mouseButton) != 0;
    if (mouseButton >= 0 && mouseButton < 16) ownedButtons &= ~(1 << mouseButton);
    return modal || owned;
  }
  public boolean key(char c, int key) {
    if (modal) {
      if (key == Keyboard.KEY_ESCAPE || Bindings.matches(minecraft.options.inventoryKey, key)) close();
      else if (SavedCreativeHotbars.key(minecraft, key)) refresh();
      return true;
    }
    if (SavedCreativeHotbars.key(minecraft, key)) return true;
    if (Keyboard.isRepeatEvent()) return false;
    if (Bindings.matches("powerbeta.sortInventory", key) && InventoryToolsConfig.current().sorting && SortingSlots.eligible(minecraft)) { SortingActions.sort(minecraft, local.luke.power.ui.TextEditing.shift()); return true; }
    if (Bindings.matches("powerbeta.depositMatching", key) && InventoryToolsConfig.current().depositMatching && SortingSlots.eligible(minecraft)) { SortingActions.depositMatching(minecraft); return true; }
    return false;
  }
  private void refresh() {
    for (int row = 0; row < 9; row++) {
      rowErrors[row] = ""; String[] stored = SavedCreativeHotbars.snapshot(row);
      for (int slot = 0; slot < 9; slot++) {
        missing[row][slot] = false;
        try { preview[row][slot] = stored[slot] == null ? null : DeathItems.decode(stored[slot]); }
        catch (IOException unavailable) { preview[row][slot] = null; missing[row][slot] = true; rowErrors[row] = "This preset contains unavailable item data. Its original data is retained."; }
      }
    }
  }
  public void close() {
    modal = false; NativeInventoryPlan.cancel();
    if (host != null) { InventoryPanels.removeExtra(host, SavedCreativeHotbars.MODAL_OWNER); InventoryPanels.modal(host, SavedCreativeHotbars.MODAL_OWNER, false); }
  }
  public void render(int x, int y, float delta) {
    if (!modal) {
      if (sort != null && sort.visible && sort.rect.contains(x, y)) foregroundTooltip("Sort", x, y);
      else if (second != null && second.visible && second.rect.contains(x, y)) foregroundTooltip(host.getClass() == class_585.class
          ? "Hotbars" : "Deposit", x, y);
      return;
    }
    var box = InventoryToolsLayout.modal(width, height, offset());
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      fill(0, 0, width, height, 0xa0000000); fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), 0xf0202020);
      drawCenteredTextWithShadow(textRenderer, "Creative hotbars", box.x() + box.width() / 2, box.y() + 7, 0xffffff);
      text(fit("Load replaces the nine hotbar slots.", box.width() - 12), box.x() + 6, box.y() + 20, 0xaaaaaa);
      for (int row = 0; row < 9; row++) {
        int top = box.y() + 32 + row * 18;
        for (int slot = 0; slot < 9; slot++) {
          int left = box.x() + 16 + slot * 16;
          fill(left, top, left + 16, top + 16, 0xff101010);
        }
      }
      GL11.glPushMatrix(); GL11.glRotatef(120, 1, 0, 0); net.minecraft.class_583.method_1930(); GL11.glPopMatrix();
      GL11.glEnable(32826);
      for (int row = 0; row < 9; row++) for (int slot = 0; slot < 9; slot++) {
          int left = box.x() + 16 + slot * 16, top = box.y() + 32 + row * 18;
          ItemStack item = preview[row][slot];
          if (item != null) { items.method_1487(minecraft.textRenderer, minecraft.textureManager, item, left, top); items.method_1488(minecraft.textRenderer, minecraft.textureManager, item, left, top); }
      }
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      String hoverError = "";
      for (int row = 0; row < 9; row++) {
        int top = box.y() + 32 + row * 18; text(Integer.toString(row + 1), box.x() + 4, top + 4, 0xcccccc);
        for (int slot = 0; slot < 9; slot++) if (missing[row][slot]) text("?", box.x() + 21 + slot * 16, top + 4, 0xffbb66);
        button("Save", box.x() + box.width() - 82, top, 36, 16, x, y, SavedCreativeHotbars.eligible(minecraft));
        button("Load", box.x() + box.width() - 42, top, 36, 16, x, y, rowErrors[row].isEmpty() && SavedCreativeHotbars.eligible(minecraft));
        if (!rowErrors[row].isEmpty() && y >= top && y < top + 18) hoverError = rowErrors[row];
      }
      button("Back", box.x() + 8, box.y() + 202, box.width() - 16, 18, x, y, true);
      if (!hoverError.isEmpty()) foregroundTooltip(hoverError, x, y);
    } finally { GL11.glPopAttrib(); }
  }
  private final class Header extends ButtonWidget {
    InventoryToolsLayout.Rect rect = new InventoryToolsLayout.Rect(0, 0, 0, 0);
    Header(int id, int width) { super(id, 0, 0, width, 14, "S"); }
    @Override public void render(Minecraft mc, int x, int y) {
      if (visible) InventoryToolsOverlay.this.button(text, rect.x(), rect.y(), rect.width(), rect.height(), x, y, active);
    }
  }
}
