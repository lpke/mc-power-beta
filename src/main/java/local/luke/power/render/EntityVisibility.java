package local.luke.power.render;

import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;

public final class EntityVisibility {
  private EntityVisibility() {}
  public static boolean hidden(Entity entity) {
    if (local.luke.power.ui.PhotoModeTools.hidden(entity)) return true;
    VisualSettings s = VisualConfig.current();
    if (!s.hideEntities) return false;
    if (entity instanceof class_142) return s.hideItems;
    if (entity instanceof PlayerEntity) return s.hidePlayers;
    if (entity instanceof LivingEntity) return s.hideMobs;
    if (entity instanceof class_365) return s.hideTnt;
    if (entity instanceof class_113 || entity instanceof class_549) return s.hideVehicles;
    if (entity instanceof class_372 || entity instanceof class_484 || entity instanceof class_499
        || entity instanceof class_410 || entity instanceof FishingBobberEntity) return s.hideProjectiles;
    return s.hideOther;
  }
}
