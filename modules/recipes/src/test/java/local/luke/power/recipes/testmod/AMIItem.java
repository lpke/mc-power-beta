package local.luke.power.recipes.testmod;

import local.luke.power.recipes.api.AMITooltipModifier;
import local.luke.power.recipes.api.ItemRarityProvider;
import local.luke.power.recipes.api.SubItemProvider;
import local.luke.power.recipes.gui.AMITextRenderer;
import local.luke.power.recipes.api.Rarity;
import local.luke.power.recipes.gui.Tooltip;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.template.item.TemplateItem;
import net.modificationstation.stationapi.api.util.Formatting;
import net.modificationstation.stationapi.api.util.Identifier;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AMIItem extends TemplateItem {
    private static final Tooltip.Image SQUARE = new Tooltip.Image("/assets/power_recipestest/square.png");
    private static final Tooltip.Image CIRCLE = new Tooltip.Image("/assets/power_recipestest/circle.png");
    private static final Tooltip.Image RED_CIRCLE = new Tooltip.Image("/assets/power_recipestest/circle.png", Tooltip.Alignment.TOP_LEFT, Color.RED);

    public AMIItem(Identifier identifier) {
        super(identifier);
        setTranslationKey(TestMod.NAMESPACE, "amitestitem");
        setHasSubtypes(true);
    }

    @AMITooltipModifier
    public void getAMITooltip(ItemStack stack, List<Object> originalTooltip) {

        originalTooltip.add(1, Formatting.GRAY + AMITextRenderer.UNDERLINE + "Index " + stack.getDamage());
        originalTooltip.add(2, Tooltip.Line.create(SQUARE, Tooltip.VerticalDivider.INSTANCE, CIRCLE, Tooltip.VerticalDivider.INSTANCE, RED_CIRCLE));
    }

    @SubItemProvider
    public List<ItemStack> getSubItems() {
        ArrayList<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(new ItemStack(this, 1, i));
        }
        return list;
    }

    @ItemRarityProvider
    public Rarity getRarity(ItemStack itemStack) {
        return new ArrayList<>(Rarity.AMI_RARITIES_BY_CODE.values()).get(itemStack.getDamage() % (Rarity.AMI_RARITIES_BY_CODE.size()));
    }
}
