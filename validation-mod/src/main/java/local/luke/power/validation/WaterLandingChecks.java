package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/** Actual collision, gravity and bucket use in a disposable world. */
final class WaterLandingChecks {
  private static final String ID = "power_mechanics:config.MOB_CONFIG.waterNegatesFallDamage";
  private static final int FLOOR = 40;
  private static Minecraft client;

  static void run(Minecraft mc, String action) throws Exception {
    client = mc;
    if (action.equals("saved")) {
      check(enabled() && find(SettingsRegistry.open(mc), ID).value.getAsBoolean(), "setting lost on restart");
      check(PowerConfig.section("power_mechanics:config").getAsJsonObject("MOB_CONFIG")
          .get("waterNegatesFallDamage").getAsBoolean(), "stored setting lost");
      log("PASS water landings survived restart"); return;
    }
    failures = 0;
    mc.setScreen(null); mc.player.closeScreen();
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
    CreativeVehicleChecks.mode(mc, "SURVIVAL");
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(false);
    mc.field_2807 = mc.player;
    for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
      mc.world.method_200(x, FLOOR, z, 1);
      for (int y = FLOOR + 1; y <= 125; y++) mc.world.method_200(x, y, z, 0);
    }
    test("default off, Gameplay / Fall damage, available without cheats; Cancel restores preview", () -> {
      configure(false);
      var session = SettingsRegistry.open(mc); var row = find(session, ID);
      check(!row.defaultValue.getAsBoolean() && !row.restart && row.page.equals("Gameplay")
          && row.group.equals("Fall damage") && SettingAccess.visible(session, row), "wrong settings metadata/access");
      byte[] before = Files.readAllBytes(PowerConfig.path()); int health = mc.player.health;
      row.parse("true"); session.preview(true); check(enabled(), "live preview missing");
      session.discard(); check(!enabled(), "Cancel failed");
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())) && mc.player.health == health,
          "settings edit changed saved data or health");
    });
    test("one-block water keeps Beta damage while off and cancels it immediately when on", () -> {
      water(mc, 0); configure(false); impact(mc, mc.player, .5, FLOOR + 3.01, -4);
      check(mc.player.health < 20, "Beta fall damage changed with option off");
      configure(true); impact(mc, mc.player, .5, FLOOR + 3.01, -4);
      check(mc.player.health == 20 && distance(mc.player) == 0, "one-block landing damaged player");
      configure(false); impact(mc, mc.player, .5, FLOOR + 3.01, -4);
      check(mc.player.health < 20, "turning off failed to restore Beta damage");
    });
    test("all source, flowing and falling water levels protect players and mobs", () -> {
      configure(true); LivingEntity pig = new net.minecraft.class_443(mc.world);
      for (int metadata = 0; metadata < 16; metadata++) {
        water(mc, metadata);
        for (LivingEntity entity : new LivingEntity[] {mc.player, pig}) {
          impact(mc, entity, .5, FLOOR + 3.01, -4);
          check(entity.health == 20 && distance(entity) == 0,
              "water level " + metadata + " damaged " + entity.getClass().getSimpleName());
        }
      }
    });
    test("actual gravity landings from varied heights and tick phases take no damage", () -> {
      configure(true);
      for (int metadata : new int[] {0, 3, 7, 8}) for (double height : new double[] {8.01, 17.35, 26.9, 55.01, 80.75}) {
        water(mc, metadata); reset(mc.player, .5, FLOOR + height); distance(mc.player, 0);
        for (int tick = 0; tick < 150 && !mc.player.field_1623; tick++) {
          mc.player.baseTick(); mc.player.method_945(0, 0);
        }
        check(mc.player.field_1623 && mc.player.health == 20,
            "gravity landing failed: water=" + metadata + " height=" + height + " health=" + mc.player.health);
      }
    });
    test("real water bucket use immediately before impact saves a survival player", () -> {
      configure(true); mc.world.method_200(0, FLOOR + 1, 0, 0);
      reset(mc.player, .5, FLOOR + 3.01);
      mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 90;
      mc.player.prevX = mc.player.x; mc.player.prevY = mc.player.y; mc.player.prevZ = mc.player.z;
      ItemStack empty = new ItemStack(326, 1, 0).method_698(mc.world, mc.player);
      check(empty.itemId == 325 && (mc.world.getBlockId(0, FLOOR + 1, 0) == 8
          || mc.world.getBlockId(0, FLOOR + 1, 0) == 9), "bucket did not place water");
      mc.player.move(0, -4, 0);
      check(mc.player.health == 20 && distance(mc.player) == 0, "bucket clutch damaged player");
    });
    test("edge overlap works; exact dry boundary, nearby water and landing before contact do not", () -> {
      configure(true); water(mc, 7);
      impact(mc, mc.player, 1.1, FLOOR + 3.01, -4);
      check(mc.player.health == 20, "overlapping water edge failed");
      impact(mc, mc.player, 1.3, FLOOR + 3.01, -4);
      check(mc.player.health < 20, "exact dry boundary counted as contact");
      impact(mc, mc.player, 1.5, FLOOR + 3.01, -4);
      check(mc.player.health < 20, "nearby water gave immunity");
      mc.world.method_200(0, FLOOR + 2, 0, 1);
      impact(mc, mc.player, .5, FLOOR + 5.01, -4);
      check(mc.player.health < 20, "water underneath a solid block gave immunity");
      mc.world.method_200(0, FLOOR + 2, 0, 0);
    });
    test("fast movement through suspended thin water resets accumulated distance", () -> {
      configure(true); mc.world.method_200(0, FLOOR + 1, 0, 0);
      mc.world.method_154(0, FLOOR + 8, 0, 8, 7);
      reset(mc.player, .5, FLOOR + 10.1); distance(mc.player, 50); mc.player.move(0, -3.9, 0);
      check(mc.player.health == 20 && Math.abs(distance(mc.player) - 3.9) < .001, "fast pass missed water");
      mc.player.method_1340(.5, FLOOR + 4 + mc.player.field_1631, .5);
      distance(mc.player, 50);
      var fall = Entity.class.getDeclaredMethod("method_1374", double.class, boolean.class); fall.setAccessible(true);
      fall.invoke(mc.player, 0D, false);
      check(distance(mc.player) == 50, "a later fall update reused old movement through water");
      mc.world.method_200(0, FLOOR + 8, 0, 0);
      reset(mc.player, .5, FLOOR + 10.1); distance(mc.player, 50); mc.player.move(0, -3.9, 0);
      check(Math.abs(distance(mc.player) - 53.9) < .001, "dry movement reset distance");
    });
    test("lava, dry falls and other damage retain their normal behavior", () -> {
      configure(true); mc.world.method_200(0, FLOOR + 1, 0, 11);
      impact(mc, mc.player, .5, FLOOR + 3.01, -4); check(mc.player.health < 20, "lava cancelled fall damage");
      water(mc, 0); reset(mc.player, .5, FLOOR + 1); mc.player.damage(null, 2);
      check(mc.player.health == 18, "water cancelled an attack");
      mc.world.method_200(0, FLOOR + 1, 0, 0);
      impact(mc, mc.player, .5, FLOOR + 3.01, -4); check(mc.player.health < 20, "dry fall gave immunity");
    });
    test("a floating boat keeps its rider out of water until the boat is submerged", () -> {
      configure(true); water(mc, 0);
      Entity boat = new net.minecraft.class_113(mc.world, .5, FLOOR + 2, .5);
      try {
        mc.player.field_1595 = boat;
        boat.method_1340(.5, FLOOR + 1.8 + boat.field_1631, .5);
        reset(mc.player, .5, FLOOR + 3.01); mc.player.move(0, -1.5, 0);
        check(Math.abs(distance(mc.player) - 7.5) < .001, "floating boat rider counted as swimming");
        boat.method_1340(.5, FLOOR + 1.01 + boat.field_1631, .5);
        reset(mc.player, .5, FLOOR + 3.01); mc.player.move(0, -1.5, 0);
        check(Math.abs(distance(mc.player) - 1.5) < .001, "submerged boat prevented water contact");
      } finally { mc.player.field_1595 = null; }
    });
    test("client water preference cannot alter multiplayer fall distance", () -> {
      configure(true); water(mc, 0); reset(mc.player, .5, FLOOR + 3.01); distance(mc.player, 6);
      boolean remote = mc.world.isRemote;
      try {
        mc.world.isRemote = true; mc.player.move(0, -1.5, 0);
        check(distance(mc.player) > 6, "client changed server-owned fall distance");
      } finally { mc.world.isRemote = remote; }
    });
    test("Apply persists the option for restart", () -> {
      configure(false); var session = SettingsRegistry.open(mc); find(session, ID).parse("true");
      session.save(Path.of(".").toAbsolutePath());
      check(enabled() && PowerConfig.section("power_mechanics:config").getAsJsonObject("MOB_CONFIG")
          .get("waterNegatesFallDamage").getAsBoolean(), "Apply did not persist");
    });
    reset(mc.player, 2.5, FLOOR + 1); distance(mc.player, 0);
    log("WATER LANDING FAILURES " + failures);
  }

  private static Field preference() throws Exception {
    Object config = Class.forName("local.luke.power.mechanics.Config").getField("config").get(null);
    Object mobs = config.getClass().getField("MOB_CONFIG").get(config);
    return mobs.getClass().getField("waterNegatesFallDamage");
  }
  private static Object mobs() throws Exception {
    Object config = Class.forName("local.luke.power.mechanics.Config").getField("config").get(null);
    return config.getClass().getField("MOB_CONFIG").get(config);
  }
  private static boolean enabled() throws Exception { return (Boolean)preference().get(mobs()); }
  private static void configure(boolean enabled) throws Exception {
    var session = SettingsRegistry.open(client);
    find(session, ID).parse(Boolean.toString(enabled)); session.preview(true);
  }
  private static float distance(Entity entity) throws Exception {
    Field field = Entity.class.getDeclaredField("field_1636"); field.setAccessible(true); return field.getFloat(entity);
  }
  private static void distance(Entity entity, float value) throws Exception {
    Field field = Entity.class.getDeclaredField("field_1636"); field.setAccessible(true); field.setFloat(entity, value);
  }
  private static void water(Minecraft mc, int metadata) {
    mc.world.method_154(0, FLOOR + 1, 0, metadata == 0 ? 9 : 8, metadata);
  }
  private static void reset(LivingEntity entity, double x, double feet) throws Exception {
    entity.health = 20; entity.dead = false; entity.fire = 0; entity.field_1613 = 0; entity.hurtTime = 0; entity.field_1041 = 0;
    entity.field_1623 = false; entity.velocityX = entity.velocityY = entity.velocityZ = 0;
    entity.method_1340(x, feet + entity.field_1631, .5); distance(entity, 6);
  }
  private static void impact(Minecraft mc, LivingEntity entity, double x, double feet, double dy) throws Exception {
    reset(entity, x, feet); entity.move(0, dy, 0);
  }
}
