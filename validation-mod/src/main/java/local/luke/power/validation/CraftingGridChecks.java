package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.inventory.CraftingGridTransfer;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.storage.PowerConfig;
import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;

final class CraftingGridChecks {
  private static final String ID = "visual.shiftClickIntoCraftingGrid";

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("saved")) {
      check(VisualConfig.current().shiftClickIntoCraftingGrid, "crafting preference lost on restart");
      log("PASS crafting table preference survived restart"); return;
    }
    failures = 0;
    ((CheatWorld) mc.world.method_262()).power$cheatsEnabled(true);
    CreativeVehicleChecks.mode(mc, "SURVIVAL");
    mc.player.closeScreen(); mc.player.method_1340(.5, 103, -2.5);
    for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
      mc.world.method_200(x, 100, z, 1);
      for (int y = 101; y < 106; y++) mc.world.method_200(x, y, z, 0);
    }
    mc.world.method_200(0, 101, 0, 58);
    if (action.equals("rollback")) { rollback(mc); return; }
    enable(false);
    test("crafting option defaults off and Cancel leaves items/settings intact", () -> {
      var c = table(mc); ItemStack source = tagged(5, 17, 0, "source"); c.method_2084(10).setStack(source);
      var session = SettingsRegistry.open(mc); var row = Release110Checks.find(session, ID);
      check(!row.defaultValue.getAsBoolean() && !new VisualSettings().shiftClickIntoCraftingGrid, "wrong default");
      byte[] before = Files.readAllBytes(PowerConfig.path());
      row.parse("true"); session.preview(); check(VisualConfig.current().shiftClickIntoCraftingGrid, "preview missing");
      session.discard(); check(!VisualConfig.current().shiftClickIntoCraftingGrid, "Cancel failed");
      check(c.method_2084(10).getStack() == source && c.field_708.getStack(0) == null, "menu edit moved items");
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "Cancel wrote settings");
    });
    test("disabled option and 2x2 grid retain ordinary Shift-click", () -> {
      var c = table(mc); c.method_2084(10).setStack(new ItemStack(5, 17, 0)); shift(mc, c, 10);
      check(c.field_708.getStack(0) == null && c.method_2084(37).getStack().count == 17, "disabled transfer changed");
      enable(true); mc.player.closeScreen(); clearPlayer(mc); mc.setScreen(new class_585(mc.player));
      var personal = (class_277) mc.player.playerContainer; personal.method_2084(9).setStack(new ItemStack(5, 17, 0));
      shift(mc, personal, 9);
      check(personal.field_1124.getStack(0) == null && personal.method_2084(36).getStack().count == 17, "2x2 behavior changed");
    });
    test("main inventory and hotbar transfer full tagged stacks", () -> {
      for (int index : new int[] {10, 37}) {
        var c = table(mc); c.method_2084(index).setStack(tagged(17, 37, 0, "wood")); shift(mc, c, index);
        check(c.method_2084(index).getStack() == null && mc.player.inventory.getCursorStack() == null, "source/cursor not empty");
        assertStack(c.field_708.getStack(0), 37, 0, "wood");
        check(c.field_709.getStack(0) != null, "recipe output did not update");
        shift(mc, c, 1); check(c.field_708.getStack(0) == null && total(c, "wood") == 37, "grid Shift-click did not return ingredients");
      }
    });
    test("matching stacks fill before empty cells without merging different NBT", () -> {
      var c = table(mc); c.field_708.setStack(0, tagged(4, 30, 0, "other"));
      c.field_708.setStack(4, tagged(4, 60, 0, "match")); c.method_2084(10).setStack(tagged(4, 17, 0, "match"));
      shift(mc, c, 10);
      assertStack(c.field_708.getStack(0), 30, 0, "other"); assertStack(c.field_708.getStack(4), 64, 0, "match");
      assertStack(c.field_708.getStack(1), 13, 0, "match"); check(total(c, "match") == 77, "merge lost/duplicated items");
    });
    test("Shift-clicking the result still crafts into inventory", () -> {
      var c = table(mc); c.field_708.setStack(0, new ItemStack(17, 3, 0)); shift(mc, c, 0);
      int planks = 0; for (ItemStack stack : mc.player.inventory.main) if (stack != null && stack.itemId == 5) planks += stack.count;
      check(planks == 4 && c.field_708.getStack(0).count == 2 && c.field_709.getStack(0).count == 4, "result transfer changed");
    });
    test("partial grid leaves the remainder in the source and full grid uses vanilla transfer", () -> {
      var c = table(mc); for (int i = 0; i < 9; i++) c.field_708.setStack(i, tagged(4, 64, 0, "stone"));
      c.field_708.setStack(4, tagged(4, 60, 0, "stone")); c.method_2084(10).setStack(tagged(4, 17, 0, "stone"));
      shift(mc, c, 10); assertStack(c.method_2084(10).getStack(), 13, 0, "stone");
      check(total(c, "stone") == 589 && mc.player.inventory.getCursorStack() == null, "partial count/cursor mismatch");
      shift(mc, c, 10); check(c.method_2084(10).getStack() == null && c.method_2084(37).getStack().count == 13, "full grid fallback failed");
      check(total(c, "stone") == 589, "full grid lost items");
    });
    test("tools retain damage/NBT; occupied cursors and remote worlds bypass the feature", () -> {
      var c = table(mc); c.method_2084(10).setStack(tagged(267, 1, 71, "tool")); shift(mc, c, 10);
      assertStack(c.field_708.getStack(0), 1, 71, "tool");
      c.method_2084(10).setStack(tagged(4, 7, 0, "source")); ItemStack cursor = tagged(264, 3, 0, "cursor");
      mc.player.inventory.setCursorStack(cursor);
      check(!CraftingGridTransfer.transfer(c, 10, mc.player) && mc.player.inventory.getCursorStack() == cursor, "occupied cursor altered");
      mc.player.inventory.setCursorStack(null); boolean remote = mc.world.isRemote;
      try { mc.world.isRemote = true; check(!CraftingGridTransfer.transfer(c, 10, mc.player), "client-side remote transfer allowed"); }
      finally { mc.world.isRemote = remote; }
      assertStack(c.method_2084(10).getStack(), 7, 0, "source");
    });
    test("Apply saves the option without moving items", () -> {
      var c = table(mc); c.method_2084(10).setStack(tagged(4, 17, 0, "apply"));
      enable(false); var session = SettingsRegistry.open(mc); Release110Checks.find(session, ID).parse("true"); session.save(Path.of("."));
      check(PowerConfig.section("visual").get("shiftClickIntoCraftingGrid").getAsBoolean(), "Apply not persisted");
      assertStack(c.method_2084(10).getStack(), 17, 0, "apply"); check(c.field_708.getStack(0) == null, "Apply moved items");
    });
    mc.player.closeScreen();
    log("CRAFTING GRID FAILURES " + failures);
  }

  private static void rollback(Minecraft mc) throws Exception {
    enable(true); var c = table(mc);
    c.field_708.setStack(0, tagged(4, 60, 0, "rollback")); c.method_2084(10).setStack(tagged(4, 17, 0, "rollback"));
    Slot original = c.method_2084(2);
    Slot faulty = new Slot(c.field_708, 1, original.x, original.y) {
      boolean first = true;
      @Override public void setStack(ItemStack stack) {
        super.setStack(stack);
        if (first) { first = false; throw new IllegalStateException("Injected crafting rollback check"); }
      }
    };
    faulty.id = 2; c.slots.set(2, faulty);
    try {
      shift(mc, c, 10);
      assertStack(c.method_2084(10).getStack(), 17, 0, "rollback");
      assertStack(c.field_708.getStack(0), 60, 0, "rollback");
      check(c.field_708.getStack(1) == null && mc.player.inventory.getCursorStack() == null, "rollback left extra items");
      check(!CraftingGridTransfer.transfer(c, 10, mc.player), "failed feature still active");
      log("PASS partial crafting transfer rolls back all counts/NBT and disables until restart");
    } finally { c.slots.set(2, original); mc.player.closeScreen(); }
  }
  private static class_196 table(Minecraft mc) {
    mc.player.closeScreen(); clearPlayer(mc); mc.player.method_484(0, 101, 0); return (class_196) mc.player.container;
  }
  private static void clearPlayer(Minecraft mc) {
    Arrays.fill(mc.player.inventory.main, null); mc.player.inventory.setCursorStack(null);
  }
  private static void enable(boolean value) { var s = VisualConfig.copy(); s.shiftClickIntoCraftingGrid = value; VisualConfig.preview(s); }
  private static void shift(Minecraft mc, Container c, int slot) { c.onSlotClick(slot, 0, true, mc.player); }
  private static NbtCompound tag(ItemStack item) throws Exception { return (NbtCompound) item.getClass().getMethod("getStationNbt").invoke(item); }
  private static ItemStack tagged(int id, int count, int damage, String name) throws Exception {
    var item = new ItemStack(id, count, damage); tag(item).putString("craft-check", name); return item;
  }
  private static void assertStack(ItemStack item, int count, int damage, String name) throws Exception {
    check(item != null && item.count == count && item.getDamage() == damage && tag(item).getString("craft-check").equals(name), "count/damage/NBT changed: " + name);
  }
  private static int total(Container c, String name) throws Exception {
    int count = 0;
    for (int i = 1; i < c.slots.size(); i++) {
      ItemStack stack = c.method_2084(i).getStack();
      if (stack != null && tag(stack).getString("craft-check").equals(name)) count += stack.count;
    }
    return count;
  }
}
