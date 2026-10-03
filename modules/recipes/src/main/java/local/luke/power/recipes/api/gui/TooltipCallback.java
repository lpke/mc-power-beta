package local.luke.power.recipes.api.gui;

import java.util.ArrayList;

public interface TooltipCallback<T> {
    /**
     * Change the tooltip for an ingredient.
     */
    void onTooltip(int slotIndex, boolean input, T ingredient, ArrayList<Object> tooltip);
}
