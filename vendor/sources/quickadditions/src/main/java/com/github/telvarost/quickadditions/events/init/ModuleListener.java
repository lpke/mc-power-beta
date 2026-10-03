package com.github.telvarost.quickadditions.events.init;

import com.github.telvarost.quickadditions.betterf3.MusicModule;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.mod.entrypoint.Entrypoint;
import net.modificationstation.stationapi.api.util.Namespace;
import ralf2oo2.betterf3.registry.ModuleRegisterEvent;
import ralf2oo2.betterf3.utils.ModulePosition;

public class ModuleListener {
    @Entrypoint.Namespace
    public static Namespace NAMESPACE;

    @EventListener
    public void registerModules(ModuleRegisterEvent event){
        event.register(NAMESPACE.id("music_module"), MusicModule::new, ModulePosition.LEFT, "music");
    }
}
