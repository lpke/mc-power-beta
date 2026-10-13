package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.Arrays;
import local.luke.power.building.config.Config;
import local.luke.power.building.config.Settings;
import local.luke.power.config.*;
import local.luke.power.flexible.FlexiblePlacement;
import local.luke.power.flexible.Modes;
import local.luke.power.input.TweakLifecycle;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;

/** Real exit hooks, queued saves and settings UI for persistent placement preferences. */
final class PlacementLifecycleChecks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("check")) { checks(mc); return; }
    String[] parts = action.split(" ");
    check(parts.length == 2, "expected exit, stop or saved followed by 0..3");
    int flags = Integer.parseInt(parts[1]);
    check(flags >= 0 && flags <= 3, "expected flags 0..3");
    switch (parts[0]) {
      case "saved" -> {
        saved(flags); Release160Checks.inactive(mc);
        log("PASS saved placement preferences " + flags + " with inactive temporary tweaks");
      }
      case "exit", "stop" -> {
        Release160Checks.activate(mc);
        // Queue the user's choice immediately before exit; the cleanup must not replace it.
        Config.update(s -> { s.flexible.enabled = (flags & 1) != 0; s.slabs.enabled = (flags & 2) != 0; });
        if (parts[0].equals("stop")) {
          mc.scheduleStop();
          log("PASS requested shutdown with placement preferences " + flags);
        } else {
          mc.setScreen(new TitleScreen()); mc.setWorld(null);
          Release160Checks.inactive(mc); PowerConfig.flushPending(); saved(flags);
          log("PASS world exit preserved placement preferences " + flags);
        }
      }
      default -> throw new IllegalArgumentException("Unknown placement lifecycle action");
    }
  }

  static void saved(int flags) {
    for (Settings s : new Settings[]{Config.current(), PowerConfig.read("building", Settings.class)}) {
      check(s.flexible.enabled == ((flags & 1) != 0) && s.slabs.enabled == ((flags & 2) != 0),
          "placement preferences changed: " + flags);
      check(!s.sneak.enabled && !s.placement.enabled && !s.placement.restrictionEnabled,
          "temporary toggle was not cleared");
    }
  }

  private static void checks(Minecraft mc) throws Exception {
    var before = Config.current().copy();
    try {
      test("cleanup preserves all placement preference combinations and unrelated values", () -> {
        for (int flags = 0; flags < 4; flags++) {
          var s = before.copy(); s.flexible.enabled = (flags & 1) != 0; s.slabs.enabled = (flags & 2) != 0;
          s.sneak.enabled = s.placement.enabled = s.placement.restrictionEnabled = true;
          Config.apply(s);
          var expected = s.copy(); expected.sneak.enabled = expected.placement.enabled = expected.placement.restrictionEnabled = false;
          TweakLifecycle.reset(); PowerConfig.flushPending(); saved(flags);
          check(Catalog.JSON.toJsonTree(expected).equals(PowerConfig.section("building")), "unrelated building preference changed");
          TweakLifecycle.reset(); PowerConfig.flushPending(); saved(flags);
        }
      });
      test("preserved flexible placement clears held modes and requires release before reuse", () -> {
        mc.setScreen(null); mc.paused = false; mc.field_2778 = true;
        Config.update(s -> s.flexible.enabled = true);
        Modes held = new Modes(true, false, true, false, false);
        FlexiblePlacement.beginTick(mc, false, Modes.NONE, true);
        FlexiblePlacement.beginTick(mc, false, held, true);
        check(FlexiblePlacement.modifiersActive(), "fixture did not start placement modes");
        TweakLifecycle.reset();
        check(Config.current().flexible.enabled && !FlexiblePlacement.modes().active(), "cleanup lost preference or retained modes");
        FlexiblePlacement.beginTick(mc, false, held, true);
        check(!FlexiblePlacement.modifiersActive(), "held modes restarted after cleanup");
        FlexiblePlacement.beginTick(mc, false, Modes.NONE, true);
        FlexiblePlacement.beginTick(mc, false, held, true);
        check(FlexiblePlacement.modifiersActive(), "fresh modifier press no longer works");
        FlexiblePlacement.interrupt();
      });
      test("slab completion has no registered hotkey and still supports menu Cancel and Apply", () -> {
        check(Arrays.stream(mc.options.allKeys).noneMatch(k -> k.translationKey.equals("Slab completion (toggle)")), "slab hotkey registered");
        Config.apply(new Settings());
        var session = SettingsRegistry.open(mc);
        check(session.settings().stream().noneMatch(s -> s.id.equals("keys.Slab completion (toggle)")), "slab hotkey row remains");
        var slab = session.settings().stream().filter(s -> s.id.equals("tweaks.slabs.enabled")).findFirst().orElseThrow();
        check(!ControlLinks.hasControls(slab), "slab links to a removed key");
        boolean original = slab.value.getAsBoolean();
        slab.parse(Boolean.toString(!original)); session.preview(true);
        check(Config.current().slabs.enabled != original, "slab preview missing");
        session.discard(); check(Config.current().slabs.enabled == original, "slab Cancel missing");
        slab.parse(Boolean.toString(!original)); session.save(java.nio.file.Path.of(".").toAbsolutePath());
        check(Config.current().slabs.enabled != original && PowerConfig.read("building", Settings.class).slabs.enabled != original,
            "slab Apply missing");
      });
    } finally { FlexiblePlacement.interrupt(); Config.apply(before); }
  }
}
