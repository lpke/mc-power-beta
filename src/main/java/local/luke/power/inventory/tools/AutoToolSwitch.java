package local.luke.power.inventory.tools;

import java.util.List;
import local.luke.power.gameplay.*;
import local.luke.power.inventory.NativeInventoryPlan;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.class_420;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Select before native mining captures its tool; ties keep the existing selection. */
public final class AutoToolSwitch {
  private static Object world;
  private static int targetX, targetY, targetZ, targetId;
  private static boolean waiting;
  private AutoToolSwitch() {}
  public static void clear() { world = null; waiting = false; }
  /** False defers a physical break while a durable storage-to-hotbar swap is prepared. */
  public static boolean prepare(Minecraft mc, int x, int y, int z) {
    var settings = InventoryToolsConfig.current();
    if (!settings.toolSwitch || !InventoryAutomation.survival(mc) || !mc.world.method_239(x, y, z)) { clear(); return true; }
    if (local.luke.power.mining.VeinMining.busy()) return true;
    int id = mc.world.getBlockId(x, y, z);
    if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) return true;
    Block block = Block.BLOCKS[id];
    if (block.method_1582(mc.player) >= 1) { clear(); return true; }
    if (TorchShovel.slot(mc.player, block) >= 0) return true;
    if (waiting && NativeInventoryPlan.busy() && world == mc.world && targetX == x && targetY == y && targetZ == z && targetId == id)
      return false;
    waiting = false;
    if (local.luke.power.inventory.InventoryActions.busy()) return true;
    var inventory = mc.player.inventory; int selected = inventory.selectedSlot, best = selected;
    double value = score(inventory.main[selected], block, -1);
    for (int i = 0; i < (settings.toolsFromInventory ? 36 : 9); i++) {
      if (!ToolWhitelist.allows(settings.toolWhitelist, inventory.main[i])) continue;
      double score = score(inventory.main[i], block, settings.toolDurabilityReserve);
      if (score > value + 1e-8) { best = i; value = score; }
    }
    if (best == selected) return true;
    if (best < 9) { inventory.selectedSlot = best; return true; }
    world = mc.world; targetX = x; targetY = y; targetZ = z; targetId = id;
    int source = best;
    ItemStack candidate = inventory.main[source];
    waiting = NativeInventoryPlan.submit(mc, "tool_switch", false, List.of(
        NativeInventoryPlan.Change.main(inventory, selected, inventory.main[source]),
        NativeInventoryPlan.Change.main(inventory, source, inventory.main[selected])),
        () -> InventoryAutomation.survival(mc) && InventoryToolsConfig.current().toolSwitch
            && ToolWhitelist.allows(InventoryToolsConfig.current().toolWhitelist, candidate)
            && miningTarget(mc, x, y, z) && mc.world.method_239(x, y, z) && mc.world.getBlockId(x, y, z) == id,
        () -> waiting = false);
    return !waiting;
  }
  private static boolean miningTarget(Minecraft mc, int x, int y, int z) {
    var target = mc.field_2823;
    return (org.lwjgl.input.Mouse.isButtonDown(0) && !local.luke.power.input.Bindings.claimsMouse(-100)
        || local.luke.power.input.AttackInput.held()) && target != null && target.field_1983 == net.minecraft.class_212.TILE
        && target.field_1984 == x && target.field_1985 == y && target.field_1986 == z;
  }
  private static double score(ItemStack item, Block block, int reserve) {
    if (item == null || item.count <= 0) return block.field_1900.method_898() ? 1.0 / 30 : 1.0 / 100;
    if (item.isDamageable() && item.getMaxDamage() - item.getDamage() + 1 <= reserve) return -1;
    double speed = item.method_708(block);
    if (!Double.isFinite(speed) || speed <= 0) return -1;
    if (GameplayConfig.current().axesSlashLeaves && block.field_1900 == Material.LEAVES && item.getItem() instanceof class_420)
      speed = Math.max(speed, 1 + item.method_708(Block.LOG) * .3);
    return block.field_1900.method_898() || item.method_714(block) ? speed / 30 : 1.0 / 100;
  }
}
