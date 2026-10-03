package local.luke.power.recipes.plugins.vanilla.crafting;

import com.mojang.datafixers.util.Either;
import local.luke.power.recipes.api.gui.AMIDrawable;
import local.luke.power.recipes.api.gui.CraftingGridHelper;
import local.luke.power.recipes.api.gui.GuiItemStackGroup;
import local.luke.power.recipes.api.gui.RecipeLayout;
import local.luke.power.recipes.api.recipe.RecipeCategory;
import local.luke.power.recipes.api.recipe.RecipeWrapper;
import local.luke.power.recipes.api.recipe.VanillaRecipeCategoryUid;
import local.luke.power.recipes.api.recipe.wrapper.CraftingRecipeWrapper;
import local.luke.power.recipes.api.recipe.wrapper.ShapedCraftingRecipeWrapper;
import local.luke.power.recipes.config.AMIConfig;
import local.luke.power.recipes.gui.DrawableHelper;
import local.luke.power.recipes.gui.Tooltip;
import local.luke.power.recipes.util.RecipeBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.tag.TagKey;
import net.modificationstation.stationapi.api.util.Formatting;
import net.modificationstation.stationapi.impl.recipe.StationShapedRecipe;

import javax.annotation.Nonnull;

public class CraftingRecipeCategory implements RecipeCategory {

    private static final int craftOutputSlot = 0;
    private static final int craftInputSlot1 = 1;

    @Nonnull
    private final AMIDrawable background;
    @Nonnull
    private final String localizedName;
    @Nonnull
    private final CraftingGridHelper craftingGridHelper;

    public CraftingRecipeCategory() {
        background = DrawableHelper.createDrawable("/gui/crafting.png", 29, 16, 116, 54);
        localizedName = TranslationStorage.getInstance().get("gui.power_recipes.category.craftingTable");
        craftingGridHelper = new local.luke.power.recipes.recipe.CraftingGridHelper(craftInputSlot1, craftOutputSlot);
    }

    @Override
    @Nonnull
    public String getUid() {
        return VanillaRecipeCategoryUid.CRAFTING;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return localizedName;
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

    @Override
    public void setRecipe(@Nonnull RecipeLayout recipeLayout, @Nonnull RecipeWrapper recipeWrapper) {
        GuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        guiItemStacks.init(craftOutputSlot, false, 94, 18);

        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                int index = craftInputSlot1 + x + (y * 3);
                guiItemStacks.init(index, true, x * 18, y * 18);
            }
        }

        if (recipeWrapper instanceof ShapedCraftingRecipeWrapper) {
            ShapedCraftingRecipeWrapper wrapper = (ShapedCraftingRecipeWrapper) recipeWrapper;
            craftingGridHelper.setInput(guiItemStacks, wrapper.getInputs(), wrapper.getWidth(), wrapper.getHeight());
            craftingGridHelper.setOutput(guiItemStacks, wrapper.getOutputs());
        } else if (recipeWrapper instanceof CraftingRecipeWrapper) {
            CraftingRecipeWrapper wrapper = (CraftingRecipeWrapper) recipeWrapper;
            craftingGridHelper.setInput(guiItemStacks, wrapper.getInputs());
            craftingGridHelper.setOutput(guiItemStacks, wrapper.getOutputs());
        } else {
            RecipeBrowser.LOGGER.error("RecipeWrapper is not a known crafting wrapper type: {}", recipeWrapper);
        }

        guiItemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (!AMIConfig.isDebugModeEnabled()) {
                return;
            }
            if (!input) {
                return;
            }
            if (recipeWrapper instanceof ShapelessOreRecipeWrapper shapelessOreRecipeWrapper) {
                Either<TagKey<Item>, ItemStack> ing = shapelessOreRecipeWrapper.getRecipe().getIngredients()[slotIndex - 1];
                ing.mapLeft(e -> {
                    tooltip.add(Tooltip.Divider.INSTANCE);
                    tooltip.add(Formatting.GRAY + "Takes any " + e.id());
                    return e;
                });
            }
            else if (recipeWrapper instanceof ShapedOreRecipeWrapper shapedOreRecipeWrapper) {
                // I. Hate. This.
                StationShapedRecipe recipe = shapedOreRecipeWrapper.getRecipe();
                int width = recipe.width;
                int height = recipe.height;

                int offsetX = (3 - width) / 2;
                int offsetY;

                if (recipe.height == 1 && recipe.width == 1) {
                    offsetY = 1;
                    offsetX = 1;
                }
                else if (recipe.width == 1) {
                    offsetY = 3 / 2;
                }
                else {
                    offsetY = (3 - height) / 2;
                }

                int x = (slotIndex - 1) % 3 - offsetX;
                int y = (slotIndex - 1) / 3 - offsetY;
                int inputIndex = y * width + x;
                Either<TagKey<Item>, ItemStack> ing = shapedOreRecipeWrapper.getRecipe().getGrid()[inputIndex];
                ing.mapLeft(e -> {
                    tooltip.add(Tooltip.Divider.INSTANCE);
                    tooltip.add(Formatting.GRAY + "Takes any " + e.id());
                    return e;
                });
            }
        });
    }

}
