package com.github.telvarost.unitweakstelsaddons.events;

import com.github.telvarost.unitweakstelsaddons.ModHelper;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.registry.DimensionRegistryEvent;

public class RegistryListener {

    @EventListener
    public void handleAfterBlockAndItemRegisterEvent(DimensionRegistryEvent event)
    {
        ModHelper.ModHelperFields.blocksAndItemsRegistered = true;
        ModHelper.AttemptToSetStackSizeOfItems();
    }
}
