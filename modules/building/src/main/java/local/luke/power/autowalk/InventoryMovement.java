package local.luke.power.autowalk;

import local.luke.power.autowalk.mixin.CraftingPosition;
import local.luke.power.autowalk.mixin.DoubleInventoryAccessor;
import local.luke.power.autowalk.mixin.SlotInventoryAccessor;
import local.luke.power.building.config.Config;
import net.minecraft.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import org.lwjgl.opengl.Display;

/** Reads input and container positions only; normal container closure owns all item handling. */
public final class InventoryMovement {
  private InventoryMovement() {}

  public static boolean supports(Screen screen) {
    if (!Config.current().inventoryWhileMoving || screen == null) return false;
    // These screens have no text input. Unknown subclasses may contain search/sign fields.
    Class<?> type = screen.getClass();
    return type == class_585.class || type == class_264.class || type == class_235.class
        || type == class_516.class || type == class_524.class;
  }

  public static boolean allows(Minecraft mc, Screen screen) {
    return supports(screen) && mc.world != null && !mc.world.isRemote && mc.player != null
        && mc.player.world == mc.world && mc.player.health > 0 && !mc.player.dead
        && !mc.player.method_943() && Display.isActive();
  }

  public static void closeOutOfReach(Minecraft mc) {
    if (!supports(mc.currentScreen) || mc.world == null || mc.world.isRemote
        || mc.player == null || mc.interactionManager == null) return;
    ContainerScreen screen = (ContainerScreen) mc.currentScreen;
    if (screen.container == mc.player.playerContainer) return;
    double distance = distanceSquared(screen, mc.player);
    double reach = mc.interactionManager.method_1715();
    if (mc.player.container != screen.container || !screen.container.method_2094(mc.player)
        || !Double.isFinite(reach) || distance > reach * reach) {
      // Uses the same path as Escape, including cursor and crafting-grid cleanup.
      mc.player.closeScreen();
    }
  }

  private static double distanceSquared(ContainerScreen screen, PlayerEntity player) {
    if (screen.container instanceof CraftingPosition c)
      return ReachDistance.toBox(player.x, player.y, player.z,
          c.power$x(), c.power$y(), c.power$z(), c.power$x()+1, c.power$y()+1, c.power$z()+1);
    if (screen.container.slots.isEmpty()) return Double.POSITIVE_INFINITY;
    Inventory inventory = ((SlotInventoryAccessor)screen.container.slots.get(0)).power$inventory();
    return inventoryDistance(inventory, player, 0);
  }

  private static double inventoryDistance(Inventory inventory, PlayerEntity p, int depth) {
    if (depth > 2) return Double.POSITIVE_INFINITY;
    if (inventory instanceof BlockEntity b)
      return b.world == p.world ? ReachDistance.toBox(p.x,p.y,p.z,b.x,b.y,b.z,b.x+1,b.y+1,b.z+1)
          : Double.POSITIVE_INFINITY;
    if (inventory instanceof DoubleInventoryAccessor d)
      return Math.min(inventoryDistance(d.power$first(),p,depth+1), inventoryDistance(d.power$second(),p,depth+1));
    if (inventory instanceof Entity e && e.world == p.world && !e.dead)
      return ReachDistance.toBox(p.x,p.y,p.z,e.boundingBox.minX,e.boundingBox.minY,e.boundingBox.minZ,
          e.boundingBox.maxX,e.boundingBox.maxY,e.boundingBox.maxZ);
    return Double.POSITIVE_INFINITY;
  }
}
