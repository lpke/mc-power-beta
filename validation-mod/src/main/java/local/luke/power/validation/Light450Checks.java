package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import local.luke.power.light.*;
import local.luke.power.map.*;
import local.luke.power.mixin.CullingChunkCacheAccessor;
import local.luke.power.ui.PowerOptionsScreen;
import local.luke.power.ui.WorldMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Survey admission uses native screen/HUD state without changing terrain or saved settings. */
final class Light450Checks {
  private Light450Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Maps.cache() != null,
        "open a disposable mapped singleplayer world first");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    failures = 0;
    var previous = mc.currentScreen;
    boolean hidden = mc.options.hideHud, debug = mc.options.debugHud;
    var lights = LightConfig.copy();
    var maps = MapConfig.copy();
    var chunks = (CullingChunkCacheAccessor)mc.world.method_259();
    var loaded = new HashSet<>(chunks.power$loadedChunks().keySet());
    long submitted = MapIO.submitted();
    var cache = (LightCache)field(LightOverlay.class, "CACHE").get(null);
    try {
      var light = LightConfig.copy();
      light.enabled = true; light.radius = 2; light.verticalRange = 1;
      LightConfig.preview(light);
      var map = MapConfig.copy();
      map.lightOverlay = map.minimap = map.worldMap = true;
      map.lightRadius = 8; map.hideWithDebug = true;
      MapConfig.preview(map);
      mc.currentScreen = null;
      mc.options.hideHud = mc.options.debugHud = false;
      test("LIGHT450 hidden world labels stop scanning and discard stale labels", () -> {
        LightOverlay.tick(mc);
        int x = (int)Math.floor(mc.player.x), z = (int)Math.floor(mc.player.z);
        cache.tick(mc.world, x, 64, z, light, (bx, by, bz) -> 7);
        check(!cache.cells().isEmpty(), "fixture did not prepare cached labels");
        mc.options.hideHud = true;
        for (int tick = 0; tick < 40; tick++) {
          LightOverlay.tick(mc);
          check(cache.cells().isEmpty() && field(LightCache.class, "context").get(cache) == null,
              "F1 retained labels or restarted a world-light scan");
          check(field(LightOverlay.class, "world").get(null) == null, "hidden overlay retained an active world");
        }
        mc.options.hideHud = false;
        LightOverlay.tick(mc);
        check(field(LightCache.class, "context").get(cache) == mc.world, "unhiding did not resume scanning");
      });
      test("LIGHT450 invisible minimaps perform zero spawn survey work", () -> {
        mc.currentScreen = null; mc.options.hideHud = mc.options.debugHud = false;
        MapLightShading.tick(mc, map);
        check(MapLightShading.checksLastTick > 0, "visible minimap did not scan");
        mc.options.hideHud = true; idle(mc, map, "F1");
        mc.options.hideHud = false; mc.options.debugHud = true; idle(mc, map, "F3");
        mc.options.debugHud = false;
        mc.currentScreen = new Screen() {}; idle(mc, map, "unrelated menu");
        mc.currentScreen = (Screen)Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen")
            .getConstructor(Screen.class).newInstance((Screen)null);
        idle(mc, map, "photo mode");
        mc.currentScreen = null;
        MapLightShading.tick(mc, map);
        check(MapLightShading.checksLastTick > 0, "visible minimap did not resume");
      });
      test("LIGHT450 an open world map retains its survey while the gameplay HUD is hidden", () -> {
        map.minimap = false; mc.options.hideHud = true;
        var screen = new WorldMapScreen(); screen.width = 640; screen.height = 360;
        mc.currentScreen = screen;
        MapLightShading.tick(mc, map);
        check(MapLightShading.checksLastTick > 0, "F1 incorrectly suppressed world-map shading");
        screen.width = 320; screen.height = 240;
        field(WorldMapScreen.class, "settingsPanel").set(screen, new PowerOptionsScreen(null));
        check(!screen.terrainVisible(), "narrow settings fixture did not cover the map");
        idle(mc, map, "fully covered world map");
        mc.currentScreen = null; idle(mc, map, "closed world-map-only view");
        map.lightOverlay = false; mc.currentScreen = new WorldMapScreen();
        idle(mc, map, "disabled shading");
      });
      check(loaded.equals(chunks.power$loadedChunks().keySet()), "light visibility checks loaded terrain");
      check(MapIO.submitted() == submitted, "light visibility checks submitted map IO");
    } finally {
      MapLightShading.clear(); cache.clear();
      LightConfig.preview(lights); MapConfig.preview(maps);
      mc.options.hideHud = hidden; mc.options.debugHud = debug;
      mc.currentScreen = previous;
    }
    log("LIGHT450 failures=" + failures);
    check(failures == 0, "light-450 failures=" + failures);
  }

  private static void idle(Minecraft mc, MapSettings settings, String context) throws Exception {
    for (int tick = 0; tick < 40; tick++) {
      MapLightShading.tick(mc, settings);
      check(MapLightShading.checksLastTick == 0, context + " started a spawn survey");
      check(field(MapLightShading.class, "world").get(null) == null, context + " retained a survey world");
    }
  }

  private static Field field(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
  }
}
