package local.luke.power.recipes.util;

import local.luke.power.recipes.recipe.ItemBlacklist;
import local.luke.power.recipes.recipe.NbtIgnoreList;
import local.luke.power.recipes.transfer.RecipeTransferHandlerHelper;

import javax.annotation.Nonnull;

public class AMIHelpers implements local.luke.power.recipes.api.AMIHelpers {
    private final StackHelper stackHelper;
    private final ItemBlacklist itemBlacklist;
    private final NbtIgnoreList nbtIgnoreList;
    private final RecipeTransferHandlerHelper recipeTransferHandlerHelper;

    public AMIHelpers() {
        this.stackHelper = new StackHelper();
        this.itemBlacklist = new ItemBlacklist();
        this.nbtIgnoreList = new NbtIgnoreList();
        this.recipeTransferHandlerHelper = new RecipeTransferHandlerHelper();
    }

    @Nonnull
    @Override
    public StackHelper getStackHelper() {
        return stackHelper;
    }

    @Nonnull
    @Override
    public ItemBlacklist getItemBlacklist() {
        return itemBlacklist;
    }

    @Nonnull
    @Override
    public NbtIgnoreList getNbtIgnoreList() {
        return nbtIgnoreList;
    }

    @Nonnull
    @Override
    public RecipeTransferHandlerHelper recipeTransferHandlerHelper() {
        return recipeTransferHandlerHelper;
    }

    @Override
    public void reload() {

    }
}
