package local.luke.power.recipes.transfer;

import local.luke.power.recipes.api.recipe.transfer.RecipeTransferError;
import local.luke.power.recipes.gui.RecipeLayout;
import local.luke.power.recipes.gui.Tooltip;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class RecipeTransferErrorTooltip implements RecipeTransferError {
    @Nonnull
    private final String message;

    public RecipeTransferErrorTooltip(@Nonnull String message) {
        this.message = message;
    }

    @Override
    public Type getType() {
        return Type.PLAYER;
    }

    @Override
    public void showError(@Nonnull Minecraft minecraft, int mouseX, int mouseY, int containerX, int containerY, @Nonnull RecipeLayout recipeLayout) {
        Tooltip.INSTANCE.setTooltip(new ArrayList<>(){{add(message);}}, mouseX, mouseY);
    }
}
