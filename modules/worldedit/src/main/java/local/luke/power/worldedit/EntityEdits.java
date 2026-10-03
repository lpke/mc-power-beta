package local.luke.power.worldedit;

import java.util.*;
import local.luke.power.worldedit.core.EntityQuery;
import net.minecraft.class_121;
import net.minecraft.class_206;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class EntityEdits {
  private EntityEdits() {}

  public static int apply(Minecraft mc, EntityQuery query, boolean remove) {
    if (!WorldEditor.available(mc))
      throw new IllegalArgumentException("A live local world is required.");
    int count = 0;
    // Snapshot: entities detach from chunks during the next normal world tick.
    for (Object value : new ArrayList<>(mc.world.field_198)) {
      if (!(value instanceof Entity e) || e.dead || e.world != mc.world || protectedEntity(e))
        continue;
      if (!query.contains(e.x - mc.player.x, e.y - mc.player.boundingBox.minY, e.z - mc.player.z))
        continue;
      String name = class_206.method_734(e);
      // Beta omits two projectile classes from the save registry.
      if (e instanceof net.minecraft.class_499) name = "Egg";
      if (e instanceof net.minecraft.class_372) name = "Fireball";
      if (!query.matches(
          name, e instanceof LivingEntity, e instanceof class_121 wolf && wolf.method_425()))
        continue;
      count++;
      if (remove) {
        // Vanilla storage carts spill their inventory from markDead(), even without damage.
        if (e instanceof net.minecraft.class_549 cart)
          for (int slot = 0; slot < cart.size(); slot++) cart.setStack(slot, null);
        e.markDead();
      }
    }
    return count;
  }

  private static boolean protectedEntity(Entity start) {
    Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    ArrayDeque<Entity> todo = new ArrayDeque<>();
    todo.add(start);
    while (!todo.isEmpty()) {
      Entity e = todo.remove();
      if (!seen.add(e)) continue;
      if (e instanceof PlayerEntity) return true;
      if (e.field_1594 != null) todo.add(e.field_1594);
      if (e.field_1595 != null) todo.add(e.field_1595);
    }
    return false;
  }
}
