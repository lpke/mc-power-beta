package local.luke.power.sprint;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.world.ExplosionSource;
import net.minecraft.class_365;
import net.minecraft.class_410;
import net.minecraft.entity.LivingEntity;
import org.junit.jupiter.api.Test;

class SprintCombatTest {
  @Test void acceptedMobDamageStartsCombatWhileRejectedAndEnvironmentalDamageDoNot() {
    var mob = new LivingEntity(null) {};
    assertTrue(SprintCombat.mobDamage(mob, true));
    assertFalse(SprintCombat.mobDamage(mob, false));
    assertFalse(SprintCombat.mobDamage(null, true));
    assertFalse(SprintCombat.mobDamage(new class_365(null), true));
    var arrow = new class_410(null); arrow.field_1576 = mob;
    assertTrue(SprintCombat.mobDamage(arrow, true));
    assertFalse(SprintCombat.mobDamage(arrow, false));
    arrow.field_1576 = null;
    assertFalse(SprintCombat.mobDamage(arrow, true));
  }

  @Test void ghastExplosionAttributionIsScopedAndRestoredOnEveryExit() {
    var mob = new LivingEntity(null) {};
    assertNull(ExplosionSource.attacker()); assertFalse(ExplosionSource.fireball());
    ExplosionSource.fireball(mob, () -> {
      assertTrue(ExplosionSource.fireball()); assertSame(mob, ExplosionSource.attacker());
      assertTrue(SprintCombat.mobDamage(null, true)); assertFalse(SprintCombat.mobDamage(null, false));
      ExplosionSource.fireball(null, () -> { assertFalse(SprintCombat.mobDamage(null, true)); return null; });
      assertSame(mob, ExplosionSource.attacker()); return null;
    });
    assertNull(ExplosionSource.attacker()); assertFalse(ExplosionSource.fireball());
    assertThrows(IllegalStateException.class, () -> ExplosionSource.fireball(mob, () -> { throw new IllegalStateException("injected"); }));
    assertNull(ExplosionSource.attacker()); assertFalse(SprintCombat.mobDamage(null, true));
  }
}
