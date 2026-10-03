package local.luke.power.recipes.api.event;

import local.luke.power.recipes.action.ActionButtonRegistry;
import local.luke.power.recipes.api.action.ActionButton;
import local.luke.power.recipes.util.RecipeBrowser;
import net.mine_diver.unsafeevents.Event;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.util.Identifier;

public class ActionButtonRegisterEvent extends Event {

    public boolean add(Identifier identifier, ActionButton button) {
        try {
            Registry.register(ActionButtonRegistry.INSTANCE, identifier, button);
            RecipeBrowser.LOGGER.info("Button {} registered.", identifier);
            return true;
        }
        catch (Exception e) {
            RecipeBrowser.LOGGER.error("Failed to register action button", e);
            return false;
        }
    }
}
