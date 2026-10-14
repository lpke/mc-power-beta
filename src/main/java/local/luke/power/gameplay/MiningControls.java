package local.luke.power.gameplay;

import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.*;
import org.lwjgl.opengl.Display;

public final class MiningControls {
  public static final KeyBinding CLICK_MINING = new KeyBinding("power_beta.clickMining", 0);
  private static boolean registered;
  private MiningControls() {}
  public static void register(GameOptions options) {
    if (!java.util.Arrays.asList(options.allKeys).contains(CLICK_MINING)) {
      options.allKeys = java.util.Arrays.copyOf(options.allKeys, options.allKeys.length + 1);
      options.allKeys[options.allKeys.length - 1] = CLICK_MINING;
    }
    if (registered) return;
    registered = true;
    Bindings.onMousePress("power_beta.mining", MiningControls::press);
    TweakLifecycle.register("click-mining", ClickMining::reset);

  }
  public static void press(int key) {
    var mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if (mc.world == null || mc.world.isRemote || mc.player == null || mc.currentScreen != null
        || mc.paused || mc.player.dead || mc.player.health <= 0 || !Display.isActive()) return;
    if (Bindings.matches(CLICK_MINING, key)) ClickMining.toggle();
  }
}
