package local.luke.power.hud;

import local.luke.power.input.DetachedCamera;
import local.luke.power.input.HeadGeometry;
import local.luke.power.input.PlayerMode;
import local.luke.power.ui.MenuPreferences;
import net.minecraft.entity.player.PlayerEntity;

public final class HudCoordinates {
  private HudCoordinates() {}
  public static double y(PlayerEntity player) {
    if (DetachedCamera.isActive()) return HeadGeometry.baseY(DetachedCamera.y());
    if (player instanceof PlayerMode mode && mode.power$spectator())
      return player.y - player.field_1631 + HeadGeometry.BASE_ABOVE_FEET;
    return MenuPreferences.useFeetYCoordinates() ? player.boundingBox.minY : player.y;
  }
}
