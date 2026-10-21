package local.luke.power.commands.util;

import local.luke.power.commands.ClientCommands;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.Arrays;

public class ParameterSuggestUtil {
    public static String[] suggestItemIdentifier(String currentInput)
    {
        if (!FabricLoader.getInstance().isModLoaded("station-registry-api-v0"))
            return new String[0];

        return ItemArgument.suggestNames(currentInput);
    }

    public static String[] suggestPlayerName(String currentInput) {
        return suggestPlayerName(currentInput, "");
    }

    public static String[] suggestPlayerName(String currentInput, String remove) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            if (ClientCommands.player_names != null) {
                // Create a new ArrayList based on the array of player names to allow modification
                ArrayList<String> playerNames = new ArrayList<>(Arrays.asList(ClientCommands.player_names));

                // Remove the specified name if it exists
                if (!remove.isEmpty()) {
                    playerNames.removeIf(name -> name.equals(remove));
                }

                // Filter and return names that start with the current input
                return playerNames.stream()
                        .filter(a -> local.luke.power.chat.ChatCompletion.startsWith(a, currentInput))
                        .map(b -> b.substring(currentInput.length()))
                        .toArray(String[]::new);
            }
        }

        // Return an empty array if the client is not in the correct environment or player names are null
        return new String[0];
    }

}
