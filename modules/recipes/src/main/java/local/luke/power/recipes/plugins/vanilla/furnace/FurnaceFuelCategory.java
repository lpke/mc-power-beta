package local.luke.power.recipes.plugins.vanilla.furnace;

import local.luke.power.recipes.api.gui.AMIDrawable;
import local.luke.power.recipes.api.gui.GuiItemStackGroup;
import local.luke.power.recipes.api.gui.RecipeLayout;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.api.recipe.VanillaRecipeCategoryUid;
import local.luke.power.recipes.gui.DrawableHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.language.TranslationStorage;

import javax.annotation.Nonnull;

public class FurnaceFuelCategory extends FurnaceRecipeCategory {
    @Nonnull
    private final AMIDrawable background;
    @Nonnull
    private final String localizedName;

    public FurnaceFuelCategory() {
        super();
        background = DrawableHelper.createDrawable("/gui/furnace.png", 55, 38, 18, 32, 0, 0, 0, 80);
        localizedName = TranslationStorage.getInstance().get("gui.power_recipes.category.fuel");
    }

    @Override
    @Nonnull
    public AMIDrawable getBackground() {
        return background;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {

    }

    @Override
    public void drawAnimations(Minecraft minecraft) {

    }

    @Nonnull
    @Override
    public String getUid() {
        return VanillaRecipeCategoryUid.FUEL;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return localizedName;
    }

    @Override
    public void setRecipe(@Nonnull RecipeLayout recipeLayout, @Nonnull RecipeWrapper recipeWrapper) {
        GuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        guiItemStacks.init(fuelSlot, true, 0, 14);
        guiItemStacks.setFromRecipe(fuelSlot, recipeWrapper.getInputs());
    }
}
