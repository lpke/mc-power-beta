package local.luke.power.commands.command.extra;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;


public class Hat implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {
        PlayerEntity player = commandSource.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack hand = player.inventory.getSelectedItem() == null ? null : player.inventory.getSelectedItem().copy();
        ItemStack hat = player.inventory.armor[3] == null ? null : player.inventory.armor[3].copy();

        player.inventory.armor[3] = hand;
        player.inventory.main[player.inventory.selectedSlot] = hat;

        commandSource.sendFeedback("Swapped hat with hand");
    }

    @Override
    public String name() {
        return "hat";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /hat");
        commandSource.sendFeedback("Info: Puts the item in your hand on your head");
    }

    @Override
    public boolean needsPermissions() {
        return false;
    }
}
