package local.luke.power.render;

import local.luke.power.input.DetachedCamera;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_549;
import net.minecraft.client.Minecraft;

/** The cart around the local camera must not disappear between culling samples. */
public final class RiddenMinecart {
  private RiddenMinecart() {}
  public static boolean visible(Object candidate) {
    if (!(candidate instanceof class_549 cart) || cart.dead || DetachedCamera.isActive()) return false;
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    return mc != null && mc.player != null && mc.world == cart.world && mc.field_2807 == mc.player
        && mc.player.field_1595 == cart && cart.field_1594 == mc.player;
  }
}
