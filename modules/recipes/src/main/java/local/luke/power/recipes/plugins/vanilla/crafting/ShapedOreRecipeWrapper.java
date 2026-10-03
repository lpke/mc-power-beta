package local.luke.power.recipes.plugins.vanilla.crafting;

import lombok.Getter;
import local.luke.power.recipes.api.recipe.wrapper.ShapedCraftingRecipeWrapper;
import local.luke.power.recipes.plugins.vanilla.VanillaRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.impl.recipe.StationShapedRecipe;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class ShapedOreRecipeWrapper extends VanillaRecipeWrapper implements ShapedCraftingRecipeWrapper {

    @Nonnull
    @Getter
    private final StationShapedRecipe recipe;

    public ShapedOreRecipeWrapper(@Nonnull StationShapedRecipe recipe) {
        this.recipe = recipe;
        for (Object input : this.recipe.getGrid()) {
            if (input instanceof ItemStack itemStack) {
                if (itemStack.count != 1) {
                    itemStack.count = 1;
                }
            }
        }
    }

    @Nonnull
    @Override
    public List getInputs() {
        return Arrays.asList(recipe.getGrid());
    }

    @Nonnull
    @Override
    public List<ItemStack> getOutputs() {
        return List.of(recipe.getOutput());
    }

    @Override
    public int getWidth() {
        return recipe.width;
    }

    @Override
    public int getHeight() {
        return recipe.height;
    }

}
