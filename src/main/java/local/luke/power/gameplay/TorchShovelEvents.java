package local.luke.power.gameplay;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.entity.player.PlayerStrengthOnBlockEvent;
import net.modificationstation.stationapi.api.item.StationFlatteningItemStack;

public final class TorchShovelEvents {
  @EventListener public void speed(PlayerStrengthOnBlockEvent event) {
    int slot = TorchShovel.slot(event.player, event.blockState.getBlock());
    if (slot < 0) return;
    var held = (StationFlatteningItemStack)(Object)event.player.method_502();
    var shovel = (StationFlatteningItemStack)(Object)event.player.inventory.main[slot];
    float ordinary = held.getMiningSpeedMultiplier(event.player, event.blockView, event.blockPos, event.blockState);
    float speed = shovel.getMiningSpeedMultiplier(event.player, event.blockView, event.blockPos, event.blockState);
    if (ordinary <= 0 || !Float.isFinite(ordinary) || !Float.isFinite(speed)) return;
    var previous = event.resultProvider;
    event.resultProvider = () -> previous.getAsFloat() * speed / ordinary;
  }
}
