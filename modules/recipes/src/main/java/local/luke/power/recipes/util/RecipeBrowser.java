package local.luke.power.recipes.util;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import local.luke.power.recipes.gui.screen.OverlayScreen;
import local.luke.power.recipes.init.CommonInit;
import local.luke.power.recipes.recipe.ItemBlacklist;
import local.luke.power.recipes.recipe.ItemFilter;
import local.luke.power.recipes.registry.AMIItemRegistry;
import local.luke.power.recipes.registry.RecipeRegistry;
import net.modificationstation.stationapi.api.util.Namespace;
import org.apache.logging.log4j.Logger;

public class RecipeBrowser {
    @SuppressWarnings("UnstableApiUsage")
    public static final Namespace NAMESPACE = Namespace.resolve();
    public static final Logger LOGGER = NAMESPACE.getLogger();

    @Getter @Setter
    private static boolean initializing = false;
    @Getter @Setter
    private static AMIHelpers helpers;
    @Getter @Setter
    private static ItemFilter itemFilter;
    @Getter @Setter
    private static AMIItemRegistry itemRegistry;
    @Getter @Setter
    private static RecipeRegistry recipeRegistry;
    @Getter @Setter
    private static boolean started;

    public static boolean overlayEnabled = true;
    private static boolean amiOnServer;

    public static StackHelper getStackHelper() {
        return helpers.getStackHelper();
    }

    public static void resetItemFilter() {
        if (itemFilter != null) {
            itemFilter.reset();
        }
    }

    public static void reloadBlacklist() {
        ItemBlacklist.reset();
        CommonInit.getPlugins().values().forEach(iModPlugin -> {
            try {
                iModPlugin.updateBlacklist(helpers);
            } catch (IncompatibleClassChangeError ignored) {}
        });
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && RecipeBrowser.getItemFilter() != null) {
            RecipeBrowser.getItemFilter().reset();
            if (OverlayScreen.INSTANCE.parent != null) {
                OverlayScreen.INSTANCE.rebuildRenderList();
            }
        }
    }

    public static boolean isAMIOnServer() {
        return amiOnServer;
    }

    public static void setAMIOnServer(boolean amiOnServer) {
        RecipeBrowser.amiOnServer = amiOnServer;
    }
}
