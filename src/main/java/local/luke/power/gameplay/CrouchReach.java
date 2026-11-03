package local.luke.power.gameplay;

import local.luke.power.input.PlayerMode;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.class_212;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.modificationstation.stationapi.api.event.entity.player.PlayerEvent;

/** Match modern Survival block reach only while the optional lower crouch pose owns the view. */
public final class CrouchReach {
  @EventListener
  public void adjust(PlayerEvent.Reach event) {
    var settings = GameplayConfig.current();
    if (!settings.modernCrawling || !settings.crouchCameraHeight
        || event.type != class_212.TILE || event.currentReach != 4.0
        || !(event.player instanceof ClientPlayerEntity player)
        || !(player instanceof PlayerMode mode) || mode.power$creative()
        || !ModernWater.localMovement(player) || !ModernSwimming.crouched(player)
        || ModernSwimming.compact(player)) return;
    // 26.3's BLOCK_INTERACTION_RANGE defaults to 4.5; ENTITY_INTERACTION_RANGE stays 3.
    event.currentReach = 4.5;
  }
}
