package local.luke.power.offhand;

import local.luke.power.inventory.HandUse;
import net.minecraft.entity.projectile.FishingBobberEntity;

/** The cast remembers its physical hand even after a hand swap. */
public interface OffhandFishing {
  boolean power$offhandCast();
  void power$offhandCast(boolean value);
  static boolean left(FishingBobberEntity hook) { return ((OffhandFishing)(Object)hook).power$offhandCast(); }
}
