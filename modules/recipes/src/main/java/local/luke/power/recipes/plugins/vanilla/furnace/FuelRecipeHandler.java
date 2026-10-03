package local.luke.power.recipes.plugins.vanilla.furnace;

import local.luke.power.recipes.api.recipe.RecipeHandler;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.api.recipe.VanillaRecipeCategoryUid;

import javax.annotation.Nonnull;

public class FuelRecipeHandler implements RecipeHandler<FuelRecipe> {
    @Override
    @Nonnull
    public Class<FuelRecipe> getRecipeClass() {
        return FuelRecipe.class;
    }

    @Nonnull
    @Override
    public String getRecipeCategoryUid() {
        return VanillaRecipeCategoryUid.FUEL;
    }

    @Override
    @Nonnull
    public RecipeWrapper getRecipeWrapper(@Nonnull FuelRecipe recipe) {
        return recipe;
    }

    @Override
    public boolean isRecipeValid(@Nonnull FuelRecipe recipe) {
        return !recipe.getInputs().isEmpty() && recipe.getOutputs().isEmpty();
    }
}
