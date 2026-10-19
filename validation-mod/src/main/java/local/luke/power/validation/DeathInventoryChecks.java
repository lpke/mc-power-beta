package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.DeathChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.*;
import local.luke.power.permissions.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import local.luke.power.validation.mixin.ScreenInput;
import org.lwjgl.input.Keyboard;

final class DeathInventoryChecks {
  static void run(Minecraft mc, boolean saved) throws Exception {
    failures = 0; alive(mc); mc.setScreen(null); mc.field_2807 = mc.player;
    var rules = AccessRules.copy(mc); var settings = DeathConfig.copy();
    var properties = AccessRules.of(mc.world.method_262()); boolean cheats = properties.allAllowed();
    var main = mc.player.inventory.main.clone(); var armor = mc.player.inventory.armor.clone();
    var cursor = mc.player.inventory.getCursorStack();
    Path reports = Path.of("power-beta-data/reports/death-inventory"); Files.createDirectories(reports);
    var enabled = new DeathSettings(); enabled.enabled = true; enabled.announceLocation = false;
    DeathConfig.preview(enabled); properties.allAllowed(true);
    try {
      if (saved) {
        var death = latest(mc);
        check(death.cause.equals("Slot restart fixture") && death.slots.equals(List.of(8, 20, 39)), "positions not persisted");
        check(DeathTracker.journal(mc).state(death.id).equals("partial") && DeathTracker.journal(mc).remaining(death.id).equals(List.of(0, 2)), "individual claim not persisted");
        clear(mc); DeathTracker.recover(mc, death);
        stack(mc.player.inventory.main[8], 35, 7, 5, "restart-hotbar");
        check(mc.player.inventory.main[20] == null, "bulk recovery duplicated individually claimed stack");
        stack(mc.player.inventory.armor[3], 310, 1, 27, "restart-helmet");
        check(DeathTracker.journal(mc).state(death.id).equals("recovered"), "bulk claim not persisted");
        log("PASS restart preserves original positions and partial claims; survival bulk recovery only returns remaining stacks");
        return;
      }
      test("survival restores original hotbar, main, armor and cursor fallback with native data", () -> {
        clear(mc);
        mc.player.inventory.main[8] = tagged(35, 7, 5, "hotbar"); mc.player.inventory.main[20] = tagged(267, 1, 83, "tool");
        mc.player.inventory.armor[3] = tagged(310, 1, 27, "helmet"); mc.player.inventory.armor[0] = tagged(313, 1, 41, "boots");
        mc.player.inventory.setCursorStack(tagged(264, 3, 0, "cursor")); record(mc, "Positions fixture");
        var death = latest(mc); check(death.slots.equals(List.of(8, 20, 36, 39, 40)), "slot snapshot is not aligned");
        clear(mc); DeathTracker.recover(mc, death);
        stack(mc.player.inventory.main[8], 35, 7, 5, "hotbar"); stack(mc.player.inventory.main[20], 267, 1, 83, "tool");
        stack(mc.player.inventory.armor[3], 310, 1, 27, "helmet"); stack(mc.player.inventory.armor[0], 313, 1, 41, "boots");
        stack(mc.player.inventory.main[0], 264, 3, 0, "cursor"); check(mc.player.inventory.getCursorStack() == null, "cursor restored as a held stack");
      });
      test("conflicting main and armor slots cancel all recovery and print a warning", () -> {
        clear(mc); mc.player.inventory.main[0] = tagged(35, 7, 5, "saved-main");
        mc.player.inventory.armor[3] = tagged(310, 1, 27, "saved-helmet"); record(mc, "Conflict fixture"); var death = latest(mc); clear(mc);
        mc.player.inventory.armor[3] = tagged(306, 1, 11, "existing-helmet");
        var screen = open(mc, death, 320, 240); String before = inventory(mc); byte[] journal = Files.readAllBytes(path(mc));
        var output = ChatChecks.record(() -> bulk(screen));
        check(output.stream().anyMatch(t -> t.contains("occupied") && t.contains("cancelled")), "missing conflict warning");
        check(before.equals(inventory(mc)) && Arrays.equals(journal, Files.readAllBytes(path(mc))), "conflict changed inventory or claim");
        mc.player.inventory.armor[3] = null; mc.player.inventory.main[0] = tagged(35, 1, 5, "saved-main");
        before = inventory(mc); click(screen, 0, 0);
        check(before.equals(inventory(mc)) && Arrays.equals(journal, Files.readAllBytes(path(mc))), "matching occupied slot was merged");
        mc.setScreen(null);
      });
      test("invalid equipment cancels before claiming and empty original slots retain overflow counts", () -> {
        clear(mc); mc.player.inventory.armor[3] = tagged(4, 2, 0, "invalid-gear"); record(mc, "Invalid gear fixture");
        var death = latest(mc); clear(mc); String before = inventory(mc);
        try { DeathTracker.recover(mc, death); throw new AssertionError("invalid gear accepted"); } catch (IllegalArgumentException expected) {}
        check(before.equals(inventory(mc)) && DeathTracker.journal(mc).state(death.id).equals("available"), "invalid gear changed items or claim");
        clear(mc); mc.player.inventory.main[0] = tagged(35, 70, 5, "overflow"); mc.player.inventory.main[1] = tagged(267, 1, 83, "reserved");
        record(mc, "Overflow fixture"); death = latest(mc); clear(mc); DeathTracker.recover(mc, death);
        stack(mc.player.inventory.main[0], 35, 64, 5, "overflow"); stack(mc.player.inventory.main[1], 267, 1, 83, "reserved");
        stack(mc.player.inventory.main[2], 35, 6, 5, "overflow");
      });
      for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
        test("locked viewer ignores clicks, drag, shift, drop and hotbar keys; width " + size[0], () -> {
          clear(mc); mc.player.inventory.main[8] = tagged(35, 7, 5, "locked"); mc.player.inventory.main[20] = tagged(267, 1, 83, "locked-tool");
          mc.player.inventory.armor[3] = tagged(310, 1, 27, "locked-helmet");
          mc.player.inventory.setCursorStack(tagged(264, 3, 0, "locked-cursor")); record(mc, "Locked fixture"); var death = latest(mc);
          clear(mc); mc.player.inventory.main[3] = tagged(264, 3, 0, "live");
          properties.allAllowed(false); var screen = open(mc, death, size[0], size[1]);
          String before = inventory(mc); byte[] journal = Files.readAllBytes(path(mc));
          for (int button : new int[]{0, 1, 2}) for (int slot : new int[]{8, 20, 39, 0, 40}) click(screen, slot, button);
          for (int code : new int[]{Keyboard.KEY_Q, Keyboard.KEY_LSHIFT, Keyboard.KEY_1, Keyboard.KEY_9}) ((ScreenInput)(Object)screen).power$key('\0', code);
          ((ScreenInput)(Object)screen).power$release(0, 0, 0);
          SettingsSnapshot.render(mc, screen, size[0], size[1], reports.resolve("locked-" + size[0] + ".png"));
          check(before.equals(inventory(mc)) && mc.player.inventory.getCursorStack() == null, "locked viewer changed inventory/cursor");
          check(Arrays.equals(journal, Files.readAllBytes(path(mc))), "locked viewer wrote a recovery claim");
          mc.setScreen(null); check(before.equals(inventory(mc)), "closing viewer changed items"); properties.allAllowed(true);
        });
      }
      test("individual clicks and bulk recovery share claims and permission changes lock immediately", () -> {
        properties.allAllowed(true);
        clear(mc); mc.player.inventory.main[8] = tagged(35, 7, 5, "individual"); mc.player.inventory.main[20] = tagged(267, 1, 83, "remaining");
        mc.player.inventory.armor[3] = tagged(310, 1, 27, "remaining-helmet"); record(mc, "Individual fixture"); var death = latest(mc); clear(mc);
        var screen = open(mc, death, 320, 240); click(screen, 8, 0); stack(mc.player.inventory.main[8], 35, 7, 5, "individual");
        String after = inventory(mc); click(screen, 8, 0); check(after.equals(inventory(mc)), "repeat GUI click duplicated stack");
        check(DeathTracker.journal(mc).state(death.id).equals("partial"), "partial state missing");
        properties.allAllowed(false); click(screen, 20, 0); check(after.equals(inventory(mc)), "cheats disable did not immediately lock viewer");
        properties.allAllowed(true);
        SettingsSnapshot.render(mc, screen, 320, 240, reports.resolve("partial-320.png"));
        bulk(screen);
        stack(mc.player.inventory.main[8], 35, 7, 5, "individual"); stack(mc.player.inventory.main[20], 267, 1, 83, "remaining");
        stack(mc.player.inventory.armor[3], 310, 1, 27, "remaining-helmet");
        screen = open(mc, death, 640, 420); after = inventory(mc); click(screen, 39, 0); check(after.equals(inventory(mc)), "completed viewer allowed recovery"); mc.setScreen(null);
      });
      test("feature disable and player change close the viewer without recovering", () -> {
        var death = latest(mc); var screen = open(mc, death, 320, 240); String before = inventory(mc);
        mc.setScreen(null); ChatChecks.submit(mc, "/death items " + death.id, Keyboard.KEY_NUMPADENTER);
        check(mc.currentScreen instanceof DeathInventoryScreen, "numpad Enter closed the viewer opened by its command");
        screen = (DeathInventoryScreen)mc.currentScreen;
        DeathConfig.preview(DeathChecks.disabled()); screen.tick(); check(mc.currentScreen == null && before.equals(inventory(mc)), "feature disable left stale viewer or changed items");
        DeathConfig.preview(enabled); screen = open(mc, death, 320, 240); String name = mc.player.name;
        mc.player.name = "OtherDeathInventoryCheck"; try { screen.tick(); check(mc.currentScreen == null, "player change left viewer open"); } finally { mc.player.name = name; }
      });
      test("death IDs are decimal, logs are old to new, and recovery has no command or completion", () -> {
        var records = DeathTracker.journal(mc).records();
        for (int i = 0; i < records.size(); i++) check(records.get(i).id.equals(Integer.toString(i + 1)), "non-sequential numeric death ID");
        var output = ChatChecks.submit(mc, "/death log"); int previous = -1;
        for (var death : records) {
          int found = -1; for (int i = 0; i < output.size(); i++) if (output.get(i).contains("§e" + death.id + " §7|")) found = i;
          if (found >= 0) { check(found > previous, "death log is reversed"); previous = found; }
        }
        String before = inventory(mc); output = ChatChecks.submit(mc, "/death recover");
        check(before.equals(inventory(mc)) && output.stream().noneMatch(t -> t.contains("Recovered items")), "removed recovery command executed");
        output = ChatChecks.submit(mc, "/death help"); check(output.stream().noneMatch(t -> t.contains("/death recover")), "removed command in help");
        var command = local.luke.power.commands.util.RetroChatUtil.commands.stream().filter(c -> c.name().equals("death")).findFirst().orElseThrow();
        var source = new local.luke.power.commands.util.SharedCommandSource(mc.player);
        check(Arrays.stream(command.suggestion(source, 1, "", "/death ")).noneMatch(t -> t.equals("recover")), "removed completion offered");
        mc.setScreen(null);
      });
      test("Escape and the inventory binding close the viewer without changing items", () -> {
        var death = latest(mc); String before = inventory(mc); var screen = open(mc, death, 320, 240);
        ((ScreenInput)(Object)screen).power$key('\0', Keyboard.KEY_ESCAPE);
        check(mc.currentScreen == null && before.equals(inventory(mc)), "Escape changed inventory or did not close");
        screen = open(mc, death, 320, 240);
        ((ScreenInput)(Object)screen).power$key('\0', mc.options.inventoryKey.code);
        check(mc.currentScreen == null && before.equals(inventory(mc)), "inventory binding changed inventory or did not close");
      });
      properties.allAllowed(true); clear(mc); mc.player.inventory.main[8] = tagged(35, 7, 5, "restart-hotbar"); mc.player.inventory.main[20] = tagged(267, 1, 83, "restart-tool");
      mc.player.inventory.armor[3] = tagged(310, 1, 27, "restart-helmet"); record(mc, "Slot restart fixture");
      var death = latest(mc); clear(mc); DeathTracker.recover(mc, death, 1);
      DeathConfig.save(enabled);
      log("DEATH INVENTORY CHECKS FAILURES " + failures);
    } finally {
      mc.setScreen(null); mc.player.inventory.main = main; mc.player.inventory.armor = armor; mc.player.inventory.setCursorStack(cursor);
      properties.allAllowed(cheats); AccessRules.restore(mc, rules);
      if (failures != 0 || saved) DeathConfig.preview(settings);
    }
  }

  private static String inventory(Minecraft mc) throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory)); }
  private static void stack(ItemStack item, int id, int count, int damage, String label) throws Exception {
    check(item != null && item.itemId == id && item.count == count && item.getDamage() == damage
        && tag(item).getString("DeathFixture").equals(label), "stack data or position changed: " + label);
  }
  static DeathInventoryScreen open(Minecraft mc, DeathRecord death, int width, int height) throws Exception {
    ChatChecks.submit(mc, "/death items " + death.id);
    check(mc.currentScreen instanceof DeathInventoryScreen, "/death items did not open inventory viewer");
    var screen = (DeathInventoryScreen)mc.currentScreen; screen.init(mc, width, height); return screen;
  }
  static void bulk(DeathInventoryScreen screen) {
    ((ScreenInput)(Object)screen).power$click((screen.width - 176) / 2 + 110, (screen.height - 210) / 2 + 200, 0);
  }
  static void click(DeathInventoryScreen screen, int slot, int button) throws Exception {
    int x = (screen.width - 176) / 2 + (slot < 36 ? 8 + slot % 9 * 18 : slot < 40 ? 8 : slot == 40 ? 144 : 88 + (slot - 41) % 2 * 18);
    int y = (screen.height - 210) / 2 + 22 + (slot < 9 ? 142 : slot < 36 ? 84 + (slot / 9 - 1) * 18 : slot < 40 ? 8 + (39 - slot) * 18 : slot == 40 ? 36 : 26 + (slot - 41) / 2 * 18);
    ((ScreenInput)(Object)screen).power$click(x + 5, y + 5, button);
  }
}
