package local.luke.power.recipes.util;

import local.luke.power.recipes.api.recipe.transfer.RecipeTransferHandler;
import local.luke.power.recipes.api.recipe.transfer.RecipeTransferInfo;
import local.luke.power.recipes.transfer.BasicRecipeTransferHandler;
import local.luke.power.recipes.transfer.BasicRecipeTransferInfo;
import net.minecraft.screen.ScreenHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RecipeTransferRegistry implements local.luke.power.recipes.api.recipe.transfer.RecipeTransferRegistry {
    private final List<RecipeTransferHandler> recipeTransferHandlers = new ArrayList<>();

    @Override
    public void addRecipeTransferHandler(@Nullable Class<? extends ScreenHandler> containerClass, @Nullable String recipeCategoryUid, int recipeSlotStart, int recipeSlotCount, int inventorySlotStart, int inventorySlotCount) {
        if (containerClass == null) {
            RecipeBrowser.LOGGER.error("Null containerClass", new NullPointerException());
            return;
        }
        if (recipeCategoryUid == null) {
            RecipeBrowser.LOGGER.error("Null recipeCategoryUid", new NullPointerException());
            return;
        }

        RecipeTransferInfo recipeTransferHelper = new BasicRecipeTransferInfo(containerClass, recipeCategoryUid, recipeSlotStart, recipeSlotCount, inventorySlotStart, inventorySlotCount);
        addRecipeTransferHandler(recipeTransferHelper);
    }

    @Override
    public void addRecipeTransferHandler(@Nullable RecipeTransferInfo recipeTransferInfo) {
        if (recipeTransferInfo == null) {
            RecipeBrowser.LOGGER.error("Null recipeTransferInfo", new NullPointerException());
            return;
        }
        RecipeTransferHandler recipeTransferHandler = new BasicRecipeTransferHandler(recipeTransferInfo);
        addRecipeTransferHandler(recipeTransferHandler);
    }

    @Override
    public void addRecipeTransferHandler(@Nullable RecipeTransferHandler recipeTransferHandler) {
        if (recipeTransferHandler == null) {
            RecipeBrowser.LOGGER.error("Null recipeTransferHandler", new NullPointerException());
            return;
        }
        this.recipeTransferHandlers.add(recipeTransferHandler);
    }

    public List<RecipeTransferHandler> getRecipeTransferHandlers() {
        return recipeTransferHandlers;
    }
}
