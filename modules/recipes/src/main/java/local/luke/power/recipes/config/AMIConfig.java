package local.luke.power.recipes.config;

import net.fabricmc.loader.api.FabricLoader;
import local.luke.power.recipes.util.RecipeBrowser;
import net.glasslauncher.mods.gcapi3.api.ConfigRoot;
import net.glasslauncher.mods.gcapi3.api.GCAPI;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;
import net.minecraft.item.ItemStack;

import java.io.File;

public class AMIConfig {

    @ConfigRoot(
            value = "config",
            visibleName = "Main Config"
    )
    public static final AMIConfigObject INSTANCE = new AMIConfigObject();

    public static boolean isRecipeAnimationsEnabled() {
        return true;
    }

    public static void addItemToConfigBlacklist(ItemStack itemStack, boolean wildcard) {
        if (itemStack == null) {
            return;
        }
        String uid = RecipeBrowser.getStackHelper().getUniqueIdentifierForStack(itemStack, wildcard);
        if (INSTANCE.itemBlacklist.add(uid)) {
            updateBlacklist();
        }
    }

    public static void removeItemFromConfigBlacklist(ItemStack itemStack, boolean wildcard) {
        if (itemStack == null) {
            return;
        }
        String uid = RecipeBrowser.getStackHelper().getUniqueIdentifierForStack(itemStack, wildcard);
        if (INSTANCE.itemBlacklist.remove(uid)) {
            updateBlacklist();
        }
    }

    public static boolean isItemOnConfigBlacklist(ItemStack itemStack, boolean wildcard) {
        String uid = RecipeBrowser.getStackHelper().getUniqueIdentifierForStack(itemStack, wildcard);
        return INSTANCE.itemBlacklist.contains(uid);
    }

    public static boolean isDebugModeEnabled() {
        return INSTANCE.debugMode;
    }

    public static boolean isEditModeEnabled() {
        return INSTANCE.editMode;
    }

    private static void updateBlacklist() {
        GCAPI.reloadConfig(RecipeBrowser.NAMESPACE.id("config").toString(), new GlassYamlFile(new File(FabricLoader.getInstance().getConfigDir().toFile(), RecipeBrowser.NAMESPACE + "/config.yml")));
        RecipeBrowser.LOGGER.info("Resetting item filter cache.");
        RecipeBrowser.getItemFilter().reset();
    }

    public static boolean showModNames() {
        return INSTANCE.showModNames;
    }

    public static int getRightClickGiveAmount() {
        return INSTANCE.rightClickGiveAmount;
    }

    public static boolean showNbtCount() {
        return INSTANCE.showNbtCount;
    }

    public static boolean ignoreUntranslatedNames() {
        return INSTANCE.ignoreUntranslatedNames;
    }

    public static boolean showRedundantItems() {
        return INSTANCE.showRedundantItems;
    }
    
    public static boolean centeredSearchBar(){
        return INSTANCE.centeredSearchBar;
    }
    
    public static OverlayMode getOverlayMode(){
        return INSTANCE.overlayMode;
    }

    public static boolean showRarities() {
        return INSTANCE.showRarities;
    }
}
