package local.luke.power.client_fixes.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

public class ClientFixesClientMod implements ClientModInitializer {
    public final static KeyBinding COMMAND_KEYBIND = new KeyBinding("Command", Keyboard.KEY_SLASH);
    public final static KeyBinding DEBUG_GRAPH_KEYBIND = new KeyBinding("Debug Graph", Keyboard.KEY_LCONTROL);
    public static boolean cancelSetScreenNull = false;

    @Override
    public void onInitializeClient() {
    }
}
