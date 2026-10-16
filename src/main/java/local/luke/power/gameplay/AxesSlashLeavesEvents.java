package local.luke.power.gameplay;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.entity.player.PlayerStrengthOnBlockEvent;
import net.modificationstation.stationapi.api.item.StationFlatteningItemStack;

public final class AxesSlashLeavesEvents {
  @EventListener public void speed(PlayerStrengthOnBlockEvent event) {
    if (!GameplayConfig.current().axesSlashLeaves
        || event.blockState.getBlock().field_1900 != net.minecraft.block.Material.LEAVES
        || event.player.world == null || event.player.world.isRemote) return;
    var held = event.player.method_502();
    if (held == null) return;
    float ordinary = ((StationFlatteningItemStack)(Object)held).getMiningSpeedMultiplier(
        event.player, event.blockView, event.blockPos, event.blockState);
    if (!Float.isFinite(ordinary) || ordinary <= 0) return;
    float speed = AxesSlashLeaves.speed(event.player, event.blockState.getBlock(), ordinary);
    if (speed == ordinary) return;
    var previous = event.resultProvider;
    event.resultProvider = () -> previous.getAsFloat() * speed / ordinary;
  }
}
