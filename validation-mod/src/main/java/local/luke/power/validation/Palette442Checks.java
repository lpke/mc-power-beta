package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.util.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;

/** Native palette events, exact cursor ownership and shared text/movement exclusion. */
final class Palette442Checks {
  private Palette442Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "focus disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0 && !NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "mute client and finish inventory actions");
    failures = 0;
    Object config = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
    Object mouse = config.getClass().getField("MOUSE_TWEAKS_CONFIG").get(config);
    Object wheel = mouse.getClass().getField("SCROLL_WHEEL_CONFIG").get(mouse);
    var enabled = wheel.getClass().getField("enableScrollWheelTweaks"); var inverted = wheel.getClass().getField("invertScrollCursorSlotDirection");
    Object oldEnabled = enabled.get(wheel), oldInverted = inverted.get(wheel);
    Class<?> registry = Class.forName("local.luke.power.creative.inventory.registry.TabRegistry");
    var savedLayout = PowerConfig.section("creativeTabs");
    try (var state = new Inventory442Checks.State(mc)) {
      enabled.set(wheel, true); inverted.set(wheel, false);
      registry.getMethod("reload", com.google.gson.JsonObject.class).invoke(null, CreativeTabsData.defaults());
      test("CREATIVE442 Search moves unusual choices behind ordinary groups while native category order stays intact", () -> {
        var nativeData = CreativeTabsData.read((com.google.gson.JsonObject)registry.getMethod("nativeLayout").invoke(null));
        var blocks = CreativeTabsData.slots(nativeData.tab(0)); var weapons = CreativeTabsData.slots(nativeData.tab(3));
        check(blocks.indexOf("7:0") >= 0 && blocks.indexOf("7:0") < blocks.indexOf("12:0"), "regular building tab moved bedrock from its original position");
        check(weapons.indexOf("302:0") >= 0 && weapons.indexOf("302:0") < weapons.indexOf("306:0"), "native equipment group moved chainmail from its original position");
        var defaults = CreativeTabsData.read((com.google.gson.JsonObject)registry.getMethod("defaultLayout").invoke(null));
        Set<String> unusual = Set.of("7:0", "19:0", "52:0", "95:0", "302:0", "303:0", "304:0", "305:0");
        Set<String> ordinary = new LinkedHashSet<>();
        for (int i = 0; i < defaults.size(); i++) for (String item : CreativeTabsData.slots(defaults.tab(i)))
          if (!item.isEmpty() && !unusual.contains(item)) ordinary.add(item);
        Object search = registry.getMethod("search").invoke(null);
        @SuppressWarnings("unchecked") var all = (List<ItemStack>)search.getClass().getMethod("getItems").invoke(search);
        var actual = all.stream().map(item -> item.itemId + ":" + item.getDamage()).toList();
        check(actual.subList(0, ordinary.size()).equals(List.copyOf(ordinary)), "Search no longer follows ordinary category order");
        for (String item : unusual) check(actual.indexOf(item) >= ordinary.size(), "unusual Search item was not retained after ordinary choices: " + item);
      });
      test("CREATIVE442 different catalog item destroys full cursor after durable flush and never copies target", () -> {
        palette(state); select(state.screen, 0); var item = items(state.screen).get(0); String target = DeathItems.encode(item);
        var cursor = DeathChecks.tagged(267, 1, 71, "palette-destroy"); mc.player.inventory.setCursorStack(cursor);
        int[] point = point(state.screen);
        try (var slow = new AutoTools400State.SlowWriter()) {
          ((ScreenInput)state.screen).power$click(point[0], point[1], 0); slow.checkBlocked();
          check(mc.player.inventory.getCursorStack() == cursor, "catalog deletion published before flush"); slow.release(); state.inventory.finish();
        }
        check(mc.player.inventory.getCursorStack() == null && target.equals(DeathItems.encode(item)), "catalog drop copied/swapped/mutated target item");
        check(Arrays.stream(mc.player.inventory.main).allMatch(Objects::isNull), "palette drop inserted an item in inventory");
      });
      test("CREATIVE442 identical ID/damage with different native NBT remains a different catalog item", () -> {
        palette(state); select(state.screen, 0); var item = items(state.screen).get(0);
        var tagged = DeathChecks.tagged(item.itemId, 7, item.getDamage(), "different-native-components"); mc.player.inventory.setCursorStack(tagged);
        int[] point = point(state.screen); ((ScreenInput)state.screen).power$click(point[0], point[1], 1); state.inventory.finish();
        check(mc.player.inventory.getCursorStack() == null, "catalog matched incompatible native NBT");
      });
      test("CREATIVE442 untouched and incompatible cursor wheel retains catalog scroll ownership", () -> {
        palette(state); select(state.screen, 0); int[] point = point(state.screen);
        InventoryWheel.event(state.screen, true, 120, false); ((ScreenInput)state.screen).power$release(point[0], point[1], -1);
        check(mc.player.inventory.getCursorStack() == null && InventoryWheel.take(state.screen, true) == 120, "untouched wheel supplied cursor or swallowed catalog scroll");
        var cursor = DeathChecks.tagged(267, 1, 71, "different-wheel"); mc.player.inventory.setCursorStack(cursor);
        InventoryWheel.event(state.screen, true, -120, false); ((ScreenInput)state.screen).power$release(point[0], point[1], -1);
        check(mc.player.inventory.getCursorStack() == cursor && InventoryWheel.take(state.screen, true) == -120, "incompatible wheel changed cursor or swallowed catalog scroll");
      });
      test("CREATIVE442 clicked palette item adjusts only matching cursor and zero requires another click", () -> {
        palette(state); select(state.screen, 0); int[] point = point(state.screen);
        ((ScreenInput)state.screen).power$click(point[0], point[1], 0); check(mc.player.inventory.getCursorStack().count == 1, "initial catalog click failed");
        InventoryWheel.event(state.screen, true, 240, false); ((ScreenInput)state.screen).power$release(point[0], point[1], -1);
        check(mc.player.inventory.getCursorStack().count == 3, "matching cursor lost wheel steps");
        InventoryWheel.event(state.screen, true, -360, false); ((ScreenInput)state.screen).power$release(point[0], point[1], -1);
        check(mc.player.inventory.getCursorStack() == null, "wheel did not reach zero");
        InventoryWheel.event(state.screen, true, 120, false); ((ScreenInput)state.screen).power$release(point[0], point[1], -1);
        check(mc.player.inventory.getCursorStack() == null, "wheel recreated cursor without another click"); InventoryWheel.clear();
        ((ScreenInput)state.screen).power$click(point[0], point[1], 0); check(mc.player.inventory.getCursorStack().count == 1, "new click could not restart amount control");
      });
      test("CREATIVE442 Search autofocus blocks manual movement until its shared editor loses focus", () -> {
        palette(state); select(state.screen, 6);
        var search = (CreativePaletteBridge.Search)field(state.screen, "power_search");
        check(search.focused() && InventoryTextInput.active(state.screen), "Search tab did not acquire shared typing ownership");
        var movement = Class.forName("local.luke.power.autowalk.InventoryMovement");
        var eligible = movement.getMethod("eligible", Minecraft.class, Screen.class);
        check(!(boolean)eligible.invoke(null, mc, state.screen), "manual inventory movement admitted focused Search");
        ((ScreenInput)state.screen).power$key('w', org.lwjgl.input.Keyboard.KEY_W);
        check(search.text().equals("w"), "Search autofocus did not accept the native typing path");
        select(state.screen, 0); check(!search.focused() && !InventoryTextInput.active(state.screen), "leaving Search retained shared movement exclusion");
      });
      test("CREATIVE442 tab wheel pages more than six regular tabs without scrolling items", () -> {
        var data = CreativeTabsData.read(CreativeTabsData.defaults()); data.customized(true);
        for (int i = 0; i < 8; i++) data.append(CreativeTabsData.tab("test_" + i, "Test " + i, "1", List.of("1", "2")));
        registry.getMethod("reload", com.google.gson.JsonObject.class).invoke(null, data.json());
        palette(state); field(state.screen, "creative_tabPage", 0); select(state.screen, 0);
        int x = (state.screen.width - 176) / 2 + InventoryPanels.offset(state.screen);
        int y = (state.screen.height - 166) / 2;
        int top = (int)Class.forName("local.luke.power.creative.inventory.CreativeGrid").getMethod("tabTop", int.class).invoke(null, y);
        InventoryWheel.event(state.screen, true, -120, false); ((ScreenInput)state.screen).power$release(x + 12, top + 8, -1);
        check((int)field(state.screen, "creative_tabPage") == 1 && (int)field(state.screen, "creative_tabIndex") == 0
            && (int)field(state.screen, "creative_rowIndex") == 0 && InventoryWheel.take(state.screen, true) == 0, "tab wheel did not exclusively page categories");
      });
    } finally { InventoryWheel.clear(); enabled.set(wheel, oldEnabled); inverted.set(wheel, oldInverted); registry.getMethod("reload", com.google.gson.JsonObject.class).invoke(null, savedLayout); }
    log("CREATIVE442 failures=" + failures); check(failures == 0, "creative-442 failures=" + failures);
  }
  private static void palette(Inventory442Checks.State state) throws Exception {
    state.reset(true, false); field(state.screen, "creative_normalGUI", false); InventoryWheel.clear();
  }
  @SuppressWarnings("unchecked") private static List<ItemStack> items(Screen screen) throws Exception { return (List<ItemStack>)field(screen, "creative_items"); }
  private static void select(Screen screen, int index) throws Exception {
    var method = screen.getClass().getDeclaredMethod("power$selectTab", int.class); method.setAccessible(true); method.invoke(screen, index);
  }
  private static int[] point(Screen screen) {
    int x = (screen.width - 176) / 2 + InventoryPanels.offset(screen), y = (screen.height - 166) / 2;
    return new int[]{x + 16, y - 20 + 22};
  }
}
