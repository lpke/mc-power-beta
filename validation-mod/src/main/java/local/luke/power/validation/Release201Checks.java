package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.hud.*;
import local.luke.power.input.*;
import local.luke.power.rules.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.block.Block;
import net.minecraft.class_180;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

/** Patch defaults, native damage/trampling, camera eligibility and physical-input setup. */
final class Release201Checks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("keys")) {
      mc.setScreen(null); FreecamPreviewChecks.active(false); DeathChecks.alive(mc);
      AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, false);
      AccessRules.set(mc, GameRule.ALLOW_SPECTATOR_MODE, true);
      CreativeVehicleChecks.mode(mc, "SURVIVAL");
      for (var key : mc.options.allKeys) switch (key.translationKey) {
        case "powerbeta.infoHud", "power_creative.picker" -> key.code = Keyboard.KEY_F4;
        case "power_creative.modifier", "key.power_controls.debug_hud" -> key.code = Keyboard.KEY_F3;
      }
      Bindings.configure(Map.of("powerbeta.debugGraph", Chord.CTRL)); Bindings.register(mc.options.allKeys);
      var hud = InfoHudConfig.copy(); hud.enabled = false; InfoHudConfig.preview(hud);
      mc.options.debugHud = mc.options.hideHud = false;
      return;
    }
    if (action.equals("export-icons")) { exportIcons(mc); return; }
    if (action.equals("state")) {
      JsonObject state = new JsonObject();
      state.addProperty("hud", InfoHudConfig.current().enabled);
      state.addProperty("debug", mc.options.debugHud);
      state.addProperty("screen", mc.currentScreen == null ? "" : mc.currentScreen.getClass().getName());
      state.addProperty("mode", mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString());
      state.addProperty("front", CameraView.front());
      Files.writeString(Path.of("power-beta-201-state.json"), state.toString());
      return;
    }
    if (!action.equals("check")) throw new IllegalArgumentException(action);
    failures = 0;
    Files.createDirectories(Path.of("power-beta-data/reports"));
    defaults(mc);
    creation(mc);
    farmland(mc);
    migration(mc);
    defuse(mc);
    camera(mc);
    log("RELEASE 201 FAILURES " + failures);
  }

  /** Offline asset capture only; the pack loads the finished PNGs as ordinary textures. */
  private static void exportIcons(Minecraft mc) throws Exception {
    org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glPushMatrix();
    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW); org.lwjgl.opengl.GL11.glPushMatrix();
    try {
      org.lwjgl.opengl.GL11.glViewport(0, 0, 48, 48);
      org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
      org.lwjgl.opengl.GL11.glClearColor(0, 0, 0, 0);
      org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glLoadIdentity();
      org.lwjgl.opengl.GL11.glOrtho(0, 16, 16, 0, 1000, 3000);
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW); org.lwjgl.opengl.GL11.glLoadIdentity();
      org.lwjgl.opengl.GL11.glTranslatef(0, 0, -2000);
      org.lwjgl.opengl.GL11.glPushMatrix(); org.lwjgl.opengl.GL11.glRotatef(120, 1, 0, 0);
      net.minecraft.class_583.method_1930(); org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
      org.lwjgl.opengl.GL11.glPopMatrix();
      new net.minecraft.client.render.item.ItemRenderer().method_1487(mc.textRenderer, mc.textureManager, new ItemStack(2, 1, 0), 0, 0);
      var pixels = org.lwjgl.BufferUtils.createByteBuffer(48 * 48 * 4);
      org.lwjgl.opengl.GL11.glReadPixels(0, 0, 48, 48, org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, pixels);
      var grass = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < 48; y++) for (int x = 0; x < 48; x++) {
        int at = ((47 - y) * 48 + x) * 4;
        int rgb = (pixels.get(at) & 255) << 16 | (pixels.get(at + 1) & 255) << 8 | pixels.get(at + 2) & 255;
        grass.setRGB(x, y, rgb == 0 ? 0 : 0xff000000 | rgb);
      }
      try (var stream = mc.getClass().getResourceAsStream("/assets/power_creative/textures/ender_eye.png")) {
        var eye = javax.imageio.ImageIO.read(stream);
        for (var entry : Map.of("disabled_creative", grass, "disabled_spectator", eye).entrySet()) {
          var source = entry.getValue();
          var image = new java.awt.image.BufferedImage(source.getWidth(), source.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
          for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int c = source.getRGB(x, y), gray = Math.round(((c >> 16) & 255) * .299f + ((c >> 8) & 255) * .587f + (c & 255) * .114f);
            image.setRGB(x, y, (c & 0xff000000) | gray << 16 | gray << 8 | gray);
          }
          javax.imageio.ImageIO.write(image, "png", Path.of(entry.getKey() + ".png").toFile());
        }
      }
    } finally {
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW); org.lwjgl.opengl.GL11.glPopMatrix();
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glPopMatrix();
      org.lwjgl.opengl.GL11.glPopAttrib();
    }
  }

  private static void defaults(Minecraft mc) throws Exception {
    var session = SettingsRegistry.open(mc);
    test("retired settings and bindings are absent", () -> {
      for (var setting : session.settings()) check(!setting.id.contains("betterBurning")
          && !Set.of("power_controls:features.burpingSounds", "power_controls:features.chestSounds",
              "power_controls:oldfeatures.punchTntToIgnite", "visual.spectatorBlockOutlines",
              "gameplay.lavaProofItems", "gameplay.cactusProofItems", "tweaks.flexible.enabled",
              "keys.Flexible placement (toggle)", "power_mechanics:config.MOB_CONFIG.disableTrampleFarmlandWhenBarefoot",
              "power_mechanics:config.MOB_CONFIG.disableTrampleFarmlandWithLeatherBoots").contains(setting.id), "retired " + setting.id);
      check(Arrays.stream(mc.options.allKeys).noneMatch(k -> k.translationKey.equals("Flexible placement (toggle)")), "retired placement key");
      check(mc.getClass().getClassLoader().getResource("assets/power_controls/stationapi/sounds/sound/random/burp.ogg") == null, "burp asset remains");
    });
    test("chest choice and percentage editor use one value each", () -> {
      var chest = find(session, "power_controls:features.moreSoundsChests");
      check(chest.label.equals("Chest sounds") && chest.choices.equals(List.of("Off", "Modern", "Door")), "chest choices " + chest.choices);
      check(chest.defaultValue.getAsInt() == 0, "chests default on");
      var drops = find(session, "rules.world.explosion_drop_chance");
      check(drops.slider() && drops.defaultValue.getAsDouble() == 100, "drop defaults/control");
      for (int percent : new int[]{0, 1, 37, 99, 100}) {
        drops.parse(Integer.toString(percent));
        check(Math.abs(drops.value.getAsDouble() - percent) < 1e-8, "percent changed storage units");
        check(drops.display().equals(Integer.toString(percent)) && drops.rangeText().equals("0 to 100 %"), "percent display " + drops.display());
      }
      drops.slide(.37); check(Math.abs(drops.value.getAsDouble() - 37) < 1e-8, "percent slider step");
      try { drops.parse("101"); throw new AssertionError("accepted >100%"); } catch (IllegalArgumentException expected) {}
    });
    test("chat, custom HUD and placement keys reset to approved defaults", () -> {
      check(find(session, "keys.key.chat").defaultValue.getAsInt() == Keyboard.KEY_RETURN, "chat reset key");
      check(find(session, "keys.powerbeta.infoHud").defaultValue.getAsInt() == Keyboard.KEY_F4, "HUD reset key");
      for (String name : List.of("offset", "adjacent", "rotation", "reverse", "into face"))
        check(find(session, "keys.Placement " + name + " (hold)").defaultValue.getAsInt() == 0, "bound placement default");
      for (int i = 0; i < 4; i++) for (String key : List.of("enabled", "coordinates", "direction", "biome", "timeOfDay", "day", "chunk", "fps"))
        check(find(session, "infoHud.clusters." + i + "." + key).defaultValue.getAsBoolean()
            == (i == 0 && Set.of("enabled", "coordinates", "direction").contains(key)), "HUD default " + i + "/" + key);
    });
    session.discard();
  }

  private static void creation(Minecraft mc) throws Exception {
    test("creation buttons remain below inputs at narrow and wide GUI sizes", () -> {
      var previous = mc.currentScreen;
      try {
        var screen = new class_180(null);
        for (int[] size : new int[][]{{320, 240}, {427, 300}, {640, 420}}) {
          screen.init(mc, size[0], size[1]);
          var buttons = ((ScreenInput)screen).power$buttons();
          for (var button : buttons) {
            check(button.y >= 156 && button.y + ((local.luke.power.validation.mixin.ButtonBounds) button).power$height() <= 236, "button left input area " + button.id);
            check(button.x >= 0 && button.x + ((local.luke.power.validation.mixin.ButtonBounds) button).power$width() <= size[0], "button outside GUI");
          }
          SettingsSnapshot.render(mc, screen, size[0], size[1], Path.of("power-beta-data/reports/create-201-" + size[0] + ".png"));
        }
      } finally { mc.setScreen(previous); }
    });
  }

  private static void farmland(Minecraft mc) throws Exception {
    var before = AccessRules.copy(mc); var boots = mc.player.inventory.armor[0];
    boolean remote = mc.world.isRemote;
    var pig = new net.minecraft.class_443(mc.world);
    try {
      for (boolean playerRule : new boolean[]{false, true}) for (boolean mobRule : new boolean[]{false, true})
        for (boolean serverOwned : new boolean[]{false, true}) {
          final boolean player = playerRule, mob = mobRule, multiplayer = serverOwned;
          test("trampling player=" + player + " mob=" + mob + " server=" + multiplayer, () -> {
            AccessRules.set(mc, GameRule.PLAYER_CROP_TRAMPLING, player);
            AccessRules.set(mc, GameRule.MOB_CROP_TRAMPLING, mob);
            mc.world.isRemote = multiplayer;
            for (int footwear : new int[]{0, 301, 313}) {
              mc.player.inventory.armor[0] = footwear == 0 ? null : new ItemStack(footwear, 1, 0);
              trample(mc, mc.player, player || multiplayer);
            }
            trample(mc, pig, mob || multiplayer);
          });
        }
    } finally {
      mc.world.isRemote = remote; AccessRules.restore(mc, before); mc.player.inventory.armor[0] = boots;
      mc.world.method_229(44, 109, 44, 0); pig.markDead();
    }
  }
  private static void trample(Minecraft mc, Entity entity, boolean allowed) {
    mc.world.method_154(44, 109, 44, 60, 7);
    for (int n = 0; n < 200; n++) Block.BLOCKS[60].method_1560(mc.world, 44, 109, 44, entity);
    check(mc.world.getBlockId(44, 109, 44) == (allowed ? 3 : 60), "trampling differs for " + entity.getClass().getSimpleName());
  }

  private static void migration(Minecraft mc) throws Exception {
    var original = local.luke.power.storage.PowerConfig.document();
    try {
      var edited = original.deepCopy();
      edited.add("worldRuleUpgrade", JsonParser.parseString("{\"gameplay\":{\"lavaProofItems\":true}}"));
      local.luke.power.storage.PowerConfig.write(edited);
      test("format-one native worlds retain explicit rules, old trampling and frozen item protection", () -> {
        var data = new net.minecraft.nbt.NbtCompound();
        RuleSet previous = new RuleSet(); previous.set(GameRule.KEEP_INVENTORY, true);
        local.luke.power.world.WorldRuleData.write(data, previous);
        var saved = data.getCompound(local.luke.power.world.WorldRuleData.TAG); saved.putInt("version", 1);
        saved.values().removeIf(entry -> Set.of("player_crop_trampling", "lava_destroys_items", "cactus_destroys_items")
            .contains(((net.minecraft.nbt.NbtElement)entry).getKey()));
        var converted = local.luke.power.world.WorldRuleData.read(data);
        check(converted.enabled(GameRule.KEEP_INVENTORY) && !converted.enabled(GameRule.MOB_CROP_TRAMPLING)
            && converted.enabled(GameRule.PLAYER_CROP_TRAMPLING) && !converted.enabled(GameRule.LAVA_DESTROYS_ITEMS)
            && converted.enabled(GameRule.CACTUS_DESTROYS_ITEMS), "converted effective behavior differs");
        var properties = new net.minecraft.world.WorldProperties(data);
        var dimension = new net.minecraft.world.WorldProperties(properties);
        check(WorldRules.of(dimension).equals(converted), "converted dimension rules lost");
        var roundTrip = new net.minecraft.world.WorldProperties(properties.asNbt());
        check(WorldRules.of(roundTrip).equals(converted), "converted rules lost after native save/reload");
        Path game = Path.of(".").toAbsolutePath().normalize(), world = game.resolve("saves/Rule migration fixture");
        Files.createDirectories(world);
        var root = new net.minecraft.nbt.NbtCompound(); root.put("Data", data);
        Path file = world.resolve("level.dat");
        try (var output = Files.newOutputStream(file)) { net.minecraft.nbt.NbtIo.writeCompressed(root, output); }
        byte[] bytes = Files.readAllBytes(file);
        local.luke.power.world.WorldRuleFiles.beforeOpen(game, "Rule migration fixture");
        check(Arrays.equals(bytes, Files.readAllBytes(file)), "pre-open validation modified original bytes");
        try (var files = Files.walk(game.resolve("power-beta-data/settings-backups"))) {
          check(files.filter(p -> p.getFileName().toString().startsWith("file-"))
              .anyMatch(p -> { try { return Arrays.equals(bytes, Files.readAllBytes(p)); } catch (Exception e) { return false; } }),
              "format-one metadata backup missing");
        }
      });
      test("pre-rule all-entity crop protection converts to both independent rules", () -> {
        var config = local.luke.power.storage.PowerConfig.document();
        config.add("legacyWorldRules", JsonParser.parseString("{\"power_mechanics:config\":{\"MOB_CONFIG\":{\"disableTramplingFarmland\":true}}}"));
        local.luke.power.storage.PowerConfig.write(config);
        var rules = local.luke.power.world.WorldRuleData.read(new net.minecraft.nbt.NbtCompound());
        check(!rules.enabled(GameRule.PLAYER_CROP_TRAMPLING) && !rules.enabled(GameRule.MOB_CROP_TRAMPLING), "old whole-world protection changed");
      });
    } finally { local.luke.power.storage.PowerConfig.write(original); }
  }

  private static void defuse(Minecraft mc) throws Exception {
    test("punch-to-defuse removes one primed TNT and drops exactly one item", () -> {
      var before = new ArrayList<>(mc.world.method_291());
      var tnt = new net.minecraft.class_365(mc.world, 44, 112, 44);
      try {
        int fuse = tnt.field_1390;
        var pig = new net.minecraft.class_443(mc.world);
        tnt.damage(pig, 1); pig.markDead();
        check(!tnt.dead && tnt.field_1390 == fuse, "non-player hit defused TNT");
        tnt.damage(mc.player, 1); tnt.damage(mc.player, 1);
        check(tnt.dead, "player did not defuse TNT");
        int dropped = 0;
        for (Object value : mc.world.method_291()) if (!before.contains(value) && value instanceof net.minecraft.class_142 item && item.field_564.itemId == 46)
          dropped += item.field_564.count;
        check(dropped == 1, "defuse duplicated or lost TNT: " + dropped);
      } finally {
        tnt.markDead();
        for (Object value : new ArrayList<>(mc.world.method_291())) if (!before.contains(value)) ((Entity)value).markDead();
      }
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void camera(Minecraft mc) throws Exception {
    var rules = AccessRules.copy(mc);
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    Object options = Class.forName("local.luke.power.controls.ControlFeatures").getField("USER_INTERFACE_CONFIG").get(null);
    var style = options.getClass().getField("frontViewThirdPerson"); Object oldStyle = style.get(options);
    var front = Class.forName("local.luke.power.controls.util.ModOptions").getField("frontView"); boolean oldFront = front.getBoolean(null);
    Object camera = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    var hide = camera.getClass().getField("hideHudOnEntry"); boolean oldHide = (Boolean) hide.get(camera);
    boolean hud = mc.options.hideHud, third = mc.options.thirdPerson;
    try {
      AccessRules.of(mc.world.method_262()).allAllowed(true);
      for (Object value : style.getType().getEnumConstants()) for (String active : List.of("SURVIVAL", "CREATIVE", "SPECTATOR"))
        for (boolean hidden : new boolean[]{false, true}) for (boolean entryHide : new boolean[]{false, true}) {
          test("front style=" + value + " mode=" + active + " HUD=" + hidden + " entry=" + entryHide, () -> {
            FreecamPreviewChecks.active(false); CreativeVehicleChecks.mode(mc, active);
            style.set(options, value); hide.set(camera, entryHide);
            front.setBoolean(null, true); mc.options.thirdPerson = true; mc.options.hideHud = hidden;
            boolean expected = !active.equals("SPECTATOR") && (value.toString().equals("Normal")
                || value.toString().equals("Hide HUD") && hidden);
            check(CameraView.front() == expected, "normal camera eligibility " + value + "/" + CameraView.front());
            FreecamPreviewChecks.active(true);
            check(DetachedCamera.isActive() && !CameraView.front(), "freecam entered front view");
            check(mc.options.hideHud == (entryHide || hidden), "freecam changed entry HUD preference");
            mc.options.hideHud = !mc.options.hideHud;
            check(!CameraView.front(), "F1 changed freecam perspective");
            FreecamPreviewChecks.active(false);
            check(mc.options.hideHud == hidden, "freecam did not restore HUD");
          });
        }
    } finally {
      FreecamPreviewChecks.active(false); style.set(options, oldStyle); front.setBoolean(null, oldFront);
      hide.set(camera, oldHide); mc.options.hideHud = hud; mc.options.thirdPerson = third;
      CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules);
    }
  }
}
