package local.luke.power.inventory.tools;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.GameRule;
import local.luke.power.storage.PowerConfig;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;

public final class SavedCreativeHotbars {
  public static final String SAVE = "powerbeta.saveCreativeHotbar", LOAD = "powerbeta.loadCreativeHotbar";
  public static final String MODAL_OWNER = "powerbeta.creativeHotbars";
  private static SavedCreativeHotbarsData data;
  private static String problem = "Saved hotbars are not ready.";
  private static final SavedHotbarKeyState KEYS = new SavedHotbarKeyState();
  private static boolean initialized;
  private SavedCreativeHotbars() {}
  /** Startup/menu initialization only: PowerConfig.section can flush pending settings. */
  public static void initialize() {
    if (initialized) return;
    initialized = true;
    try {
      data = SavedCreativeHotbarsData.read(PowerConfig.section("savedCreativeHotbars"));
      if (!HotbarRows.available()) throw new IllegalStateException("Inventory row controls could not be linked. Restart with the complete Power Beta pack; original hotbars are retained.");
      problem = "";
    }
    catch (RuntimeException unsupported) { data = null; problem = Objects.toString(unsupported.getMessage(), "Saved hotbars could not be read. Original presets retained."); }
  }
  public static String problem() { return problem; }
  public static String[] snapshot(int row) { return data == null ? new String[9] : data.bar(row); }
  public static boolean eligible(Minecraft mc) {
    return InventoryToolsConfig.current().creativeHotbars && data != null && PowerConfig.problem().isEmpty()
        && mc != null && mc.world != null && mc.player != null && !mc.world.isRemote && WorldCompatibility.ready(mc.world)
        && mc.player.world == mc.world && mc.field_2807 == mc.player && mc.player.health > 0 && !mc.player.dead
        && mc.player instanceof PlayerMode mode && mode.power$creative() && !mode.power$spectator()
        && CommandPermissions.allowed(GameRule.ALLOW_CREATIVE_MODE) && !DetachedCamera.isActive() && !InteractionState.carryingContainer
        && Display.isActive() && (mc.currentScreen == null ? mc.field_2778 && !mc.paused : !Mouse.isGrabbed()) && !InventoryTextInput.active(mc.currentScreen)
        && !InventoryPanels.modalOther(mc.currentScreen, MODAL_OWNER)
        && mc.player.inventory.getCursorStack() == null && mc.player.inventory.main.length == 36 && mc.player.inventory.armor.length == 4
        && mc.player.container == mc.player.playerContainer && (mc.currentScreen == null || mc.currentScreen.getClass() == class_585.class);
  }
  public static boolean save(Minecraft mc, int row) {
    if (!eligible(mc)) { unavailable(mc); return false; }
    try {
      NativeInventoryPlan.cancel();
      if (!HotbarRows.cancel()) throw new IOException("Inventory row controls could not be canceled safely. No preset changed.");
      if (mc.currentScreen instanceof InventoryDrag drag) drag.power$cancelItemDrag();
      if (mc.currentScreen instanceof CreativePaint paint) paint.power$cancelCreativePaint();
      ItemStack[] references = Arrays.copyOf(mc.player.inventory.main, 9); String[] items = new String[9];
      for (int i = 0; i < 9; i++) if (references[i] != null) { items[i] = DeathItems.encode(references[i]); DeathItems.decode(items[i]); }
      for (int i = 0; i < 9; i++) if (references[i] != mc.player.inventory.main[i]
          || !Objects.equals(items[i], references[i] == null ? null : DeathItems.encode(references[i]))) throw new IOException("Hotbar changed before its preset could be captured.");
      var next = data.with(row, items); PowerConfig.saveDeferred("savedCreativeHotbars", next.json()); data = next;
      message(mc, "Hotbar " + (row + 1) + " updated."); return true;
    } catch (IOException | RuntimeException failure) { SortingActions.feedback(mc, failure.getMessage()); return false; }
  }
  public static boolean load(Minecraft mc, int row) {
    if (!eligible(mc)) { unavailable(mc); return false; }
    try {
      String[] items = data.bar(row); List<NativeInventoryPlan.Change> changes = new ArrayList<>();
      for (int i = 0; i < 9; i++) changes.add(NativeInventoryPlan.Change.main(mc.player.inventory, i, items[i] == null ? null : DeathItems.decode(items[i])));
      if (!HotbarRows.cancel()) throw new IOException("Inventory row controls could not be canceled safely. No inventory changed.");
      return NativeInventoryPlan.submit(mc, "creative_hotbar_load", true, changes, () -> eligible(mc),
          () -> message(mc, "Hotbar " + (row + 1) + " loaded."), true);
    } catch (IOException | RuntimeException failure) { SortingActions.feedback(mc, "Preset retained. " + failure.getMessage()); return false; }
  }
  public static boolean key(Minecraft mc, int key) {
    if (key < Keyboard.KEY_1 || key > Keyboard.KEY_9) return false;
    return KEYS.event(Keyboard.getEventNanoseconds(), key, Keyboard.getEventKeyState(), Keyboard.isRepeatEvent(), () -> {
      if (!eligible(mc) || rowGesture(mc, key)
          || (Bindings.down("power_creative.modifier") && Bindings.matches("power_creative.picker", key))) return false;
      boolean load = Bindings.down(LOAD), save = Bindings.down(SAVE); if (!load && !save) return false;
      if (load) load(mc, key - Keyboard.KEY_1); else save(mc, key - Keyboard.KEY_1); return true;
    });
  }
  public static void reconcileKeys() { KEYS.reconcile(Bindings::physical); }
  private static boolean rowGesture(Minecraft mc, int key) {
    return HotbarRows.owns(mc, key);
  }
  private static void unavailable(Minecraft mc) { SortingActions.feedback(mc, problem.isEmpty() ? "Saved hotbars require singleplayer Creative, Creative access and an empty cursor." : problem); }
  private static void message(Minecraft mc, String message) { if (mc.inGameHud != null) mc.inGameHud.addChatMessage(message); }
}
