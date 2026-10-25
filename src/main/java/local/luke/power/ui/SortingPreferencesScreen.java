package local.luke.power.ui;

import java.util.*;
import local.luke.power.config.*;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import local.luke.power.inventory.tools.SortingLayoutsData;
import local.luke.power.inventory.tools.SortingProtectionDrag;
import local.luke.power.storage.PowerConfig;
import com.google.gson.*;

/** A settings-only slot diagram. It never reads or edits a gameplay inventory. */
public final class SortingPreferencesScreen extends UiScreen {
  private final Screen parent;
  private final Runnable accepted;
  private final boolean immediate;
  private final ScrollBar presetBar = new ScrollBar();
  private final SlotDoubleClick clicks = new SlotDoubleClick();
  private final Setting protectedSetting, preferredSetting;
  private final Set<Integer> protectedSlots = new LinkedHashSet<>();
  private final Map<Integer, String> preferredItems = new LinkedHashMap<>();
  private final Map<Integer, ItemCatalogue.Entry> icons = new HashMap<>();
  private final ItemIcons renderer = new ItemIcons();
  private final DraftExitPrompt exit = new DraftExitPrompt();
  private final SortingProtectionDrag drag = new SortingProtectionDrag();
  private final TextInput presetName = new TextInput("", 64);
  private final String layoutWorld;
  private JsonElement originalPresets;
  private SortingLayoutsData presets;
  private int presetIndex;
  private boolean choosingPreset;
  private int presetFocus, presetScroll;
  private String presetProblem = "";
  private int selected = 9;
  private String error = "";

  public SortingPreferencesScreen(Screen parent, ConfigSession session, Runnable accepted) {
    this(parent, session, accepted, false);
  }
  public SortingPreferencesScreen(Screen parent, ConfigSession session, Runnable accepted, boolean immediate) {
    this.parent = parent; this.accepted = accepted; this.immediate = immediate;
    layoutWorld = local.luke.power.inventory.tools.WorldSortingLayouts.world();
    protectedSetting = setting(session, "inventoryTools.protectedSlots");
    preferredSetting = setting(session, "inventoryTools.preferredItems");
    for (var value : protectedSetting.value.getAsJsonArray()) protectedSlots.add(value.getAsInt());
    for (var entry : preferredSetting.value.getAsJsonObject().entrySet())
      preferredItems.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
    originalPresets = local.luke.power.inventory.tools.SortingLayoutPresets.snapshot();
    try { presets = SortingLayoutsData.read(originalPresets == null ? new JsonObject() : originalPresets.getAsJsonObject()); }
    catch (RuntimeException unsupported) { presetProblem = Objects.toString(unsupported.getMessage(), "Sorting presets are unavailable"); }
    refreshIcons();
  }
  private static Setting setting(ConfigSession session, String id) {
    return session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
  }
  private void refreshIcons() {
    icons.clear();
    for (var entry : preferredItems.entrySet()) try {
      icons.put(entry.getKey(), ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, entry.getValue()));
    } catch (IllegalArgumentException unavailable) { /* Retain unavailable preferences until explicitly changed. */ }
  }
  private int left() { return (width - 198) / 2; }
  private int gridTop() { return Math.max(32, (height - (immediate ? 152 : 190)) / 2); }
  private int controlsTop() { return gridTop() + 76; }
  private int presetsTop() { return controlsTop() + 38; }
  private int presetRows() { return Math.min(presets.size(), Math.max(1, (presetsTop() - 28) / 20)); }
  private int presetMenuTop() { return presetsTop() - presetRows() * 20 - 4; }
  private List<String> presetLabels() {
    var labels = new ArrayList<String>();
    for (int i = 0; i < presets.size(); i++) labels.add(presets.preset(i).get("name").getAsString());
    return labels;
  }
  private ScrollBar.Track presetTrack() {
    return new ScrollBar.Track(left() + 193, presetMenuTop(), presetRows() * 20, presets.size() * 20, presetScroll * 20d);
  }
  private void dragPreset(int y) { presetScroll = (int)presetBar.drag(presetTrack(), y, true) / 20; }
  private void revealPreset() {
    presetScroll = Math.max(0, Math.min(presetScroll, presets.size() - presetRows()));
    if (presetFocus < presetScroll) presetScroll = presetFocus;
    if (presetFocus >= presetScroll + presetRows()) presetScroll = presetFocus - presetRows() + 1;
  }
  @Override public void init() { Keyboard.enableRepeatEvents(true); }
  @Override public void removed() { presetBar.release(); Keyboard.enableRepeatEvents(false); drag.cancel(); clicks.cancel(); TextDrag.cancel(); }
  private boolean changed() {
    return presetChanged() || !Catalog.JSON.toJsonTree(protectedSlots).equals(protectedSetting.value)
        || !Catalog.JSON.toJsonTree(preferredItems).equals(preferredSetting.value);
  }
  private boolean presetChanged() {
    return presets != null && !presets.json().equals(SortingLayoutsData.read(originalPresets == null ? new JsonObject() : originalPresets.getAsJsonObject()).json());
  }
  private int slotAt(int x, int y) {
    return inside(x, y, left(), gridTop(), 198, 66) ? 9 + (y - gridTop()) / 22 * 9 + (x - left()) / 22 : -1;
  }
  @Override public void onMouseEvent() {
    super.onMouseEvent();
    if (!Display.isActive() || exit.active()) { drag.cancel(); return; }
    if (choosingPreset) {
      layerWheel(Mouse.getEventDWheel());
      return;
    }
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    int y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    drag.move(slotAt(x, y), this::paint);
  }
  @Override public void layerWheel(int wheel) {
    if (choosingPreset && !exit.active() && wheel != 0)
      presetScroll = Math.max(0, Math.min(presets.size() - presetRows(), presetScroll - Integer.signum(wheel)));
  }
  private void paint(int slot, boolean protect) {
    clicks.cancel(); if (protect) protectedSlots.add(slot); else protectedSlots.remove(slot); applyNow();
  }
  private void applyNow() {
    if (!immediate) return;
    try {
      local.luke.power.inventory.tools.WorldSortingLayouts.requireWorld(layoutWorld);
      var locked = Catalog.JSON.toJsonTree(protectedSlots); var preferred = Catalog.JSON.toJsonTree(preferredItems);
      protectedSetting.validate(locked); preferredSetting.validate(preferred); SortingLayoutsData.layout(locked, preferred);
      if (presetChanged()) {
        local.luke.power.inventory.tools.SortingLayoutPresets.queued(originalPresets, presets.json());
        originalPresets = presets.json().deepCopy();
      }
      local.luke.power.inventory.tools.InventoryToolsConfig.queuedLayout(protectedSlots, preferredItems);
      protectedSetting.value = locked; preferredSetting.value = preferred; error = "";
    } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Invalid sorting layout"); }
  }
  @Override protected void mouseReleased(int x, int y, int button) {
    if (choosingPreset && button == 0) { dragPreset(y); presetBar.release(); }
    if (Display.isActive() && !exit.active() && !choosingPreset) drag.release(slotAt(x, y), button, this::paint); else drag.cancel();
  }
  private void presetAction(int action) {
    if (presets == null) { error = presetProblem; return; }
    drag.cancel();
    try {
      if (action == 1) {
        presets = presets.save(presetName.text(), Catalog.JSON.toJsonTree(protectedSlots), Catalog.JSON.toJsonTree(preferredItems));
        presetIndex = presets.size() - 1; presetName.setText(""); presetName.focused = false;
      } else if (presets.size() > 0 && action == 2) {
        var preset = presets.preset(presetIndex);
        // Validate the complete preset before replacing either part of the local draft.
        SortingLayoutsData.layout(preset.get("protectedSlots"), preset.get("preferredItems"));
        protectedSlots.clear(); preferredItems.clear();
        for (var value : preset.getAsJsonArray("protectedSlots")) protectedSlots.add(value.getAsInt());
        for (var entry : preset.getAsJsonObject("preferredItems").entrySet()) preferredItems.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
        refreshIcons();
      } else if (presets.size() > 0 && action == 3) { presets = presets.delete(presetIndex); presetIndex = Math.max(0, Math.min(presetIndex, presets.size() - 1)); }
      error = ""; applyNow();
    } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Invalid sorting preset"); }
  }
  private void done() {
    try {
      drag.cancel();
      local.luke.power.inventory.tools.WorldSortingLayouts.requireWorld(layoutWorld);
      // Unknown saved items survive a visit. Validate only new or changed selections.
      for (var entry : preferredItems.entrySet()) {
        var original = preferredSetting.value.getAsJsonObject().get(Integer.toString(entry.getKey()));
        if (original == null || !original.getAsString().equals(entry.getValue()))
          ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, entry.getValue());
      }
      var locked = Catalog.JSON.toJsonTree(protectedSlots);
      var preferred = Catalog.JSON.toJsonTree(preferredItems);
      protectedSetting.validate(locked); preferredSetting.validate(preferred);
      SortingLayoutsData.layout(locked, preferred);
      if (presetChanged()) local.luke.power.inventory.tools.SortingLayoutPresets.replace(originalPresets, presets.json());
      protectedSetting.value = locked; preferredSetting.value = preferred;
      accepted.run(); showScreen(parent);
    } catch (java.io.IOException | RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Invalid sorting layout"); }
  }
  private void discard() { drag.cancel(); showScreen(parent); }
  private void chooseItem() {
    showScreen(new ItemPickerScreen(this, "Preferred item for slot " + (selected - 8),
        ItemCatalogue.Domain.FILTER, preferredItems.containsKey(selected) ? List.of(preferredItems.get(selected)) : List.of(),
        false, new ItemPickerScreen.Options(true, entry -> true),
        values -> { preferredItems.put(selected, values.get(0)); refreshIcons(); error = ""; applyNow(); }));
  }
  @Override protected void keyPressed(char character, int key) {
    if (exit.key(key)) return;
    if (choosingPreset) {
      if (key == Keyboard.KEY_ESCAPE) choosingPreset = false;
      else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
        presetFocus = Math.floorMod(presetFocus + (key == Keyboard.KEY_UP ? -1 : 1), presets.size()); revealPreset();
      } else if (key == Keyboard.KEY_HOME || key == Keyboard.KEY_END) {
        presetFocus = key == Keyboard.KEY_HOME ? 0 : presets.size() - 1; revealPreset();
      } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
        click(); presetIndex = presetFocus; choosingPreset = false;
      }
      return;
    }
    if (presetName.focused) {
      if (key == Keyboard.KEY_ESCAPE) { presetName.focused = false; presetName.setText(""); }
      else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); presetAction(1); }
      else presetName.key(character, key);
      return;
    }
    if (key == Keyboard.KEY_ESCAPE) { drag.cancel(); if (immediate) discard(); else exit.leave(changed(), this::discard); }
    else if (!immediate && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) done();
  }
  @Override protected void mouseClicked(int x, int y, int button) {
    if (exit.click(this, x, y, button, this::done, this::discard)) return;
    if (button != 0 && button != 1) return;
    if (choosingPreset) {
      if (button == 0 && presetBar.press(presetTrack(), x, y)) { dragPreset(y); return; }
      if (button == 0 && inside(x, y, left(), presetMenuTop(), 198, presetRows() * 20)) {
        presetIndex = presetScroll + (y - presetMenuTop()) / 20; click();
      }
      choosingPreset = false;
      return;
    }
    if (inside(x, y, left(), gridTop(), 198, 66)) {
      presetName.focused = false;
      selected = slotAt(x, y);
      if (button == 1 && preferredItems.containsKey(selected)) {
        click(); drag.cancel(); clicks.cancel(); preferredItems.remove(selected); refreshIcons(); applyNow(); return;
      }
      click();
      if (clicks.press(selected, button, Mouse.getEventNanoseconds())) { drag.cancel(); chooseItem(); return; }
      drag.begin(selected, button, protectedSlots.contains(selected));
      return;
    }
    clicks.cancel();
    int top = controlsTop();
    if (inside(x, y, left(), presetsTop() + 20, 148, 18) && presets != null) { presetName.focused = true; presetName.click(x, button); return; }
    if (button != 0) return;
    presetName.focused = false;
    if (inside(x, y, left(), presetsTop(), 110, 18)) {
      if (presets != null && presets.size() > 0) {
        click(); drag.cancel(); choosingPreset = true; presetFocus = presetIndex; revealPreset();
      }
      return;
    }
    if (inside(x, y, left() + 114, presetsTop(), 40, 18)) { click(); presetAction(2); return; }
    if (inside(x, y, left() + 158, presetsTop(), 40, 18)) { click(); presetAction(3); return; }
    if (inside(x, y, left() + 152, presetsTop() + 20, 46, 18)) { click(); presetAction(1); return; }
    if (inside(x, y, left(), top, 64, 18)) { click(); chooseItem(); }
    else if (inside(x, y, left() + 67, top, 64, 18) && preferredItems.containsKey(selected)) {
      click(); preferredItems.remove(selected); refreshIcons(); applyNow();
    } else if (inside(x, y, left() + 134, top, 64, 18)) {
      click(); protectedSlots.clear(); preferredItems.clear();
      for (var value : protectedSetting.defaultValue.getAsJsonArray()) protectedSlots.add(value.getAsInt());
      for (var entry : preferredSetting.defaultValue.getAsJsonObject().entrySet())
        preferredItems.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
      refreshIcons(); applyNow();
    } else if (!immediate && inside(x, y, width / 2 - 80, height - 28, 76, 20)) { click(); done(); }
    else if (!immediate && inside(x, y, width / 2 + 4, height - 28, 76, 20)) { click(); discard(); }
  }
  @Override public void render(int mouseX, int mouseY, float delta) {
    if (!Display.isActive() || exit.active()) { drag.cancel(); clicks.cancel(); choosingPreset = false; }
    else if (drag.button() >= 0 && Mouse.isButtonDown(drag.button())) drag.move(slotAt(mouseX, mouseY), this::paint);
    int pointerX = mouseX, pointerY = mouseY;
    if (exit.active() || choosingPreset) { mouseX = -1; mouseY = -1; }
    renderBackground();
    if (inLayer()) fill(left() - 8, gridTop() - 8, left() + 206, presetsTop() + 46, 0xf0202020);
    titleBar("Sorting layout");
    if (!error.isEmpty()) drawCenteredTextWithShadow(textRenderer, fit(error, width - 24), width / 2, gridTop() - 12, 0xffaaaa);
    String tooltip = "";
    for (int slot = 9; slot < 36; slot++) {
      int x = left() + (slot - 9) % 9 * 22, y = gridTop() + (slot - 9) / 9 * 22;
      boolean locked = protectedSlots.contains(slot), active = slot == selected;
      fill(x, y, x + 22, y + 22, active ? 0xffaaaaaa : 0xff555555);
      fill(x + 1, y + 1, x + 21, y + 21, locked ? 0xff5b3434 : 0xff202020);
      var item = icons.get(slot);
      if (item != null) renderer.draw(this, minecraft, item, x + 3, y + 3);
      else if (preferredItems.containsKey(slot)) text("?", x + 8, y + 7, 0xffbb66);
      if (locked) text("x", x + 15, y + 2, 0xff8888);
      if (inside(mouseX, mouseY, x, y, 22, 22)) tooltip = "Slot " + (slot - 8) + (locked ? " · Protected" : " · Sortable")
          + (preferredItems.containsKey(slot) ? "\n" + (item == null ? preferredItems.get(slot) : item.name()) + "\nRight-click to clear" : "\nRight-click to " + (locked ? "unprotect" : "protect"));
    }
    int top = controlsTop();
    button("Item", left(), top, 64, 18, mouseX, mouseY, true);
    button("Clear", left() + 67, top, 64, 18, mouseX, mouseY, preferredItems.containsKey(selected));
    button("Reset", left() + 134, top, 64, 18, mouseX, mouseY, true);
    WaypointSetDropdown.button(this, presets == null ? "Presets unavailable" : presets.size() == 0 ? "No presets" : presets.preset(presetIndex).get("name").getAsString(),
        left(), presetsTop(), 110, mouseX, mouseY, presets != null && presets.size() > 0, choosingPreset);
    button("Load", left() + 114, presetsTop(), 40, 18, mouseX, mouseY, presets != null && presets.size() > 0);
    button("Delete", left() + 158, presetsTop(), 40, 18, mouseX, mouseY, presets != null && presets.size() > 0);
    input(presetName, left(), presetsTop() + 20, 148, mouseX, mouseY, "New preset name");
    button("Save", left() + 152, presetsTop() + 20, 46, 18, mouseX, mouseY, presets != null && !presetName.text().isBlank());
    if (presets == null && inside(mouseX, mouseY, left(), presetsTop(), 198, 40)) tooltip = presetProblem;
    if (!immediate) {
      button("Done", width / 2 - 80, height - 28, 76, 20, mouseX, mouseY, true);
      button("Discard", width / 2 + 4, height - 28, 76, 20, mouseX, mouseY, true);
    }
    if (!error.isEmpty() && mouseY >= 22 && mouseY < gridTop()) tooltip = error;
    if (!tooltip.isEmpty()) foregroundTooltip(tooltip, mouseX, mouseY);
    if (choosingPreset) {
      dragPreset(pointerY);
      WaypointSetDropdown.render(this, presetLabels(), presetIndex, presetFocus, presetScroll,
          left(), presetMenuTop(), 198, presetRows(), pointerX, pointerY);
      presetBar.render(this, presetTrack());
    } else presetBar.release();
    if (!immediate) exit.render(this, "Save sorting layout?", pointerX, pointerY);
  }
}
