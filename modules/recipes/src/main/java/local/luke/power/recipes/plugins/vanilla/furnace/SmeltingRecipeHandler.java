package local.luke.power.recipes.plugins.vanilla.furnace;

import local.luke.power.recipes.api.recipe.RecipeHandler;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.api.recipe.VanillaRecipeCategoryUid;

import javax.annotation.Nonnull;

public class SmeltingRecipeHandler implements RecipeHandler<SmeltingRecipe> {

    @Override
    @Nonnull
    public Class<SmeltingRecipe> getRecipeClass() {
        return SmeltingRecipe.class;
    }

    @Nonnull
    @Override
    public String getRecipeCategoryUid() {
        return VanillaRecipeCategoryUid.SMELTING;
    }

    @Override
    @Nonnull
    public RecipeWrapper getRecipeWrapper(@Nonnull SmeltingRecipe recipe) {
        return recipe;
    }

    @Override
    public boolean isRecipeValid(@Nonnull SmeltingRecipe recipe) {
        return !recipe.getInputs().isEmpty() && !recipe.getOutputs().isEmpty();
    }

}
