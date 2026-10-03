package local.luke.power.recipes.plugins.vanilla.crafting;

import local.luke.power.recipes.api.recipe.RecipeHandler;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.api.recipe.VanillaRecipeCategoryUid;
import net.modificationstation.stationapi.impl.recipe.StationShapedRecipe;

import javax.annotation.Nonnull;
import java.util.List;

public class ShapedOreRecipeHandler implements RecipeHandler<StationShapedRecipe> {

    @Override
    @Nonnull
    public Class<StationShapedRecipe> getRecipeClass() {
        return StationShapedRecipe.class;
    }

    @Nonnull
    @Override
    public String getRecipeCategoryUid() {
        return VanillaRecipeCategoryUid.CRAFTING;
    }

    @Override
    @Nonnull
    public RecipeWrapper getRecipeWrapper(@Nonnull StationShapedRecipe recipe) {
        return new ShapedOreRecipeWrapper(recipe);
    }

    @Override
    public boolean isRecipeValid(@Nonnull StationShapedRecipe recipe) {
        if (recipe.getGrid() == null) {
            return false;
        }
        int inputCount = 0;
        try {
            for (Object input : recipe.getGrid()) {
                if (input instanceof List) {
                    if (((List) input).isEmpty()) {
                        return false;
                    }
                }
                if (input != null) {
                    inputCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return inputCount > 0;
    }
}
