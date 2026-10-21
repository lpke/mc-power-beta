package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.*;
import local.luke.power.mixin.DeathSlotAccessor;
import local.luke.power.recipes.RecipeBooks;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.ContainerInput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;

/** Kept alive between command phases. Only stop/failed checks restore its owned temporary state. */
final class Input400State implements AutoCloseable {
  final Minecraft mc;
  final Object owner, world;
  final String scenario;
  final int number;
  final Path report;
  final AutoTools400State nativeState;
  final Map<Long, Event> events = new LinkedHashMap<>();
  final List<Integer> actionRows = new ArrayList<>();
  int saves, loads;
  String[] beforeItems, expected;
  private final JsonObject document;
  private final RuleSet rules;
  private final Field dataField, buildingField;
  private final Object savedData, building;
  private final Map<KeyBinding, Integer> codes = new IdentityHashMap<>();
  private final Map<String, Integer> modifiers;
  private final boolean repeat;
  private final double x, y, z;
  private final Class<?> rows;
  private final Map<Field, Object> rowState = new LinkedHashMap<>(), keyState = new LinkedHashMap<>();
  private final Object keyOwner;
  private SavedCreativeHotbarsData seeded;
  private int pointerX, pointerY;

  Input400State(Minecraft mc, String scenario) throws Exception {
    this.mc = mc; this.scenario = scenario; owner = mc.player; world = mc.world;
    number = scenario.startsWith("row-") || scenario.equals("scroll-save") ? Keyboard.KEY_1 : Keyboard.KEY_4;
    PowerConfig.flushPending(); document = PowerConfig.document(); rules = AccessRules.copy(mc);
    dataField = field(SavedCreativeHotbars.class, "data"); savedData = dataField.get(null);
    check(savedData != null, "saved hotbars must be initialized before physical testing");
    buildingField = field(Class.forName("local.luke.power.building.config.Config"), "current"); building = buildingField.get(null);
    rows = Class.forName("local.luke.power.building.hotbar.Hotbars");
    for (Field f : rows.getDeclaredFields()) if (Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
      f.setAccessible(true); Object value = f.get(null); rowState.put(f, value instanceof boolean[] a ? a.clone() : value);
    }
    // held[] is final but its contents still belong to the previous input owner.
    Field held = field(rows, "held"); rowState.put(held, ((boolean[])held.get(null)).clone());
    keyOwner = field(SavedCreativeHotbars.class, "KEYS").get(null);
    for (Field f : keyOwner.getClass().getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers())) { f.setAccessible(true); keyState.put(f, f.get(keyOwner)); }
    for (KeyBinding key : mc.options.allKeys) codes.put(key, key.code);
    modifiers = Map.copyOf(Bindings.modifiers()); repeat = Keyboard.areRepeatEventsEnabled();
    x = mc.player.x; y = mc.player.y; z = mc.player.z;
    report = FabricLoader.getInstance().getGameDir().resolve("power-beta-data/reports/input-400").resolve(UUID.randomUUID().toString());
    Files.createDirectories(report); nativeState = new AutoTools400State(mc);
  }

  boolean saved() { return scenario.equals("game-save") || scenario.equals("game-load") || scenario.equals("inventory-save") || scenario.equals("inventory-load"); }
  boolean row() { return scenario.startsWith("row-") || scenario.equals("scroll-save"); }
  int activator() { return scenario.endsWith("load") ? Keyboard.KEY_X : Keyboard.KEY_C; }
  String[][] seedBars() { String[][] values = new String[9][]; for (int row = 0; row < 9; row++) values[row] = seeded.bar(row); return values; }

  void prepare() throws Exception {
    nativeState.reset(); nativeState.attack.set(false);
    AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, true); CreativeVehicleChecks.mode(mc, "CREATIVE");
    Field restoring = inheritedField(mc.player.getClass(), "power$restoringFlight"); boolean wasRestoring = restoring.getBoolean(mc.player);
    restoring.setBoolean(mc.player, true);
    try { Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer").getMethod("creative_setFlying", boolean.class).invoke(mc.player, true); }
    finally { restoring.setBoolean(mc.player, wasRestoring); }
    var tools = InventoryToolsConfig.copy(); tools.creativeHotbars = true; tools.toolSwitch = tools.restock = tools.sorting = tools.depositMatching = false;
    InventoryToolsConfig.preview(tools);
    Object temp = building.getClass().getMethod("copy").invoke(building);
    Object hotbar = temp.getClass().getField("hotbar").get(temp);
    hotbar.getClass().getField("numberRowKeys").setBoolean(hotbar, true);
    hotbar.getClass().getField("rememberRow").setBoolean(hotbar, false);
    hotbar.getClass().getField("overlay").setBoolean(hotbar, false);
    hotbar.getClass().getField("selectedRow").setInt(hotbar, 0); buildingField.set(null, temp);
    for (Field f : rowState.keySet()) {
      if (f.getType() == boolean[].class) Arrays.fill((boolean[])f.get(null), false);
      else if (f.getType() == boolean.class) f.setBoolean(null, false);
      else if (f.getName().equals("owner")) f.set(null, null);
    }
    for (KeyBinding key : mc.options.allKeys) if (key.code == Keyboard.KEY_C || key.code == Keyboard.KEY_X || key.code == Keyboard.KEY_V || key.code >= Keyboard.KEY_1 && key.code <= Keyboard.KEY_9
        || key.translationKey.startsWith("power_building.hotbar.")) key.code = 0;
    bind(SavedCreativeHotbars.SAVE, Keyboard.KEY_C); bind(SavedCreativeHotbars.LOAD, Keyboard.KEY_X);
    bind("powerbeta.swapHands", scenario.startsWith("mouse-") ? -100 : Keyboard.KEY_V);
    // Keep both real native selectors active so a missing cancellation changes selectedSlot.
    for (int i = 1; i <= 9; i++) bind("key.power_controls.hotbar_" + i, Keyboard.KEY_1 + i - 1);
    if (scenario.startsWith("row-")) bind("power_building.hotbar.base", activator());
    if (scenario.equals("scroll-save")) bind("power_building.hotbar.scroll", Keyboard.KEY_C);
    Map<String, Integer> masks = new HashMap<>(modifiers);
    for (String id : List.of(SavedCreativeHotbars.SAVE, SavedCreativeHotbars.LOAD, "powerbeta.swapHands", "power_building.hotbar.base", "power_building.hotbar.scroll",
        "power_building.hotbar.row1", "power_building.hotbar.row2", "power_building.hotbar.row3")) masks.remove(id);
    for (int i = 1; i <= 9; i++) masks.remove("key.power_controls.hotbar_" + i);
    if (scenario.startsWith("mouse-")) masks.put("powerbeta.swapHands", Chord.CTRL);
    Bindings.configure(masks); Bindings.register(mc.options.allKeys); Keyboard.enableRepeatEvents(true);
    var inventory = mc.player.inventory;
    for (int i = 0; i < 36; i++) inventory.main[i] = DeathChecks.tagged(35, i + 1, i % 16, "input400-live-" + i);
    inventory.armor[0] = DeathChecks.tagged(313, 1, 37, "input400-armor");
    Offhand.set(inventory, DeathChecks.tagged(35, 11, 14, "input400-offhand")); inventory.selectedSlot = 7;
    seeded = SavedCreativeHotbarsData.empty();
    for (int row = 0; row < 9; row++) {
      String[] items = new String[9];
      for (int i = 0; i < 9; i++) if (i != 4) items[i] = DeathItems.encode(DeathChecks.tagged(35, 33 + row + i, (row + i) % 16, "input400-saved-" + row + "-" + i));
      seeded = seeded.with(row, items);
    }
    PowerConfig.save("savedCreativeHotbars", seeded.json()); dataField.set(null, seeded);
    boolean inventoryScreen = scenario.startsWith("inventory-") || scenario.equals("offhand-inventory") || scenario.startsWith("mouse-");
    if (inventoryScreen) mc.setScreen(new class_585(mc.player));
    else if (scenario.equals("offhand-text")) mc.setScreen(new ChatScreen());
    if (mc.currentScreen != null) Mouse.setGrabbed(false);
    Keyboard.enableRepeatEvents(true);
    if (inventoryScreen) pointer();
    beforeItems = items(); expected = beforeItems.clone();
    if (saved() && scenario.endsWith("load")) System.arraycopy(seeded.bar(number - Keyboard.KEY_1), 0, expected, 0, 9);
    if (row()) for (int i = 0; i < 9; i++) { expected[i] = beforeItems[9 + i]; expected[9 + i] = beforeItems[i]; }
    if (scenario.equals("offhand-game") || scenario.equals("offhand-inventory") || scenario.equals("mouse-slot")) {
      int slot = scenario.equals("offhand-game") ? 7 : 0; expected[slot] = beforeItems[45]; expected[45] = beforeItems[slot];
    }
    check(SavedCreativeHotbars.eligible(mc) || scenario.startsWith("offhand-") || scenario.startsWith("mouse-"), "physical Creative event admission was not established");
    writeState();
  }

  private void pointer() {
    var screen = (ContainerScreen)mc.currentScreen; var panel = (ContainerInput)screen; Slot slot = screen.container.method_2084(36);
    pointerX = (screen.width - panel.power$width()) / 2 + InventoryPanels.offset(screen) + slot.x + 8;
    pointerY = (screen.height - panel.power$height()) / 2 + slot.y + 8;
    if (scenario.equals("mouse-header")) { pointerX = (screen.width - panel.power$width()) / 2 + InventoryPanels.offset(screen) + 3; pointerY = (screen.height - panel.power$height()) / 2 + 3; }
    if (scenario.equals("mouse-outside")) { pointerX = 2; pointerY = screen.height - 3; }
  }
  private void bind(String id, int code) {
    boolean found = false; for (KeyBinding key : mc.options.allKeys) if (key.translationKey.equals(id)) { key.code = code; found = true; }
    check(found, "physical fixture could not find registered key " + id);
  }
  String[] items() throws Exception {
    var items = DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer); String[] bytes = new String[items.length];
    for (int i = 0; i < items.length; i++) bytes[i] = AutoTools400State.bytes(items[i]); return bytes;
  }
  JsonObject json() throws Exception {
    JsonObject value = new JsonObject(); value.addProperty("case", scenario); value.addProperty("report", report.toString());
    value.addProperty("screen", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName());
    value.addProperty("number", number == Keyboard.KEY_1 ? "1" : "4"); value.addProperty("activator", activator() == Keyboard.KEY_X ? "x" : "c");
    value.addProperty("slot", mc.player.inventory.selectedSlot); value.addProperty("saves", saves); value.addProperty("loads", loads);
    value.addProperty("focused", Display.isActive()); value.addProperty("grabbed", Mouse.isGrabbed()); value.addProperty("pending", NativeInventoryPlan.busy());
    value.add("events", Catalog.JSON.toJsonTree(events)); value.add("actionRows", Catalog.JSON.toJsonTree(actionRows));
    value.addProperty("guiX", pointerX); value.addProperty("guiY", pointerY);
    int width = mc.currentScreen == null ? Display.getWidth() : mc.currentScreen.width, height = mc.currentScreen == null ? Display.getHeight() : mc.currentScreen.height;
    value.addProperty("windowX", (int)Math.round((pointerX + .5) * Display.getWidth() / width));
    value.addProperty("windowY", (int)Math.round((pointerY + .5) * Display.getHeight() / height));
    value.addProperty("guiWidth", width); value.addProperty("guiHeight", height); value.add("items", Catalog.JSON.toJsonTree(items()));
    value.add("offhandContext", offhandContext()); return value;
  }
  private JsonObject offhandContext() {
    JsonObject value = new JsonObject(); value.addProperty("available", OffhandInput.available(mc));
    value.addProperty("bindingCode", GameplayKeys.SWAP_HANDS.code); value.addProperty("bindingModifiers", Bindings.modifiers("powerbeta.swapHands"));
    value.addProperty("vHeld", Bindings.physical(Keyboard.KEY_V)); value.addProperty("heldModifiers", Bindings.heldModifiers());
    if (!(mc.currentScreen instanceof OffhandSlotPicker picker)) return value;
    var screen = mc.currentScreen;
    int rawX = Mouse.getX() * screen.width / mc.displayWidth, rawY = screen.height - Mouse.getY() * screen.height / mc.displayHeight - 1;
    value.addProperty("container", OffhandInput.container(mc));
    value.addProperty("cachedX", picker.power$offhandPointerX()); value.addProperty("cachedY", picker.power$offhandPointerY());
    value.addProperty("physicalX", rawX); value.addProperty("physicalY", rawY);
    value.addProperty("widget", picker.power$offhandWidgetAt(rawX, rawY)); value.addProperty("panel", InventoryPanels.contains(screen, rawX, rawY));
    value.addProperty("recipe", RecipeBooks.owns(screen, rawX, rawY));
    value.add("cachedNativeSlot", slot(picker.power$offhandSlotAt(picker.power$offhandPointerX(), picker.power$offhandPointerY())));
    value.add("physicalNativeSlot", slot(picker.power$offhandSlotAt(rawX, rawY)));
    value.add("physicalAdmittedSlot", slot(OffhandInput.pick(mc, rawX, rawY))); value.add("hoveredSlot", slot(OffhandInput.hovered(mc)));
    return value;
  }
  private JsonElement slot(Slot slot) {
    if (slot == null) return JsonNull.INSTANCE;
    var access = (DeathSlotAccessor)(Object)slot; var owner = access.power$deathInventory();
    JsonObject value = new JsonObject(); value.addProperty("id", slot.id); value.addProperty("index", access.power$deathIndex());
    value.addProperty("class", slot.getClass().getName()); value.addProperty("owner", owner == null ? "none" : owner.getClass().getName());
    value.addProperty("playerInventory", owner == mc.player.inventory); return value;
  }
  void writeState() throws Exception {
    String json = Catalog.JSON.toJson(json()) + "\n"; Files.writeString(report.resolve("state.json"), json);
    Files.writeString(FabricLoader.getInstance().getGameDir().resolve("power-beta-input-400.json"), json);
  }
  @Override public void close() throws Exception {
    try {
      try {
        rows.getMethod("cancelPending").invoke(null); nativeState.cancel();
        Method tick = Class.forName("local.luke.power.building.hotbar.HotbarTransaction").getDeclaredMethod("tick", Minecraft.class);
        tick.setAccessible(true); tick.invoke(null, mc);
      } finally { nativeState.close(); }
    }
    finally {
      try { PowerConfig.write(document); }
      finally {
        dataField.set(null, savedData); buildingField.set(null, building); AccessRules.restore(mc, rules);
        for (var entry : codes.entrySet()) entry.getKey().code = entry.getValue();
        Bindings.configure(modifiers); Bindings.register(mc.options.allKeys); Keyboard.enableRepeatEvents(repeat);
        for (var entry : rowState.entrySet()) {
          Field f = entry.getKey(); Object value = entry.getValue();
          if (value instanceof boolean[] a) System.arraycopy(a, 0, f.get(null), 0, a.length); else f.set(null, value);
        }
        for (var entry : keyState.entrySet()) entry.getKey().set(keyOwner, entry.getValue());
        mc.player.method_1340(x, y, z);
      }
    }
    check(PowerConfig.document().equals(document), "physical fixture did not restore complete settings/user data");
    check(dataField.get(null) == savedData && buildingField.get(null) == building, "physical fixture replaced original cached user data");
    for (var entry : codes.entrySet()) check(entry.getKey().code == entry.getValue(), "physical fixture did not restore key " + entry.getKey().translationKey);
  }
  private static Field field(Class<?> type, String name) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
  private static Field inheritedField(Class<?> type, String name) throws Exception {
    for (Class<?> current = type; current != null; current = current.getSuperclass()) try { return field(current, name); } catch (NoSuchFieldException absent) { }
    throw new NoSuchFieldException(name);
  }
  static final class Event {
    int reads;
    boolean numberDown, repeat, controls, minecraft, gui;
    @Override public String toString() { return "reads=" + reads + ",down=" + numberDown + ",repeat=" + repeat + ",controls=" + controls + ",minecraft=" + minecraft + ",gui=" + gui; }
  }
}
