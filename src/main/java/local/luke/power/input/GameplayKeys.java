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
  public static final KeyBinding LIGHT = new KeyBinding("powerbeta.lightOverlay", Keyboard.KEY_F7);
  @EventListener public void register(KeyBindingRegisterEvent event) {
    event.keyBindings.add(PREVIEW);
    event.keyBindings.add(LIGHT);
    Bindings.onMousePress("powerbeta.lightOverlay", GameplayKeys::toggleLight);
  }

  private static void toggleLight(int key) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.player == null || mc.world == null || mc.currentScreen != null || !Bindings.matches(LIGHT, key)) return;
    try { local.luke.power.light.LightConfig.toggle(); }
    catch (Exception e) { local.luke.power.PowerBeta.LOG.error("Could not save light overlay toggle", e); }
  }
  @EventListener public void key(KeyStateChangedEvent event) {
    if (event.environment != KeyStateChangedEvent.Environment.IN_GAME || !Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
    toggleLight(Keyboard.getEventKey());
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (VisualConfig.current().slashChat && Keyboard.getEventKey() == Keyboard.KEY_SLASH && Bindings.heldModifiers() == 0
        && mc.currentScreen == null && mc.player != null) {
      ChatScreen chat = new ChatScreen();
      mc.setScreen(chat);
      ((ChatTextAccessor)chat).power$text("/");
      try {
        Class.forName("local.luke.power.commands.integration.chat.ChatWidgetAccess")
            .getMethod("setText", String.class).invoke(null, "/");
      } catch (ReflectiveOperationException e) {
        local.luke.power.PowerBeta.LOG.warn("Could not seed the chat input", e);
      }
    }
  }
}
