package local.luke.power.recipes.event;

import local.luke.power.recipes.util.RecipeBrowser;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.modificationstation.stationapi.api.client.event.network.MultiplayerLogoutEvent;
import net.modificationstation.stationapi.api.event.world.WorldEvent;
import net.modificationstation.stationapi.api.network.ModdedPacketHandler;

public class MultiplayerLogoutListener {

    @EventListener
    public static void onLogout(MultiplayerLogoutEvent event) {
        RecipeBrowser.setAMIOnServer(true);
    }

    @EventListener
    public void loadWorld(WorldEvent.Init event) {
        if (Minecraft.INSTANCE.currentScreen instanceof ConnectScreen connectScreen) {
            ModdedPacketHandler handler = (ModdedPacketHandler) connectScreen.networkHandler;
            RecipeBrowser.setAMIOnServer(handler.isModded() && handler.getMods().containsKey(RecipeBrowser.NAMESPACE.toString()));
        }
        else {
            RecipeBrowser.setAMIOnServer(true);
        }
    }
}
