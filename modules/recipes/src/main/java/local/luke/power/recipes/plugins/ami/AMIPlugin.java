package local.luke.power.recipes.plugins.ami;

import local.luke.power.recipes.api.*;
import local.luke.power.recipes.plugins.ami.description.ItemDescriptionRecipe;
import local.luke.power.recipes.plugins.ami.description.ItemDescriptionRecipeCategory;
import local.luke.power.recipes.plugins.ami.description.ItemDescriptionRecipeHandler;
import local.luke.power.recipes.plugins.vanilla.VanillaPlugin;
import local.luke.power.recipes.util.RecipeBrowser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;

public class AMIPlugin implements ModPluginProvider {
    public static final Identifier ID = RecipeBrowser.NAMESPACE.id("always_more_items");

    @Override
    public String getName() {
        return "AMI";
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onAMIHelpersAvailable(AMIHelpers amiHelpers) {
    }

    @Override
    public void onItemRegistryAvailable(ItemRegistry itemRegistry) {

    }

    @Override
    public void register(ModRegistry registry) {

        registry.addRecipeCategories(
                new ItemDescriptionRecipeCategory()
        );

        registry.addRecipeHandlers(
                new ItemDescriptionRecipeHandler()
        );
    }

    @Override
    public void onRecipeRegistryAvailable(RecipeRegistry recipeRegistry) {

    }

    @Override
    public SyncableRecipe deserializeRecipe(NbtCompound recipe) {
        return (SyncableRecipe) new ItemDescriptionRecipe(Arrays.asList(VanillaPlugin.parseInputs(recipe.getList("output"))), nbtListToArrayList(recipe.getList("description")));
    }

    @Override
    public void updateBlacklist(AMIHelpers amiHelpers) {

    }

    public static <T> ArrayList<T> nbtListToArrayList(NbtList nbtList) {
        ArrayList<T> arrayList = new ArrayList<>(nbtList.size());
        for (int i = 0; i < nbtList.size(); i++) {
            //noinspection unchecked Good. Crash.
            arrayList.add((T) nbtList.get(i));
        }
        return arrayList;
    }
}
