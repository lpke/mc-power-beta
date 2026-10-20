package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.nio.file.*;
import java.util.*;
import local.luke.power.commands.GameRuleCommand;
import local.luke.power.config.Catalog;
import local.luke.power.input.CameraView;
import local.luke.power.input.SprintView;
import local.luke.power.map.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.WorldMapScreen;
import local.luke.power.world.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Native maps and durable sprint-rule command checks. */
public final class Release261Checks {
  private static final int X = 16, Z = 16, FLOOR = 105;

  private Release261Checks() {}

  public static void run(Minecraft mc, String phase) throws Exception {
    failures = 0;
    if (phase.equals("state")) {
      state(mc);
      return;
    }
    if (phase.equals("open")) {
      mc.setScreen(new WorldMapScreen());
      state(mc);
      return;
    }
    if (phase.equals("rules")) {
      rules(mc);
      log("RELEASE261 RULES failures=" + failures);
      return;
    }
    if (phase.equals("textures")) {
      Release260Checks.run(mc, "textures");
      log("RELEASE261 TEXTURES failures=" + failures);
      return;
    }
    if (!Set.of("scene", "terrain", "cadence", "pose", "render").contains(phase))
      throw new IllegalArgumentException(
          "Use release-261 scene|terrain|cadence|pose|render|textures|rules|open|state");
    scene(mc);
    switch (phase) {
      case "terrain" -> terrain(mc);
      case "cadence" -> cadence(mc);
      case "pose" -> pose(mc);
      case "render" -> render(mc);
    }
    log("RELEASE261 " + phase.toUpperCase(Locale.ROOT) + " failures=" + failures);
  }

  private static void rules(Minecraft mc) throws Exception {
    check(
        mc.world != null && !mc.world.isRemote && mc.player != null,
        "open a disposable singleplayer world first");
    test(
        "sprint commands durably save both spellings and revoke immediately without changing other"
            + " settings",
        () -> {
          var original = AccessRules.copy(mc);
          var screen = mc.currentScreen;
          int difficulty = ((WorldDifficulty) mc.world.method_262()).power$difficulty();
          String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
          PowerConfig.flushPending();
          byte[] preferences = Files.readAllBytes(PowerConfig.path());
          var tuning = Catalog.JSON.toJsonTree(SprintConfig.current());
          WorldRuleChanges.save(mc);
          Path directory = WorldRuleChanges.directory(mc);
          String savedMode =
              WorldRuleFiles.read(directory.resolve("level.dat"))
                  .getCompound("Data")
                  .getCompound("Player")
                  .getString("PowerBetaGameMode");
          try {
            mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen());
            var expected = original.copy();
            expected.set(GameRule.ALLOW_SPRINTING, true);
            GameRuleCommand.execute(mc, new String[] {"gamerule", "allow_sprinting", "true"});
            verifySprintRules(
                mc, directory, expected, difficulty, mode, savedMode, preferences, tuning);
            check(SprintController.allowed(mc), "canonical command did not enable sprinting");

            // Seed only transient activity after opening the screen. No tick can hide a
            // missing synchronous cancellation in the command's mutation boundary.
            var sprint = ((SprintPlayer) mc.player).power$sprint();
            double stamina = sprint.stamina;
            int combat = sprint.combatTicks, recharge = sprint.rechargeTicks;
            boolean exhausted = sprint.exhausted;
            sprint.sprinting = true;
            SprintView.publish(SprintView.Kind.GROUND, true, 1.3);
            expected.set(GameRule.ALLOW_SPRINTING, false);
            GameRuleCommand.execute(mc, new String[] {"gamerule", "allowSprinting", "false"});
            check(
                mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen,
                "command closed the open screen");
            check(
                !sprint.sprinting && !SprintController.allowed(mc),
                "alias command did not cancel sprint immediately");
            near(
                SprintView.ratio(SprintView.Kind.GROUND),
                1,
                "published sprint after command revocation");
            check(
                sprint.stamina == stamina
                    && sprint.combatTicks == combat
                    && sprint.rechargeTicks == recharge
                    && sprint.exhausted == exhausted,
                "command revocation changed stamina or combat recovery");
            verifySprintRules(
                mc, directory, expected, difficulty, mode, savedMode, preferences, tuning);
          } finally {
            try {
              WorldRuleChanges.apply(mc, original);
              WorldRuleChanges.save(mc);
              verifySprintRules(
                  mc, directory, original, difficulty, mode, savedMode, preferences, tuning);
            } finally {
              ((SprintPlayer) mc.player).power$sprint().cancel(false);
              SprintView.publish(SprintView.Kind.GROUND, false, 1);
              mc.setScreen(screen);
            }
          }
        });
  }

  private static void verifySprintRules(
      Minecraft mc,
      Path directory,
      RuleSet expected,
      int difficulty,
      String mode,
      String savedMode,
      byte[] preferences,
      com.google.gson.JsonElement tuning)
      throws Exception {
    WorldRuleFiles.verifySaved(directory, expected, difficulty);
    var data = WorldRuleFiles.read(directory.resolve("level.dat")).getCompound("Data");
    check(WorldRuleData.read(data).equals(expected), "reloaded metadata changed unrelated rules");
    check(AccessRules.copy(mc).equals(expected), "live rules differ from the saved command result");
    check(
        ((WorldDifficulty) mc.world.method_262()).power$difficulty() == difficulty,
        "sprint command changed live difficulty");
    check(
        mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString().equals(mode)
            && data.getCompound("Player").getString("PowerBetaGameMode").equals(savedMode),
        "sprint command changed live or saved player mode");
    check(
        Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())),
        "sprint command wrote global settings");
    check(
        tuning.equals(Catalog.JSON.toJsonTree(SprintConfig.current())),
        "sprint command changed global tuning");
  }

  private static void scene(Minecraft mc) throws Exception {
    DeathChecks.alive(mc);
    mc.setScreen(null);
    FreecamPreviewChecks.active(false);
    mc.field_2807 = mc.player;
    mc.options.hideHud = false;
    check(MapSampler.loaded(mc.world, X >> 4, Z >> 4) != null, "fixture chunk is not loaded");
    for (int x = X; x < X + 16; x++)
      for (int z = Z; z < Z + 16; z++) {
        for (int y = FLOOR - 7; y < 128; y++) mc.world.method_154(x, y, z, 0, 0);
        mc.world.method_154(x, FLOOR, z, 1, 0);
      }
    // Two-block patches remain legible in screenshots; the tests sample their centers.
    for (int dx = 0; dx < 2; dx++)
      for (int dz = 0; dz < 2; dz++) {
        water(mc, X + 1 + dx, Z + 2 + dz, 1);
        water(mc, X + 4 + dx, Z + 2 + dz, 6);
        cover(mc, X + 7 + dx, Z + 2 + dz, 20, 41);
        cover(mc, X + 10 + dx, Z + 2 + dz, 79, 41);
        cover(mc, X + 1 + dx, Z + 6 + dz, 78, 1);
        cover(mc, X + 4 + dx, Z + 6 + dz, 80, 1);
        cover(mc, X + 7 + dx, Z + 6 + dz, 20, 35);
        cover(mc, X + 10 + dx, Z + 6 + dz, 79, 35);
      }
    mc.player.method_1341(X + 8.5, FLOOR + 1, Z + 12.5, 180, 30);
    mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
    var settings = MapConfig.copy();
    settings.minimap = settings.worldMap = true;
    settings.caveMode = 0;
    settings.updateRate = 20;
    settings.zoom = 4;
    settings.size = 180;
    MapConfig.preview(settings);
    for (int n = 0; n < 160; n++) {
      Maps.tick(mc);
      if (n % 8 == 0) Thread.sleep(1);
    }
    state(mc);
  }

  private static void water(Minecraft mc, int x, int z, int depth) {
    for (int y = FLOOR + 2 - depth; y <= FLOOR + 1; y++) mc.world.method_154(x, y, z, 9, 0);
    mc.world.method_154(x, FLOOR + 1 - depth, z, 41, 0);
  }

  private static void cover(Minecraft mc, int x, int z, int top, int bottom) {
    mc.world.method_154(x, FLOOR, z, bottom, bottom == 35 ? 14 : 0);
    mc.world.method_154(x, FLOOR + 1, z, top, 0);
  }

  private static MapTile sample(Minecraft mc, MapPalette palette) throws Exception {
    var sampler = new MapSampler(palette);
    var tile = new MapTile(MapMath.key(mc.world.dimension.id, -1, 0, X, Z));
    var sample =
        MapSampler.class.getDeclaredMethod(
            "sample",
            net.minecraft.world.World.class,
            net.minecraft.class_43.class,
            MapTile.class,
            int.class,
            int.class,
            CaveWindow.class);
    sample.setAccessible(true);
    var chunk = MapSampler.loaded(mc.world, X >> 4, Z >> 4);
    check(chunk != null, "loaded column source vanished");
    // A null cave window scans the full surface column, as the original 127 ceiling did.
    for (int x = X; x < X + 16; x++)
      for (int z = Z; z < Z + 16; z++) sample.invoke(sampler, mc.world, chunk, tile, x, z, (CaveWindow)null);
    return tile;
  }

  private static void terrain(Minecraft mc) throws Exception {
    var palette = new MapPalette();
    palette.tick(mc);
    MapIO.awaitQueued();
    palette.tick(mc);
    test(
        "native loaded columns blend water depth, glass and ice over their floor and retain snow"
            + " height",
        () -> {
          var tile = sample(mc, palette);
          var flat = MapConfig.copy();
          flat.slopeShading = flat.heightShading = flat.caveLighting = false;
          for (int style = 0; style <= 1; style++) {
            flat.colourMode = style;
            int shallow = tile.color(MapMath.index(X + 1, Z + 2), flat);
            int deep = tile.color(MapMath.index(X + 4, Z + 2), flat);
            check(
                brightness(shallow) > brightness(deep),
                "deep water did not attenuate floor, style=" + style);
            check(
                tile.color(MapMath.index(X + 7, Z + 2), flat)
                    != tile.color(MapMath.index(X + 7, Z + 6), flat),
                "glass hid floor pigment");
            check(
                tile.color(MapMath.index(X + 10, Z + 2), flat)
                    != tile.color(MapMath.index(X + 10, Z + 6), flat),
                "ice hid floor pigment");
          }
          check(
              tile.height(MapMath.index(X + 1, Z + 6)) == FLOOR, "thin snow raised relief height");
          check(tile.height(MapMath.index(X + 4, Z + 6)) == FLOOR + 1, "full snow lost height");
          for (int n = 0; n < 100; n++)
            check(
                MapSampler.loaded(mc.world, 1800000 + n, 1800000 - n) == null,
                "sampling loaded distant terrain");
          log("261 terrain shallow/deep/glass/ice/snow passed at " + X + "," + Z);
        });
  }

  private static int brightness(int rgb) {
    return (rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255);
  }

  private static void cadence(Minecraft mc) throws Exception {
    var original = MapConfig.copy();
    try {
      for (int rate : new int[] {1, 5, 10, 20})
        test(
            "native sampler refreshes at " + rate + " Hz within its column budget",
            () -> {
              var settings = MapConfig.copy();
              settings.updateRate = rate;
              settings.refreshSeconds = 2;
              settings.columnsPerTick = 512;
              MapConfig.preview(settings);
              Path temporary = Files.createTempDirectory("power-beta-map-261-");
              var cache =
                  new MapCache(
                      temporary,
                      64,
                      message -> {
                        throw new AssertionError(message);
                      });
              var sampler = new MapSampler(new MapPalette());
              long first = -1, second = -1;
              int worked = 0;
              long key = (long) (X >> 4) << 32 | ((Z >> 4) & 0xffffffffL);
              try {
                cache.get(MapMath.key(mc.world.dimension.id, -1, 0, X, Z));
                MapIO.awaitQueued();
                cache.tick();
                mc.world.method_154(X + 15, FLOOR + 1, Z + 15, 0, 0);
                for (int tick = 1; tick <= 800 && second < 0; tick++) {
                  cache.tick();
                  sampler.tick(mc, cache, -1, X + 8, FLOOR + 1, Z + 8);
                  check(
                      sampler.columnsLastTick <= settings.columnsPerTick, "column budget exceeded");
                  if (sampler.columnsLastTick > 0) worked++;
                  check(
                      tick == 1 || (tick - 1) % (20 / rate) == 0 || sampler.columnsLastTick == 0,
                      "sampler worked between scheduled updates");
                  var sampled = (Map<?, ?>) UiChecks.field(sampler, "sampled");
                  Long completed = (Long) sampled.get(key);
                  if (completed != null && first < 0) {
                    first = completed;
                    mc.world.method_154(X + 15, FLOOR + 1, Z + 15, 41, 0);
                  } else if (completed != null && completed > first) second = completed;
                  if (tick % 8 == 0) Thread.sleep(1);
                }
                check(first >= 0 && second > first, "center chunk never completed two samples");
                check(
                    second - first >= 40 && second - first <= 40 + 12 * (20 / rate),
                    "refresh interval changed with update rate: " + (second - first));
                var tile = cache.peek(MapMath.key(mc.world.dimension.id, -1, 0, X, Z));
                check(
                    tile != null && tile.block(MapMath.index(X + 15, Z + 15)) == 41,
                    "periodic refresh missed native edit");
                log(
                    "261 cadence rate="
                        + rate
                        + " first="
                        + first
                        + " second="
                        + second
                        + " workTicks="
                        + worked);
              } finally {
                cache.finish();
                MapIO.awaitQueued();
                try (var paths = Files.walk(temporary)) {
                  for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.delete(path);
                }
              }
            });
    } finally {
      MapConfig.preview(original);
    }
  }

  private static void pose(Minecraft mc) throws Exception {
    test(
        "native frame pose interpolates position and wraps heading without changing simulation",
        () -> {
          var p = mc.player;
          double x = p.x, y = p.y, z = p.z, px = p.prevX, py = p.prevY, pz = p.prevZ;
          float yaw = p.yaw, previousYaw = p.prevYaw;
          try {
            p.prevX = p.x - 4;
            p.prevY = p.y - 2;
            p.prevZ = p.z - 8;
            p.prevYaw = 170;
            p.yaw = -170;
            var start = Maps.framePose(mc, 0);
            var middle = Maps.framePose(mc, .5f);
            var end = Maps.framePose(mc, 1);
            near(middle.x(), x - 2, "interpolated X");
            near(middle.z(), z - 4, "interpolated Z");
            near(middle.y(), (start.y() + end.y()) / 2, "interpolated Y");
            near(Math.IEEEremainder(middle.yaw() - CameraView.yaw(180), 360), 0, "wrapped heading");
            near(p.x, x, "simulation X");
            near(p.y, y, "simulation Y");
            near(p.z, z, "simulation Z");
          } finally {
            p.prevX = px;
            p.prevY = py;
            p.prevZ = pz;
            p.yaw = yaw;
            p.prevYaw = previousYaw;
          }
        });
  }

  private static void render(Minecraft mc) throws Exception {
    var original = MapConfig.copy();
    int nativeScale = mc.options.guiScale;
    var options = Class.forName("local.luke.power.controls.util.ModOptions");
    var guiScaleField = options.getField("guiScale");
    var realScaleField = options.getField("realGuiScale");
    float enhancedScale = guiScaleField.getFloat(null);
    float realScale = realScaleField.getFloat(null);
    try {
      test(
          "minimap FBO restores native GL state across GUI scale, rotation, opacity and style",
          () -> {
            check(
                GLContext.getCapabilities().GL_EXT_framebuffer_object,
                "native context has no FBO support");
            for (int gui : new int[] {1, 2, 4}) {
              mc.options.guiScale = gui;
              guiScaleField.setFloat(null, gui / 8f);
              realScaleField.setFloat(null, gui / 8f);
              var screen =
                  new net.minecraft.class_564(mc.options, mc.displayWidth, mc.displayHeight);
              for (boolean circle : new boolean[] {false, true})
                for (boolean rotate : new boolean[] {false, true})
                  for (double zoom : new double[] {.25, 1, 4})
                    for (double opacity : new double[] {.35, 1})
                      for (int style = 0; style <= 1; style++) {
                        var s = MapConfig.copy();
                        s.circular = circle;
                        s.rotate = rotate;
                        s.zoom = zoom;
                        s.opacity = opacity;
                        s.colourMode = style;
                        MapConfig.preview(s);
                        MapRenderer.beginFrame(.5f);
                        int texture = mc.textureManager.getTextureId("/gui/gui.png");
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                        int framebuffer =
                            GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
                        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
                        int[] viewport = integers(GL11.GL_VIEWPORT, 4);
                        float[] projection = floats(GL11.GL_PROJECTION_MATRIX, 16);
                        float[] model = floats(GL11.GL_MODELVIEW_MATRIX, 16);
                        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND),
                            depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
                        Minimap.draw(
                            mc,
                            MapConfig.current(),
                            screen.method_1857(),
                            screen.method_1858(),
                            false,
                            false);
                        check(
                            GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT)
                                == framebuffer,
                            "FBO binding leaked");
                        check(GL11.glGetInteger(GL11.GL_MATRIX_MODE) == mode, "matrix mode leaked");
                        check(
                            Arrays.equals(viewport, integers(GL11.GL_VIEWPORT, 4)),
                            "viewport leaked");
                        check(
                            Arrays.equals(projection, floats(GL11.GL_PROJECTION_MATRIX, 16)),
                            "projection leaked");
                        check(
                            Arrays.equals(model, floats(GL11.GL_MODELVIEW_MATRIX, 16)),
                            "model matrix leaked");
                        check(
                            GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == texture,
                            "texture binding leaked");
                        check(
                            GL11.glIsEnabled(GL11.GL_BLEND) == blend
                                && GL11.glIsEnabled(GL11.GL_DEPTH_TEST) == depth,
                            "native enable flags leaked");
                        check(GL11.glGetError() == GL11.GL_NO_ERROR, "minimap GL error");
                        mc.textRenderer.drawWithShadow("Native GUI after minimap", 4, 4, 0xffffff);
                        check(GL11.glGetError() == GL11.GL_NO_ERROR, "native text GL error");
                      }
            }
            var field = MinimapBuffer.class.getDeclaredField("active");
            field.setAccessible(true);
            Object active = field.get(null); check(active != null, "FBO path was never used");
            var failed = active.getClass().getDeclaredField("failed"); failed.setAccessible(true);
            check(!failed.getBoolean(active), "FBO path fell back after allocation failure");
          });
    } finally {
      mc.options.guiScale = nativeScale;
      guiScaleField.setFloat(null, enhancedScale);
      realScaleField.setFloat(null, realScale);
      MapConfig.preview(original);
      MinimapBuffer.clear();
    }
  }

  private static int[] integers(int name, int count) {
    var buffer = BufferUtils.createIntBuffer(16);
    GL11.glGetInteger(name, buffer);
    int[] values = new int[count];
    buffer.get(values);
    return values;
  }

  private static float[] floats(int name, int count) {
    var buffer = BufferUtils.createFloatBuffer(16);
    GL11.glGetFloat(name, buffer);
    float[] values = new float[count];
    buffer.get(values);
    return values;
  }

  private static void near(double actual, double expected, String label) {
    check(
        Math.abs(actual - expected) < 1e-6, label + " expected=" + expected + " actual=" + actual);
  }

  private static void state(Minecraft mc) throws Exception {
    log(
        "261 STATE world="
            + (mc.world == null ? "none" : mc.world.dimension.id)
            + " screen="
            + (mc.currentScreen == null ? "HUD" : mc.currentScreen.getClass().getSimpleName())
            + " tiles="
            + (Maps.cache() == null ? 0 : Maps.cache().size())
            + " columns="
            + (Maps.sampler() == null ? 0 : Maps.sampler().columnsLastTick)
            + " error="
            + Maps.error());
  }
}
