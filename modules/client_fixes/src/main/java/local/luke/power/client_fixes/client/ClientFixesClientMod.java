package local.luke.power.client_fixes.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

public class ClientFixesClientMod implements ClientModInitializer {
    public static boolean cancelSetScreenNull = false;

    @Override
    public void onInitializeClient() {
    }
}
