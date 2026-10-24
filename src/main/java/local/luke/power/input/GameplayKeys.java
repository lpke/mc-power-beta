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
  public static final KeyBinding INFO_HUD = new KeyBinding("powerbeta.infoHud", Keyboard.KEY_F4);
  public static final KeyBinding COMMANDS = new KeyBinding("powerbeta.openCommands", Keyboard.KEY_SLASH);
  public static final KeyBinding GRAPH = new KeyBinding("powerbeta.debugGraph", Keyboard.KEY_F3);
  public static final KeyBinding SPRINT = new KeyBinding("powerbeta.sprint", Keyboard.KEY_LCONTROL);
  public static final KeyBinding SWAP_HANDS = new KeyBinding("powerbeta.swapHands", 0);
  public static final KeyBinding TOOL_SWITCH = new KeyBinding("powerbeta.toolSwitch", 0);
  public static final KeyBinding RESTOCK = new KeyBinding("powerbeta.restock", 0);
  public static final KeyBinding RECIPE_BOOK = new KeyBinding("powerbeta.recipeBook", 0);
  public static final KeyBinding SORT_INVENTORY = new KeyBinding("powerbeta.sortInventory", 0);
  public static final KeyBinding SORTING_LAYOUT = new KeyBinding("powerbeta.sortingLayout", 0);
  public static final KeyBinding DEPOSIT_MATCHING = new KeyBinding("powerbeta.depositMatching", 0);
  public static final KeyBinding SAVE_HOTBAR = new KeyBinding("powerbeta.saveCreativeHotbar", 0);
  public static final KeyBinding LOAD_HOTBAR = new KeyBinding("powerbeta.loadCreativeHotbar", 0);
  private static boolean graph;
  public static boolean graph() { return graph; }
  public static void debugText() { graph = false; }
  @EventListener public void register(KeyBindingRegisterEvent event) {
    event.keyBindings.add(PREVIEW);
    event.keyBindings.add(LIGHT);
    event.keyBindings.add(INFO_HUD);
    event.keyBindings.add(COMMANDS);
    event.keyBindings.add(GRAPH);
    event.keyBindings.add(SPRINT);
    event.keyBindings.add(SWAP_HANDS);
    event.keyBindings.add(RESTOCK);
    event.keyBindings.add(TOOL_SWITCH);
    event.keyBindings.addAll(java.util.List.of(RECIPE_BOOK, SORT_INVENTORY, SORTING_LAYOUT, DEPOSIT_MATCHING, SAVE_HOTBAR, LOAD_HOTBAR));
    event.keyBindings.addAll(local.luke.power.map.MapKeys.all());
    event.keyBindings.addAll(local.luke.power.debug.DebugKeys.all());
    Bindings.onMousePress("powerbeta.mapActions", key -> local.luke.power.map.MapKeys.press((Minecraft)FabricLoader.getInstance().getGameInstance(), key));
    Bindings.onMousePress("powerbeta.lightOverlay", GameplayKeys::toggleLight);
    Bindings.onMousePress("powerbeta.infoHud", GameplayKeys::toggleInfoHud);
    Bindings.onMousePress("powerbeta.interface", GameplayKeys::interfaceKey);
    Bindings.onMousePress("powerbeta.inventory", GameplayKeys::inventoryKey);
  }

  private static void toggleInfoHud(int key) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.world == null || mc.player == null || mc.currentScreen != null || mc.paused
        || mc.player.dead || mc.player.health <= 0 || !Display.isActive() || !Bindings.matches(INFO_HUD, key)) return;
    // The picker owns its modifier gesture, even when its cycle key is also the HUD key.
    if (!mc.world.isRemote && mc.field_2778 && mc.field_2807 == mc.player && !DetachedCamera.isActive()
        && Bindings.down("power_creative.modifier") && Bindings.matches("power_creative.picker", key)) return;
    local.luke.power.hud.InfoHudConfig.toggle();
  }

  private static void toggleLight(int key) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.player == null || mc.world == null || mc.currentScreen != null || !Bindings.matches(LIGHT, key)) return;
    try { local.luke.power.light.LightConfig.toggle(); }
    catch (Exception e) { local.luke.power.PowerBeta.LOG.error("Could not save light overlay toggle", e); }
  }
  @EventListener public void key(KeyStateChangedEvent event) {
    if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
    if (local.luke.power.map.MinimapQuickMenu.ownsKeyEvent()) return;
    Bindings.keyPress(Keyboard.getEventKey());
    if (event.environment != KeyStateChangedEvent.Environment.IN_GAME) return;
    toggleLight(Keyboard.getEventKey());
    toggleInfoHud(Keyboard.getEventKey());
    local.luke.power.map.MapKeys.press((Minecraft)FabricLoader.getInstance().getGameInstance(), Keyboard.getEventKey());
    interfaceKey(Keyboard.getEventKey());
    inventoryKey(Keyboard.getEventKey());
  }

  private static void inventoryKey(int code) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (mc.currentScreen == null && Display.isActive() && Bindings.matches(RESTOCK, code))
      local.luke.power.inventory.tools.InventoryAutomation.toggleRestock(mc);
    if (mc.currentScreen == null && Display.isActive() && Bindings.matches(TOOL_SWITCH, code))
      local.luke.power.inventory.tools.InventoryAutomation.toggleToolSwitch(mc);
    if (mc.currentScreen == null && Display.isActive() && Bindings.matches(SWAP_HANDS, code))
      local.luke.power.offhand.Offhand.swap(mc);
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
