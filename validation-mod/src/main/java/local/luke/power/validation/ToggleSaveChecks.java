package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.*;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.fastplace.FastPlace;
import local.luke.power.fakesneak.FakeSneak;
import local.luke.power.light.LightConfig;
import local.luke.power.slabplacement.SlabPlacement;
import local.luke.power.status.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;

final class ToggleSaveChecks {
  private static final Path RESTART = Path.of("power-beta-toggle-restart.json");

  static void run(Minecraft mc, String action) throws Exception {
    if (!action.equals("state")) { mc.setScreen(null); mc.paused = false; mc.field_2778 = true; }
    if (action.equals("save")) {
      JsonObject expected = new JsonObject();
      expected.add("originalBuilding", Catalog.JSON.toJsonTree(Config.current()));
      expected.add("originalLight", Catalog.JSON.toJsonTree(LightConfig.current()));
      Config.update(s -> { s.placement.setEnabled(!s.placement.enabled); s.sneak.enabled = !s.sneak.enabled; });
      LightConfig.toggle();
      expected.add("building", Catalog.JSON.toJsonTree(Config.current()));
      expected.add("lightOverlay", Catalog.JSON.toJsonTree(LightConfig.current()));
      Files.writeString(RESTART, Catalog.JSON.toJson(expected));
      mc.scheduleStop(); // Deliberately omit an explicit flush before normal client shutdown.
      log("PASS queued gameplay settings handed to shutdown");
      return;
    }
    if (action.equals("reload")) {
      JsonObject expected = JsonParser.parseString(Files.readString(RESTART)).getAsJsonObject();
      check(expected.get("building").equals(Catalog.JSON.toJsonTree(Config.current())), "building toggle lost across restart");
      check(expected.get("lightOverlay").equals(Catalog.JSON.toJsonTree(LightConfig.current())), "light toggle lost across restart");
      check(expected.get("building").equals(PowerConfig.section("building")), "stored snapshot differs after restart");
      Config.apply(Catalog.JSON.fromJson(expected.get("originalBuilding"), local.luke.power.building.config.Settings.class));
      LightConfig.save(Catalog.JSON.fromJson(expected.get("originalLight"), local.luke.power.light.LightSettings.class));
      Files.delete(RESTART);
      log("PASS queued gameplay settings survive shutdown and restart");
      return;
    }
    if (action.equals("setup") || action.equals("setup-chord")) {
      FastPlace.KEY.code = org.lwjgl.input.Keyboard.KEY_K;
      local.luke.power.fakesneak.Keys.ALL[0].code = org.lwjgl.input.Keyboard.KEY_J;
      var modifiers = new java.util.HashMap<>(local.luke.power.input.Bindings.modifiers());
      for (String id : java.util.List.of(FastPlace.KEY.translationKey, local.luke.power.input.GameplayKeys.LIGHT.translationKey)) {
        if (action.equals("setup-chord")) modifiers.put(id, local.luke.power.input.Chord.CTRL);
        else modifiers.remove(id);
      }
      local.luke.power.input.Bindings.configure(modifiers);
      log("Physical toggles: K fast placement, J edge protection, F7 light overlay; Ctrl=" + action.equals("setup-chord")); return;
    }
    if (action.equals("state")) {
      log("TOGGLE STATE placement=" + Config.current().placement.enabled + " sneak=" + Config.current().sneak.enabled
          + " light=" + LightConfig.current().enabled
          + " focused=" + org.lwjgl.opengl.Display.isActive() + " menu=" + (mc.currentScreen != null)
          + " position=" + mc.player.x + "," + mc.player.y + "," + mc.player.z
          + " HUD=" + ActiveTweaks.lines(StatusConfig.current())); return;
    }
    check(mc.world != null && mc.player != null && FastPlace.canOperate(mc), "load a disposable world first");
    var before = Config.current().copy(); var display = StatusConfig.copy(); var light = LightConfig.copy();
    try {
      test("gameplay toggles and live HUD never wait for stalled settings I/O", () -> {
        var setup = before.copy(); setup.placement.restrictionTiedToFast = true;
        setup.placement.setEnabled(false); setup.sneak.enabled = false; setup.slabs.enabled = false; Config.apply(setup);
        var hud = StatusConfig.copy(); hud.slabCompletion = true; hud.enabled = true; StatusConfig.preview(hud);
        var disk = PowerConfig.class.getDeclaredField("DISK"); disk.setAccessible(true);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var blocker = Executors.newSingleThreadExecutor();
        try {
          Future<?> held = blocker.submit(() -> {
            synchronized (disk.get(null)) {
              entered.countDown(); check(release.await(5, TimeUnit.SECONDS), "fixture disk lock timed out");
            }
            return null;
          });
          check(entered.await(5, TimeUnit.SECONDS), "fixture did not lock disk");
          FastPlace.beginTick(mc, false, false, true); FakeSneak.beginTick(mc, false, true); SlabPlacement.beginTick(mc, false, true);
          long start = System.nanoTime();
          FastPlace.beginTick(mc, false, true, true); FakeSneak.beginTick(mc, true, true); SlabPlacement.beginTick(mc, true, true);
          LightConfig.toggle();
          long duration = System.nanoTime() - start;
          check(duration < 50_000_000, "input waited for disk: " + duration / 1_000_000.0 + "ms");
          check(Config.current().placement.enabled && Config.current().placement.restrictionEnabled, "linked fast/restriction state delayed");
          check(Config.current().sneak.enabled && Config.current().slabs.enabled, "live state delayed");
          check(LightConfig.current().enabled != light.enabled, "light overlay toggle delayed");
          var lines = ActiveTweaks.lines(StatusConfig.current());
          check(lines.containsAll(java.util.List.of("Fast placement", "Fake sneak", "Slab completion")), "HUD delayed " + lines);
          FastPlace.beginTick(mc, false, true, true); FakeSneak.beginTick(mc, true, true); SlabPlacement.beginTick(mc, true, true);
          check(Config.current().placement.enabled && Config.current().sneak.enabled, "held key repeated toggle");
          log("TIMING four toggles including light overlay with disk stalled ms=" + duration / 1_000_000.0);
          release.countDown(); held.get(5, TimeUnit.SECONDS); PowerConfig.flushPending();
          check(PowerConfig.section("building").equals(Catalog.JSON.toJsonTree(Config.current())), "latest combined snapshot lost");
          check(PowerConfig.section("lightOverlay").equals(Catalog.JSON.toJsonTree(LightConfig.current())), "light overlay snapshot lost");
        } finally { release.countDown(); blocker.shutdownNow(); PowerConfig.flushPending(); }
      });
      test("queued toggles remain accepted through menu Cancel and Apply", () -> {
        Config.update(s -> s.sneak.enabled = true);
        var session = SettingsRegistry.open(mc); var setting = find(session, "tweaks.sneak.enabled");
        byte[] accepted = Files.readAllBytes(PowerConfig.path());
        setting.parse("false"); session.preview(true);
        check(!Config.current().sneak.enabled, "menu preview missing");
        session.discard(); check(Config.current().sneak.enabled, "Cancel lost gameplay toggle");
        check(Arrays.equals(accepted, Files.readAllBytes(PowerConfig.path())), "Cancel wrote settings");
        setting.parse("false"); session.save(Path.of(".").toAbsolutePath());
        check(!Config.current().sneak.enabled, "Apply missing");
        check(!PowerConfig.section("building").getAsJsonObject("sneak").get("enabled").getAsBoolean(), "older queued state overwrote Apply");
      });
    } finally {
      FastPlace.beginTick(mc, false, false, true); FakeSneak.beginTick(mc, false, true); SlabPlacement.beginTick(mc, false, true);
      Config.apply(before); StatusConfig.preview(display); LightConfig.save(light);
    }
  }

  private static Setting find(ConfigSession session, String id) {
    return session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
  }
}
