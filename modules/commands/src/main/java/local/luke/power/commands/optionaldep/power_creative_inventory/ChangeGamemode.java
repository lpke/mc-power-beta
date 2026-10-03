package local.luke.power.commands.optionaldep.power_creative_inventory;

import net.minecraft.entity.player.PlayerEntity;
import local.luke.power.creative.inventory.interfaces.CreativePlayer;

public class ChangeGamemode {
    public static void set(PlayerEntity player, boolean creative) {
        ((CreativePlayer) (player)).creative_setCreative(creative);
    }
}
