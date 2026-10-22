package local.luke.power.debug;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.gui.screen.container.TooltipBuildEvent;
import net.modificationstation.stationapi.api.registry.ItemRegistry;

public final class DebugTooltips {
  @EventListener public void tooltip(TooltipBuildEvent event) {
    if(!DebugConfig.enabled() || !DebugActions.tooltips || event.itemStack==null) return;
    var stack=event.itemStack;
    event.add("\u00a78"+ItemRegistry.INSTANCE.getId(stack.getItem())+" (#"+stack.itemId+":"+stack.getDamage()+")");
    if(stack.getMaxDamage()>0) event.add("\u00a77Durability: "+(stack.getMaxDamage()-stack.getDamage())+" / "+stack.getMaxDamage());
  }
}
