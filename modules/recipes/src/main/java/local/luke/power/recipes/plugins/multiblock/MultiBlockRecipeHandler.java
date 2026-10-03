package local.luke.power.recipes.plugins.multiblock;

import local.luke.power.recipes.api.recipe.RecipeHandler;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.recipe.multiblock.MultiBlockRecipe;
import org.jetbrains.annotations.NotNull;

public class MultiBlockRecipeHandler implements RecipeHandler<MultiBlockRecipe> {
    @Override
    public @NotNull Class<MultiBlockRecipe> getRecipeClass() {
        return MultiBlockRecipe.class;
    }

    @Override
    public @NotNull String getRecipeCategoryUid() {
        return "multi_block";
    }

    @Override
    public @NotNull RecipeWrapper getRecipeWrapper(@NotNull MultiBlockRecipe multiBlockRecipe) {
        return new MultiBlockRecipeWrapper(multiBlockRecipe);
    }

    @Override
    public boolean isRecipeValid(@NotNull MultiBlockRecipe multiBlockRecipe) {
        return true;
    }
}
