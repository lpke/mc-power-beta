package local.luke.power.recipes.plugins.vanilla.furnace;

import com.mojang.datafixers.util.Either;
import local.luke.power.recipes.api.gui.AMIDrawable;
import local.luke.power.recipes.api.gui.GuiItemStackGroup;
import local.luke.power.recipes.api.gui.RecipeLayout;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.api.recipe.VanillaRecipeCategoryUid;
import local.luke.power.recipes.gui.DrawableHelper;
import local.luke.power.recipes.gui.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.tag.TagKey;
import net.modificationstation.stationapi.api.util.Formatting;

import javax.annotation.Nonnull;

public class FurnaceSmeltingCategory extends FurnaceRecipeCategory {
    @Nonnull
    private final AMIDrawable background;
    @Nonnull
    private final String localizedName;

    public FurnaceSmeltingCategory() {
        super();
        background = DrawableHelper.createDrawable("/gui/furnace.png", 55, 16, 82, 54);
        localizedName = TranslationStorage.getInstance().get("gui.power_recipes.category.smelting");
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
        flame.draw(minecraft, 1, 20);
        arrow.draw(minecraft, 24, 18);
    }

    @Nonnull
    @Override
    public String getTitle() {
        return localizedName;
    }

    @Nonnull
    @Override
    public String getUid() {
        return VanillaRecipeCategoryUid.SMELTING;
    }

    @Override
    public void setRecipe(@Nonnull RecipeLayout recipeLayout, @Nonnull RecipeWrapper recipeWrapper) {
        GuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        guiItemStacks.init(inputSlot, true, 0, 0);
        guiItemStacks.init(outputSlot, false, 60, 18);

        guiItemStacks.setFromRecipe(inputSlot, recipeWrapper.getInputs());
        guiItemStacks.setFromRecipe(outputSlot, recipeWrapper.getOutputs());

        guiItemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (!input) {
                return;
            }
            if (recipeWrapper instanceof SmeltingRecipe smeltingRecipe) {
                Either<TagKey<Item>, ItemStack> ing = smeltingRecipe.getInputs().get(0).get(0);
                ing.mapLeft(tagKey -> {
                    tooltip.add(Tooltip.Divider.INSTANCE);
                    tooltip.add(Formatting.GRAY + "Takes any " + tagKey.id());
                    return tagKey;
                });
            }
        });
    }
}
