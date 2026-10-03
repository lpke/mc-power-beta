package local.luke.power.recipes.plugins.ami.description;

import local.luke.power.recipes.api.gui.AMIDrawable;
import local.luke.power.recipes.api.gui.GuiItemStackGroup;
import local.luke.power.recipes.api.gui.RecipeLayout;
import local.luke.power.recipes.api.recipe.RecipeCategory;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.api.recipe.VanillaRecipeCategoryUid;
import local.luke.power.recipes.gui.DrawableHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.language.TranslationStorage;

import javax.annotation.Nonnull;

public class ItemDescriptionRecipeCategory implements RecipeCategory {
    public static final int recipeWidth = 160;
    public static final int recipeHeight = 125;
    @Nonnull
    private final AMIDrawable background;
    @Nonnull
    private final String localizedName;

    public ItemDescriptionRecipeCategory() {
        background = DrawableHelper.createBlankDrawable(recipeWidth, recipeHeight);
        localizedName = TranslationStorage.getInstance().get("gui.power_recipes.category.itemDescription");
    }

    @Nonnull
    @Override
    public String getUid() {
        return VanillaRecipeCategoryUid.DESCRIPTION;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return localizedName;
    }

    @Nonnull
    @Override
    public AMIDrawable getBackground() {
        return background;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {

    }

    @Override
    public void drawAnimations(Minecraft minecraft) {

    }

    @Override
    public void setRecipe(@Nonnull RecipeLayout recipeLayout, @Nonnull RecipeWrapper recipeWrapper) {
        GuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        int xPos = (recipeWidth - 18) / 2;
        guiItemStacks.init(0, false, xPos, 0);
        guiItemStacks.setFromRecipe(0, recipeWrapper.getOutputs());
    }
}
