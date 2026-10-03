package local.luke.power.commands.command.extra;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.entity.player.PlayerEntity;


public class Heal implements Command {
    @Override
    public void command(SharedCommandSource commandSource, String[] parameters) {

        PlayerEntity player = commandSource.getPlayer();
        if (player == null) {
            return;
        }

        if (parameters.length > 1) {
            int amount = Integer.parseInt(parameters[1]);
            commandSource.sendFeedback((amount > 0 ? "Healed" : "Damaged") + " " + Math.abs(amount) / 2.f + " hearts!");
            player.health += amount;
            return;
        }

        commandSource.sendFeedback("Healed fully!");

        player.health = 20;

    }

    @Override
    public String name() {
        return "heal";
    }

    @Override
    public void manual(SharedCommandSource commandSource) {
        commandSource.sendFeedback("Usage: /heal {optional: amount}");
        commandSource.sendFeedback("Info: Restores health");
    }
}
