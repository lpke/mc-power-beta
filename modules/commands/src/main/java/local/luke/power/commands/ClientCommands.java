package local.luke.power.commands;

import local.luke.power.commands.util.RetroChatUtil;
import local.luke.power.commands.util.VanillaMobs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class ClientCommands implements ModInitializer {
    public static final String MOD_ID = "power_commands";

    // other mods located
    public static boolean chatWidgetsAvailable = false;
    public static boolean creativeInventoryAvailable = false;

    // Multiplayer
    public static boolean mp_rc = false;
    public static boolean mp_op = false;

    // String arrays for completions help
    public static String[] player_names = null;
    public static List<String> disabled_commands = List.of(new String[]{});

    @Override
    public void onInitialize() {
        chatWidgetsAvailable = FabricLoader.getInstance().isModLoaded("power_client_fixes");
        creativeInventoryAvailable = FabricLoader.getInstance().isModLoaded("power_creative_inventory");

        RetroChatUtil.addDefaultCommands();
        VanillaMobs.setupSummons();
    }
}
