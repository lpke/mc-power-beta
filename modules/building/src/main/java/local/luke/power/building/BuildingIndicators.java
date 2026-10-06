package local.luke.power.building;

import java.util.Locale;
import java.util.function.Supplier;
import local.luke.power.autowalk.AutoWalk;
import local.luke.power.building.camera.FreeLook;
import local.luke.power.building.config.Config;
import local.luke.power.input.TweakIndicators;
import static local.luke.power.input.TweakIndicators.Tweak.*;
import net.minecraft.client.Minecraft;

public final class BuildingIndicators {
  private static Minecraft client;
  private BuildingIndicators() {}
  public static void register(Minecraft mc) {
    if (client == mc) return;
    client = mc;
    add(FAKE_SNEAK, () -> Config.current().sneak.enabled ? "" : null);
    add(FAST_PLACEMENT, () -> Config.current().placement.enabled ? "" : null);
    add(PLACEMENT_RESTRICTION, () -> {
      var s = Config.current().placement;
      if (!s.enabled || !s.restrictionEnabled) return null;
      String rule = s.restrictionMode.name().toLowerCase(Locale.ROOT);
      return Character.toUpperCase(rule.charAt(0)) + rule.substring(1);
    });
    add(AUTO_WALK, () -> AutoWalk.isWalking() ? "" : null);
    add(FREE_LOOK, () -> Config.current().freeLookToggle && FreeLook.active() ? "" : null);
    add(SLAB_COMPLETION, () -> Config.current().slabs.enabled ? "" : null);
  }
  private static void add(TweakIndicators.Tweak tweak, Supplier<String> value) {
    TweakIndicators.register(tweak, () -> client.world != null && !client.world.isRemote
        ? value.get() : null);
  }
}
