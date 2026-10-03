package local.luke.power.recipes.plugins.vanilla.furnace;

import local.luke.power.recipes.api.gui.AnimatedDrawable;
import local.luke.power.recipes.api.gui.StaticDrawable;
import local.luke.power.recipes.api.recipe.RecipeCategory;
import local.luke.power.recipes.gui.DrawableHelper;

import javax.annotation.Nonnull;

public abstract class FurnaceRecipeCategory implements RecipeCategory {
    protected static final int inputSlot = 0;
    protected static final int fuelSlot = 1;
    protected static final int outputSlot = 2;

    @Nonnull
    protected final AnimatedDrawable flame;
    @Nonnull
    protected final AnimatedDrawable arrow;

    public FurnaceRecipeCategory() {

        StaticDrawable flameDrawable = DrawableHelper.createDrawable("/gui/furnace.png", 176, 0, 14, 14);
        flame = DrawableHelper.createAnimatedDrawable(flameDrawable, 300, AnimatedDrawable.StartDirection.TOP, true);

        StaticDrawable arrowDrawable = DrawableHelper.createDrawable("/gui/furnace.png", 176, 14, 24, 17);
        this.arrow = DrawableHelper.createAnimatedDrawable(arrowDrawable, 200, AnimatedDrawable.StartDirection.LEFT, false);
    }
}
