package local.luke.power.inventory.tools;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.ui.UiScreen;
import local.luke.power.ui.CompactInventoryButton;
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
  private int modalVisit;
  private local.luke.power.ui.InventorySortingLayer sortingLayer;
  private final ItemStack[][] preview = new ItemStack[9][9];
  private final boolean[][] missing = new boolean[9][9];
  private final String[] rowErrors = new String[9];
  private final ItemRenderer items = new ItemRenderer();
  public boolean modal() { return modal || sortingLayer != null && sortingLayer.active(); }
  public boolean openSortingLayout() {
    if (!local.luke.power.ui.InventorySortingLayer.available(minecraft)) return false;
    try {
      NativeInventoryPlan.cancel();
      if (host instanceof InventoryDrag drag) drag.power$cancelItemDrag();
      if (host instanceof CreativePaint paint) paint.power$cancelCreativePaint();
      sortingLayer = new local.luke.power.ui.InventorySortingLayer(minecraft, (net.minecraft.client.gui.screen.container.ContainerScreen)host);
      click(); prepare(); return true;
    } catch (RuntimeException | ReflectiveOperationException failure) {
      SortingActions.feedback(minecraft, java.util.Objects.toString(failure.getMessage(), "Sorting layout is unavailable")); return false;
    }
  }
  public void wheel(int delta) { if (sortingLayer != null && sortingLayer.active()) sortingLayer.wheel(delta); }
  public void attach(Minecraft mc, Screen host, int width, int height, int panelWidth, int panelHeight, List<ButtonWidget> buttons) {
    init(mc, width, height); this.host = host; this.panelWidth = panelWidth; this.panelHeight = panelHeight;
    sort = new Header(195400); second = new Header(195401); buttons.add(sort); buttons.add(second); prepare();
  }
  private int offset() { return InventoryPanels.offset(host); }
  private boolean hotbars() { return host.getClass() == class_585.class && InventoryToolsConfig.current().creativeHotbars
      && minecraft.player instanceof PlayerMode mode && mode.power$creativeInventory(); }
  public void prepare() {
    if (sort == null) return;
    if (sortingLayer != null) { sortingLayer.prepare(); if (!sortingLayer.active()) sortingLayer = null; }
    boolean base = SortingSlots.screen(host) && minecraft.world != null && !minecraft.world.isRemote
        && !(host instanceof InventoryGui gui && gui.power$usesCatalogWheel()) && !InventoryPanels.modalOther(host, SavedCreativeHotbars.MODAL_OWNER);
    boolean storage = host.getClass() != class_585.class;
    var settings = InventoryToolsConfig.current();
    boolean showSecond = base && (storage ? settings.depositMatchingButton : hotbars() && settings.creativeHotbarsButton);
    sort.visible = base && settings.sortingButton && !modal;
    second.visible = showSecond && !modal; second.text = storage ? "D" : "H";
    var header = InventoryToolsLayout.header(width, height, panelWidth, panelHeight, offset(), sort.visible, second.visible, false);
    if (sort.visible) place(sort, header.sort());
    if (second.visible) place(second, header.second());
    // Keep header appearance stable while a carried stack temporarily blocks an action.
    sort.active = true; second.active = true;
    if (modal && (!Display.isActive() || !SavedCreativeHotbars.eligible(minecraft))) close();
    if (modal) {
      InventoryPanels.extra(host, SavedCreativeHotbars.MODAL_OWNER, List.of(new InventoryPanels.Rect(0, 0, width, height)));
      InventoryPanels.modal(host, SavedCreativeHotbars.MODAL_OWNER, true);
    }
  }
  public InventoryToolsLayout.Rect recipeButtonBounds() {
    prepare();
    return InventoryToolsLayout.header(width, height, panelWidth, panelHeight, offset(), sort.visible, second.visible, true).recipe();
  }
  private void place(Header button, InventoryToolsLayout.Rect rect) { button.x = rect.x(); button.y = rect.y(); button.rect = rect; }
  public boolean press(int x, int y, int mouseButton) {
    if (sortingLayer != null && sortingLayer.active()) {
      if (mouseButton >= 0 && mouseButton < 16) ownedButtons |= 1 << mouseButton;
      sortingLayer.press(x, y, mouseButton); return true;
    }
    boolean wasModal = modal;
    if (wasModal && mouseButton >= 0 && mouseButton < 16) ownedButtons |= 1 << mouseButton;
    prepare();
    if (wasModal && !modal) return true;
    if (modal) {
      if (mouseButton >= 0 && mouseButton < 16) ownedButtons |= 1 << mouseButton;
      if (mouseButton != 0) return true;
      var box = InventoryToolsLayout.modal(width, height, offset());
      if (new InventoryToolsLayout.Rect(box.x() + 8, box.y() + InventoryToolsLayout.MODAL_BACK_TOP, box.width() - 16, 18).contains(x, y)) { click(); close(); return true; }
      int row = (y - box.y() - InventoryToolsLayout.MODAL_ROW_TOP) / 18;
      if (y >= box.y() + InventoryToolsLayout.MODAL_ROW_TOP && row >= 0 && row < 9) {
        if (new InventoryToolsLayout.Rect(box.x() + box.width() - 82, box.y() + InventoryToolsLayout.MODAL_ROW_TOP + row * 18, 36, 16).contains(x, y)) {
          click(); if (SavedCreativeHotbars.save(minecraft, row)) refresh();
        } else if (new InventoryToolsLayout.Rect(box.x() + box.width() - 42, box.y() + InventoryToolsLayout.MODAL_ROW_TOP + row * 18, 36, 16).contains(x, y) && rowErrors[row].isEmpty()) {
          click(); int visit = modalVisit;
          SavedCreativeHotbars.load(minecraft, row, () -> {
            if (modal && modalVisit == visit && minecraft.currentScreen == host) hideModal();
          });
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
      modal = true; modalVisit++; InventoryPanels.modal(host, SavedCreativeHotbars.MODAL_OWNER, true); refresh(); prepare();
    }
    else SortingActions.depositMatching(minecraft);
    return true;
  }
  public boolean release(int mouseButton) {
    var picker = (local.luke.power.offhand.OffhandSlotPicker)host;
    return release(picker.power$offhandPointerX(), picker.power$offhandPointerY(), mouseButton);
  }
  public boolean release(int x, int y, int mouseButton) {
    boolean owned = mouseButton >= 0 && mouseButton < 16 && (ownedButtons & 1 << mouseButton) != 0;
    if (mouseButton >= 0 && mouseButton < 16) ownedButtons &= ~(1 << mouseButton);
    if (sortingLayer != null && sortingLayer.active()) {
      sortingLayer.release(x, y, mouseButton);
      return true;
    }
    return modal || owned;
  }
  public boolean key(char c, int key) {
    if (sortingLayer != null && sortingLayer.active()) { sortingLayer.key(c, key); return true; }
    if (!Keyboard.isRepeatEvent() && Bindings.matches("powerbeta.sortingLayout", key)) return openSortingLayout();
    if (modal) {
      if (key == Keyboard.KEY_ESCAPE || Bindings.matches(minecraft.options.inventoryKey, key)) close();
      else if (SavedCreativeHotbars.key(minecraft, key)) refresh();
      return true;
    }
    if (SavedCreativeHotbars.key(minecraft, key)) return true;
    if (Keyboard.isRepeatEvent()) return false;
    if (Bindings.matches("powerbeta.sortInventory", key) && SortingSlots.eligible(minecraft)) { SortingActions.sortAtPointer(minecraft); return true; }
    if (Bindings.matches("powerbeta.depositMatching", key) && SortingSlots.eligible(minecraft)) { SortingActions.depositMatching(minecraft); return true; }
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
    if (sortingLayer != null) { sortingLayer.close(); sortingLayer = null; }
    NativeInventoryPlan.cancel(); hideModal();
  }
  private void hideModal() {
    modal = false; modalVisit++;
    if (host != null) { InventoryPanels.removeExtra(host, SavedCreativeHotbars.MODAL_OWNER); InventoryPanels.modal(host, SavedCreativeHotbars.MODAL_OWNER, false); }
  }
  public void render(int x, int y, float delta) {
    if (sortingLayer != null && sortingLayer.active()) { sortingLayer.render(x, y, delta); return; }
    if (!modal) {
      if (sort != null && sort.visible && sort.rect.contains(x, y)) foregroundTooltip("Sort", x, y);
      else if (second != null && second.visible && second.rect.contains(x, y)) foregroundTooltip(host.getClass() == class_585.class
          ? "Hotbars" : "Deposit", x, y);
      return;
    }
    var box = InventoryToolsLayout.modal(width, height, offset());
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
    try {
      // Native player previews retain depth. Keep every dialog item in front of that layer.
      GL11.glTranslatef(0, 0, 300);
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      fill(0, 0, width, height, 0xa0000000); fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), 0xf0202020);
      for (int row = 0; row < 9; row++) {
        int top = box.y() + InventoryToolsLayout.MODAL_ROW_TOP + row * 18;
        for (int slot = 0; slot < 9; slot++) {
          int left = box.x() + 16 + slot * 16;
          fill(left, top, left + 16, top + 16, 0xff101010);
        }
      }
      GL11.glPushMatrix(); GL11.glRotatef(120, 1, 0, 0); net.minecraft.class_583.method_1930(); GL11.glPopMatrix();
      GL11.glEnable(32826);
      for (int row = 0; row < 9; row++) for (int slot = 0; slot < 9; slot++) {
          int left = box.x() + 16 + slot * 16, top = box.y() + InventoryToolsLayout.MODAL_ROW_TOP + row * 18;
          ItemStack item = preview[row][slot];
          if (item != null) { items.method_1487(minecraft.textRenderer, minecraft.textureManager, item, left, top); items.method_1488(minecraft.textRenderer, minecraft.textureManager, item, left, top); }
      }
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      String hoverError = "";
      for (int row = 0; row < 9; row++) {
        int top = box.y() + InventoryToolsLayout.MODAL_ROW_TOP + row * 18; text(Integer.toString(row + 1), box.x() + 4, top + 4, 0xcccccc);
        for (int slot = 0; slot < 9; slot++) if (missing[row][slot]) text("?", box.x() + 21 + slot * 16, top + 4, 0xffbb66);
        button("Save", box.x() + box.width() - 82, top, 36, 16, x, y, SavedCreativeHotbars.eligible(minecraft));
        button("Load", box.x() + box.width() - 42, top, 36, 16, x, y, rowErrors[row].isEmpty() && SavedCreativeHotbars.eligible(minecraft));
        if (!rowErrors[row].isEmpty() && y >= top && y < top + 18) hoverError = rowErrors[row];
      }
      button("Back", box.x() + 8, box.y() + InventoryToolsLayout.MODAL_BACK_TOP, box.width() - 16, 18, x, y, true);
      if (!hoverError.isEmpty()) foregroundTooltip(hoverError, x, y);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
  public void renderHeader() {
    if (sort != null) sort.draw();
    if (second != null) second.draw();
  }
  private final class Header extends ButtonWidget {
    InventoryToolsLayout.Rect rect = new InventoryToolsLayout.Rect(0, 0, 0, 0);
    Header(int id) { super(id, 0, 0, InventoryToolsLayout.BUTTON_SIZE, InventoryToolsLayout.BUTTON_SIZE, "S"); }
    // Native Screen.render runs after item tooltips. These widgets retain input ownership only.
    @Override public void render(Minecraft mc, int x, int y) {}
    void draw() {
      if (visible) CompactInventoryButton.draw(minecraft, text, rect.x(), rect.y(), active);
    }
  }
}
