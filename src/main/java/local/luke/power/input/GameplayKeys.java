package local.luke.power.input;

import local.luke.power.mixin.ChatTextAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.ChatScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.keyboard.KeyStateChangedEvent;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public final class GameplayKeys {
  public static final KeyBinding PREVIEW = new KeyBinding("powerbeta.containerPreview", 0);
  public static final KeyBinding LIGHT = new KeyBinding("powerbeta.lightOverlay", Keyboard.KEY_F7);
  public static final KeyBinding INFO_HUD = new KeyBinding("powerbeta.infoHud", Keyboard.KEY_NONE);
  public static final KeyBinding COMMANDS = new KeyBinding("powerbeta.openCommands", Keyboard.KEY_SLASH);
  public static final KeyBinding GRAPH = new KeyBinding("powerbeta.debugGraph", Keyboard.KEY_F3);
  private static boolean graph;
  public static boolean graph() { return graph; }
  public static void debugText() { graph = false; }
  @EventListener public void register(KeyBindingRegisterEvent event) {
    event.keyBindings.add(PREVIEW);
    event.keyBindings.add(LIGHT);
    event.keyBindings.add(INFO_HUD);
    event.keyBindings.add(COMMANDS);
    event.keyBindings.add(GRAPH);
    Bindings.onMousePress("powerbeta.lightOverlay", GameplayKeys::toggleLight);
    Bindings.onMousePress("powerbeta.infoHud", GameplayKeys::toggleInfoHud);
    Bindings.onMousePress("powerbeta.interface", GameplayKeys::interfaceKey);
  }

  private static void toggleInfoHud(int key) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.world == null || mc.player == null || mc.currentScreen != null || mc.paused
        || mc.player.dead || mc.player.health <= 0 || !Display.isActive() || !Bindings.matches(INFO_HUD, key)) return;
    local.luke.power.hud.InfoHudConfig.toggle();
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
    toggleInfoHud(Keyboard.getEventKey());
    interfaceKey(Keyboard.getEventKey());
  }

  private static void interfaceKey(int code) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.world == null || mc.player == null || mc.currentScreen != null || !Display.isActive()) return;
    if (Bindings.matches(GRAPH, code)) {
      graph = !mc.options.debugHud || !graph;
      mc.options.debugHud = true;
    } else {
      for (KeyBinding binding : mc.options.allKeys)
        if (binding.translationKey.equals("key.power_controls.debug_hud") && Bindings.matches(binding, code)) debugText();
    }
    if (Bindings.matches(COMMANDS, code)) {
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
