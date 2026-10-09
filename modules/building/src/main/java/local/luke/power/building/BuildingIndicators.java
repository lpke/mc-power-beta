package local.luke.power.building;

import java.util.Locale;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
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
    add(FAKE_SNEAK, () -> Config.current().sneak.enabled ? "" : null,
        () -> Config.current().sneak.announceToggle);
    add(FAST_PLACEMENT, () -> Config.current().placement.enabled ? "" : null,
        () -> Config.current().placement.announceToggle);
    add(PLACEMENT_RESTRICTION, () -> {
      var s = Config.current().placement;
      if (!s.enabled || !s.restrictionEnabled) return null;
      String rule = s.restrictionMode.name().toLowerCase(Locale.ROOT);
      return Character.toUpperCase(rule.charAt(0)) + rule.substring(1);
    }, () -> Config.current().placement.announceRestrictionToggle);
    add(AUTO_WALK, () -> AutoWalk.isWalking() ? "" : null,
        () -> Config.current().autoWalkAnnounceToggle);
    TweakIndicators.register(AUTO_MINE, () -> local.luke.power.building.mining.AutoMine.active() ? "" : null,
        () -> Config.current().autoMineAnnounceToggle);
    add(FREE_LOOK, () -> Config.current().freeLookToggle && FreeLook.active() ? "" : null,
        () -> Config.current().freeLookAnnounceToggle);
    add(SLAB_COMPLETION, () -> Config.current().slabs.enabled ? "" : null,
        () -> Config.current().slabs.announceToggle);
  }
  private static void add(TweakIndicators.Tweak tweak, Supplier<String> value, BooleanSupplier announce) {
    TweakIndicators.register(tweak, () -> client.world != null && !client.world.isRemote
        ? value.get() : null, announce);
  }
}
