package local.luke.power.client_fixes.mixinterface;

import net.minecraft.client.gui.screen.ChatScreen;

public interface ChatScreenAccessor {
    ChatScreen setInitialMessage(String initialMessage);
}
