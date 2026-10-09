package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.util.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_142;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Real inventory-screen clicks, with tagged drops counted in the disposable world. */
final class CreativeDropChecks {
  private static final String TAG = "creative-drop-check";

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && !mc.world.isRemote, "run new-world first");
    mc.player.closeScreen();
    var inventory = mc.player.inventory.main.clone();
    var properties = (CheatWorld) mc.world.method_262();
    boolean cheats = properties.power$cheatsEnabled();
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    Object config = Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null);
    var destroy = config.getClass().getField("destroySlot");
    boolean oldDestroy = destroy.getBoolean(config);
    String run = UUID.randomUUID().toString();
    try {
      properties.power$cheatsEnabled(true);
      CreativeVehicleChecks.mode(mc, "CREATIVE");
      for (int i = 0; i < inventory.length; i++) mc.player.inventory.main[i] = tagged(4, i % 5 + 1, 0, "inventory-" + i);
      var screen = new class_585(mc.player); mc.setScreen(screen);
      field(screen, "creative_normalGUI", false);
      for (int[] size : new int[][] {{320, 240}, {640, 420}}) {
        screen.init(mc, size[0], size[1]);
        int x = (size[0] - 176) / 2, y = (size[1] - 166) / 2;
        String prefix = run + "/" + size[0];
        test("creative outside left-click drops full tagged stacks at all four edges, width " + size[0], () -> {
          int[][] points = {{x - 1, y}, {x + 176, y}, {x, y - 21}, {x, y + 166}};
          for (int i = 0; i < points.length; i++) {
            String name = prefix + "/edge-" + i;
            mc.player.inventory.setCursorStack(tagged(35, 37, 5, name));
            click(screen, points[i][0], points[i][1], 0);
            check(mc.player.inventory.getCursorStack() == null, "cursor not cleared");
            drops(mc, name, 1, 37, 35, 5);
            click(screen, points[i][0], points[i][1], 0);
            drops(mc, name, 1, 37, 35, 5);
          }
          inventory(mc);
        });
        test("creative outside right-click splits one item and preserves the remainder, width " + size[0], () -> {
          String name = prefix + "/split";
          mc.player.inventory.setCursorStack(tagged(35, 4, 9, name));
          for (int i = 1; i <= 4; i++) {
            click(screen, x - 1, y, 1);
            if (i < 4) stack(mc.player.inventory.getCursorStack(), 35, 4 - i, 9, name);
            else check(mc.player.inventory.getCursorStack() == null, "empty cursor retained");
            drops(mc, name, i, i, 35, 9);
          }
          inventory(mc);
        });
        test("creative tool drops retain durability and ignore middle/extra mouse buttons, width " + size[0], () -> {
          String name = prefix + "/tool";
          mc.player.inventory.setCursorStack(tagged(267, 1, 83, name));
          for (int button : new int[] {2, 3}) click(screen, x - 1, y, button);
          stack(mc.player.inventory.getCursorStack(), 267, 1, 83, name);
          drops(mc, name, 0, 0, 267, 83);
          click(screen, x - 1, y, 0);
          check(mc.player.inventory.getCursorStack() == null, "tool retained on cursor");
          drops(mc, name, 1, 1, 267, 83);
        });
        test("creative borders, category/view tabs and destroy-slot border do not drop items, width " + size[0], () -> {
          destroy.setBoolean(config, true);
          String name = prefix + "/protected";
          ItemStack cursor = tagged(264, 7, 0, name); mc.player.inventory.setCursorStack(cursor);
          int[][] points = {{x, y - 20}, {x + 170, y - 10}, {x + 170, y + 80},
              {x + 90, y + 138}, {x + 175, y + 165}, {x - 22, y + 137},
              {x - 1, y + 165}, {x + 4, Math.max(0, y - 41)}, {x + 175, y + 120}, {x + 175, y + 145}};
          for (int[] point : points) {
            field(screen, "creative_normalGUI", false);
            click(screen, point[0], point[1], 0);
            stack(mc.player.inventory.getCursorStack(), 264, 7, 0, name);
            drops(mc, name, 0, 0, 264, 0);
          }
          field(screen, "creative_normalGUI", false);
          inventory(mc);
          destroy.setBoolean(config, false);
          click(screen, x - 22, y + 137, 0);
          check(mc.player.inventory.getCursorStack() == null, "hidden destroy border still blocks drops");
          drops(mc, name, 1, 7, 264, 0);
        });
        test("hotbar pickup can be dropped; inventory view and close do not duplicate drops, width " + size[0], () -> {
          String name = prefix + "/hotbar";
          mc.player.inventory.main[0] = tagged(267, 1, 41, name);
          click(screen, x + 8, y + 142, 0);
          stack(mc.player.inventory.getCursorStack(), 267, 1, 41, name);
          check(mc.player.inventory.main[0] == null, "hotbar pickup duplicated source");
          click(screen, x - 1, y, 0); drops(mc, name, 1, 1, 267, 41);
          mc.player.inventory.main[0] = tagged(4, 1, 0, "inventory-0");
          field(screen, "creative_normalGUI", true);
          mc.player.inventory.setCursorStack(tagged(264, 3, 0, prefix + "/normal"));
          click(screen, x - 1, y, 1);
          stack(mc.player.inventory.getCursorStack(), 264, 2, 0, prefix + "/normal");
          click(screen, x - 1, y, 0); drops(mc, prefix + "/normal", 2, 3, 264, 0);
          mc.player.closeScreen();
          drops(mc, name, 1, 1, 267, 41); drops(mc, prefix + "/normal", 2, 3, 264, 0);
          inventory(mc);
          mc.setScreen(screen); screen.init(mc, size[0], size[1]); field(screen, "creative_normalGUI", false);
        });
      }
    } finally {
      mc.player.inventory.setCursorStack(null); mc.player.closeScreen();
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      destroy.setBoolean(config, oldDestroy);
      CreativeVehicleChecks.mode(mc, mode); properties.power$cheatsEnabled(cheats);
      for (Object entity : mc.world.field_198) if (entity instanceof class_142 drop
          && tag(drop.field_564).getString(TAG).startsWith(run)) drop.dead = true;
    }
    log("CREATIVE DROP FAILURES " + failures);
  }

  private static void click(class_585 screen, int x, int y, int button) { ((ScreenInput) screen).power$click(x, y, button); }
  private static NbtCompound tag(ItemStack stack) throws Exception { return (NbtCompound) stack.getClass().getMethod("getStationNbt").invoke(stack); }
  private static ItemStack tagged(int id, int count, int damage, String name) throws Exception {
    ItemStack stack = new ItemStack(id, count, damage); tag(stack).putString(TAG, name); return stack;
  }
  private static void stack(ItemStack item, int id, int count, int damage, String name) throws Exception {
    check(item != null && item.itemId == id && item.count == count && item.getDamage() == damage
        && tag(item).getString(TAG).equals(name), "count/damage/NBT changed: " + name);
  }
  private static void inventory(Minecraft mc) throws Exception {
    for (int i = 0; i < mc.player.inventory.main.length; i++) stack(mc.player.inventory.main[i], 4, i % 5 + 1, 0, "inventory-" + i);
  }
  private static void drops(Minecraft mc, String name, int entities, int count, int id, int damage) throws Exception {
    int found = 0, total = 0;
    for (Object entity : mc.world.field_198) if (entity instanceof class_142 drop
        && !drop.dead && tag(drop.field_564).getString(TAG).equals(name)) {
      stack(drop.field_564, id, drop.field_564.count, damage, name);
      found++; total += drop.field_564.count;
    }
    check(found == entities && total == count, "drop loss/duplication: " + name + " entities=" + found + " count=" + total);
  }
}
