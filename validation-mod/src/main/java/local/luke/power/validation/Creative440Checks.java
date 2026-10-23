package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.creativeui.CreativeTabsBackend;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Disposable client only. Tests production screens without persisting a layout or touching world terrain. */
public final class Creative440Checks {
  public record RenderedTooltip(String text, int x, int y) {}
  private static boolean captureTooltips;
  private static final List<RenderedTooltip> renderedTooltips = new ArrayList<>();
  public static void captureTooltipText(String text, int x, int y) {
    if (captureTooltips) renderedTooltips.add(new RenderedTooltip(text, x, y));
  }
  private static final Path REPORT = Path.of("power-beta-data/reports/creative-440");
  private static Class<?> registry() throws Exception { return Class.forName("local.luke.power.creative.inventory.registry.TabRegistry"); }
  private static void layout(JsonObject value) throws Exception { registry().getMethod("reload", JsonObject.class).invoke(null, value); }
  private static int count() throws Exception { return (int) registry().getMethod("getTabsCount").invoke(null); }
  private static List<ItemStack> items(Object tab) throws Exception { return (List<ItemStack>) tab.getClass().getMethod("getItems").invoke(tab); }
  private static Object tab(int index) throws Exception { return registry().getMethod("getTabByIndex", int.class).invoke(null, index); }
  private static void invoke(Object owner, String name, Class<?>[] types, Object... arguments) throws Exception {
    Method method = owner.getClass().getDeclaredMethod(name, types); method.setAccessible(true); method.invoke(owner, arguments);
  }
  private static void press(Screen screen, int x, int y) { ((ScreenInput)(Object)screen).power$click(x, y, 0); }
  private static void release(Screen screen, int x, int y) { ((ScreenInput)(Object)screen).power$release(x, y, 0); }
  private static void key(Screen screen, char c, int key) { ((ScreenInput)(Object)screen).power$key(c, key); }
  private static List<String> inventory(Minecraft mc) {
    List<String> value = new ArrayList<>();
    for (ItemStack stack : mc.player.inventory.main) value.add(stack == null ? "" : stack.count + ":" + stack.writeNbt(new NbtCompound()));
    return value;
  }
  private static class_585 palette(Minecraft mc, int width, int height) throws Exception {
    var screen = new class_585(mc.player); mc.setScreen(screen); screen.init(mc, width, height);
    field(screen, "creative_normalGUI", false); return screen;
  }
  private static void select(class_585 screen, int index) throws Exception {
    invoke(screen, "power$selectTab", new Class<?>[]{int.class}, index);
  }
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && !mc.world.isRemote && Display.isActive(), "Use a focused muted disposable world");
    byte[] config = Files.readAllBytes(PowerConfig.path());
    JsonObject savedLayout = PowerConfig.section("creativeTabs");
    var savedRules = AccessRules.copy(mc); String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    ItemStack[] inventory = mc.player.inventory.main.clone(); ItemStack cursor = mc.player.inventory.getCursorStack();
    Screen parent = mc.currentScreen;
    Object inventoryConfig = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
    Object mouseConfig = inventoryConfig.getClass().getField("MOUSE_TWEAKS_CONFIG").get(inventoryConfig);
    Object wheel = mouseConfig.getClass().getField("SCROLL_WHEEL_CONFIG").get(mouseConfig);
    Field enabled = wheel.getClass().getField("enableScrollWheelTweaks"), invert = wheel.getClass().getField("invertScrollCursorSlotDirection");
    Object oldEnabled = enabled.get(wheel), oldInvert = invert.get(wheel);
    try {
      Files.createDirectories(REPORT); AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, true);
      CreativeVehicleChecks.mode(mc, "CREATIVE"); mc.player.inventory.setCursorStack(null);
      layout(CreativeTabsData.defaults());
      test("CREATIVE440 six defaults preserve original order and all seven native groups", () -> {
        check(count() == 6, "Expected six default regular tabs, got " + count());
        CreativeTabsData defaults = CreativeTabsData.read(CreativeTabsBackend.defaultLayout());
        check(defaults.tab(0).get("title").getAsString().equals("Full Blocks"), "First default tab moved");
        check(defaults.tab(2).get("title").getAsString().equals("Tools & Weapons"), "Merged Tools & Weapons tab missing");
        CreativeTabsData nativeTabs = CreativeTabsData.read(CreativeTabsBackend.nativeLayout());
        check(nativeTabs.size() >= 7, "Original groups are unavailable");
        List<String> tools = CreativeTabsData.slots(nativeTabs.tab(2)), weapons = CreativeTabsData.slots(nativeTabs.tab(3));
        List<String> combined = CreativeTabsData.slots(defaults.tab(2));
        check(combined.subList(0, tools.size()).equals(tools), "Tools do not precede weapons");
        check(combined.subList(tools.size(), combined.size()).equals(weapons), "Weapons were omitted");
        for (int i = 0; i < defaults.size(); i++) check(!defaults.tab(i).get("title").getAsString().startsWith("Minecraft"), "Default branding retained");
      });
      test("CREATIVE440 pinned Search includes every native variant once", () -> {
        Object search = registry().getMethod("search").invoke(null); List<ItemStack> all = items(search);
        Set<String> identities = new HashSet<>();
        for (ItemStack stack : all) {
          check(identities.add(stack.itemId + ":" + stack.getDamage()), "Duplicate search variant");
        }
        for (int i = 0; i < 7; i++) {
          JsonObject group = CreativeTabsData.read(CreativeTabsBackend.nativeLayout()).tab(i);
          for (String item : CreativeTabsData.slots(group)) check(item.isEmpty() || identities.contains(item), "Search omitted " + item);
        }
      });
      test("CREATIVE440 category/view tabs and individual deletion stay silent", () -> {
        var screen = palette(mc, 640, 420); int x = (640 - 176) / 2, y = (420 - 166) / 2;
        int clicks = Release240Checks.clicks;
        press(screen, x + 32, y - 40); release(screen, x + 32, y - 40);
        press(screen, x + 180, y + 145); release(screen, x + 180, y + 145);
        press(screen, x + 180, y + 121); release(screen, x + 180, y + 121);
        check(Release240Checks.clicks == clicks, "Inventory tab navigation played click audio");
        var creative = Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null);
        boolean destroy = creative.getClass().getField("destroySlot").getBoolean(creative);
        creative.getClass().getField("destroySlot").setBoolean(creative, true);
        try {
          mc.player.inventory.setCursorStack(new ItemStack(1, 3, 0));
          press(screen, x - 15, y + (int)call(screen, "power$destroyY") + 2);
          local.luke.power.storage.JournalQueue.awaitQueued(); local.luke.power.inventory.NativeInventoryPlan.tick(mc);
        }
        finally { creative.getClass().getField("destroySlot").setBoolean(creative, destroy); }
        check(Release240Checks.clicks == clicks, "Individual deletion played click audio");
      });
      test("CREATIVE440 palette wheel exchanges only cursor counts and respects the tweak", () -> {
        enabled.set(wheel, true); invert.set(wheel, false);
        var screen = palette(mc, 640, 420); select(screen, 0);
        List<ItemStack> shown = (List<ItemStack>)field(screen, "creative_items"); ItemStack item = shown.get(0);
        int x = (640 - 176) / 2 + 9, y = (420 - 166) / 2 - 20 + 15;
        mc.player.inventory.setCursorStack(null); List<String> before = inventory(mc);
        InventoryWheel.event(screen, true, 120, false); ((ScreenInput)(Object)screen).power$release(x, y, -1);
        check(mc.player.inventory.getCursorStack() == null, "Untouched palette wheel supplied an item");
        InventoryWheel.clear(); press(screen, x, y); release(screen, x, y);
        check(mc.player.inventory.getCursorStack() != null && mc.player.inventory.getCursorStack().count == 1, "Click did not supply one item");
        check(mc.player.inventory.getCursorStack().itemId == item.itemId, "Wheel supplied wrong item");
        InventoryWheel.event(screen, true, 240, false); ((ScreenInput)(Object)screen).power$release(x, y, -1);
        check(mc.player.inventory.getCursorStack().count == 3, "Multi-notch wheel lost steps");
        InventoryWheel.event(screen, true, -120, false); ((ScreenInput)(Object)screen).power$release(x, y, -1);
        check(mc.player.inventory.getCursorStack().count == 2, "Wheel down did not remove one cursor item");
        enabled.set(wheel, false); InventoryWheel.event(screen, true, 120, false); ((ScreenInput)(Object)screen).power$release(x, y, -1);
        check(mc.player.inventory.getCursorStack().count == 2, "Disabled wheel tweak changed cursor");
        check(inventory(mc).equals(before), "Palette wheel touched a hidden native inventory slot"); InventoryWheel.clear();
      });
      test("CREATIVE440 Search uses selectable shared input and fuzzy item names", () -> {
        var screen = palette(mc, 640, 420); select(screen, 6);
        SettingsSnapshot.render(mc, screen, 640, 420, REPORT.resolve("search-640.png"));
        var search = (CreativePaletteBridge.Search)field(screen, "power_search");
        int x = (int)field(search, "left") + 4, y = (int)field(search, "top") + 4;
        press(screen, x, y); check(search.focused() && InventoryTextInput.active(screen), "Search did not own typing");
        for (char c : "dimond".toCharArray()) key(screen, c, Keyboard.KEY_NONE);
        List<ItemStack> matches = (List<ItemStack>)field(screen, "creative_items");
        check(matches.stream().anyMatch(stack -> stack.itemId == 264), "Small typo did not find diamond");
        Object input = field(search, "search"); ((TextInput)input).selectAll(); key(screen, '1', Keyboard.KEY_1);
        check(search.text().equals("1"), "Selected search text did not replace atomically");
        key(screen, (char)0, Keyboard.KEY_ESCAPE); check(!search.focused() && !InventoryTextInput.active(screen), "Escape kept inventory typing ownership");
        check(mc.currentScreen == screen, "Escape closed the inventory while search owned it");
      });
      test("CREATIVE440 F3+H details travel through the native palette tooltip and fit a narrow GUI", () -> {
        boolean oldDebug = local.luke.power.debug.DebugConfig.enabled();
        boolean oldTooltips = local.luke.power.debug.DebugActions.tooltips;
        try {
          local.luke.power.debug.DebugConfig.preview(true);
          local.luke.power.debug.DebugActions.tooltips = false;
          var screen = palette(mc, 320, 240); select(screen, 0);
          ItemStack sword = new ItemStack(267, 1, 42);
          List<ItemStack> catalogue = new ArrayList<>(Collections.nCopies(64, null)); catalogue.set(63, sword);
          field(screen, "creative_items", catalogue); field(screen, "creative_rowIndex", 0); field(screen, "creative_maxIndex", 0);
          mc.player.inventory.setCursorStack(null);
          int x = (320 - 176) / 2 + 8 + 7 * 18 + 8, y = (240 - 166) / 2 + 120 + 8;
          captureTooltips = true; renderedTooltips.clear();
          SettingsSnapshot.render(mc, screen, 320, 240, REPORT.resolve("tooltip-normal-320.png"), x, y);
          check(renderedTooltips.stream().noneMatch(line -> line.text().contains("#267:42") || line.text().contains("Durability:")),
              "Advanced tooltip details appeared while F3+H was off");
          local.luke.power.debug.DebugActions.run(mc, local.luke.power.debug.DebugAction.TOOLTIPS);
          check(local.luke.power.debug.DebugActions.tooltips, "The F3+H action did not enable advanced tooltips");
          renderedTooltips.clear();
          SettingsSnapshot.render(mc, screen, 320, 240, REPORT.resolve("tooltip-advanced-320.png"), x, y);
          List<RenderedTooltip> details = renderedTooltips.stream().filter(line -> line.text().contains("#267:42") || line.text().contains("Durability:")).toList();
          check(details.stream().anyMatch(line -> line.text().contains("minecraft:iron_sword") && line.text().contains("#267:42")),
              "The palette did not render native item ID and metadata after F3+H");
          String durability = (sword.getMaxDamage() - 42) + " / " + sword.getMaxDamage();
          check(details.stream().anyMatch(line -> line.text().contains("Durability: " + durability)),
              "The palette did not render native remaining/maximum durability after F3+H");
          for (RenderedTooltip line : details)
            check(line.x() >= 1 && line.y() >= 1 && line.x() + mc.textRenderer.getWidth(line.text()) < 320 && line.y() + 8 < 240,
                "Advanced tooltip line was clipped: " + line);
        } finally {
          captureTooltips = false; renderedTooltips.clear();
          local.luke.power.debug.DebugActions.tooltips = oldTooltips;
          local.luke.power.debug.DebugConfig.preview(oldDebug);
        }
      });
      test("CREATIVE440 custom tab holes, page memory and pinned Search survive reopening", () -> {
        CreativeTabsData custom = CreativeTabsData.read(CreativeTabsBackend.defaultLayout());
        custom.append(CreativeTabsData.tab("extra", "menu.options", "264", List.of("", "264", "", "35:5")));
        layout(custom.json()); var screen = palette(mc, 640, 420);
        field(screen, "creative_tabPage", 1); invoke(screen, "creative_updateMaxIndex", new Class<?>[0]); select(screen, 0);
        invoke(screen, "power$remember", new Class<?>[0]);
        var reopened = palette(mc, 640, 420);
        check((int)field(reopened, "creative_tabPage") == 1, "Tab page position was forgotten");
        Object selectedTab = field(reopened, "power_selectedTab");
        Method literal = selectedTab.getClass().getMethod("literalTitle"); literal.setAccessible(true);
        check((boolean)literal.invoke(selectedTab) && field(reopened, "creative_tabKey").equals("menu.options"), "Custom title was treated as a translation key");
        List<ItemStack> shown = (List<ItemStack>)field(reopened, "creative_items");
        check(shown.size() == 4 && shown.get(0) == null && shown.get(2) == null && shown.get(3).getDamage() == 5, "Ordered holes or metadata were lost");
        select(reopened, 6); check(((List<?>)field(reopened, "creative_items")).size() > 100, "Pinned Search disappeared on later page");
        for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
          reopened.init(mc, size[0], size[1]); SettingsSnapshot.render(mc, reopened, size[0], size[1], REPORT.resolve("palette-" + size[0] + ".png"));
        }
      });
      test("CREATIVE440 editor picker, rename, groups and rows remain in its isolated draft", () -> {
        ConfigSession session = new ConfigSession(); CreativeTabsBackend.register(session);
        Setting setting = session.settings().get(0); setting.value = CreativeTabsData.defaults(); JsonElement before = setting.value.deepCopy();
        List<String> inv = inventory(mc); int[] accepted = {0};
        CreativeTabsScreen editor = new CreativeTabsScreen(parent, setting, () -> accepted[0]++); mc.setScreen(editor); editor.init(mc, 640, 420);
        invoke(editor, "addTab", new Class<?>[0]);
        CreativeTabsData draft = (CreativeTabsData)field(editor, "data");
        check(draft.size() == 7 && CreativeTabsData.slots(draft.tab(6)).size() == 64, "New tab does not start with eight rows");
        List<String> seed = new ArrayList<>(Collections.nCopies(64, "")); seed.set(0, "1:0"); seed.set(5, "267:0");
        CreativeTabsData.slots(draft.tab(6), seed);
        String tabId = draft.tab(6).get("id").getAsString(), tabTitle = draft.tab(6).get("title").getAsString(), tabIcon = draft.tab(6).get("icon").getAsString();
        invoke(editor, "fillNative", new Class<?>[]{int.class}, 3);
        check(draft.size() == 7 && draft.tab(6).get("id").getAsString().equals(tabId)
            && draft.tab(6).get("title").getAsString().equals(tabTitle) && draft.tab(6).get("icon").getAsString().equals(tabIcon), "Fill group changed tab identity, title, icon or tab count");
        List<String> filled = CreativeTabsData.slots(draft.tab(6));
        check(filled.get(0).equals("1:0") && filled.get(5).equals("267:0"), "Fill group overwrote existing cells");
        check(filled.contains("261:0") && filled.stream().filter(value -> value.equals("267:0")).count() == 1, "Fill group omitted native items or duplicated existing identities");
        JsonObject once = draft.json(); invoke(editor, "fillNative", new Class<?>[]{int.class}, 3);
        check(draft.json().equals(once), "Filling the same group twice duplicated items");
        CreativeTabsData.slots(draft.tab(6), Collections.nCopies(CreativeTabsData.MAX_ROWS * 8, "1:0"));
        JsonObject full = draft.json(); invoke(editor, "fillNative", new Class<?>[]{int.class}, 3);
        check(draft.json().equals(full) && !((String)field(editor, "error")).isEmpty(), "Insufficient group capacity made a partial edit");
        CreativeTabsData.slots(draft.tab(6), filled); field(editor, "error", "");
        int gridX = (int)call(editor, "gridLeft"), gridY = (int)call(editor, "gridTop");
        press(editor, gridX + 3, gridY + 3); release(editor, gridX + 3, gridY + 3);
        check(mc.currentScreen == editor, "Single click did not stay in cell selection");
        ((SlotDoubleClick)field(editor, "clicks")).press(0, 0, Mouse.getEventNanoseconds() - 100_000_000L);
        press(editor, gridX + 3, gridY + 3); release(editor, gridX + 3, gridY + 3);
        check(mc.currentScreen instanceof ItemPickerScreen, "Double click did not open the shared picker");
        ItemPickerScreen picker = (ItemPickerScreen)mc.currentScreen;
        invoke(picker, "choose", new Class<?>[]{String.class}, "264");
        check(mc.currentScreen == editor, "One-click picker needed another confirmation");
        check(CreativeTabsData.slots(draft.tab(6)).get(0).equals("264:0"), "Picker did not update the isolated slot");
        int panelLeft = (int)call(editor, "left") + (int)call(editor, "listWidth");
        press(editor, panelLeft + 30, 65); ((TextInput)field(editor, "title")).setText("Renamed"); key(editor, (char)0, Keyboard.KEY_RETURN);
        check(draft.tab(6).get("title").getAsString().equals("Renamed"), "Inline tab title did not commit");
        // Returning from the native picker recomputes GUI scale and screen size.
        int currentGridX = (int)call(editor, "gridLeft");
        press(editor, currentGridX + 4, (int)call(editor, "rowControlsY") + 4);
        check(CreativeTabsData.slots(draft.tab(6)).size() >= 72, "Add row did not retain eight initial rows");
        check(setting.value.equals(before) && accepted[0] == 0, "Draft editor published before Save");
        check(inventory(mc).equals(inv), "Settings editor edited inventory");
        for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
          editor.init(mc, size[0], size[1]); SettingsSnapshot.render(mc, editor, size[0], size[1], REPORT.resolve("editor-" + size[0] + ".png"));
        }
        invoke(editor, "save", new Class<?>[0]);
        check(accepted[0] == 1 && setting.value.getAsJsonObject().get("customized").getAsBoolean(), "Save did not return one draft to Options");
      });
      check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "Creative checks wrote configuration");
    } finally {
      mc.setScreen(null); InventoryTextInput.clear(); InventoryWheel.clear();
      enabled.set(wheel, oldEnabled); invert.set(wheel, oldInvert); layout(savedLayout);
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length); mc.player.inventory.setCursorStack(cursor);
      CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, savedRules); mc.setScreen(parent);
    }
    log("CREATIVE440 FAILURES " + failures);
  }
}
