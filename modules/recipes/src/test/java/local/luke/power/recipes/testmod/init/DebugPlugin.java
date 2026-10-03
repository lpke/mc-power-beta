package local.luke.power.recipes.testmod.init;

import local.luke.power.recipes.api.AMIHelpers;
import local.luke.power.recipes.api.SyncableRecipe;
import local.luke.power.recipes.api.ItemRegistry;
import local.luke.power.recipes.api.ModPluginProvider;
import local.luke.power.recipes.api.ModRegistry;
import local.luke.power.recipes.api.RecipeRegistry;
import local.luke.power.recipes.testmod.TestMod;
import local.luke.power.recipes.testmod.recipe.DebugRecipeCategory;
import local.luke.power.recipes.testmod.recipe.DebugRecipeHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.modificationstation.stationapi.api.util.Identifier;

public class DebugPlugin implements ModPluginProvider {

    @Override
    public String getName() {
        return "Debug";
    }

    @Override
    public Identifier getId() {
        return TestMod.NAMESPACE.id("debug");
    }

    @Override
    public void onAMIHelpersAvailable(AMIHelpers amiHelpers) {

    }

    @Override
    public void onItemRegistryAvailable(ItemRegistry itemRegistry) {

    }

    @Override
    public void register(ModRegistry registry) {

        registry.addDescription(
                new ItemStack(Item.WOODEN_DOOR),
                "description.power_recipestest.wooden.door.1", // actually 2 lines
                "description.power_recipestest.wooden.door.2",
                "description.power_recipestest.wooden.door.3"
        );

        registry.addRecipeCategories(new DebugRecipeCategory());
        registry.addRecipeHandlers(new DebugRecipeHandler());
//        registry.addRecipes(Arrays.asList(
//                new DebugRecipe(),
//                new DebugRecipe()
//        ));
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
