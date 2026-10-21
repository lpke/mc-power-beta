package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.SortingSlots;
import local.luke.power.offhand.*;
import local.luke.power.recipes.RecipeBookConfig;
import local.luke.power.storage.JournalQueue;
import local.luke.power.validation.mixin.*;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

/** Native layouts, clicks and recovery ownership in an already muted disposable world. */
final class ContainerOffhand402Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/container-offhand-402");
  private ContainerOffhand402Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && WorldCompatibility.ready(mc.world)
        && Display.isActive(), "focus a verified disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client first");
    check(!NativeInventoryPlan.busy() && !DetachedCamera.isActive() && mc.player.health > 0 && !mc.player.dead,
        "finish inventory work and use a living player outside freecam");
    check(!Keyboard.isKeyDown(42) && !Keyboard.isKeyDown(54) && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1),
        "release Shift and the mouse buttons");
    check(Display.getWidth() >= 1280 && Display.getHeight() >= 720, "resize the disposable client to at least 1280x720");
    failures = 0; Files.createDirectories(REPORT);
    var inventory = mc.player.inventory; var main = inventory.main; var armor = inventory.armor;
    var cursor = inventory.getCursorStack(); var held = Offhand.get(inventory);
    var previousScreen = mc.currentScreen; var previousContainer = mc.player.container; var view = mc.field_2807;
    boolean focused = mc.field_2778, paused = mc.paused, grabbed = Mouse.isGrabbed();
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var hands = OffhandConfig.copy(); var books = RecipeBookConfig.copy();
    int x = (int)Math.floor(mc.player.x) + 1, y = Math.max(1, Math.min(126, (int)Math.floor(mc.player.y))), z = (int)Math.floor(mc.player.z) + 1;
    for (int i = 0; i < 5; i++) check(mc.world.getBlockId(x + i, y, z) == 0, "fixture needs five nearby air blocks");
    var screens = new ArrayList<ContainerScreen>();
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.field_2807 = mc.player; mc.field_2778 = true; mc.paused = false; Mouse.setGrabbed(false);
      inventory.main = new ItemStack[36]; inventory.armor = new ItemStack[4]; inventory.setCursorStack(null); Offhand.set(inventory, null);
      var enabled = OffhandConfig.copy(); enabled.enabled = true; OffhandConfig.preview(enabled);
      var hiddenBooks = RecipeBookConfig.copy(); hiddenBooks.craftingOpen = hiddenBooks.furnaceOpen = false; RecipeBookConfig.preview(hiddenBooks);
      int[] blocks = {54, 54, 58, 61, 23};
      for (int i = 0; i < blocks.length; i++) mc.world.method_200(x + i, y, z, blocks[i]);
      Inventory first = (Inventory)mc.world.method_1777(x, y, z), second = (Inventory)mc.world.method_1777(x + 1, y, z);
      var furnace = (class_138)mc.world.method_1777(x + 3, y, z); var dispenser = (class_137)mc.world.method_1777(x + 4, y, z);
      screens.add(new class_585(mc.player)); screens.add(new class_264(inventory, first));
      screens.add(new class_264(inventory, new class_320("Chest", first, second)));
      screens.add(new class_516(inventory, mc.world, x + 2, y, z)); screens.add(new class_524(inventory, furnace)); screens.add(new class_235(inventory, dispenser));
      String[] names = {"player", "single-chest", "double-chest", "workbench", "furnace", "dispenser"};
      int[] counts = {45, 63, 90, 46, 39, 45}, hotbars = {142, 143, 197, 142, 142, 142};
      for (int i = 0; i < screens.size(); i++) {
        var screen = screens.get(i); String name = names[i]; int nativeCount = counts[i], hotbarY = hotbars[i];
        install(mc, screen, 640, 360);
        if (i == 0) UiChecks.field(screen, "creative_normalGUI", true);
        Offhand.set(inventory, DeathChecks.tagged(267, 1, 73, "offhand-" + name));
        for (int[] size : new int[][]{{320, 240}, {640, 360}}) {
          install(mc, screen, size[0], size[1]);
          SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(name + "-" + size[0] + ".png"));
        }
        var slot = OffhandContainers.slot(screen.container, inventory);
        test("OFFHAND402 native layout and shared hit geometry " + name, () -> {
          check(slot != null && slot.id == nativeCount && screen.container.slots.size() == nativeCount + 1, "offhand replaced a native transport slot");
          if (screen.getClass() == class_585.class) {
            for (var handler : java.util.List.of(new class_277(inventory), new class_277(inventory, false)))
              check(handler.slots.size() == 46 && OffhandContainers.slot(handler, inventory) != null,
                  "a player constructor omitted or duplicated the offhand slot");
          }
          check(inventory.main.length == 36 && inventory.armor.length == 4 && inventory.size() == 40, "native player arrays changed");
          check(slot.x == -17 && slot.y == hotbarY && OffhandContainers.hotbarY(screen.container, inventory) == hotbarY, "offhand does not follow the native hotbar");
          var change = NativeInventoryPlan.Change.slot(slot, null);
          check(change.owner() == inventory && change.index() == 45, "transport ID leaked into recovery ownership");
          int[] point = point(screen, slot);
          check(((InventoryGui)(Object)screen).power$containsGuiSpace(point[0], point[1]), "inventory looking owns the offhand slot");
          check(!InventoryPanels.contains(screen, point[0], point[1]), "gameplay slot was registered as a modal panel");
          check(((OffhandSlotPicker)(Object)screen).power$offhandSlotAt(point[0], point[1]) == slot, "native hit test lost the offhand");
        });
        test("OFFHAND402 native mouse pickup and full-data putback " + name, () -> {
          String before = bytes(Offhand.get(inventory)); int[] point = point(screen, slot);
          click(screen, point[0], point[1]);
          check(Offhand.get(inventory) == null && before.equals(bytes(inventory.getCursorStack())), "pickup dropped or changed native item data");
          // Click the frame's upper-left padding while carrying the item. It must never become an outside drop.
          var bounds = (ContainerInput)(Object)screen;
          int left = (screen.width - bounds.power$width()) / 2 + InventoryPanels.offset(screen), top = (screen.height - bounds.power$height()) / 2;
          click(screen, left - 21, top + hotbarY - 4);
          check(before.equals(bytes(inventory.getCursorStack())), "frame padding dropped the carried stack");
          click(screen, point[0], point[1]);
          check(inventory.getCursorStack() == null && before.equals(bytes(Offhand.get(inventory))), "putback changed native item data");
          check(OffhandSlotClick.nativeSlot(mc, -999) == -999, "outside-click scope leaked after release");
        });
        test("OFFHAND402 disabled offhand keeps extraction available " + name, () -> {
          var disabled = OffhandConfig.copy(); disabled.enabled = false; OffhandConfig.preview(disabled);
          try {
            check(Offhand.visible(mc.player) && !slot.canInsert(new ItemStack(4, 1, 0)), "disabled offhand allowed insertion or hid existing data");
            String before = bytes(Offhand.get(inventory)); int[] point = point(screen, slot); click(screen, point[0], point[1]);
            check(Offhand.get(inventory) == null && before.equals(bytes(inventory.getCursorStack())), "disabled extraction failed");
            inventory.setCursorStack(null);
          } finally { OffhandConfig.preview(enabled); }
        });
        test("OFFHAND402 durable hovered-slot swap " + name, () -> {
          Arrays.fill(inventory.main, null); inventory.main[9] = DeathChecks.tagged(35, 17, 5, "swap-source");
          Offhand.set(inventory, DeathChecks.tagged(267, 1, 73, "swap-hand"));
          Slot source = playerSlot(screen, inventory, 9); int[] point = point(screen, source);
          check(OffhandInput.pick(mc, point[0], point[1]) == source, "new slot count blocked native hovered swaps");
          String before = bytes(inventory.main[9]), hand = bytes(Offhand.get(inventory));
          check(Offhand.swapSlot(mc, source), "durable swap rejected");
          check(before.equals(bytes(inventory.main[9])) && hand.equals(bytes(Offhand.get(inventory))), "swap wrote before journal completion");
          JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc);
          check(!NativeInventoryPlan.busy() && hand.equals(bytes(inventory.main[9])) && before.equals(bytes(Offhand.get(inventory))), "durable swap changed native data");
        });
        if (i == 0 || i == 1 || i == 2 || i == 5) test("OFFHAND402 sorting excludes offhand " + name, () -> {
          var regions = SortingSlots.read(mc);
          check(regions.player().size() == 27 && regions.player().stream().noneMatch(candidate -> candidate == slot)
              && regions.storage().stream().noneMatch(candidate -> candidate == slot), "sorting claimed the offhand slot");
        });
        if (i == 1 || i == 2 || i == 5) test("OFFHAND402 storage Shift-transfer cannot fill offhand " + name, () -> {
          for (int index = 0; index < 36; index++) inventory.main[index] = new ItemStack(4, 64, 0);
          Offhand.set(inventory, null); Slot stored = screen.container.method_2084(0);
          stored.setStack(DeathChecks.tagged(35, 7, 5, "storage-source")); String before = bytes(stored.getStack());
          screen.container.onSlotClick(stored.id, 0, true, mc.player);
          check(before.equals(bytes(stored.getStack())) && Offhand.get(inventory) == null, "storage transfer borrowed the offhand as inventory capacity");
          stored.setStack(null);
        });
        if (i == 3 || i == 4) test("OFFHAND402 recipe book leaves the shifted offhand clickable " + name, () -> {
          var openBooks = RecipeBookConfig.copy(); openBooks.enabled = openBooks.inventory = true;
          openBooks.craftingOpen = openBooks.furnaceOpen = true; RecipeBookConfig.preview(openBooks);
          try {
            install(mc, screen, 640, 360);
            var book = local.luke.power.recipes.RecipeBooks.book(screen);
            UiChecks.field(book, "open", true); book.tick();
            Offhand.set(inventory, DeathChecks.tagged(267, 1, 73, "shifted-hand"));
            SettingsSnapshot.render(mc, screen, 640, 360, REPORT.resolve(name + "-book-640.png"));
            check(InventoryPanels.offset(screen) > 0 && !book.replacesNative(), "recipe book did not shift the native panel");
            int[] point = point(screen, slot);
            check(!book.layout().background().contains(point[0], point[1]), "recipe panel covered the offhand");
            String before = bytes(Offhand.get(inventory)); click(screen, point[0], point[1]);
            check(Offhand.get(inventory) == null && before.equals(bytes(inventory.getCursorStack())), "shifted offhand click dropped or lost its stack");
            click(screen, point[0], point[1]);
            check(inventory.getCursorStack() == null && before.equals(bytes(Offhand.get(inventory))), "shifted offhand putback failed");
          } finally {
            UiChecks.field(local.luke.power.recipes.RecipeBooks.book(screen), "open", false);
            RecipeBookConfig.preview(hiddenBooks); install(mc, screen, 640, 360);
          }
        });
        Arrays.fill(inventory.main, null); inventory.setCursorStack(null); Offhand.set(inventory, null);
      }
      test("OFFHAND402 remote constructors preserve every native protocol layout", () -> {
        mc.world.isRemote = true;
        try {
          var remote = List.of(new class_277(inventory), new class_506(inventory, first), new class_196(inventory, mc.world, x + 2, y, z),
              new class_337(inventory, furnace), new class_407(inventory, dispenser));
          int[] expected = {45, 63, 46, 39, 45};
          for (int index = 0; index < remote.size(); index++) check(remote.get(index).slots.size() == expected[index]
              && OffhandContainers.slot(remote.get(index), inventory) == null, "remote protocol gained an offhand slot");
        } finally { mc.world.isRemote = false; }
      });
    } finally {
      try { NativeInventoryPlan.cancel(); JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); }
      finally {
        for (var screen : screens) InventoryPanels.remove(screen);
        inventory.main = main; inventory.armor = armor; inventory.setCursorStack(cursor); Offhand.set(inventory, held);
        OffhandConfig.preview(hands); RecipeBookConfig.preview(books);
        CreativeVehicleChecks.mode(mc, mode); mc.currentScreen = previousScreen; mc.player.container = previousContainer;
        mc.field_2807 = view; mc.field_2778 = focused; mc.paused = paused;
        for (int i = 0; i < 5; i++) {
          if (mc.world.method_1777(x + i, y, z) instanceof Inventory contents)
            for (int slot = 0; slot < contents.size(); slot++) contents.setStack(slot, null);
          mc.world.method_200(x + i, y, z, 0);
        }
        Mouse.setGrabbed(grabbed);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glMatrixMode(matrixMode); GL11.glPopAttrib();
      }
    }
    log("OFFHAND402 failures=" + failures + " captures=" + REPORT);
    check(failures == 0, "container offhand checks failed=" + failures);
  }

  private static void install(Minecraft mc, ContainerScreen screen, int width, int height) {
    mc.currentScreen = screen; mc.player.container = screen.container; screen.init(mc, width, height);
  }
  private static int[] point(ContainerScreen screen, Slot slot) {
    var bounds = (ContainerInput)(Object)screen;
    return new int[]{(screen.width - bounds.power$width()) / 2 + InventoryPanels.offset(screen) + slot.x + 8,
        (screen.height - bounds.power$height()) / 2 + slot.y + 8};
  }
  private static Slot playerSlot(ContainerScreen screen, net.minecraft.entity.player.PlayerInventory owner, int index) {
    for (Object entry : screen.container.slots) if (entry instanceof Slot slot) {
      var access = (local.luke.power.mixin.DeathSlotAccessor)(Object)slot;
      if (access.power$deathInventory() == owner && access.power$deathIndex() == index) return slot;
    }
    throw new AssertionError("Native player slot missing");
  }
  private static void click(ContainerScreen screen, int x, int y) {
    var input = (ScreenInput)(Object)screen; input.power$click(x, y, 0); input.power$release(x, y, 0);
  }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
}
