package local.luke.power.recipes.util;

import local.luke.power.recipes.api.recipe.RecipeCategory;
import local.luke.power.recipes.api.recipe.RecipeHandler;
import local.luke.power.recipes.api.recipe.transfer.RecipeTransferHandler;
import local.luke.power.recipes.api.recipe.transfer.RecipeTransferRegistry;
import local.luke.power.recipes.gui.RecipeClickableArea;
import local.luke.power.recipes.plugins.ami.description.ItemDescriptionRecipe;
import local.luke.power.recipes.registry.RecipeRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.*;

public class ModRegistry implements local.luke.power.recipes.api.ModRegistry {
    private final List<RecipeCategory> recipeCategories;
    private final List<RecipeHandler> recipeHandlers;
    private final List<Object> recipes = new ArrayList<>();
    private final local.luke.power.recipes.util.RecipeTransferRegistry recipeTransferRegistry;
    private final Map<Class<? extends HandledScreen>, RecipeClickableArea> recipeClickableAreas;

    public ModRegistry() {
        recipeCategories = new ArrayList<>();
        recipeHandlers = new ArrayList<>();
        recipeTransferRegistry = new local.luke.power.recipes.util.RecipeTransferRegistry();
        recipeClickableAreas = new HashMap<>();
    }

    public ModRegistry(ModRegistry oldRegistry) {
        recipeCategories = oldRegistry.recipeCategories;
        recipeHandlers = oldRegistry.recipeHandlers;
        recipeTransferRegistry = oldRegistry.recipeTransferRegistry;
        recipeClickableAreas = oldRegistry.recipeClickableAreas;
    }

    @Override
    public void addRecipeCategories(RecipeCategory... recipeCategories) {
        Collections.addAll(this.recipeCategories, recipeCategories);
    }

    @Override
    public void addRecipeHandlers(RecipeHandler... recipeHandlers) {
        Collections.addAll(this.recipeHandlers, recipeHandlers);
    }

    @Override
    public void addRecipes(List recipes) {
        if (recipes != null) {
            this.recipes.addAll(recipes);
        }
    }

    @Override
    public void addRecipeClickArea(@Nonnull Class<? extends HandledScreen> guiClass, int xPos, int yPos, int width, int height, @Nonnull String... recipeCategoryUids) {
        RecipeClickableArea recipeClickableArea = new RecipeClickableArea(yPos, yPos + height, xPos, xPos + width, recipeCategoryUids);
        this.recipeClickableAreas.put(guiClass, recipeClickableArea);
    }

    @Override
    public void addDescription(List<ItemStack> itemStacks, String... descriptionKeys) {
        if (itemStacks == null || itemStacks.isEmpty()) {
            IllegalArgumentException e = new IllegalArgumentException();
            RecipeBrowser.LOGGER.error("Tried to add description with no itemStacks.", e);
            return;
        }
        if (descriptionKeys.length == 0) {
            IllegalArgumentException e = new IllegalArgumentException();
            RecipeBrowser.LOGGER.error("Tried to add an empty list of descriptionKeys for itemStacks {}.", itemStacks, e);
            return;
        }
        List<ItemDescriptionRecipe> recipes = ItemDescriptionRecipe.create(itemStacks, descriptionKeys);
        this.recipes.addAll(recipes);
    }

    @Override
    public void addDescription(ItemStack itemStack, String... descriptionKeys) {
        addDescription(Collections.singletonList(itemStack), descriptionKeys);
    }

    @Override
    public RecipeTransferRegistry getRecipeTransferRegistry() {
        return recipeTransferRegistry;
    }

    @Nonnull
    public RecipeRegistry createRecipeRegistry() {
        List<RecipeTransferHandler> recipeTransferHandlers = recipeTransferRegistry.getRecipeTransferHandlers();
        return new RecipeRegistry(recipeCategories, recipeHandlers, recipeTransferHandlers, recipes, recipeClickableAreas);
    }
}
