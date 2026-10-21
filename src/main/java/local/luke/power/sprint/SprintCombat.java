package local.luke.power.sprint;

import local.luke.power.world.ExplosionSource;
import net.minecraft.class_410;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Combat restrictions follow accepted incoming mob damage, including native projectiles. */
public final class SprintCombat {
  private SprintCombat() {}

  public static boolean mobDamage(Entity source, boolean successful) {
    if (!successful) return false;
    if (source instanceof class_410 arrow) source = arrow.field_1576;
    if (source == null) source = ExplosionSource.attacker();
    return source instanceof LivingEntity && !(source instanceof PlayerEntity);
  }
}
