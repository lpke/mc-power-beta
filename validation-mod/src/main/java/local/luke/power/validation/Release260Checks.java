package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.nio.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.ui.*;
import local.luke.power.world.WorldDifficulty;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Native map sampling/rendering, shared drafts, sprint travel and persisted stamina. */
public final class Release260Checks {
  public static boolean captureDust;
  public static final List<SprintParticle> dust = new ArrayList<>();
  private static final int X = 16, Y = 106, Z = 16;

  private static void alive(Minecraft mc) throws Exception {
    DeathChecks.alive(mc);
    mc.setScreen(null);
    FreecamPreviewChecks.active(false);
    mc.field_2807 = mc.player;
  }

  private static void ground(Minecraft mc) {
    for (int x = X - 12; x <= X + 12; x++)
      for (int z = Z - 12; z <= Z + 90; z++) {
        mc.world.method_154(x, Y - 1, z, 1, 0);
        for (int y = Y; y < Y + 5; y++) mc.world.method_154(x, y, z, 0, 0);
      }
  }

  private static void position(Minecraft mc, float yaw) {
    mc.player.method_1341(X + .5, Y, Z + .5, yaw, 0);
    mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
    mc.player.field_1623 = true;
    mc.player.field_1624 = false;
    mc.player.field_161.field_2532 = 0;
    mc.player.field_161.field_2533 = 0;
    mc.player.field_161.field_2535 = false;
    mc.player.field_161.field_2536 = false;
    ((SprintPlayer) mc.player).power$sprint().cancel(false);
  }

  public static void run(Minecraft mc, String phase) throws Exception {
    failures = 0;
    if (phase.equals("lifecycle")) {
      lifecycle(mc);
      log("RELEASE260 LIFECYCLE failures=" + failures);
      return;
    }
    if (phase.equals("textures")) {
      textures(mc);
      log("RELEASE260 TEXTURES failures=" + failures);
      return;
    }
    if (phase.equals("raw") || phase.equals("native")) {
      mc.setScreen(new WorldMapScreen());
      mc.field_2767 =
          phase.equals("raw")
              ? (net.minecraft.class_596)
                  Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseHelper")
                      .getConstructor(java.awt.Component.class)
                      .newInstance(mc.canvas)
              : new net.minecraft.class_596(mc.canvas);
      log("Map pointer helper " + mc.field_2767.getClass().getName());
      return;
    }
    if (phase.equals("menus")) {
      menus(mc);
      log("RELEASE260 MENUS failures=" + failures);
      return;
    }
    if (phase.equals("ui")) {
      ui(mc);
      log("RELEASE260 UI failures=" + failures);
      return;
    }
    if (phase.equals("map")) {
      maps(mc);
      log("RELEASE260 MAP failures=" + failures);
      return;
    }
    if (phase.equals("sprint")) {
      sprint(mc);
      log("RELEASE260 SPRINT failures=" + failures);
      return;
    }
    if (phase.equals("perf")) {
      performance(mc);
      log("RELEASE260 PERF failures=" + failures);
      return;
    }
    if (phase.equals("effects")) {
      effects(mc);
      log("RELEASE260 EFFECTS failures=" + failures);
      return;
    }
    if (phase.equals("boats")) {
      boats(mc);
      log("RELEASE260 BOATS failures=" + failures);
      return;
    }
    if (phase.equals("open")) {
      mc.setScreen(new WorldMapScreen());
      return;
    }
    if (phase.equals("state")) {
      log(
          "MAP cache="
              + (Maps.cache() == null ? 0 : Maps.cache().size())
              + " queues="
              + MapIO.queued()
              + " path="
              + Maps.path().size()
              + " layer="
              + Maps.layer()
              + " error="
              + Maps.error());
      return;
    }
    if (phase.equals("save")) {
      Waypoint point = MapKeys.newWaypoint();
      point.id = "release260-restart";
      point.name = "Restart proof";
      Maps.waypoints().put(point);
      ((SprintPlayer) mc.player).power$sprint().stamina = 37.25;
      Maps.waypoints().finish();
      mc.world.method_195(true, null);
      log("RELEASE260 restart data saved");
      return;
    }
    if (phase.equals("saved")) {
      check(
          Maps.points().stream().anyMatch(p -> p.id.equals("release260-restart")),
          "waypoint did not survive restart");
      near(((SprintPlayer) mc.player).power$sprint().stamina, 37.25, 1e-9, "saved stamina");
      log("RELEASE260 SAVED failures=" + failures);
      return;
    }
    throw new IllegalArgumentException("Use release-260 menus|map|sprint|open|state|save|saved");
  }

  private static void lifecycle(Minecraft mc) throws Exception {
    alive(mc);
    var saved = new NbtCompound();
    mc.player.write(saved);
    int originalDimension = mc.world.dimension.id;
    var originalSettings = MapConfig.copy();
    Path originalRoot = Maps.cache().root();
    var point = MapKeys.newWaypoint();
    point.id = "release260-dimension";
    point.name = "Dimension isolation";
    Maps.waypoints().put(point);
    Maps.waypoints().finish();
    try {
      test(
          "native Nether travel shares world identity and preserves independent terrain layers",
          () -> {
            mc.method_2139();
            Maps.tick(mc);
            check(Maps.dimension() != originalDimension, "dimension did not change");
            check(
                Maps.cache().root().equals(originalRoot),
                "dimension changed stable world identity");
            check(Maps.waypoints().contains(point.id), "dimension lost waypoint");
            int current = Maps.dimension();
            var a = MapMath.key(originalDimension, -1, 0, 0, 0);
            var b = MapMath.key(current, -1, 0, 0, 0);
            check(!a.file().equals(b.file()), "dimensions share a tile path");
            mc.method_2139();
            Maps.tick(mc);
            check(Maps.dimension() == originalDimension, "return dimension wrong");
            check(Maps.waypoints().contains(point.id), "return travel lost waypoint");
          });
      test(
          "disabling maps retains waypoint bytes and re-enabling restores the same store",
          () -> {
            Maps.waypoints().finish();
            byte[] bytes = Files.readAllBytes(originalRoot.resolve("waypoints.json"));
            var off = MapConfig.copy();
            off.minimap =
                off.worldMap = off.waypoints = off.worldWaypoints = off.deathpoints = false;
            MapConfig.preview(off);
            for (int n = 0; n < 40; n++) Maps.tick(mc);
            check(
                Arrays.equals(bytes, Files.readAllBytes(originalRoot.resolve("waypoints.json"))),
                "disabled maps rewrote waypoints");
            check(Maps.waypoints().contains(point.id), "disabled maps erased data");
            MapConfig.preview(originalSettings);
            Maps.tick(mc);
            check(
                Maps.points().stream().anyMatch(p -> p.id.equals(point.id)),
                "re-enabled maps lost waypoint");
          });
    } finally {
      if (mc.world.dimension.id != originalDimension) mc.method_2139();
      MapConfig.preview(originalSettings);
      mc.player.read(saved);
      Maps.tick(mc);
      Maps.waypoints().remove(point.id);
      Maps.waypoints().finish();
    }
  }

  private static void textures(Minecraft mc) throws Exception {
    var original = mc.field_2768.field_1175;
    var palette = new MapPalette();
    try {
      test(
          "native texture-pack changes refresh worker-owned map palette and restore cleanly",
          () -> {
            palette.tick(mc);
            MapIO.awaitQueued();
            palette.tick(mc);
            int first = palette.revision();
            int[] before = ((int[]) UiChecks.field(palette, "colours")).clone();
            net.minecraft.class_285 alternate = null;
            for (Object value : mc.field_2768.method_1000()) {
              var pack = (net.minecraft.class_285) value;
              if (!pack.field_1137.equals(original.field_1137)
                  && pack.field_1137.equals("1.14 Textures.zip")) alternate = pack;
            }
            check(alternate != null, "alternate native texture pack absent");
            mc.field_2768.method_999(alternate);
            mc.textureManager.method_1096();
            palette.tick(mc);
            MapIO.awaitQueued();
            palette.tick(mc);
            check(palette.revision() > first, "palette did not refresh");
            check(
                !Arrays.equals(before, (int[]) UiChecks.field(palette, "colours")),
                "texture averages did not change");
            mc.field_2768.method_999(original);
            mc.textureManager.method_1096();
            palette.tick(mc);
            MapIO.awaitQueued();
            palette.tick(mc);
            check(
                Arrays.equals(before, (int[]) UiChecks.field(palette, "colours")),
                "restored palette changed");
          });
    } finally {
      mc.field_2768.method_999(original);
      mc.textureManager.method_1096();
    }
  }

  private static void menus(Minecraft mc) throws Exception {
    test(
        "Map tab routes appearance into the isolated HUD editor and preserves storage IDs",
        () -> {
          var session = SettingsRegistry.open(mc);
          check(PowerOptionsScreen.PAGES.contains("Map"), "Map tab absent");
          var size = find(session, "maps.size");
          check(HudEditorSettings.contains(size), "size outside HUD editor");
          check(
              !HudEditorSettings.contains(find(session, "maps.caveMode")),
              "terrain setting incorrectly hidden in HUD");
          check(find(session, "maps.hudEditor").page.equals("Map"), "HUD editor link absent");
          check(
              find(session, "sprint.activation").page.equals("Gameplay"),
              "ground sprint tuning left Gameplay");
          check(
              find(session, mc.world == null ? "rules.survival.allow_sprinting"
                  : "rules.world.allow_sprinting").page.equals("Game rules"),
              "ground sprint rule absent");
          check(session.settings().stream().noneMatch(s -> s.id.equals("sprint.enabled")),
              "legacy global sprint enablement is editable");
          check(
              !find(session, "sprint.stamina").group.equals(find(session, "sprint.activation").group),
              "balance controls were not grouped");
          check(
              find(session, "sprint.fovStrength").page.equals("Camera"),
              "shared FOV strength outside Camera");
          var draft = new SettingsDraft(session.settings(), HudEditorSettings::contains);
          draft.get("maps.size").parse("130");
          check(
              size.value.getAsInt() != 130 && MapConfig.current().size != 130, "HUD draft leaked");
          draft.checkpoint();
          draft.get("maps.offsetX").parse("40");
          draft.undo();
          check(
              draft.get("maps.offsetX").value.equals(find(session, "maps.offsetX").value),
              "HUD undo failed");
        });
    test(
        "only Power Beta enables ground sprinting; dynamic FOV remains optional",
        () -> {
          check(!new RuleSet().enabled(GameRule.ALLOW_SPRINTING)
                  && !RuleSet.cheats().enabled(GameRule.ALLOW_SPRINTING),
              "factory creation preset grants sprinting");
          for (var base : DefaultBases.Base.values()) {
            for (String scope : List.of("world", "survival", "cheats"))
              check(DefaultBases.value(base, "rules." + scope + ".allow_sprinting",
                      new com.google.gson.JsonPrimitive(false)).getAsBoolean() == (base == DefaultBases.Base.POWER_BETA),
                  "sprint rule defaults on in " + base + " " + scope);
            for (String field :
                List.of("dynamicFov", "creativeFov", "spectatorFov", "freecamFov"))
              check(
                  !DefaultBases.value(
                          base, "sprint." + field, new com.google.gson.JsonPrimitive(false))
                      .getAsBoolean(),
                  "enabled default " + field + " " + base);
          }
        });
    test(
        "ConfigSession previews map preferences and restores them without writes",
        () -> {
          byte[] bytes = Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
          int before = MapConfig.current().colourMode;
          var session = SettingsRegistry.open(mc);
          var row = find(session, "maps.colourMode");
          row.cycle(1);
          session.preview();
          check(MapConfig.current().colourMode != before, "preview absent");
          session.discard();
          check(MapConfig.current().colourMode == before, "Cancel leaked");
          check(
              Arrays.equals(bytes, Files.readAllBytes(local.luke.power.storage.PowerConfig.path())),
              "preview wrote configuration");
        });
    var options = new PowerOptionsScreen(mc.currentScreen);
    mc.setScreen(options);
    test(
        "minimap preview follows enablement and F3 while keeping complete selection bounds",
        () -> {
          var toggle = find(options.session(), "maps.minimap");
          var original = toggle.value.deepCopy();
          try {
            for (boolean enabled : new boolean[] {false, true}) {
              toggle.value = new com.google.gson.JsonPrimitive(enabled);
              var hud = new HudEditorScreen(options, "maps.size");
              mc.setScreen(hud);
              hud.render(-1, -1, 0);
              Object preview = UiChecks.field(hud, "preview");
              Object mini = null;
              for (Object element : (List<?>) UiChecks.call(preview, "elements"))
                if (UiChecks.call(element, "id").equals("minimap")) mini = element;
              check(mini != null, "minimap selection missing");
              check(
                  (boolean) UiChecks.call(mini, "hidden") == !enabled,
                  "preview enablement mismatch");
              if (enabled) {
                check(
                    (double) UiChecks.call(mini, "height")
                        > find(options.session(), "maps.size").value.getAsDouble(),
                    "information lines outside selection bounds");
              }
              if (enabled) {
                UiChecks.field(hud, "scenario", 2);
                hud.render(-1, -1, 0);
                for (Object element : (List<?>) UiChecks.call(preview, "elements"))
                  if (UiChecks.call(element, "id").equals("minimap"))
                    check((boolean) UiChecks.call(element, "hidden"), "F3 visibility mismatch");
              }
            }
          } finally {
            toggle.value = original;
          }
        });
    mc.setScreen(new HudEditorScreen(options, "maps.size"));
  }

  private static void ui(Minecraft mc) throws Exception {
    alive(mc);
    double playerX = mc.player.x, playerZ = mc.player.z;
    test(
        "map context menus cancel panning and Close works with the pointer",
        () -> {
          var screen = new WorldMapScreen();
          mc.setScreen(screen);
          var input = (local.luke.power.validation.mixin.ScreenInput) (Object) screen;
          input.power$click(screen.width / 2, screen.height / 2, 0);
          check((boolean) UiChecks.field(screen, "dragging"), "pan did not begin");
          input.power$click(screen.width / 2, screen.height / 2, 1);
          check(!(boolean) UiChecks.field(screen, "dragging"), "context menu kept panning");
          input.power$key((char) 0, Keyboard.KEY_ESCAPE);
          input.power$click(screen.width - 36, 13, 0);
          check(mc.currentScreen == null, "map Close failed");
        });
    test(
        "coordinate navigation validates the border without moving or loading the player",
        () -> {
          var screen = new WorldMapScreen();
          mc.setScreen(screen);
          var input = (local.luke.power.validation.mixin.ScreenInput) (Object) screen;
          input.power$key('g', Keyboard.KEY_G);
          check(mc.currentScreen instanceof MapCoordinatesScreen, "coordinate screen missing");
          var editor = mc.currentScreen;
          ((TextInput) UiChecks.field(editor, "x")).setText("32000001");
          ((TextInput) UiChecks.field(editor, "z")).setText("0");
          ((local.luke.power.validation.mixin.ScreenInput) editor)
              .power$key((char) 0, Keyboard.KEY_RETURN);
          check(mc.currentScreen == editor, "invalid coordinate accepted");
          ((TextInput) UiChecks.field(editor, "x")).setText("32000000");
          ((local.luke.power.validation.mixin.ScreenInput) editor)
              .power$key((char) 0, Keyboard.KEY_RETURN);
          check(mc.currentScreen == screen, "valid border coordinate rejected");
          screen.render(-1, -1, 0);
          int cell = ((Number) UiChecks.call(screen, "buttonWidth")).intValue();
          input.power$click(8 + 5 * (cell + 2) + cell / 2, screen.height - 15, 0);
          MapIO.awaitQueued();
          check(
              MapExport.status().startsWith("Exported"),
              "native border export failed: " + MapExport.status());
          near(mc.player.x, playerX, 1e-9, "map navigation player X");
          near(mc.player.z, playerZ, 1e-9, "map navigation player Z");
        });
    mc.setScreen(new WorldMapScreen());
  }

  private static void maps(Minecraft mc) throws Exception {
    alive(mc);
    var settings = MapConfig.copy();
    var saved = new NbtCompound();
    mc.player.write(saved);
    try {
      var next = MapConfig.copy();
      next.minimap = true;
      next.worldMap = true;
      next.caveMode = 0;
      MapConfig.preview(next);
      test(
          "sampler never creates a far native chunk",
          () -> {
            int far = 1800000;
            check(
                MapSampler.loaded(mc.world, far, far) == null, "far fixture chunk already loaded");
            for (int i = 0; i < 200; i++)
              check(
                  MapSampler.loaded(mc.world, far + i, far - i) == null,
                  "loaded lookup generated terrain");
          });
      ground(mc);
      position(mc, 0);
      Maps.tick(mc);
      test(
          "surface sampling records native height, edits and biomes within the configured work cap",
          () -> {
            for (int i = 0; i < 240; i++) {
              Maps.tick(mc);
              Thread.sleep(1);
              check(Maps.sampler().columnsLastTick <= next.columnsPerTick, "column cap exceeded");
            }
            var tile = Maps.cache().get(MapMath.key(Maps.dimension(), -1, 0, X, Z));
            check(tile != null, "terrain tile missing");
            int i = MapMath.index(X, Z);
            check(
                tile.known(i) && tile.height(i) == Y - 1,
                "surface height wrong: " + (tile == null ? -1 : tile.height(i)));
            check(tile.block(i) == 1, "wrong block identity");
            check(tile.biome(i) > 0, "missing biome");
            mc.world.method_154(X, Y, Z, 35, 14);
            for (int n = 0; n < 260; n++) {
              Maps.tick(mc);
              Thread.sleep(1);
            }
            check(tile.height(i) == Y && tile.block(i) == 35, "terrain edits did not refresh");
            log(
                "MAP sampling last="
                    + Maps.sampler().nanosLastTick
                    + "ns resident="
                    + Maps.cache().size()
                    + " queue="
                    + MapIO.queued());
          });
      test(
          "partially sampled chunks reach distant zoom levels before camera changes",
          () -> {
            var partial = MapConfig.copy();
            partial.columnsPerTick = 32;
            partial.caveMode = 0;
            MapConfig.preview(partial);
            Path root =
                net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getGameDir()
                    .resolve("reports/partial-map-" + System.nanoTime());
            var localCache =
                new MapCache(
                    root,
                    64,
                    message -> {
                      throw new IllegalStateException(message);
                    });
            var localSampler = new MapSampler(new MapPalette());
            var key = MapMath.key(Maps.dimension(), -1, 0, X, Z);
            for (int n = 0; n < 1000 && localCache.get(key) == null; n++) {
              localCache.tick();
              Thread.sleep(1);
            }
            for (int n = 0; n < 10; n++) {
              localSampler.tick(mc, localCache, -1, X + .5, Y, Z + .5);
              if (localSampler.columnsLastTick > 0) break;
            }
            check(
                localSampler.columnsLastTick > 0 && localSampler.columnsLastTick < 256,
                "fixture did not interrupt a partial scan");
            localCache.finish();
            var parent = MapCodec.read(root.resolve(key.parent().file()), key.parent());
            int index = Math.floorMod(Z / 2, 64) * 64 + Math.floorMod(X / 2, 64);
            check(parent.known(index), "partial scan never reached its parent");
            MapConfig.preview(next);
          });
      test(
          "automatic caves require a solid roof and surface/cave caches stay separate",
          () -> {
            next.caveMode = 1;
            next.roofSize = 3;
            MapConfig.preview(next);
            check(
                MapSampler.layer(mc, mc.player.x, mc.player.boundingBox.minY, mc.player.z) == -1,
                "open air became cave");
            for (int x = X - 2; x <= X + 2; x++)
              for (int z = Z - 2; z <= Z + 2; z++) mc.world.method_154(x, Y + 4, z, 1, 0);
            int layer = MapSampler.layer(mc, mc.player.x, mc.player.boundingBox.minY, mc.player.z);
            check(layer >= 0, "roof not detected");
            for (int n = 0; n < 200; n++) {
              Maps.tick(mc);
              Thread.sleep(1);
            }
            check(
                Maps.cache().get(MapMath.key(Maps.dimension(), layer, 0, X, Z))
                    != Maps.cache().get(MapMath.key(Maps.dimension(), -1, 0, X, Z)),
                "cave overwrote surface");
          });
      test(
          "native map rendering restores GL state across shapes, rotation, overlays and scales",
          () -> {
            var renderer = mc.textureManager;
            int texture = renderer.getTextureId("/gui/gui.png");
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                blend = GL11.glIsEnabled(GL11.GL_BLEND);
            for (boolean circle : new boolean[] {false, true})
              for (double scale : new double[] {.125, .5, 1, 4}) {
                MapRenderer.beginFrame();
                MapRenderer.terrain(
                    Maps.cache(),
                    new MapProjection(Maps.x(), Maps.z(), scale, 4, 4, 108, 108, .7),
                    Maps.dimension(),
                    Maps.layer(),
                    circle,
                    next,
                    true,
                    mc.world.getSeed());
                check(
                    GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == texture,
                    "texture binding leaked");
                check(
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST) == depth
                        && GL11.glIsEnabled(GL11.GL_BLEND) == blend,
                    "GL enable flags leaked");
                check(GL11.glGetError() == GL11.GL_NO_ERROR, "map GL error");
              }
          });
    } finally {
      MapConfig.preview(settings);
      mc.player.read(saved);
    }
  }

  private static void sprint(Minecraft mc) throws Exception {
    alive(mc);
    var settings = SprintConfig.copy();
    var saved = new NbtCompound();
    mc.player.write(saved);
    var input = mc.player.field_161;
    boolean paused = mc.paused;
    var fixtureInput = new net.minecraft.class_41();
    mc.player.field_161 = fixtureInput;
    var keyField = Keyboard.class.getDeclaredField("keyDownBuffer");
    keyField.setAccessible(true);
    var keys = (ByteBuffer) keyField.get(null);
    byte ctrl = keys.get(29);
    var rules = AccessRules.copy(mc);
    try {
      CreativeVehicleChecks.mode(mc, "SURVIVAL");
      ground(mc);
      mc.paused = false;
      mc.field_2778 = true;
      var next = SprintConfig.copy();
      next.enabled = true;
      next.activation = 0;
      next.speed = 130;
      next.jumpBoost = true;
      next.noSprintInCombat = false;
      next.fovStrength = 100;
      next.doubleTap = false;
      next.stamina = false;
      SprintConfig.preview(next);
      sprintRule(mc, false);
      test(
          "sprint rule query, help and completion leave rules and global preferences unchanged",
          () -> {
            RuleSet before = AccessRules.copy(mc);
            byte[] global = Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
            int difficulty = ((WorldDifficulty) mc.world.method_262()).power$difficulty();
            local.luke.power.commands.GameRuleCommand.execute(mc, new String[]{"gamerule", "allow_sprinting"});
            local.luke.power.commands.GameRuleCommand.execute(mc, new String[]{"gamerule", "allowSprinting"});
            local.luke.power.commands.GameRuleCommand.execute(mc, new String[]{"gamerule", "help"});
            check(Arrays.asList(local.luke.power.commands.GameRuleCommand.suggestions(1, "allow_s", "/gamerule allow_s"))
                    .contains("printing"), "sprint rule completion absent");
            check(GameRule.ALLOW_SPRINTING.help().contains("/gamerule allow_sprinting"), "sprint help absent");
            check(before.equals(AccessRules.copy(mc)), "read-only command changed world rules");
            check(difficulty == ((WorldDifficulty) mc.world.method_262()).power$difficulty(), "read-only command changed difficulty");
            check(Arrays.equals(global, Files.readAllBytes(local.luke.power.storage.PowerConfig.path())), "read-only command changed preferences");
          });
      test(
          "full native ground travel matches modern sprint displacement and 0.2 jump impulse",
          () -> {
            double[] distances = new double[2];
            for (int mode = 0; mode < 2; mode++) {
              sprintRule(mc, mode == 1);
              position(mc, 0);
              keys.put(29, (byte) 1);
              double start = mc.player.z;
              for (int i = 0; i < 80; i++) {
                fixtureInput.field_2533 = 1;
                mc.player.tick();
              }
              distances[mode] = mc.player.z - start;
            }
            near(distances[1] / distances[0], 1.3, .002, "sprint displacement ratio");
            position(mc, 0);
            fixtureInput.field_2533 = 1;
            SprintController.movement(mc.player);
            double before = mc.player.velocityZ;
            SprintController.jumped(mc.player);
            near(mc.player.velocityZ - before, .2, 1e-6, "sprint jump boost");
            log("SPRINT full native displacement walk=" + distances[0] + " sprint=" + distances[1]);
          });
      test(
          "menus and a revoked world rule cancel sprint physics despite legacy global enablement",
          () -> {
            position(mc, 0);
            fixtureInput.field_2533 = 1;
            SprintController.movement(mc.player);
            check(SprintController.active(mc.player), "sprint unavailable");
            mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen());
            SprintController.movement(mc.player);
            check(!SprintController.active(mc.player), "sprint survived menu");
            mc.setScreen(null);
            sprintRule(mc, false);
            SprintController.movement(mc.player);
            check(!SprintController.active(mc.player), "disabled sprint active");
            near(
                SprintController.acceleration(mc.player, .1f),
                .1f,
                1e-8,
                "disabled native acceleration");
          });
      test(
          "player NBT retains bounded stamina and combat recovery without enabling sprinting",
          () -> {
            SprintState state = ((SprintPlayer) mc.player).power$sprint();
            state.stamina = 37.25;
            state.combatTicks = 80;
            state.exhausted = true;
            var nbt = new NbtCompound();
            mc.player.write(nbt);
            state.stamina = 99;
            state.combatTicks = 0;
            mc.player.read(nbt);
            near(state.stamina, 37.25, 1e-9, "stamina NBT");
            check(state.combatTicks == 80 && state.exhausted, "stamina flags lost");
            check(!SprintController.allowed(mc), "NBT enabled the world sprint rule");
            check(SprintConfig.current().enabled, "NBT changed inert legacy data");
          });
      test(
          "world-rule revocation immediately clears sprint and FOV while preserving tuning and recovery",
          () -> {
            sprintRule(mc, true);
            position(mc, 0);
            next.enabled = false;
            next.dynamicFov = true;
            SprintConfig.preview(next);
            fixtureInput.field_2533 = 1;
            keys.put(29, (byte) 1);
            SprintController.movement(mc.player);
            check(SprintController.active(mc.player), "legacy false overrode allowed world");
            SprintController.reset();
            var state = ((SprintPlayer) mc.player).power$sprint();
            state.sprinting = true;
            state.stamina = 37.25;
            state.combatTicks = 80;
            SprintController.tick(mc);
            check(SprintController.fov(1) > 1, "sprint FOV did not start");
            var tuning = Catalog.JSON.toJsonTree(SprintConfig.current());
            mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen());
            sprintRule(mc, false);
            check(!state.sprinting && !SprintController.active(mc.player), "revocation retained sprint");
            near(SprintController.fov(0), 1, 1e-8, "previous FOV after revocation");
            near(SprintController.fov(1), 1, 1e-8, "current FOV after revocation");
            near(SprintView.ratio(SprintView.Kind.GROUND), 1, 1e-8, "published sprint after revocation");
            near(state.stamina, 37.25, 1e-9, "revocation erased stamina");
            check(state.combatTicks == 80, "revocation erased combat state");
            check(tuning.equals(Catalog.JSON.toJsonTree(SprintConfig.current())), "world edit changed global tuning");
            mc.setScreen(null);
          });
    } finally {
      keys.put(29, ctrl);
      SprintConfig.preview(settings);
      AccessRules.restore(mc, rules);
      mc.player.read(saved);
      mc.player.field_161 = input;
      mc.paused = paused;
      SprintController.reset();
    }
  }

  private static void sprintRule(Minecraft mc, boolean enabled) throws Exception {
    RuleSet rules = AccessRules.copy(mc);
    rules.set(GameRule.ALLOW_SPRINTING, enabled);
    local.luke.power.world.WorldRuleChanges.apply(mc, rules);
  }

  private static void near(double actual, double expected, double tolerance, String name) {
    check(
        Math.abs(actual - expected) <= tolerance,
        name + " actual=" + actual + " expected=" + expected);
  }

  private static void performance(Minecraft mc) throws Exception {
    alive(mc);
    var settings = MapConfig.copy();
    var saved = new NbtCompound();
    mc.player.write(saved);
    try {
      var next = MapConfig.copy();
      next.caveMode = 0;
      next.entityNames = true;
      next.radarLimit = 16;
      MapConfig.preview(next);
      long[] times = new long[800];
      long maxSampling = 0;
      for (int i = 0; i < times.length; i++) {
        if (i % 80 == 0)
          mc.player.method_1341(X + (i / 80 % 3) * 16, Y, Z + (i / 240 % 3) * 16, 0, 0);
        long start = System.nanoTime();
        Maps.tick(mc);
        times[i] = System.nanoTime() - start;
        maxSampling = Math.max(maxSampling, Maps.sampler().nanosLastTick);
        check(Maps.sampler().columnsLastTick <= next.columnsPerTick, "sampler work count exceeded");
        check(Maps.cache().size() <= next.cacheTiles, "resident cache bound exceeded");
        check(MapIO.queued() <= 64, "worker queue bound exceeded");
        if (i % 8 == 0) Thread.sleep(1);
      }
      Arrays.sort(times);
      log(
          "MAP tick median="
              + times[400]
              + "ns p95="
              + times[760]
              + "ns max="
              + times[799]
              + "ns maxSampler="
              + maxSampling
              + "ns resident="
              + Maps.cache().size()
              + " queue="
              + MapIO.queued());
      long[] frames = new long[100];
      for (int i = 0; i < frames.length; i++) {
        long start = System.nanoTime();
        MapRenderer.beginFrame();
        var view = new MapProjection(Maps.x(), Maps.z(), 1, 0, 0, 512, 240, 0);
        MapRenderer.terrain(
            Maps.cache(), view, Maps.dimension(), -1, false, next, true, mc.world.getSeed());
        MapRenderer.markers(mc, view, Maps.dimension(), false, false);
        GL11.glFinish();
        frames[i] = System.nanoTime() - start;
        check(GL11.glGetError() == 0, "map benchmark GL error");
      }
      Arrays.sort(frames);
      log("MAP draw median=" + frames[50] + "ns p95=" + frames[95] + "ns max=" + frames[99] + "ns");
      check(
          Arrays.stream(Maps.radar().toArray(Maps.Radar[]::new)).allMatch(p -> p.name() != null),
          "radar null name");
    } finally {
      MapConfig.preview(settings);
      mc.player.read(saved);
    }
  }

  private static void effects(Minecraft mc) throws Exception {
    alive(mc);
    ground(mc);
    var saved = new NbtCompound();
    mc.player.write(saved);
    var settings = SprintConfig.copy();
    var rules = AccessRules.copy(mc);
    boolean oldPause = mc.paused;
    try {
      CreativeVehicleChecks.mode(mc, "SURVIVAL");
      sprintRule(mc, true);
      position(mc, 0);
      mc.paused = false;
      mc.player.tick();
      position(mc, 0);
      test(
          "sprint dust uses modern gravity, drag, collision stopping and loaded terrain",
          () -> {
            var p =
                new SprintParticle(
                    mc.world,
                    X + .5,
                    Y + 1,
                    Z + .5,
                    .1,
                    .2,
                    .3,
                    net.minecraft.block.Block.STONE,
                    0,
                    X,
                    Y - 1,
                    Z);

            p.velocityX = .1;
            p.velocityY = .2;
            p.velocityZ = .3;
            double x = p.x, y = p.y, z = p.z;
            p.tick();
            near(p.x - x, .1, 1e-7, "dust X");
            near(p.y - y, .16, 1e-7, "dust Y");
            near(p.z - z, .3, 1e-7, "dust Z");
            near(p.velocityX, .098, 1e-7, "dust horizontal friction");
            near(p.velocityY, .1568, 1e-7, "dust vertical friction");
            p.method_1340(X + .5, Y + .02, Z + .5);
            p.velocityX = .1;
            p.velocityY = -.2;
            p.velocityZ = 0;
            p.tick();
            check(p.field_1623, "dust did not meet the native floor");
            p.tick();
            double stopX = p.x, stopY = p.y;
            p.tick();
            near(p.x, stopX, 1e-8, "stopped dust X");
            near(p.y, stopY, 1e-8, "stopped dust Y");
            p.markDead();
          });
      test(
          "native particle dispatch emits one dust particle at modern feet height",
          () -> {
            var s = SprintConfig.copy();
            s.particles = true;
            SprintConfig.preview(s);
            position(mc, 0);
            ((SprintPlayer) mc.player).power$sprint().sprinting = true;
            captureDust = true;
            dust.clear();
            SprintController.particles(mc.player);
            check(dust.size() == 1, "native dust missing or repeated");
            near(dust.get(0).y, Y + .1, 1e-7, "dust origin");
            mc.player.field_1623 = false;
            SprintController.particles(mc.player);
            check(dust.size() == 1, "airborne dust emitted");
            mc.player.field_1623 = true;
            captureDust = false;
            dust.forEach(net.minecraft.entity.Entity::markDead);
            dust.clear();
          });
      test(
          "ground sprint FOV follows modern half-tick smoothing and cancels after menus",
          () -> {
            var s = SprintConfig.copy();
            s.dynamicFov = true;
            SprintConfig.preview(s);
            SprintController.reset();
            var state = ((SprintPlayer) mc.player).power$sprint();
            state.sprinting = true;
            SprintController.tick(mc);
            near(
                SprintController.fov(1),
                1.075,
                1e-6,
                "initial sprint FOV paused="
                    + mc.paused
                    + " water="
                    + mc.player.method_1334()
                    + " active="
                    + org.lwjgl.opengl.Display.isActive());
            SprintController.tick(mc);
            near(SprintController.fov(1), 1.1125, 1e-6, "second sprint FOV");
            mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen());
            for (int n = 0; n < 25; n++) SprintController.tick(mc);
            near(SprintController.fov(1), 1, 1e-5, "menu FOV cancellation");
            mc.setScreen(null);
            s.dynamicFov = false;
            SprintConfig.preview(s);
            state.sprinting = true;
            for (int n = 0; n < 25; n++) SprintController.tick(mc);
            near(SprintController.fov(1), 1, 1e-5, "disabled sprint FOV");
          });
      test(
          "native sprint hits apply modern extra knockback and combat blocks later sprints",
          () -> {
            CreativeVehicleChecks.mode(mc, "SURVIVAL");
            position(mc, 0);
            mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
            var settingsNow = SprintConfig.copy();
            settingsNow.sprintKnockback = true;
            settingsNow.noSprintInCombat = true;
            SprintConfig.preview(settingsNow);
            var target = net.minecraft.class_206.method_732("Cow", mc.world);
            target.method_1341(X + .5, Y, Z + 2.5, 0, 0);
            target.field_1623 = true;
            try {
              var state = ((SprintPlayer) mc.player).power$sprint();
              state.combatTicks = 0;
              state.sprinting = true;
              mc.player.velocityZ = 1;
              mc.player.attack(target);
              near(mc.player.velocityZ, .6, 1e-7, "sprint hit slowdown");
              check(state.combatTicks > 0 && !state.sprinting, "hit did not enter combat");
              check(
                  target.velocityZ > .45 && target.velocityY <= .4,
                  "modern extra knockback missing");
              mc.player.field_161.field_2533 = 1;
              SprintController.movement(mc.player);
              check(!SprintController.active(mc.player), "combat restriction ignored");
            } finally {
              target.markDead();
            }
          });
      test(
          "Creative, Spectator and freecam FOV remain independent",
          () -> {
            AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats());
            check(!SprintController.allowed(mc), "Cheats preset unexpectedly grants ground sprinting");
            for (String mode : List.of("CREATIVE", "SPECTATOR", "FREECAM")) {
              CreativeVehicleChecks.mode(mc, mode.equals("FREECAM") ? "CREATIVE" : mode);
              mc.player
                  .getClass()
                  .getMethod("creative_setFlying", boolean.class)
                  .invoke(mc.player, true);
              FreecamPreviewChecks.active(mode.equals("FREECAM"));
              SprintController.reset();
              var s = SprintConfig.copy();
              s.creativeFov = s.spectatorFov = s.freecamFov = false;
              if (mode.equals("CREATIVE")) s.creativeFov = true;
              else if (mode.equals("SPECTATOR")) s.spectatorFov = true;
              else s.freecamFov = true;
              SprintConfig.preview(s);
              var kind = SprintView.Kind.valueOf(mode);
              SprintView.publish(kind, true, 1.6);
              for (int n = 0; n < 25; n++) SprintController.tick(mc);
              near(SprintController.fov(1), 1.3, 1e-5, mode + " FOV");
              s.creativeFov = s.spectatorFov = s.freecamFov = false;
              if (mode.equals("FREECAM")) s.creativeFov = true;
              SprintConfig.preview(s);
              for (int n = 0; n < 25; n++) SprintController.tick(mc);
              near(SprintController.fov(1), 1, 1e-5, mode + " independent off");
              SprintView.publish(kind, false, 1);
              FreecamPreviewChecks.active(false);
            }
          });
    } finally {
      FreecamPreviewChecks.active(false);
      captureDust = false;
      dust.forEach(net.minecraft.entity.Entity::markDead);
      dust.clear();
      SprintConfig.preview(settings);
      AccessRules.restore(mc, rules);
      mc.player.read(saved);
      mc.paused = oldPause;
      SprintController.reset();
    }
  }

  private static void boats(Minecraft mc) throws Exception {
    alive(mc);
    var saved = new NbtCompound();
    mc.player.write(saved);
    var rules = AccessRules.copy(mc);
    var created = new ArrayList<net.minecraft.entity.Entity>();
    try {
      AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats());
      for (String mode : List.of("SURVIVAL", "CREATIVE"))
        for (boolean occupied : new boolean[] {false, true})
          test(
              "boat interaction consumes held boat placement in " + mode + " occupied=" + occupied,
              () -> {
                if (mc.player.field_1595 != null) mc.player.method_1376(null);
                CreativeVehicleChecks.mode(mc, mode);
                mc.player.inventory.selectedSlot = 0;
                mc.player.inventory.main[0] = DeathChecks.tagged(333, 8, 0, "boat-interaction");
                var target = new net.minecraft.class_113(mc.world, X + 3, Y, Z + 3);
                created.add(target);
                mc.world.method_210(target);
                if (occupied) {
                  var rider =
                      new net.minecraft.entity.player.PlayerEntity(mc.world) {
                        public void method_494() {}
                      };
                  created.add(rider);
                  rider.method_1376(target);
                }
                mc.field_2823 = new net.minecraft.class_27(target);
                String stack =
                    local.luke.power.death.DeathItems.encode(mc.player.inventory.main[0]);
                long count =
                    mc.world.field_198.stream()
                        .filter(e -> e instanceof net.minecraft.class_113 b && !b.dead)
                        .count();
                ((local.luke.power.validation.mixin.Client253Access) mc).power$use(1);
                check(
                    count
                        == mc.world.field_198.stream()
                            .filter(e -> e instanceof net.minecraft.class_113 b && !b.dead)
                            .count(),
                    "interacting placed another boat");
                check(
                    stack.equals(
                        local.luke.power.death.DeathItems.encode(mc.player.inventory.main[0])),
                    "held boat count or native data changed");
                if (!occupied)
                  check(mc.player.field_1595 == target, "native empty-boat mounting was lost");
                if (mc.player.field_1595 != null) mc.player.method_1376(null);
              });
    } finally {
      for (var entity : created) {
        if (entity.field_1595 != null) entity.method_1376(null);
        entity.markDead();
      }
      AccessRules.restore(mc, rules);
      mc.player.read(saved);
      mc.field_2823 = null;
    }
  }
}
