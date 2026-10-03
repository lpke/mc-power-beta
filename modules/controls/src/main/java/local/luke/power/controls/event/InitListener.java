package local.luke.power.controls.event;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.CompatHelper;
import local.luke.power.controls.util.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.mine_diver.unsafeevents.listener.ListenerPriority;
import net.minecraft.block.Block;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.client.StationRenderAPI;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases;
import net.modificationstation.stationapi.api.event.init.InitFinishedEvent;
import net.modificationstation.stationapi.api.event.mod.InitEvent;
import net.modificationstation.stationapi.api.event.registry.AfterBlockAndItemRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import net.modificationstation.stationapi.api.util.Identifier;

public class InitListener {
    @EventListener(priority = ListenerPriority.HIGHEST, phase = InitEvent.PRE_INIT_PHASE)
    public void preInit(InitEvent event) {
        FabricLoader.getInstance().getEntrypointContainers("power_controls:event_bus", Object.class).forEach(EntrypointManager::setup);

        StationAPI.EVENT_BUS.post(new RegisterControlFeaturesCompatEvent());
    }

    @Environment(EnvType.CLIENT)
    @EventListener
    public void postInit(InitFinishedEvent event) {
        Util.atlasHeight = StationRenderAPI.getBakedModelManager().getAtlas(Atlases.GAME_ATLAS_TEXTURE).getHeight();
    }
    
    @EventListener
    public void afterBlocksAndItems(AfterBlockAndItemRegisterEvent event) {
        StationAPI.EVENT_BUS.post(new RegisterShiftPlacingCompatEvent());
        
        for (String id : ControlFeatures.GAMEPLAY_CONFIG.shiftPlacingBlacklist) {
            Block block = BlockRegistry.INSTANCE.get(Identifier.of(id));
            if (block != null) {
                CompatHelper.addToShiftPlacingBlacklist(block);
            }
        }
    }
}
