package local.luke.power.inventory.tools;

import local.luke.power.input.*;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.client.Minecraft;

/** Shared physical-player eligibility; settings menus never run automatic inventory actions. */
public final class InventoryAutomation {
  private InventoryAutomation() {}
  public static void registerIndicators(Minecraft mc) {
    TweakIndicators.register(TweakIndicators.Tweak.RESTOCK,
        () -> enabledForPlayer(mc) && InventoryToolsConfig.current().restock ? "" : null);
    TweakIndicators.register(TweakIndicators.Tweak.TOOL_SWITCH,
        () -> enabledForPlayer(mc) && InventoryToolsConfig.current().toolSwitch ? "" : null);
  }
  private static boolean enabledForPlayer(Minecraft mc) {
    return mc.player != null && mc.world != null && !mc.world.isRemote
        && !(mc.player instanceof PlayerMode mode && (mode.power$creative() || mode.power$spectator()));
  }
  public static boolean playable(Minecraft mc) {
    return mc != null && mc.player != null && mc.world != null && !mc.world.isRemote
        && mc.player.world == mc.world && !mc.player.dead && mc.player.health > 0
        && mc.currentScreen == null && !mc.paused && mc.field_2778 && mc.field_2807 == mc.player
        && org.lwjgl.opengl.Display.isActive() && !DetachedCamera.isActive()
        && !InteractionState.carryingContainer && WorldCompatibility.ready(mc.world)
        && mc.player.container == mc.player.playerContainer && mc.player.inventory.getCursorStack() == null
        && !(mc.player instanceof PlayerMode mode && mode.power$spectator());
  }
  public static boolean survival(Minecraft mc) {
    return playable(mc) && !(mc.player instanceof PlayerMode mode && mode.power$creative());
  }
  public static void toggleRestock(Minecraft mc) {
    if (!survival(mc)) return;
    var next = InventoryToolsConfig.copy(); next.restock = !next.restock;
    InventoryToolsConfig.preview(next);
    InventoryToolsConfig.queuedPreferences(next);
    Restock.clear();
  }
  public static void toggleToolSwitch(Minecraft mc) {
    if (!survival(mc)) return;
    var next = InventoryToolsConfig.copy(); next.toolSwitch = !next.toolSwitch;
    InventoryToolsConfig.preview(next);
    InventoryToolsConfig.queuedPreferences(next);
    AutoToolSwitch.clear();
  }
  public static void tick(Minecraft mc) { Restock.tick(mc); }
  public static void clear() { Restock.clear(); AutoToolSwitch.clear(); }
}
