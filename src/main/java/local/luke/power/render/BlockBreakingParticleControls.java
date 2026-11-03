package local.luke.power.render;

import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.*;
import org.lwjgl.opengl.Display;

public final class BlockBreakingParticleControls {
  public static final KeyBinding TOGGLE = new KeyBinding("power_beta.blockBreakingParticles", 0);
  private static boolean registered;
  private BlockBreakingParticleControls() {}

  public static void register(GameOptions options) {
    if (!java.util.Arrays.asList(options.allKeys).contains(TOGGLE)) {
      options.allKeys = java.util.Arrays.copyOf(options.allKeys, options.allKeys.length + 1);
      options.allKeys[options.allKeys.length - 1] = TOGGLE;
    }
    if (registered) return;
    registered = true;
    Bindings.onMousePress("power_beta.block-breaking-particles", BlockBreakingParticleControls::press);
    TweakLifecycle.register("block-breaking-particles", BlockBreakingParticles::reset);
    TweakIndicators.register(TweakIndicators.Tweak.BLOCK_BREAKING_PARTICLES,
        () -> BlockBreakingParticles.inverted() ? BlockBreakingParticles.enabled() ? "enabled" : "disabled" : null);
  }

  public static void press(int key) {
    var mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if (mc.world == null || mc.player == null || mc.currentScreen != null || mc.paused
        || mc.player.dead || mc.player.health <= 0 || !Display.isActive()) return;
    if (Bindings.matches(TOGGLE, key)) BlockBreakingParticles.toggle();
  }
}
