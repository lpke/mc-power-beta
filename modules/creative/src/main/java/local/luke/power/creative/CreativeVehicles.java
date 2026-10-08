package local.luke.power.creative;

import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;

/** Creative attacks discard vehicles and their contents without invoking damage drops. */
public final class CreativeVehicles {
  private CreativeVehicles() {}

  public static boolean destroy(PlayerEntity player, Entity target) {
    if (!player.creative_isCreative() || player.level.isRemote || target.level != player.level
        || !(target instanceof BoatEntity || target instanceof MinecartEntity)) return false;
    if (!target.removed) {
      if (target.passenger != null) target.passenger.stopRiding(null);
      if (target instanceof MinecartEntity cart)
        for (int slot = 0; slot < cart.getInventorySize(); slot++) cart.setItem(slot, null);
      target.remove();
    }
    return true;
  }
}
