package local.luke.power.render;

import local.luke.power.input.DetachedCamera;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_549;
import net.minecraft.class_113;
import net.minecraft.entity.Entity;
import net.minecraft.client.Minecraft;

/** The vehicle around the local camera must not disappear between culling samples. */
public final class RiddenVehicle {
  private RiddenVehicle() {}
  public static boolean visible(Object candidate) {
    if (!(candidate instanceof Entity vehicle) || !(vehicle instanceof class_549 || vehicle instanceof class_113)
        || vehicle.dead || DetachedCamera.isActive()) return false;
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    return mc != null && mc.player != null && mc.world == vehicle.world && mc.field_2807 == mc.player
        && mc.player.field_1595 == vehicle && vehicle.field_1594 == mc.player;
  }
}
