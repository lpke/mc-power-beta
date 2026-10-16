package local.luke.power.world;

import java.util.Objects;
import local.luke.power.ui.*;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.modificationstation.stationapi.api.client.event.gui.screen.EditWorldScreenEvent;
import net.modificationstation.stationapi.api.client.gui.widget.ButtonWidgetDetachedContext;

/** Add to StationAPI's actual world Edit menu using its scrollable button registry. */
public final class SavedWorldMenu {
  @EventListener public void buttons(EditWorldScreenEvent.ScrollableButtonContextRegister event) {
    event.contexts.add(screen -> new ButtonWidgetDetachedContext(
        id -> new ButtonWidget(id, 0, 0, "Edit game rules"),
        button -> {
          Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
          try { PowerOptionsScreen.openSavedWorldRules(mc, screen, screen.worldData.method_1956()); }
          catch (Exception e) {
            mc.setScreen(new DataProblemScreen(Objects.toString(e.getMessage(), "World metadata could not be read."), false));
          }
        }));
  }
}
