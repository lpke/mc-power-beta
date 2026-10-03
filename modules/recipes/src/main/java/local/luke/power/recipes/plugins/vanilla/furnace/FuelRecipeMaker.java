package local.luke.power.recipes.plugins.vanilla.furnace;

import local.luke.power.recipes.api.AMIHelpers;
import local.luke.power.recipes.api.ItemRegistry;
import local.luke.power.recipes.api.recipe.StackHelper;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.recipe.FuelRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FuelRecipeMaker {

    @Nonnull
    public static List<FuelRecipe> getFuelRecipes(ItemRegistry itemRegistry, AMIHelpers helpers) {
        StackHelper stackHelper = helpers.getStackHelper();
        List<ItemStack> fuelStacks = itemRegistry.getFuels();
        List<FuelRecipe> fuelRecipes = new ArrayList<>();
        for (ItemStack fuelStack : fuelStacks) {
            if (fuelStack == null) {
                continue;
            }

            List<ItemStack> fuels = stackHelper.getSubtypes(fuelStack);
            removeNoBurnTime(fuels);
            if (fuels.isEmpty()) {
                continue;
            }
            int burnTime = getBurnTime(fuels.get(0));
            fuelRecipes.add(new FuelRecipe(fuels, burnTime));
        }
        return fuelRecipes;
    }

    private static void removeNoBurnTime(Collection<ItemStack> itemStacks) {
        itemStacks.removeIf(itemStack -> getBurnTime(itemStack) == 0);
    }

    private static int getBurnTime(ItemStack itemStack) {
        return FuelRegistry.getFuelTime(itemStack);
    }
}
