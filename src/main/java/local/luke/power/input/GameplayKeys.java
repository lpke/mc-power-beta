package local.luke.power.input;

import local.luke.power.visual.VisualConfig;
import local.luke.power.mixin.ChatTextAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.ChatScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.keyboard.KeyStateChangedEvent;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import org.lwjgl.input.Keyboard;

public final class GameplayKeys {
  public static final KeyBinding PREVIEW = new KeyBinding("powerbeta.containerPreview", 0);
  @EventListener public void register(KeyBindingRegisterEvent event) { event.keyBindings.add(PREVIEW); }
  @EventListener public void key(KeyStateChangedEvent event) {
    if (event.environment != KeyStateChangedEvent.Environment.IN_GAME || !Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (VisualConfig.current().slashChat && Keyboard.getEventKey() == Keyboard.KEY_SLASH && Bindings.heldModifiers() == 0
        && mc.currentScreen == null && mc.player != null) {
      ChatScreen chat = new ChatScreen();
      mc.setScreen(chat);
      ((ChatTextAccessor)chat).power$text("/");
    }
  }
}
