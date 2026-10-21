package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.InventoryPanels;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.Offhand;
import local.luke.power.storage.JournalQueue;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Installed native-slot and durable-publication probes for the disposable validation client. */
public final class InventoryTools400Checks {
  private InventoryTools400Checks() {}
  public static void run(Minecraft mc, String phase) throws Exception {
    if (phase.equals("modal")) { InventoryToolsModal400Checks.run(mc); return; }
    boolean headers = phase.equals("headers");
    check(phase.equals("native") || headers, "expected native, headers or modal");
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "focus a loaded disposable world first");
    check(!NativeInventoryPlan.busy(), "finish the previous inventory plan first");
    if (headers) {
      check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client before header captures");
      check(Display.getWidth() >= 1280 && Display.getHeight() >= 720, "resize the disposable client to at least 1280x720");
    }
    failures = 0;
    diagnostic(mc, "before fixture");
    var settings = InventoryToolsConfig.copy(); var enabled = InventoryToolsConfig.copy(); var previousScreen = mc.currentScreen; var previousContainer = mc.player.container;
    var main = mc.player.inventory.main; int selected = mc.player.inventory.selectedSlot;
    var cursor = mc.player.inventory.getCursorStack(); boolean grabbed = Mouse.isGrabbed();
    String offhand = encode(Offhand.get(mc.player.inventory));
    int x = (int)Math.floor(mc.player.x) + 1, y = Math.max(1, Math.min(126, (int)Math.floor(mc.player.y))), z = (int)Math.floor(mc.player.z) + 1;
    for (int i = 0; i < 3; i++) check(mc.world.getBlockId(x + i, y, z) == 0, "fixture needs three nearby air blocks");
    var screens = new ArrayList<Screen>();
    try {
      enabled.sorting = true; enabled.depositMatching = true; InventoryToolsConfig.preview(enabled);
      if (headers) { enabled.sortingButton = enabled.depositMatchingButton = true; InventoryToolsConfig.preview(enabled); }
      Mouse.setGrabbed(false); mc.player.inventory.main = new ItemStack[36]; mc.player.inventory.setCursorStack(null);
      mc.world.method_200(x, y, z, 54); mc.world.method_200(x + 1, y, z, 54); mc.world.method_200(x + 2, y, z, 23);
      var first = (Inventory)mc.world.method_1777(x, y, z); var second = (Inventory)mc.world.method_1777(x + 1, y, z);
      var dispenser = (class_137)mc.world.method_1777(x + 2, y, z);
      check(first != null && second != null && dispenser != null, "native block entities missing");
      var player = new class_585(mc.player); var chest = new class_264(mc.player.inventory, first);
      var doubleChest = new class_264(mc.player.inventory, new class_320("Chest", first, second));
      var dispenserScreen = new class_235(mc.player.inventory, dispenser);
      for (ContainerScreen screen : List.of(player, chest, doubleChest, dispenserScreen)) {
        screens.add(screen); install(mc, screen); screen.init(mc, 640, 360);
        if (screen == player) UiChecks.field(screen, "creative_normalGUI", true);
        test("exact native region " + screen.getClass().getSimpleName(), () -> {
          diagnostic(mc, "region " + screen.getClass().getSimpleName());
          var regions = SortingSlots.read(mc); check(regions.player().size() == 27, "main storage size");
          int expected = screen == player ? 0 : screen == chest ? 27 : screen == doubleChest ? 54 : 9;
          check(regions.storage().size() == expected, "external storage size");
        });
      }
      if (headers) {
        first.setStack(7, new ItemStack(278, 1, 0));
        headerCaptures(mc, chest, doubleChest);
        log("INVENTORY HEADERS401 failures=" + failures);
        check(failures == 0, "native chest header regression failures=" + failures);
        return;
      }
      install(mc, player);
      test("player sort conserves native tags, damage and pinned oversize without equipment changes", () -> {
        Arrays.fill(mc.player.inventory.main, null);
        mc.player.inventory.main[0] = DeathChecks.tagged(264, 3, 0, "hotbar-protected");
        mc.player.inventory.main[9] = DeathChecks.tagged(35, 1000, 5, "oversize");
        mc.player.inventory.main[10] = DeathChecks.tagged(35, 40, 5, "same");
        mc.player.inventory.main[11] = DeathChecks.tagged(35, 30, 5, "same");
        mc.player.inventory.main[12] = DeathChecks.tagged(35, 8, 5, "different");
        mc.player.inventory.main[13] = DeathChecks.tagged(267, 1, 83, "tool");
        var totals = totals(mc.player.inventory.main); String pinned = encode(mc.player.inventory.main[9]), hotbar = encode(mc.player.inventory.main[0]);
        check(SortingActions.sort(mc), "sort was not admitted");
        check(pinned.equals(encode(mc.player.inventory.main[9])) && mc.player.inventory.main[10].count == 40, "preview wrote slots");
        finish(mc);
        check(totals.equals(totals(mc.player.inventory.main)), "sort lost native identity/counts");
        check(pinned.equals(encode(mc.player.inventory.main[9])) && hotbar.equals(encode(mc.player.inventory.main[0])), "protected slot changed");
        check(Objects.equals(offhand, encode(Offhand.get(mc.player.inventory))), "offhand changed");
      });
      install(mc, chest);
      test("matching deposit freezes native full-data identities and excludes the hotbar", () -> {
        Arrays.fill(mc.player.inventory.main, null); for (int i = 0; i < first.size(); i++) first.setStack(i, null);
        first.setStack(0, DeathChecks.tagged(35, 63, 5, "matching"));
        mc.player.inventory.main[0] = DeathChecks.tagged(35, 4, 5, "matching");
        mc.player.inventory.main[9] = DeathChecks.tagged(35, 9, 5, "matching");
        mc.player.inventory.main[10] = DeathChecks.tagged(35, 7, 5, "other-tag");
        String hotbar = encode(mc.player.inventory.main[0]), unmatched = encode(mc.player.inventory.main[10]);
        check(SortingActions.depositMatching(mc), "deposit was not admitted"); finish(mc);
        check(first.getStack(0).count == 64 && first.getStack(1).count == 8 && mc.player.inventory.main[9] == null, "matching count transfer failed");
        check(hotbar.equals(encode(mc.player.inventory.main[0])) && unmatched.equals(encode(mc.player.inventory.main[10])), "unmatched or hotbar data moved");
      });
      for (String target : List.of("player slot", "player gap", "hotbar", "chest", "outside")) {
        test("SORT HOTKEY401 follows " + target + " and preserves the other inventory", () -> {
          var sortSettings = InventoryToolsConfig.copy(); sortSettings.sortDescending = false;
          sortSettings.protectedSlots = List.of(); sortSettings.preferredItems = Map.of();
          InventoryToolsConfig.preview(sortSettings);
          try {
            Arrays.fill(mc.player.inventory.main, null);
            for (int i = 0; i < first.size(); i++) first.setStack(i, null);
            mc.player.inventory.main[0] = DeathChecks.tagged(264, 3, 0, "hotbar-kept");
            mc.player.inventory.main[9] = DeathChecks.tagged(35, 1, 0, "player-high");
            mc.player.inventory.main[10] = DeathChecks.tagged(4, 1, 0, "player-low");
            first.setStack(0, DeathChecks.tagged(35, 1, 0, "chest-high"));
            first.setStack(1, DeathChecks.tagged(4, 1, 0, "chest-low"));
            String hotbar = encode(mc.player.inventory.main[0]);
            var regions = SortingSlots.read(mc);
            int[] pointer = switch (target) {
              case "player slot", "player gap" -> point(chest, regions.player().get(0));
              case "hotbar" -> point(chest, chest.container.method_2084(first.size() + 27));
              case "chest" -> point(chest, regions.storage().get(0));
              default -> new int[]{0, 0};
            };
            if (target.equals("player gap")) pointer[0] += 9;
            if (target.equals("player slot")) {
              int oldX = Mouse.getX(), oldY = Mouse.getY();
              try {
                Mouse.setCursorPosition(pointer[0] * mc.displayWidth / chest.width,
                    (chest.height - pointer[1] - 1) * mc.displayHeight / chest.height);
                check(SortingActions.sortAtPointer(mc), "pointer hotkey sort was not admitted");
              } finally { Mouse.setCursorPosition(oldX, oldY); }
            } else check(SortingActions.sortAt(mc, pointer[0], pointer[1]), "region hotkey sort was not admitted");
            finish(mc);
            boolean playerTarget = target.startsWith("player") || target.equals("hotbar");
            check(mc.player.inventory.main[9].itemId == (playerTarget ? 4 : 35), "player region was sorted incorrectly");
            check(first.getStack(0).itemId == (playerTarget ? 35 : 4), "container region was sorted incorrectly");
            check(hotbar.equals(encode(mc.player.inventory.main[0])), "hotkey changed the protected hotbar");
          } finally { InventoryToolsConfig.preview(enabled); }
        });
      }
      install(mc, player);
      test("slot aliases, custom slots and mismatched visible handlers are refused", () -> {
        Slot original = player.container.method_2084(9), other = player.container.method_2084(10);
        try {
          player.container.slots.set(9, other); refused(mc);
          Slot custom = new Slot(mc.player.inventory, 9, original.x, original.y) {}; custom.id = 9;
          player.container.slots.set(9, custom); refused(mc);
        } finally { player.container.slots.set(9, original); }
        mc.player.container = chest.container; try { refused(mc); } finally { mc.player.container = player.container; }
        mc.player.inventory.setCursorStack(new ItemStack(4, 1, 0)); try { refused(mc); } finally { mc.player.inventory.setCursorStack(null); }
      });
      test("canceling a durably queued sort never publishes native inventory changes", () -> {
        Arrays.fill(mc.player.inventory.main, null); mc.player.inventory.main[10] = DeathChecks.tagged(35, 7, 5, "cancel");
        String before = Arrays.toString(encoded(mc.player.inventory.main));
        check(SortingActions.sort(mc), "cancel test sort was not admitted"); NativeInventoryPlan.cancel(); finish(mc);
        check(before.equals(Arrays.toString(encoded(mc.player.inventory.main))), "canceled sort published");
      });
      test("native slot picking excludes output, text, modal and registered widget ownership", () -> {
        Slot mainSlot = player.container.method_2084(9); int[] point = point(player, mainSlot);
        check(local.luke.power.offhand.OffhandInput.pick(mc, point[0], point[1]) == mainSlot, "native main slot not selected");
        int[] output = point(player, player.container.method_2084(0));
        check(local.luke.power.offhand.OffhandInput.pick(mc, output[0], output[1]) == null, "output slot admitted");
        Object modal = new Object(); InventoryPanels.modal(player, modal, true);
        try { check(local.luke.power.offhand.OffhandInput.pick(mc, point[0], point[1]) == null, "modal leaked a slot"); }
        finally { InventoryPanels.modal(player, modal, false); }
        local.luke.power.input.InventoryTextInput.focus(player, true);
        try { check(local.luke.power.offhand.OffhandInput.pick(mc, point[0], point[1]) == null, "text input leaked a slot"); }
        finally { local.luke.power.input.InventoryTextInput.focus(player, false); }
        var button = new net.minecraft.client.gui.widget.ButtonWidget(195499, point[0] - 4, point[1] - 4, 18, 18, "");
        var buttons = ((local.luke.power.validation.mixin.ScreenInput)player).power$buttons(); buttons.add(button);
        try { check(local.luke.power.offhand.OffhandInput.pick(mc, point[0], point[1]) == null, "header widget leaked a slot"); }
        finally { buttons.remove(button); }
      });
    } finally {
      try { NativeInventoryPlan.cancel(); JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); }
      finally {
        for (Screen screen : screens) InventoryPanels.remove(screen);
        mc.currentScreen = previousScreen; mc.player.container = previousContainer;
        mc.player.inventory.main = main; mc.player.inventory.selectedSlot = selected; mc.player.inventory.setCursorStack(cursor);
        InventoryToolsConfig.preview(settings);
        try {
          for (int i = 0; i < 3; i++) {
            if (mc.world.method_1777(x + i, y, z) instanceof Inventory inventory) for (int slot = 0; slot < inventory.size(); slot++) inventory.setStack(slot, null);
            mc.world.method_200(x + i, y, z, 0);
          }
        } finally { Mouse.setGrabbed(grabbed); }
      }
    }
    log("INVENTORY TOOLS400 NATIVE failures=" + failures);
  }
  private static void headerCaptures(Minecraft mc, ContainerScreen chest, ContainerScreen doubleChest) throws Exception {
    Path report = Path.of("power-beta-data/reports/inventory-headers-401"); Files.createDirectories(report);
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (ContainerScreen screen : List.of(chest, doubleChest)) for (int[] size : new int[][]{{320,240},{640,360}}) {
        install(mc, screen); screen.init(mc, size[0], size[1]);
        String name = (screen == chest ? "single" : "double") + "-" + size[0];
        test("HEADERS401 native tooltip covers compact buttons and first row stays clear " + name, () -> {
          var panel = (local.luke.power.validation.mixin.ContainerInput)screen;
          int panelY = (screen.height - panel.power$height()) / 2;
          int firstSlotY = panelY + screen.container.method_2084(0).y;
          var buttons = ((local.luke.power.validation.mixin.ScreenInput)screen).power$buttons().stream()
              .filter(button -> button.visible && (button.id == 195400 || button.id == 195401)).toList();
          check(buttons.size() == 2, "native chest did not expose both compact header buttons");
          for (var button : buttons) {
            var bounds = (local.luke.power.mixin.ButtonBoundsAccessor)(Object)button;
            check(bounds.power$width() == 10 && bounds.power$height() == 10, "native header hit bounds differ from compact drawing");
            check(button.y + bounds.power$height() < firstSlotY, "compact chest header overlaps the first item row");
          }
          var slot = screen.container.method_2084(7);
          int hoverX = (screen.width - panel.power$width()) / 2 + InventoryPanels.offset(screen) + slot.x + 1;
          int hoverY = panelY + slot.y + 1;
          check(slot.hasStack() && mc.player.inventory.getCursorStack() == null, "native top-row tooltip probe is unavailable");
          Path base = report.resolve(name + "-base.png"), tooltip = report.resolve(name + "-tooltip.png");
          SettingsSnapshot.render(mc, screen, size[0], size[1], base);
          SettingsSnapshot.render(mc, screen, size[0], size[1], tooltip, hoverX, hoverY);
          var baseline = javax.imageio.ImageIO.read(base.toFile()); var hovered = javax.imageio.ImageIO.read(tooltip.toFile());
          for (var button : buttons) {
            // The tooltip background covers this upper-border pixel, above either text baseline.
            int px = (button.x + 2) * 2 + 1, py = button.y * 2 + 1;
            int before = luminance(baseline.getRGB(px, py)), after = luminance(hovered.getRGB(px, py));
            check(before > 100 && after < before * .6, "native item tooltip was overpainted by compact " + button.text + " button");
          }
        });
      }
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode);
    }
  }
  private static int luminance(int rgb) { return ((rgb >>> 16 & 255) + (rgb >>> 8 & 255) + (rgb & 255)) / 3; }
  private static void install(Minecraft mc, ContainerScreen screen) { mc.currentScreen = screen; mc.player.container = screen.container; }
  private static int[] point(ContainerScreen screen, Slot slot) {
    var panel = (local.luke.power.validation.mixin.ContainerInput)screen;
    return new int[]{(screen.width - panel.power$width()) / 2 + InventoryPanels.offset(screen) + slot.x + 8,
        (screen.height - panel.power$height()) / 2 + slot.y + 8};
  }
  private static void diagnostic(Minecraft mc, String phase) throws Exception {
    log("INVENTORY TOOLS400 ELIGIBILITY " + phase
        + " ready=" + local.luke.power.world.WorldCompatibility.ready(mc.world)
        + " remote=" + mc.world.isRemote + " playerWorld=" + (mc.player.world == mc.world)
        + " health=" + mc.player.health + " dead=" + mc.player.dead + " viewOwner=" + (mc.field_2807 == mc.player)
        + " detached=" + local.luke.power.input.DetachedCamera.isActive() + " carry=" + local.luke.power.input.InteractionState.carryingContainer
        + " display=" + Display.isActive() + " grabbed=" + Mouse.isGrabbed()
        + " mode=" + (mc.player instanceof local.luke.power.input.PlayerMode mode ? "creative:" + mode.power$creative() + ",spectator:" + mode.power$spectator() : "no bridge")
        + " text=" + local.luke.power.input.InventoryTextInput.active(mc.currentScreen)
        + " modal=" + InventoryPanels.modal(mc.currentScreen) + " cursor=" + (mc.player.inventory.getCursorStack() != null)
        + " nativeScreen=" + SortingSlots.screen(mc.currentScreen)
        + " catalog=" + (mc.currentScreen instanceof local.luke.power.input.InventoryGui gui && gui.power$usesCatalogWheel())
        + " handlerSame=" + (mc.currentScreen instanceof ContainerScreen screen && screen.container == mc.player.container)
        + " focus=" + mc.field_2778 + " paused=" + mc.paused + " eligible=" + SortingSlots.eligible(mc));
  }
  private static void finish(Minecraft mc) throws IOException { JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); check(!NativeInventoryPlan.busy(), "durable native plan did not finish"); }
  private static void refused(Minecraft mc) throws IOException {
    try { SortingSlots.read(mc); throw new AssertionError("unsupported ownership admitted"); } catch (IOException expected) {}
  }
  private static String encode(ItemStack stack) throws IOException { return stack == null ? null : DeathItems.encode(stack); }
  private static String[] encoded(ItemStack[] main) throws IOException { String[] data = new String[main.length]; for (int i = 0; i < main.length; i++) data[i] = encode(main[i]); return data; }
  private static Map<String, Long> totals(ItemStack[] items) throws IOException {
    Map<String, Long> result = new HashMap<>();
    for (ItemStack stack : items) if (stack != null) { ItemStack identity = DeathItems.decode(DeathItems.encode(stack)); identity.count = 1; result.merge(DeathItems.encode(identity), (long)stack.count, Long::sum); }
    return result;
  }
}
