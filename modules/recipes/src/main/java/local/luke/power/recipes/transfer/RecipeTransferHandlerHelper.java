package local.luke.power.recipes.transfer;

import local.luke.power.recipes.api.recipe.transfer.RecipeTransferError;
import local.luke.power.recipes.util.RecipeBrowser;

import javax.annotation.Nullable;
import java.util.Collection;

public class RecipeTransferHandlerHelper implements local.luke.power.recipes.api.recipe.transfer.RecipeTransferHandlerHelper {
    @Override
    public RecipeTransferError createInternalError() {
        RecipeBrowser.LOGGER.warn("Internal error created", new Throwable());
        return RecipeTransferErrorInternal.instance;
    }

    @Override
    public RecipeTransferError createUserErrorWithTooltip(@Nullable String tooltipMessage) {
        if (tooltipMessage == null) {
            RecipeBrowser.LOGGER.error("Null tooltipMessage", new NullPointerException());
            return RecipeTransferErrorInternal.instance;
        }
        return new RecipeTransferErrorTooltip(tooltipMessage);
    }

    @Override
    public RecipeTransferError createUserErrorForSlots(@Nullable String tooltipMessage, @Nullable Collection<Integer> missingItemSlots) {
        if (tooltipMessage == null) {
            RecipeBrowser.LOGGER.error("Null tooltipMessage", new NullPointerException());
            return RecipeTransferErrorInternal.instance;
        }
        if (missingItemSlots == null) {
            RecipeBrowser.LOGGER.error("Null missingItemSlots", new NullPointerException());
            return RecipeTransferErrorInternal.instance;
        }
        if (missingItemSlots.isEmpty()) {
            RecipeBrowser.LOGGER.error("Empty missingItemSlots", new IllegalArgumentException());
            return RecipeTransferErrorInternal.instance;
        }

        return new RecipeTransferErrorSlots(tooltipMessage, missingItemSlots);
    }
}
