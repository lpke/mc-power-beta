package local.luke.power.creative;

import local.luke.power.creative.config.Config;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.util.hit.HitType;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.event.entity.player.PlayerEvent;

public final class Reach {
  @EventListener
  public void adjust(PlayerEvent.Reach event) {
    var mc = ClientRuntime.minecraft();
    if (ClientRuntime.local(mc) && event.player == mc.player && mc.player.creative_isCreative())
      event.currentReach =
          (event.type == HitType.BLOCK ? Config.current().blockReach : Config.current().entityReach)
              / 10.0;
  }

  public static double entity() {
    return StationAPI.EVENT_BUS.post(
            PlayerEvent.Reach.builder()
                .player(ClientRuntime.minecraft().player)
                .type(HitType.ENTITY)
                .currentReach(3)
                .build())
        .currentReach;
  }
}
