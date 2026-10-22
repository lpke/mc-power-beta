package local.luke.power.inventory;

import local.luke.power.input.DetachedCamera;
import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.class_212;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.World;

/** Checks native interaction overrides without invoking them or opening inventories. */
public final class InteractionTargets {
  private static final ClassValue<Boolean> BLOCK_USE = new ClassValue<>() {
    protected Boolean computeValue(Class<?> type) {
      return overrides(type, Block.class, World.class, int.class, int.class, int.class, PlayerEntity.class);
    }
  };
  private static final ClassValue<Boolean> ENTITY_USE = new ClassValue<>() {
    protected Boolean computeValue(Class<?> type) {
      return overrides(type, Entity.class, PlayerEntity.class);
    }
  };

  private InteractionTargets() {}

  private static boolean overrides(Class<?> type, Class<?> base, Class<?>... parameters) {
    // Match the unique native use signature; literal mapped method names differ in production.
    for (var method : base.getMethods()) {
      if (method.getReturnType() != boolean.class || !java.util.Arrays.equals(method.getParameterTypes(), parameters)) continue;
      try { return type.getMethod(method.getName(), parameters).getDeclaringClass() != base; }
      catch (ReflectiveOperationException failure) { return false; }
    }
    return false;
  }

  public static boolean spectator(Minecraft mc) {
    if (mc.player == null || !(mc.player instanceof PlayerMode mode) || !mode.power$spectator()
        || mc.currentScreen != null || mc.world == null || mc.world.isRemote || DetachedCamera.isActive()) return false;
    var target = ContainerTarget.find(mc, local.luke.power.map.Minimap.frameDelta());
    if (target == null) return false;
    if (target.field_1983 == class_212.TILE) {
      int x = target.field_1984, y = target.field_1985, z = target.field_1986;
      if (!mc.world.method_239(x, y, z)) return false;
      int id = mc.world.getBlockId(x, y, z);
      return mc.world.method_1777(x, y, z) instanceof Inventory
          || id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null
              && BLOCK_USE.get(Block.BLOCKS[id].getClass());
    }
    Entity entity = target.field_1989;
    return entity != null && (entity instanceof Inventory || ENTITY_USE.get(entity.getClass()));
  }
}
