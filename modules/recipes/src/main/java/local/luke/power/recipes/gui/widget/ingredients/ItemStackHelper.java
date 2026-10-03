package local.luke.power.recipes.gui.widget.ingredients;

import local.luke.power.recipes.recipe.Focus;
import local.luke.power.recipes.util.RecipeBrowser;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collection;

public class ItemStackHelper implements IIngredientHelper<ItemStack> {
    @Override
    public Collection<ItemStack> expandSubtypes(Collection<ItemStack> contained) {
        return RecipeBrowser.getStackHelper().getAllSubtypes(contained);
    }

    @Override
    public ItemStack getMatch(Iterable<ItemStack> contained, @Nonnull Focus toMatch) {
        return RecipeBrowser.getStackHelper().containsStack(contained, toMatch.getStack());
    }
}
