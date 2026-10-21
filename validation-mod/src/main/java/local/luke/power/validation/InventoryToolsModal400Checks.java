package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.*;
import local.luke.power.storage.JournalQueue;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;

/** Dispatches real modal controls through the installed PlayerScreen override in a disposable client. */
final class InventoryToolsModal400Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "focus a loaded disposable world first");
    check(!NativeInventoryPlan.busy() && !DetachedCamera.isActive(), "finish inventory plans and exit freecam first");
    check(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), "release Shift before synthetic modal checks");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute this disposable clone before checks");
    failures = 0;
    var settings = InventoryToolsConfig.copy();
    var rules = AccessRules.copy(mc);
    Field presetField = SavedCreativeHotbars.class.getDeclaredField("data"); presetField.setAccessible(true);
    var presetsBefore = presetField.get(null);
    check(presetsBefore != null && PowerConfig.problem().isEmpty(), "saved presets/settings must be writable before modal checks");
    var documentBefore = PowerConfig.section("savedCreativeHotbars");
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var screenBefore = mc.currentScreen; var containerBefore = mc.player.container;
    var mainBefore = mc.player.inventory.main; var armorBefore = mc.player.inventory.armor;
    var cursorBefore = mc.player.inventory.getCursorStack(); int keyBefore = mc.options.inventoryKey.code;
    boolean grabbed = Mouse.isGrabbed();
    var screens = new ArrayList<class_585>();
    try {
      presetField.set(null, SavedCreativeHotbarsData.empty());
      AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, true);
      CreativeVehicleChecks.mode(mc, "CREATIVE");
      var enabled = InventoryToolsConfig.copy(); enabled.creativeHotbars = enabled.creativeHotbarsButton = true; InventoryToolsConfig.preview(enabled);
      mc.player.inventory.main = new ItemStack[36]; mc.player.inventory.armor = new ItemStack[4];
      mc.player.inventory.main[0] = DeathChecks.tagged(35, 117, 5, "modal-protected");
      mc.player.inventory.setCursorStack(null); Mouse.setGrabbed(false);
      for (int[] size : new int[][]{{320, 240}, {640, 360}}) {
        var screen = new class_585(mc.player); screens.add(screen);
        mc.currentScreen = screen; mc.player.container = screen.container; screen.init(mc, size[0], size[1]);
        UiChecks.field(screen, "creative_normalGUI", true);
        var tools = (InventoryToolsOverlay)field(screen, "power$inventoryTools");
        var input = (ScreenInput)(Object)screen;
        int left = (screen.width - 176) / 2, top = (screen.height - 166) / 2;
        String protectedItem = DeathItems.encode(mc.player.inventory.main[0]);
        test("saved-hotbar preview items clear native player-model depth at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem);
          var before = presetField.get(null); String inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
          String[] items = new String[9]; Arrays.fill(items, DeathItems.encode(new ItemStack(42, 32, 0)));
          var previews = SavedCreativeHotbarsData.empty(); for (int row = 0; row < 9; row++) previews = previews.with(row, items);
          try {
            presetField.set(null, previews); open(screen, tools, input); previewDepth(mc, screen, tools, size[0]);
            check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))) && !NativeInventoryPlan.busy(), "preview capture changed inventory or queued work");
          } finally { tools.close(); presetField.set(null, before); }
        });
        test("saved-hotbar modal owns hidden Creative tabs at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem);
          open(screen, tools, input);
          capture(mc, screen, size[0]);
          int sounds = Release240Checks.clicks;
          // These positions lie in the gap between Save and Load, over the native view tabs.
          input.power$click(left + 174, top + 115, 0); input.power$release(left + 174, top + 115, 0);
          input.power$click(left + 174, top + 139, 0); input.power$release(left + 174, top + 139, 0);
          check(Boolean.TRUE.equals(UiChecks.field(screen, "creative_normalGUI")), "hidden Creative tab received a modal click");
          check(tools.modal() && InventoryPanels.modal(screen), "modal owner disappeared");
          check(Release240Checks.clicks == sounds, "hidden native tab played a click");
          check(protectedItem.equals(DeathItems.encode(mc.player.inventory.main[0])), "modal preview changed inventory");
        });
        test("saved-hotbar modal owns the mouse inventory-close binding at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem);
          open(screen, tools, input);
          mc.options.inventoryKey.code = -99;
          try {
            input.power$click(left + 30, top + 30, 1); input.power$release(left + 30, top + 30, 1);
            check(mc.currentScreen == screen && mc.player.container == screen.container && tools.modal(), "mouse binding closed native inventory behind modal");
          } finally { mc.options.inventoryKey.code = keyBefore; }
        });
        test("lost modal eligibility preserves a cursor over the hidden destroy slot at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem);
          open(screen, tools, input);
          ItemStack held = DeathChecks.tagged(267, 1, 73, "modal-cursor"); String encoded = DeathItems.encode(held);
          mc.player.inventory.setCursorStack(held);
          try {
            input.power$click(left - 10, top + 145, 0);
            check(!tools.modal() && !InventoryPanels.modal(screen), "ineligible modal did not close");
            check(((Integer)field(tools, "ownedButtons") & 1) != 0, "closing modal lost the originating press before release");
            input.power$release(left - 10, top + 145, 0);
            check(((Integer)field(tools, "ownedButtons") & 1) == 0, "modal release ownership did not clear");
            check(mc.player.inventory.getCursorStack() == held && encoded.equals(DeathItems.encode(held)), "hidden destroy slot consumed cursor data");
            check(protectedItem.equals(DeathItems.encode(mc.player.inventory.main[0])), "hidden destroy slot changed inventory");
          } finally { mc.player.inventory.setCursorStack(null); }
        });
        test("native modal Save updates only its selected preset at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem);
          mc.player.inventory.main[8] = DeathChecks.tagged(267, 1, 73, "modal-save");
          String before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
          String[] expected = new String[9];
          for (int i = 0; i < 9; i++) expected[i] = encode(mc.player.inventory.main[i]);
          String[] untouched = SavedCreativeHotbars.snapshot(0);
          open(screen, tools, input); int clicks = Release240Checks.clicks;
          var box = InventoryToolsLayout.modal(screen.width, screen.height, InventoryPanels.offset(screen));
          int x = box.x() + box.width() - 78, y = box.y() + InventoryToolsLayout.MODAL_ROW_TOP + 8 * 18 + 4;
          input.power$click(x, y, 0); input.power$release(x, y, 0);
          check(Arrays.equals(expected, SavedCreativeHotbars.snapshot(8)), "native Save button did not capture the selected row");
          check(Arrays.equals(untouched, SavedCreativeHotbars.snapshot(0)), "Save changed another preset row");
          check(tools.modal() && mc.currentScreen == screen && mc.player.container == screen.container, "Save closed the native host or modal");
          check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "Save changed inventory bytes");
          check(Release240Checks.clicks == clicks + 1, "Save did not play exactly one native click");
          PowerConfig.flushPending();
          check(Arrays.equals(expected, SavedCreativeHotbarsData.read(PowerConfig.section("savedCreativeHotbars")).bar(8)), "Save did not persist its row");
        });
        test("native modal Load waits for durable publication at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem);
          String[] saved = new String[9]; saved[0] = protectedItem;
          saved[8] = DeathItems.encode(DeathChecks.tagged(267, 1, 81, "modal-load"));
          presetField.set(null, ((SavedCreativeHotbarsData)presetField.get(null)).with(8, saved));
          mc.player.inventory.main[0] = DeathChecks.tagged(4, 11, 0, "modal-before-load");
          mc.player.inventory.main[8] = null;
          String before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
          var protectedSlots = Arrays.copyOfRange(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer), 9, 46);
          String protectedBytes = DeathItems.inventory(protectedSlots);
          open(screen, tools, input); int clicks = Release240Checks.clicks;
          var box = InventoryToolsLayout.modal(screen.width, screen.height, InventoryPanels.offset(screen));
          int x = box.x() + box.width() - 38, y = box.y() + InventoryToolsLayout.MODAL_ROW_TOP + 8 * 18 + 4;
          try (var slow = new AutoTools400State.SlowWriter()) {
            input.power$click(x, y, 0);
            check(NativeInventoryPlan.busy(), "native Load button did not prepare a plan");
            NativeInventoryPlan.tick(mc); slow.checkBlocked();
            check(tools.modal() && InventoryPanels.modal(screen), "Load closed the dialog before durable completion");
            check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "Load wrote inventory before its journal flush");
            slow.release(); finishPlans(mc);
          }
          for (int i = 0; i < 9; i++) check(Objects.equals(saved[i], encode(mc.player.inventory.main[i])), "native Load row mismatch at slot " + i);
          check(protectedBytes.equals(DeathItems.inventory(Arrays.copyOfRange(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer), 9, 46))), "Load changed protected storage/armor/crafting/offhand bytes");
          check(!tools.modal() && !InventoryPanels.modal(screen) && mc.currentScreen == screen && mc.player.container == screen.container, "durable Load did not return to the same native inventory");
          check(!NativeInventoryPlan.failed() && ((Integer)field(tools, "ownedButtons") & 1) != 0, "Load canceled its transaction or lost its pending release");
          input.power$release(x, y, 0);
          check(((Integer)field(tools, "ownedButtons") & 1) == 0, "completed Load leaked its originating release");
          check(Release240Checks.clicks == clicks + 1, "Load did not play exactly one native click");
        });
        test("a canceled Load cannot close a later modal visit at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem); open(screen, tools, input);
          String before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
          var box = InventoryToolsLayout.modal(screen.width, screen.height, InventoryPanels.offset(screen));
          int loadX = box.x() + box.width() - 38, loadY = box.y() + InventoryToolsLayout.MODAL_ROW_TOP + 8 * 18 + 4;
          try (var slow = new AutoTools400State.SlowWriter()) {
            input.power$click(loadX, loadY, 0); input.power$release(loadX, loadY, 0);
            check(NativeInventoryPlan.busy(), "cancel test Load was not prepared");
            input.power$click(box.x() + 14, box.y() + InventoryToolsLayout.MODAL_BACK_TOP + 6, 0);
            input.power$release(box.x() + 14, box.y() + InventoryToolsLayout.MODAL_BACK_TOP + 6, 0);
            open(screen, tools, input); slow.release(); finishPlans(mc);
          }
          check(tools.modal() && InventoryPanels.modal(screen) && !NativeInventoryPlan.failed(), "canceled completion closed the reopened dialog or stopped actions");
          check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "canceled Load changed inventory");
        });
        test("native modal Back closes only the modal and owns its release at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem); open(screen, tools, input);
          String before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
          var box = InventoryToolsLayout.modal(screen.width, screen.height, InventoryPanels.offset(screen));
          int x = box.x() + 14, y = box.y() + InventoryToolsLayout.MODAL_BACK_TOP + 6, clicks = Release240Checks.clicks;
          input.power$click(x, y, 0);
          check(!tools.modal() && !InventoryPanels.modal(screen), "native Back button did not close the modal");
          check(((Integer)field(tools, "ownedButtons") & 1) != 0, "Back lost its release ownership");
          input.power$release(x, y, 0);
          check(((Integer)field(tools, "ownedButtons") & 1) == 0, "Back release ownership remained held");
          check(mc.currentScreen == screen && mc.player.container == screen.container, "Back closed the native inventory");
          check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "Back changed inventory bytes");
          check(Release240Checks.clicks == clicks + 1, "Back did not play exactly one native click");
        });
        test("native modal keys bypass hovered-slot drop and number-swap callbacks at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem); open(screen, tools, input);
          Object config = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
          Object modern = config.getClass().getField("MODERN_MINECRAFT_CONFIG").get(config);
          Field drop = modern.getClass().getField("UseDropKeyInInventory"), numbers = modern.getClass().getField("NumKeyHotbarSwap");
          Object oldDrop = drop.get(modern), oldNumbers = numbers.get(modern);
          int dropKey = mc.options.dropKey.code;
          Set<Object> entities = Collections.newSetFromMap(new IdentityHashMap<>()); entities.addAll(mc.world.field_198);
          String before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
          try {
            drop.set(modern, true); numbers.set(modern, true); mc.options.dropKey.code = Keyboard.KEY_Q;
            nativeField(screen, "slot").set(screen, screen.container.method_2084(36));
            input.power$key('q', Bindings.eventCode(mc.options.dropKey)); input.power$key('2', Keyboard.KEY_2);
            check(tools.modal() && InventoryPanels.modal(screen), "unrelated native keys closed the modal");
            check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "modal key reached a native slot callback");
            check(mc.world.field_198.stream().noneMatch(value -> value instanceof net.minecraft.class_142 && !entities.contains(value)), "modal key created an item drop");
            input.power$key((char)0, Keyboard.KEY_ESCAPE);
            check(!tools.modal() && !InventoryPanels.modal(screen) && mc.currentScreen == screen, "Escape did not close only the modal");
          } finally {
            drop.set(modern, oldDrop); numbers.set(modern, oldNumbers); mc.options.dropKey.code = dropKey;
            nativeField(screen, "slot").set(screen, null);
            for (Object value : new ArrayList<>(mc.world.field_198))
              if (value instanceof net.minecraft.class_142 item && !entities.contains(value)) item.markDead();
          }
        });
        test("resizing the native saved-hotbar host clears only transient ownership at " + size[0], () -> {
          resetHost(mc, screen, tools, protectedItem);
          open(screen, tools, input);
          screen.init(mc, size[0], size[1]);
          check(!((InventoryToolsHost)screen).power$inventoryToolsModal() && !InventoryPanels.modal(screen), "resize retained modal input owner");
          check(protectedItem.equals(DeathItems.encode(mc.player.inventory.main[0])), "resize changed native item bytes");
        });
        InventoryPanels.remove(screen);
      }
    } finally {
      NativeInventoryPlan.cancel();
      for (var screen : screens) InventoryPanels.remove(screen);
      mc.currentScreen = screenBefore; mc.player.container = containerBefore;
      mc.player.inventory.main = mainBefore; mc.player.inventory.armor = armorBefore;
      mc.player.inventory.setCursorStack(cursorBefore); mc.options.inventoryKey.code = keyBefore;
      InventoryToolsConfig.preview(settings); Mouse.setGrabbed(grabbed);
      try { CreativeVehicleChecks.mode(mc, mode); }
      finally {
        AccessRules.restore(mc, rules); presetField.set(null, presetsBefore);
        PowerConfig.save("savedCreativeHotbars", documentBefore);
      }
    }
    log("INVENTORY TOOLS400 MODAL failures=" + failures);
  }

  private static void resetHost(Minecraft mc, class_585 screen, InventoryToolsOverlay tools, String protectedItem) throws Exception {
    tools.close(); InventoryPanels.remove(screen);
    finishPlans(mc);
    mc.currentScreen = screen; mc.player.container = screen.container;
    Arrays.fill(mc.player.inventory.main, 0, 9, null);
    mc.player.inventory.main[0] = DeathItems.decode(protectedItem);
    mc.player.inventory.setCursorStack(null); Mouse.setGrabbed(false);
    UiChecks.field(screen, "creative_normalGUI", true);
  }

  private static void finishPlans(Minecraft mc) throws Exception {
    JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc);
    check(!NativeInventoryPlan.busy(), "modal left a pending native inventory plan");
  }
  private static String encode(ItemStack item) throws Exception { return item == null ? null : DeathItems.encode(item); }

  private static void capture(Minecraft mc, class_585 screen, int width) throws Exception { capture(mc, screen, "popup-" + width); }
  private static java.nio.file.Path capture(Minecraft mc, Screen screen, String name) throws Exception {
    var dir = java.nio.file.Path.of("power-beta-data/reports/hotbar-popup-401"); java.nio.file.Files.createDirectories(dir);
    var image = dir.resolve(name + ".png");
    int mode = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MATRIX_MODE);
    org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glPushMatrix();
    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW); org.lwjgl.opengl.GL11.glPushMatrix();
    try { SettingsSnapshot.render(mc, screen, screen.width, screen.height, image); }
    finally {
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW); org.lwjgl.opengl.GL11.glPopMatrix();
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glPopMatrix();
      org.lwjgl.opengl.GL11.glPopAttrib(); org.lwjgl.opengl.GL11.glMatrixMode(mode);
    }
    return image;
  }
  private static void previewDepth(Minecraft mc, class_585 screen, InventoryToolsOverlay tools, int width) throws Exception {
    var nativeImage = javax.imageio.ImageIO.read(capture(mc, screen, "items-over-player-" + width).toFile());
    var isolated = javax.imageio.ImageIO.read(capture(mc, tools, "items-isolated-" + width).toFile());
    var box = InventoryToolsLayout.modal(screen.width, screen.height, InventoryPanels.offset(screen));
    int samples = 0, clipped = 0;
    for (int row = 0; row < 9; row++) for (int slot = 0; slot < 9; slot++)
      for (int dy = 3; dy < 13; dy++) for (int dx = 3; dx < 13; dx++) {
        int x = (box.x() + 16 + slot * 16 + dx) * 2 + 1;
        int y = (box.y() + InventoryToolsLayout.MODAL_ROW_TOP + row * 18 + dy) * 2 + 1;
        int expected = luminance(isolated.getRGB(x, y));
        if (expected <= 80) continue;
        samples++; if (luminance(nativeImage.getRGB(x, y)) + 32 < expected) clipped++;
      }
    check(samples > 100 && clipped < samples / 100, "native player depth darkened preview items: " + clipped + "/" + samples);
  }
  private static int luminance(int rgb) { return ((rgb >>> 16 & 255) + (rgb >>> 8 & 255) + (rgb & 255)) / 3; }

  private static void open(class_585 screen, InventoryToolsOverlay tools, ScreenInput input) {
    tools.prepare();
    var button = InventoryToolsLayout.second(screen.width, screen.height, 176, 166, InventoryPanels.offset(screen));
    input.power$click(button.x() + 3, button.y() + 3, 0); input.power$release(button.x() + 3, button.y() + 3, 0);
    check(tools.modal() && InventoryPanels.modal(screen), "native Hotbars button did not open modal; check Creative access and saved-data format");
  }

  private static Object field(Object object, String name) throws Exception {
    return nativeField(object, name).get(object);
  }
  private static Field nativeField(Object object, String name) throws Exception {
    for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
      try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
      catch (NoSuchFieldException next) {}
    }
    throw new NoSuchFieldException(name);
  }
}
