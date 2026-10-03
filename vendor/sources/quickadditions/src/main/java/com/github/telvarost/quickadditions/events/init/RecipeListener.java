package com.github.telvarost.quickadditions.events.init;

import com.github.telvarost.quickadditions.Config;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.event.recipe.RecipeRegisterEvent;
import net.modificationstation.stationapi.api.recipe.CraftingRegistry;
import net.modificationstation.stationapi.api.util.Identifier;

public class RecipeListener {

    @EventListener
    public void registerRecipes(RecipeRegisterEvent event) {
        Identifier type = event.recipeId;

        if (type == RecipeRegisterEvent.Vanilla.CRAFTING_SHAPED.type()) {
            if (Config.config.ENABLE_SOUL_SAND_RECIPE) {
                CraftingRegistry.addShapedRecipe(new ItemStack(Block.SOUL_SAND.asItem(), Config.config.SOUL_SAND_OUTPUT), "XY", "YX", 'X', Block.NETHERRACK, 'Y', Block.DIRT);
            }
        }
    }
}