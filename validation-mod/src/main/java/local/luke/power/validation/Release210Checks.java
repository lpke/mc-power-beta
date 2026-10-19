package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.rules.*;
import local.luke.power.ui.*;
import local.luke.power.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;

/** Runtime checks use only disposable worlds and restore their prior settings. */
final class Release210Checks {
  private static Object creative(String name, Object... arguments) throws Exception {
    Class<?> api = Class.forName("local.luke.power.creative.Modes");
    return api.getMethod(name, Object.class).invoke(null, arguments);
  }
  private static void fly(String text) throws Exception {
    check((Boolean)Class.forName("local.luke.power.creative.command.CreativeCommands").getMethod("execute", String.class).invoke(null, text), "command not dispatched");
  }
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var rules = AccessRules.copy(mc);
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var playerData = new NbtCompound(); mc.player.write(playerData);
    var previousScreen = mc.currentScreen;
    try {
      mc.setScreen(null); DeathChecks.alive(mc);
      AccessRules.restore(mc, RuleSet.cheats());
      test("all editors use bounded sleep and drop sliders with the requested defaults", () -> {
        ConfigSession session = SettingsRegistry.open(mc);
        Setting sleep = find(session, "rules.world.players_sleeping_percentage");
        check(sleep.slider() && sleep.min == 0 && sleep.max == 100, "sleep slider");
        Setting drop = find(session, "rules.world.explosion_drop_chance");
        check(drop.slider() && drop.min == 0 && drop.max == 100 && drop.defaultValue.getAsInt() == 100, "drop slider");
        check(find(session, "rules.world.allow_sleep").defaultValue.getAsBoolean(), "sleep default");
        check(!find(session, "rules.world.lightning_strike_fire").defaultValue.getAsBoolean(), "lightning default");
        check(find(session, "power_controls:features.fastLeafDecay.enableFastLeafDecay").defaultValue.getAsBoolean(), "leaf default");
        for (Setting setting : session.settings()) check(!Set.of("power_controls:tweaks.disableSleeping", "power_environment:config.WEATHER_CONFIG.disableRain",
            "power_mechanics:config.EXPLOSION_AND_FIRE_CONFIG.chanceBlocksDropWhenExploded", "power_mechanics:config.EXPLOSION_AND_FIRE_CONFIG.disableGhastExplosionCausingFire",
            "power_mechanics:config.EXPLOSION_AND_FIRE_CONFIG.disableLightningStrikeCausingFire").contains(setting.id), "retired setting " + setting.id);
        for (Setting setting : session.settings()) if (setting.group.equals("Active tweaks HUD") && setting.kind == Setting.Kind.BOOLEAN && !setting.id.equals("activeTweaks.enabled"))
          check(setting.label.startsWith("Include "), "ambiguous HUD label " + setting.id);
        check(find(session, "native.fov").page.equals("General"), "FOV tab");
        check(find(session, "native.sound").label.equals("Game sounds"), "sound label");
        for (String id : List.of("gameplay.fastItemCollisions", "gameplay.mergeItems", "gameplay.mergeThreshold", "gameplay.limitTnt", "gameplay.tntPerTick", "gameplay.tntMillis")) {
          Setting setting = find(session, id); check(setting.page.equals("Video") && setting.group.equals("Performance"), "performance placement " + id);
        }
        session.discard();
      });
      test("HUD position groups follow their draft mode without losing values", () -> {
        ConfigSession session = SettingsRegistry.open(mc);
        Setting positioning = find(session, "power_hud:hudpositions.hudPositioningSystem");
        Map<String, JsonElement> values = new HashMap<>();
        session.settings().forEach(s -> values.put(s.id, s.value.deepCopy()));
        for (int selected : new int[]{1, 2, 0, 1}) {
          positioning.value = new JsonPrimitive(selected);
          for (Setting setting : session.settings()) {
            if (setting.id.contains(".SIMPLE_HUD_POSITION_CONFIG.")) check(SettingAccess.visible(session, setting) == (selected == 1), setting.id);
            if (setting.id.contains(".ADVANCED_HUD_POSITIONS_CONFIG.")) check(SettingAccess.visible(session, setting) == (selected == 2), setting.id);
            if (!setting.id.equals(positioning.id)) check(setting.value.equals(values.get(setting.id)), "draft changed " + setting.id);
          }
        }
        session.discard();
      });
      test("Creative /fly disables only flight and keeps inventory and protection", () -> {
        CreativeVehicleChecks.mode(mc, "CREATIVE"); AccessRules.set(mc, GameRule.ALLOW_FLIGHT, false);
        var stacks = mc.player.inventory.main.clone(); int health = mc.player.health;
        boolean before = (Boolean)creative("flightAvailable", mc.player);
        fly("/fly"); check((Boolean)creative("flightAvailable", mc.player) != before, "Creative toggle");
        check(((PlayerMode)mc.player).power$creative(), "mode changed");
        mc.player.damage(null, 2); check(mc.player.health == health, "Creative protection changed");
        check(Arrays.equals(stacks, mc.player.inventory.main), "inventory changed");
        fly("/fly"); check((Boolean)creative("flightAvailable", mc.player) == before, "second toggle");
      });
      test("survival /fly requires permission, persists native data and revokes safely", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); AccessRules.set(mc, GameRule.ALLOW_FLIGHT, false);
        fly("/fly help"); fly("/fly"); check(!(Boolean)creative("flightAvailable", mc.player), "unpermitted flight");
        AccessRules.set(mc, GameRule.ALLOW_FLIGHT, true); fly("/fly");
        check((Boolean)creative("flightAvailable", mc.player), "flight unavailable");
        check(!((PlayerMode)mc.player).power$creative(), "Survival became Creative");
        var saved = new NbtCompound(); mc.player.write(saved);
        check(saved.getInt("PowerBetaFlightOverride") == 1 && saved.getBoolean("Flying"), "flight not saved");
        mc.player.read(saved); check((Boolean)creative("flightAvailable", mc.player), "flight lost on reload");
        AccessRules.set(mc, GameRule.ALLOW_FLIGHT, false); mc.player.tick();
        check(!(Boolean)creative("flightAvailable", mc.player), "revocation failed");
        check(!(Boolean)mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player), "revoked flight retained");
        var revoked = new NbtCompound(); mc.player.write(revoked);
        check(revoked.getFloat("FallDistance") == 0, "unsafe fall distance");
      });
      test("survival flight moves the player while detached auto-walk owns movement", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); AccessRules.set(mc, GameRule.ALLOW_FLIGHT, true); fly("/fly");
        var source = MovementOwnership.class.getDeclaredField("autoWalkInput"); source.setAccessible(true);
        var previous = (java.util.function.DoubleSupplier)source.get(null);
        double x = mc.player.x, y = mc.player.y, z = mc.player.z;
        var screen = mc.currentScreen;
        try {
          mc.player.method_1340(42.5, 118, 42.5); mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
          MovementOwnership.registerAutoWalkInput(() -> 1);
          var travel = Class.forName("local.luke.power.creative.FlightController").getMethod("travel", net.minecraft.entity.player.PlayerEntity.class);
          double startX = mc.player.x, startZ = mc.player.z;
          for (int tick = 0; tick < 20; tick++) check((Boolean)travel.invoke(null, mc.player), "flight travel not owned");
          check(Math.hypot(mc.player.x - startX, mc.player.z - startZ) > 1, "flight did not displace player");
        } finally { MovementOwnership.registerAutoWalkInput(previous); mc.player.method_1340(x, y, z); mc.setScreen(screen); }
      });
      test("non-flight uses native gravity, jumping, walking and limb animation", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL");
        var controller = Class.forName("local.luke.power.creative.FlightController");
        var tick = controller.getMethod("tick", Minecraft.class);
        check((Boolean)Class.forName("local.luke.power.creative.ClientRuntime").getMethod("moving", Minecraft.class)
            .invoke(null, mc), "focus the disposable client before testing native movement");
        var flying = mc.player.getClass().getMethod("creative_setFlying", boolean.class);
        var jump = net.minecraft.entity.LivingEntity.class.getDeclaredMethod("method_944"); jump.setAccessible(true);
        for (String path : List.of("unavailable", "command off", "revoked", "creative off")) {
          CreativeVehicleChecks.mode(mc, "SURVIVAL");
          AccessRules.set(mc, GameRule.ALLOW_FLIGHT, false);
          if (!path.equals("unavailable")) {
            AccessRules.set(mc, GameRule.ALLOW_FLIGHT, true); fly("/fly");
            if (path.equals("command off")) fly("/fly");
            else if (path.equals("revoked")) { AccessRules.set(mc, GameRule.ALLOW_FLIGHT, false); mc.player.tick(); }
            else {
              CreativeVehicleChecks.mode(mc, "CREATIVE");
              if ((Boolean)creative("flightAvailable", mc.player)) fly("/fly");
            }
          }
          // Native player ticks apply permission revocation before measuring travel.
          mc.player.tick();
          check(!(Boolean)mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player), path + " retained flight state");
          check(!(Boolean)Class.forName("local.luke.power.creative.Modes")
              .getMethod("flying", net.minecraft.entity.player.PlayerEntity.class).invoke(null, mc.player), path + " still flying");
          mc.player.method_1340(42.5, 120, 42.5); mc.player.field_1623 = false;
          mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
          double startY = mc.player.y;
          for (int i = 0; i < 8; i++) { tick.invoke(null, mc); mc.player.method_945(0, 0); }
          check(mc.player.velocityY < -.5 && startY - mc.player.y > 2, path + " native falling suppressed");
          double falling = mc.player.velocityY;
          flying.invoke(mc.player, false);
          check(mc.player.velocityY == falling, path + " redundant flight reset changed gravity");
          if (path.equals("command off")) {
            var fallingData = new NbtCompound(); mc.player.write(fallingData);
            check(fallingData.getFloat("FallDistance") > 2, "native fall distance suppressed");
            mc.player.read(fallingData);
            var reloaded = new NbtCompound(); mc.player.write(reloaded);
            check(reloaded.getFloat("FallDistance") == fallingData.getFloat("FallDistance"), "reload cleared fall distance");
          }
          mc.player.method_1340(42.5, 120, 42.5); mc.player.field_1623 = true;
          mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
          jump.invoke(mc.player); startY = mc.player.y;
          for (int i = 0; i < 5; i++) { tick.invoke(null, mc); mc.player.method_945(0, 0); }
          check(mc.player.y - startY > 1, path + " native jump suppressed");
          for (int x = 39; x < 48; x++) for (int z = 39; z < 60; z++) {
            mc.world.method_200(x, 109, z, 1);
            for (int y = 110; y < 124; y++) mc.world.method_200(x, y, z, 0);
          }
          mc.player.method_1340(42.5, 110 + mc.player.field_1631, 42.5);
          mc.player.field_1623 = true; mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
          double startZ = mc.player.z; float limb = mc.player.field_1050;
          for (int i = 0; i < 20; i++) { tick.invoke(null, mc); mc.player.method_945(0, 1); }
          check(Math.hypot(mc.player.x - 42.5, mc.player.z - startZ) > 2, path + " walking did not displace player");
          check(mc.player.field_1050 > limb, path + " native limb animation stopped");
        }
        CreativeVehicleChecks.mode(mc, "SURVIVAL");
        CreativeVehicleChecks.mode(mc, "CREATIVE");
        check((Boolean)creative("flightAvailable", mc.player), "Creative entry did not restore flight ability");
      });
      test("Spectator ignores /fly and help has no side effects", () -> {
        CreativeVehicleChecks.mode(mc, "SPECTATOR");
        fly("/fly"); fly("/fly help");
        check(((PlayerMode)mc.player).power$spectator(), "Spectator changed");
        check((Boolean)mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player), "Spectator flight stopped");
      });
      test("sleep toggle overrides every percentage while zero still needs a sleeper", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL");
        var sleep = (local.luke.power.validation.mixin.PlayerSleepAccess)mc.player;
        try {
          for (boolean allowed : new boolean[]{true, false}) for (int percentage : new int[]{0, 50, 100}) {
            AccessRules.set(mc, GameRule.ALLOW_SLEEP, allowed);
            WorldRules.of(mc.world.method_262()).set(GameRule.PLAYERS_SLEEPING_PERCENTAGE, percentage);
            sleep.power$sleeping(false); sleep.power$sleepTimer(0); check(!mc.world.method_268(), "skipped without sleeper");
            sleep.power$sleeping(true); sleep.power$sleepTimer(100); check(mc.world.method_268() == allowed, "sleep permission/percentage");
          }
        } finally { sleep.power$sleeping(false); sleep.power$sleepTimer(0); }
      });
      test("disabled sleeping keeps native bed respawn without entering sleep", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); AccessRules.set(mc, GameRule.ALLOW_SLEEP, false);
        var bed = Arrays.stream(net.minecraft.block.Block.BLOCKS).filter(b -> b instanceof net.minecraft.class_485).findFirst().orElseThrow();
        int x = 44, y = 110, z = 44;
        mc.world.method_200(x, y - 1, z, 1); mc.world.method_154(x, y, z, bed.id, 8);
        mc.player.method_506(null); bed.method_1608(mc.world, x, y, z, mc.player);
        var spawn = mc.player.method_505();
        check(spawn != null && spawn.x == x && spawn.y == y && spawn.z == z, "bed did not retain respawning");
        check(!mc.player.method_943(), "disabled bed entered sleeping");
      });
      test("lightning rule changes native ignition in constructors and later ticks", () -> {
        CreativeVehicleChecks.mode(mc, "CREATIVE"); AccessRules.set(mc, GameRule.FIRE_SPREAD, true);
        int difficulty = mc.world.field_213; mc.world.field_213 = 3;
        try {
          // Native lightning requires its entire ten-block neighbourhood to be loaded.
          for (int x = 2; x <= 3; x++) for (int z = 2; z <= 3; z++) mc.world.method_214(x,z);
          check(mc.world.method_153(42,110,42,10), "lightning fixture chunks are not loaded");
          for (boolean enabled : new boolean[]{false, true}) {
            for (int x = 38; x <= 46; x++) for (int z = 38; z <= 46; z++) {
              mc.world.method_200(x, 109, z, 1); for (int y = 110; y <= 114; y++) mc.world.method_200(x, y, z, 0);
            }
            AccessRules.set(mc, GameRule.LIGHTNING_STRIKE_FIRE, enabled);
            var lightning = new net.minecraft.class_411(mc.world, 42, 110, 42);
            check((mc.world.getBlockId(42, 110, 42) == 51) == enabled, "constructor ignition " + enabled);
            for (int tick = 0; tick < 15; tick++) lightning.tick();
            if (!enabled) for (int x = 38; x <= 46; x++) for (int z = 38; z <= 46; z++)
              check(mc.world.getBlockId(x, 110, z) != 51, "tick ignited while disabled");
          }
        } finally { mc.world.field_213 = difficulty; }
      });
      test("ghast explosion ignition follows its rule independently of block griefing", () -> {
        CreativeVehicleChecks.mode(mc, "CREATIVE"); AccessRules.set(mc, GameRule.GHAST_GRIEFING, false);
        for (boolean enabled : new boolean[]{false, true}) {
          AccessRules.set(mc, GameRule.GHAST_EXPLOSION_FIRE, enabled);
          for (int x = 40; x <= 46; x++) for (int z = 40; z <= 46; z++) mc.world.method_200(x, 109, z, 1);
          var fireball = new net.minecraft.class_372(mc.world, 43.5, 112, 43.5, 0, -1, 0);
          fireball.velocityY = -1;
          ExplosionProbe.calls = 0;
          for (int tick = 0; tick < 10 && ExplosionProbe.calls == 0; tick++) fireball.tick();
          check(ExplosionProbe.calls > 0, "fireball did not collide");
          check(ExplosionProbe.fire == enabled, "ghast fire flag " + enabled);
          fireball.markDead();
        }
      });
      test("explosion block-drop endpoints keep native destruction and item counts", () -> {
        for (int percentage : new int[]{0, 100}) {
          WorldRules.of(mc.world.method_262()).set(GameRule.EXPLOSION_DROP_CHANCE, percentage);
          var before = new HashSet<>(mc.world.method_291());
          mc.world.method_200(44, 111, 44, 4);
          var explosion = new net.minecraft.class_60(mc.world, null, 44.5, 111.5, 44.5, 2);
          explosion.field_1397.add(new net.minecraft.class_339(44, 111, 44)); explosion.method_1196(false);
          int count = 0;
          for (Object value : mc.world.method_291()) if (!before.contains(value) && value instanceof net.minecraft.class_142 item && !item.dead) {
            if (item.field_564.itemId == 4) count += item.field_564.count;
            item.markDead();
          }
          check(mc.world.getBlockId(44, 111, 44) == 0, "drop chance changed destruction");
          check(count == (percentage == 0 ? 0 : 1), "drop count " + percentage + ": " + count);
        }
      });
      test("official panorama faces decode and load through the native texture manager", () -> {
        check(PanoramaCatalog.entries().size() == 15, "unique panorama choices");
        check(PanoramaCatalog.folder("b1_7_3").isEmpty(), "Beta background");
        for (var entry : PanoramaCatalog.entries()) if (!entry.folder().isEmpty()) for (int face = 0; face < 6; face++) {
          String path = "/assets/power_controls/textures/" + entry.folder() + "/panorama" + face + ".png";
          try (var in = Release210Checks.class.getResourceAsStream(path)) {
            check(in != null, "missing " + path); var image = javax.imageio.ImageIO.read(in);
            check(image != null && image.getWidth() >= 256 && image.getHeight() == image.getWidth(), "bad panorama face");
          }
          check(mc.textureManager.getTextureId(path) > 0, "texture upload failed");
        }
      });
      test("options and panorama picker render at narrow and wide GUI sizes", () -> {
        Files.createDirectories(Path.of("power-beta-data/reports/210"));
        for (int[] size : new int[][]{{320,240},{640,420}}) {
          var options = new PowerOptionsScreen(null); options.init(mc, size[0], size[1]);
          SettingsSnapshot.render(mc, options, size[0], size[1], Path.of("power-beta-data/reports/210/options-" + size[0] + ".png"));
          Setting panorama = find(options.session(), "power_controls:userinterface.panoramaConfig.panoramaFolder");
          var picker = new PanoramaScreen(options, panorama); picker.init(mc, size[0], size[1]);
          SettingsSnapshot.render(mc, picker, size[0], size[1], Path.of("power-beta-data/reports/210/panoramas-" + size[0] + ".png"));
          options.session().discard();
        }
      });
      test("panorama clicks keep the picker open; Back keeps the immediate selection", () -> {
        boolean auto = MenuPreferences.current().autoApply;
        MenuPreferences.update(v -> v.autoApply = false);
        try {
          var options = new PowerOptionsScreen(null); mc.setScreen(options);
          Setting setting = find(options.session(), "power_controls:userinterface.panoramaConfig.panoramaFolder");
          JsonElement original = setting.value.deepCopy();
          var picker = new PanoramaScreen(options, setting); mc.setScreen(picker);
          selectPanorama(picker, "26_3");
          check(mc.currentScreen == picker && setting.value.getAsString().equals("26_3"), "selection closed the picker");
          picker = new PanoramaScreen(options, setting); mc.setScreen(picker);
          ((local.luke.power.validation.mixin.ScreenInput)(Object)picker).power$click(picker.width / 2, picker.height - 20, 0);
          check(mc.currentScreen == options && setting.value.getAsString().equals("26_3"), "Back changed selection");
          options.session().discard(); check(setting.value.equals(original), "discard lost original panorama");
          MenuPreferences.update(v -> v.autoApply = true);
          picker = new PanoramaScreen(options, setting); mc.setScreen(picker);
          selectPanorama(picker, "26_2");
          check(mc.currentScreen == picker && !setting.changed(), "auto-apply selection was not committed");
          check(local.luke.power.storage.PowerConfig.section("power_controls:userinterface").getAsJsonObject("panoramaConfig")
              .get("panoramaFolder").getAsString().equals("26_2"), "panorama not applied on click");
          setting.value = original; options.changed(setting);
        } finally { MenuPreferences.update(v -> v.autoApply = auto); }
      });
      test("title styles render official logos and retain retro customization", () -> {
        ConfigSession session = SettingsRegistry.open(mc);
        Setting style = find(session, "logo.style");
        check(style.kind == Setting.Kind.CHOICE && style.choices.size() == 5 && style.defaultValue.getAsInt() == 0, "style choices/default");
        var retro = local.luke.power.storage.PowerConfig.section("title");
        for (int selected = 0; selected < 5; selected++) {
          style.value = new JsonPrimitive(selected); session.save(Path.of("."));
          var title = new net.minecraft.client.gui.screen.TitleScreen();
          title.init(mc, 320, 240);
          SettingsSnapshot.render(mc, title, 320, 240, Path.of("power-beta-data/reports/210/title-" + selected + ".png"));
        }
        style.value = new JsonPrimitive(0); session.save(Path.of("."));
        var after = local.luke.power.storage.PowerConfig.section("title"); after.remove("style"); retro.remove("style");
        check(after.equals(retro), "title style changed custom lettering");
      });
    } finally {
      AccessRules.restore(mc, rules); mc.player.read(playerData); CreativeVehicleChecks.mode(mc, mode); mc.setScreen(previousScreen);
    }
    log("RELEASE 210 FAILURES " + failures);
  }
}
