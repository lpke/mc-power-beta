package local.luke.power.recipes.plugins.multiblock;

import local.luke.power.recipes.api.*;
import local.luke.power.recipes.registry.multiblock.MultiBlockRecipeRegistry;
import local.luke.power.recipes.util.RecipeBrowser;
import net.minecraft.nbt.NbtCompound;
import net.modificationstation.stationapi.api.util.Identifier;

public class MultiblockPluginProvider implements ModPluginProvider {
    @Override
    public String getName() {
        return "Multiblock Tab";
    }

    @Override
    public Identifier getId() {
        return RecipeBrowser.NAMESPACE.id("power_recipes");
    }

    @Override
    public void onAMIHelpersAvailable(AMIHelpers amiHelpers) {

    }

    @Override
    public void onItemRegistryAvailable(ItemRegistry itemRegistry) {

    }

    @Override
    public void register(ModRegistry registry) {
        registry.addRecipeCategories(new MultiBlockRecipeCategory());

        registry.addRecipeHandlers(new MultiBlockRecipeHandler());
        registry.addRecipes(MultiBlockRecipeRegistry.INSTANCE.getRecipes());

    }

    @Override
    public void onRecipeRegistryAvailable(RecipeRegistry recipeRegistry) {

    }

    @Override
    public SyncableRecipe deserializeRecipe(NbtCompound recipe) {
        return null;
    }

    @Override
    public void updateBlacklist(AMIHelpers amiHelpers) {

    }
}
