package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.*;
import local.luke.power.storage.JournalQueue;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Native sorting/restock integration, sharing the disposable inventory-tools fixture. */
final class Sorting472Checks {
  private Sorting472Checks() {}
  static void capture(Minecraft mc) throws Exception {
    var previous = mc.currentScreen;
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    Path report = Path.of("power-beta-data/reports/sorting-472"); Files.createDirectories(report);
    try {
      for (int[] size : new int[][]{{854,480},{320,240}}) {
        var screen = new PowerOptionsScreen(previous); screen.init(mc, size[0], size[1]);
        field(screen, "page", "Inventory"); ((Set<?>)field(screen, "collapsed")).clear();
        field(screen, "scroll", 0d); call(screen, "layout");
        for (Object row : (List<?>)field(screen, "rows")) {
          if (call(row, "setting") == null && call(row, "group").equals("Sorting")) {
            field(screen, "scroll", ((Number)call(row, "y")).doubleValue()); break;
          }
        }
        call(screen, "layout");
        SettingsSnapshot.render(mc, screen, size[0], size[1], report.resolve("inventory-" + size[0] + ".png"));
        var setting = screen.session().settings().stream().filter(s -> s.id.equals("inventoryTools.restockHotbarWhenSorting")).findFirst().orElseThrow();
        check(setting.label.equals("Restock hotbar when sorting inventory") && setting.group.equals("Sorting"), "setting label/group");
        for (var base : DefaultBases.Base.values()) check(!DefaultBases.value(base, setting.id, setting.powerBetaDefault).getAsBoolean(), "restock must default off in " + base);
        List<?> rows = (List<?>)field(screen, "rows"); int restock = -1, reverse = -1;
        for (int i = 0; i < rows.size(); i++) {
          Setting row = (Setting)call(rows.get(i), "setting");
          if (row == setting) restock = i;
          if (row != null && row.id.equals("inventoryTools.sortDescending")) reverse = i;
        }
        check(restock >= 0 && reverse == restock + 1, "restock row must immediately precede Reverse item order");
        log("SORTING472 UI " + size[0] + " defaults/order PASS");
      }
      var picker = new local.luke.power.ui.ItemPickerScreen(previous, "Preferred item for slot 1",
          local.luke.power.ui.ItemCatalogue.Domain.FILTER, List.of(), false,
          new local.luke.power.ui.ItemPickerScreen.Options(true, entry -> true), values -> {});
      picker.init(mc, 854, 480);
      SettingsSnapshot.render(mc, picker, 854, 480, report.resolve("picker-854.png"));
      check(((local.luke.power.ui.TextInput)field(picker, "search")).focused, "picker capture search not focused");
      picker.removed();
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode); mc.setScreen(previous);
    }
  }

  static void nativeChecks(Minecraft mc, List<ContainerScreen> screens) throws Exception {
    var original = InventoryToolsConfig.copy();
    try {
      for (ContainerScreen screen : screens) {
        mc.currentScreen = screen; mc.player.container = screen.container;
        var enabled = InventoryToolsConfig.copy(); enabled.restockHotbarWhenSorting = true;
        enabled.protectedSlots = List.of(9); enabled.preferredItems = Map.of(10, "35:5"); enabled.sortDescending = true;
        InventoryToolsConfig.preview(enabled);
        test("SORTING472 inventory-only top-up " + screen.getClass().getSimpleName(), () -> {
          seed(mc); var regions = SortingSlots.read(mc);
          String before = snapshot(mc), protectedStack = encode(mc.player.inventory.main[9]);
          check(regions.hotbar().size() == 9, "missing native hotbar");
          check(SortingActions.sort(mc, true), "inventory sort was not admitted");
          check(before.equals(snapshot(mc)), "top-up changed inventory before durable publication");
          finish(mc);
          check(mc.player.inventory.main[0].count == 64 && mc.player.inventory.main[2].count == 15, "top-up count or left-to-right priority");
          check(mc.player.inventory.main[1] == null, "empty hotbar slot filled");
          check(mc.player.inventory.main[3].count == 64 && mc.player.inventory.main[4].count == 1000, "full/oversized hotbar changed");
          check(mc.player.inventory.main[5].count == 1 && mc.player.inventory.main[5].getDamage() == 83, "tool changed");
          check(protectedStack.equals(encode(mc.player.inventory.main[9])), "protected source changed");
          check(mc.player.inventory.main[10].getDamage() == 5 && mc.player.inventory.main[10].count == 7, "preferred slot or foreign NBT changed");
          check(mc.player.inventory.main[11].itemId == 264, "reverse sorting ignored");
        });
        if (!SortingSlots.read(mc).storage().isEmpty()) test("SORTING472 external sort never restocks " + screen.getClass().getSimpleName(), () -> {
          seed(mc); var regions = SortingSlots.read(mc);
          for (var slot : regions.storage()) slot.setStack(null);
          regions.storage().get(1).setStack(new ItemStack(4, 10, 0));
          String player = snapshot(mc);
          check(SortingActions.sort(mc), "external sort was not admitted"); finish(mc);
          check(player.equals(snapshot(mc)), "container sort restocked the hotbar or changed player storage");
        });
      }
      var player = screens.get(0); mc.currentScreen = player; mc.player.container = player.container;
      test("SORTING472 default-off preference leaves hotbar intact", () -> {
        var disabled = InventoryToolsConfig.copy(); disabled.restockHotbarWhenSorting = false; InventoryToolsConfig.preview(disabled);
        seed(mc); String hotbar = encode(mc.player.inventory.main[0]);
        check(SortingActions.sort(mc), "disabled restock sort not admitted"); finish(mc);
        check(hotbar.equals(encode(mc.player.inventory.main[0])), "disabled restock changed hotbar");
      });
      var enabled = InventoryToolsConfig.copy(); enabled.restockHotbarWhenSorting = true; InventoryToolsConfig.preview(enabled);
      test("SORTING472 canceled top-up preserves every slot", () -> {
        seed(mc); String before = snapshot(mc); check(SortingActions.sort(mc), "cancel sort not admitted");
        NativeInventoryPlan.cancel(); finish(mc); check(before.equals(snapshot(mc)), "canceled top-up published");
      });
      test("SORTING472 foreign hotbar change cancels entire sort", () -> {
        seed(mc); check(SortingActions.sort(mc), "stale sort not admitted");
        mc.player.inventory.main[0].count++; String foreign = snapshot(mc); finish(mc);
        check(foreign.equals(snapshot(mc)), "foreign hotbar mutation was overwritten or sources moved");
      });
    } finally { InventoryToolsConfig.preview(original); }
  }
  private static void seed(Minecraft mc) throws Exception {
    var main = mc.player.inventory.main; Arrays.fill(main, null);
    main[0] = DeathChecks.tagged(35, 60, 5, "same");
    main[2] = DeathChecks.tagged(35, 10, 5, "same");
    main[3] = DeathChecks.tagged(35, 64, 5, "same");
    main[4] = DeathChecks.tagged(35, 1000, 5, "same");
    main[5] = DeathChecks.tagged(267, 1, 83, "tool");
    main[9] = DeathChecks.tagged(35, 20, 5, "same");
    main[10] = DeathChecks.tagged(35, 9, 5, "same");
    main[11] = DeathChecks.tagged(35, 7, 5, "different");
    main[12] = DeathChecks.tagged(264, 3, 0, "diamond");
  }
  private static String encode(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static String snapshot(Minecraft mc) throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); }
  private static void finish(Minecraft mc) throws Exception { JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); check(!NativeInventoryPlan.busy(), "inventory plan did not finish"); }
}
