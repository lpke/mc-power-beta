package local.luke.power.recipes.action;

import com.mojang.serialization.Lifecycle;
import local.luke.power.recipes.api.action.ActionButton;
import local.luke.power.recipes.util.RecipeBrowser;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.registry.RegistryKey;
import net.modificationstation.stationapi.api.registry.SimpleRegistry;

public class ActionButtonRegistry extends SimpleRegistry<ActionButton> {
    public static final RegistryKey<Registry<ActionButton>> KEY = RegistryKey.ofRegistry(RecipeBrowser.NAMESPACE.id("action_buttons"));
    public static final ActionButtonRegistry INSTANCE = new ActionButtonRegistry(KEY, Lifecycle.stable());

    public ActionButtonRegistry(RegistryKey<? extends Registry<ActionButton>> key, Lifecycle lifecycle) {
        super(key, lifecycle);
    }
}
