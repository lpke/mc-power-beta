package local.luke.power.creative.inventory.registry;

import net.mine_diver.unsafeevents.Event;
import local.luke.power.creative.inventory.api.CreativeTab;

public class TabRegistryEvent extends Event {
	public TabRegistryEvent() {}
	
	public void register(CreativeTab tab) {
		TabRegistry.register(tab.getID(), tab);
	}
}
