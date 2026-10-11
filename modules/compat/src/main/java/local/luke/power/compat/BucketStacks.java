package local.luke.power.compat;

import local.luke.power.creative.inventory.interfaces.CreativePlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Exchange one empty bucket without discarding the remaining stack. */
public final class BucketStacks {
    private BucketStacks() {}

    public static ItemStack fill(ItemStack input, PlayerEntity player, ItemStack filled) {
        if (input == null || input.itemId != Item.BUCKET.id || input.count <= 0
                || (filled.itemId != Item.WATER_BUCKET.id && filled.itemId != Item.LAVA_BUCKET.id
                && filled.itemId != Item.MILK_BUCKET.id)) return filled;
        // Stacks saved while enabled must remain usable after disabling the option.
        if (!Config.config.STACK_SIZE_CONFIG.stackableEmptyBuckets && input.count == 1) return filled;
        if (player instanceof CreativePlayer creative && creative.creative_isCreative()) {
            for (ItemStack item : player.inventory.main) {
                if (item != null && item.itemId == filled.itemId) return input;
            }
        } else {
            if (input.count == 1) return filled;
            --input.count;
        }
        if (!player.inventory.addStack(filled) && !player.world.isRemote) player.dropItem(filled);
        return input;
    }
}
