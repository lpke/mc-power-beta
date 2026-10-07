package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.util.*;
import local.luke.power.commands.api.*;
import local.luke.power.commands.util.*;
import local.luke.power.permissions.CommandPermissions;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * All mutations are confined to the disposable test world; restore its original state afterward.
 */
public final class ModernCommandChecks {
  private static List<String> run(Minecraft mc, String command) throws Exception {
    var output = ChatChecks.submit(mc, command);
    check(output.stream().noneMatch(s -> s.contains("§c")), command + ": " + output);
    return output;
  }

  private static long count(Minecraft mc, int id) {
    long count = 0;
    for (var stack : mc.player.inventory.main)
      if (stack != null && stack.itemId == id) count += stack.count;
    return count;
  }

  private static String items(Minecraft mc) throws Exception {
    var tag = new NbtCompound();
    mc.player.writeNbt(tag);
    var inventory = new NbtCompound();
    inventory.put("Inventory", tag.getList("Inventory"));
    var bytes = new java.io.ByteArrayOutputStream();
    net.minecraft.nbt.NbtIo.write(inventory, new java.io.DataOutputStream(bytes));
    return java.util.HexFormat.of().formatHex(bytes.toByteArray());
  }

  public static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && !mc.world.isRemote, "requires disposable singleplayer world");
    var policy = CommandPermissions.copy();
    var allowed = new CommandPermissions.Settings();
    for (var command : CommandPermissions.COMMANDS)
      allowed.rules.put(command.name(), CommandPermissions.Rule.ALLOWED);
    CommandPermissions.preview(allowed);
    var main = mc.player.inventory.main.clone();
    var armor = mc.player.inventory.armor.clone();
    var cursor = mc.player.inventory.getCursorStack();
    double x = mc.player.x, y = mc.player.y, z = mc.player.z;
    float yaw = mc.player.yaw, pitch = mc.player.pitch;
    int health = mc.player.health;
    int difficulty = mc.options.difficulty;
    long time = ((local.luke.power.world.WorldCycles)mc.world.method_262()).power$daylightTime();
    var properties = mc.world.method_262();
    boolean rain = properties.getRaining(), thunder = properties.getThundering();
    int rainTime = properties.getRainTime(), thunderTime = properties.getThunderTime();
    var worldSpawn = mc.world.getSpawnPos();
    var spawn = mc.player.method_505();
    var forced = (ForcedSpawn) mc.player;
    boolean forcedBefore = forced.power$forcedSpawn();
    float angleBefore = forced.power$spawnAngle();
    String warps = ((PlayerWarps) mc.player).spc$getWarpString();
    var entities = new HashSet<>(mc.world.field_198);
    Object hud = Class.forName("local.luke.power.hud.Config").getField("config").get(null);
    var scrolling = hud.getClass().getField("enableChatScroll");
    boolean scrollBefore = (boolean) scrolling.get(hud);
    var cheatWorld = (local.luke.power.permissions.CheatWorld) properties;
    boolean cheatsBefore = cheatWorld.power$cheatsEnabled();
    cheatWorld.power$cheatsEnabled(true);
    try {
      run(mc, "/gamemode creative");
      test(
          "tp accepts local name relative decimal local and facing coordinates",
          () -> {
            double baseY = mc.player.boundingBox.minY;
            var output = run(mc, "/tp " + mc.player.name + " ~1 ~2 ~3");
            check(output.size() == 1, "duplicate feedback");
            check(
                Math.abs(mc.player.x - x - 1) < .001
                    && Math.abs(mc.player.boundingBox.minY - baseY - 2) < .001,
                "relative teleport misplaced feet");
            run(mc, "/teleport @s 10.0 80 10.0 90 0");
            check(Math.abs(mc.player.boundingBox.minY - 80) < .001, "absolute height is not feet");
            run(mc, "/tp @s ^ ^ ^2");
            check(
                Math.abs(mc.player.x - 8) < .001 && Math.abs(mc.player.z - 10) < .001,
                "local direction wrong");
            run(mc, "/tp @s ~ ~ ~ facing 8.0 80 20.0");
            check(Math.abs(mc.player.yaw) < .001, "facing failed");
            run(mc, "/tp @s @s");
            run(mc, "/tp 8.0 80 10.0 facing entity @s");
            run(mc, "/tp 8.0 80 10.0 0 0");
          });
      test(
          "invalid teleports fail once without moving player",
          () -> {
            double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
            for (String command :
                List.of(
                    "/tp missing_player 0 80 0",
                    "/tp @s NaN 80 0",
                    "/tp @s ~ ~",
                    "/tp @s ^ ~ ^",
                    "/tp @s 30000000 80 0",
                    "/tp @s 0 Infinity 0",
                    "/tp @s ~ ~ ~ 1e100 0")) {
              var output = ChatChecks.submit(mc, command);
              check(output.size() == 1 && output.get(0).contains("§c"), command + output);
              check(
                  mc.player.x == px && mc.player.y == py && mc.player.z == pz,
                  "invalid command moved player");
            }
          });
      test(
          "give splits valid stacks and clear supports filtering limits and count only",
          () -> {
            Arrays.fill(mc.player.inventory.main, null);
            Arrays.fill(mc.player.inventory.armor, null);
            mc.player.inventory.setCursorStack(null);
            run(mc, "/give " + mc.player.name + " minecraft:stone 130");
            check(count(mc, 1) == 130, "give count differs");
            for (var stack : mc.player.inventory.main)
              if (stack != null) check(stack.count <= 64, "oversized stack");
            var result = run(mc, "/clear @s minecraft:stone 0");
            check(count(mc, 1) == 130 && result.get(0).contains("130"), "count-only removed items");
            run(mc, "/clear @s minecraft:stone 65");
            check(count(mc, 1) == 65, "maximum count ignored");
            run(mc, "/give @p minecraft:stone");
            check(count(mc, 1) == 66, "default count is not one");
            String before = items(mc);
            for (String command :
                List.of(
                    "/give @s -1 5",
                    "/give @s 1 -2",
                    "/give @s 1 99999999",
                    "/give nobody 1 2",
                    "/clear @s 1 -1")) {
              var error = ChatChecks.submit(mc, command);
              check(error.size() == 1 && error.get(0).contains("§c"), error.toString());
              check(items(mc).equals(before), "invalid inventory command changed data");
            }
            run(mc, "/give @s 35:2 3");
            run(mc, "/give @s 35:3 4");
            run(mc, "/clear @s 35:2");
            check(count(mc, 35) == 4, "metadata filter removed other colours");
            run(mc, "/clear");
            check(
                count(mc, 1) == 0 && count(mc, 35) == 0,
                "clear still clears chat instead of items");
          });
      test(
          "full inventories keep existing items and drop only the newly given overflow",
          () -> {
            for (int i = 0; i < mc.player.inventory.main.length; i++)
              mc.player.inventory.main[i] = new ItemStack(1, 64, 0);
            long before = count(mc, 1);
            var old = new HashSet<>(mc.world.field_198);
            run(mc, "/give @s minecraft:stone 2");
            check(count(mc, 1) == before, "existing items changed");
            long dropped = 0;
            for (Object value : mc.world.field_198)
              if (!old.contains(value) && value instanceof net.minecraft.class_142 item)
                dropped += item.field_564.count;
            check(dropped == 2, "overflow was lost or duplicated: " + dropped);
            Arrays.fill(mc.player.inventory.main, null);
          });
      test(
          "entity selectors summon teleport ride and kill act on the intended loaded entity",
          () -> {
            var old = new HashSet<>(mc.world.field_198);
            run(mc, "/summon minecraft:chicken ~2 ~ ~");
            Entity chicken =
                (Entity)
                    mc.world.field_198.stream()
                        .filter(e -> !old.contains(e))
                        .findFirst()
                        .orElseThrow();
            var source = new SharedCommandSource(mc.player);
            check(
                EntityTargets.resolve(
                            source, "@e[type=chicken,distance=..10,sort=nearest,limit=1]", false)
                        .get(0)
                    == chicken,
                "selector target differs");
            run(mc, "/tp " + chicken.id + " ~3 ~ ~");
            check(Math.abs(chicken.x - mc.player.x - 3) < .001, "entity teleport failed");
            run(mc, "/summon minecraft:boat ~ ~ ~2");
            Entity boat = EntityTargets.one(source, "@e[type=boat,sort=nearest,limit=1]");
            run(mc, "/ride @s mount " + boat.id);
            check(mc.player.field_1595 == boat, "mount failed");
            var blocked = ChatChecks.submit(mc, "/ride " + boat.id + " mount @s");
            check(blocked.size() == 1 && blocked.get(0).contains("§c"), "riding cycle accepted");
            run(mc, "/ride @s dismount");
            check(mc.player.field_1595 == null, "dismount failed");
            run(mc, "/kill " + chicken.id);
            check(
                ((net.minecraft.entity.LivingEntity) chicken).health == 0, "kill did not kill mob");
            int size = mc.world.field_198.size();
            ChatChecks.submit(mc, "/summon not_a_beta_entity");
            check(mc.world.field_198.size() == size, "unknown entity spawned");
          });
      test(
          "time weather difficulty and seed follow the documented syntax",
          () -> {
            run(mc, "/time set day");
            check(((local.luke.power.world.WorldCycles)mc.world.method_262()).power$daylightTime() == 1000, "day preset wrong");
            run(mc, "/time add 1s");
            check(((local.luke.power.world.WorldCycles)mc.world.method_262()).power$daylightTime() == 1020, "duration units wrong");
            check(run(mc, "/time query daytime").get(0).contains("1020"), "query wrong");
            ChatChecks.submit(mc, "/time set");
            check(((local.luke.power.world.WorldCycles)mc.world.method_262()).power$daylightTime() == 1020, "incomplete time changed world");
            run(mc, "/weather thunder 20t");
            check(
                properties.getThundering()
                    && properties.getRaining()
                    && properties.getRainTime() == 20,
                "thunder wrong");
            run(mc, "/weather clear 10s");
            check(
                !properties.getThundering()
                    && !properties.getRaining()
                    && properties.getRainTime() == 200,
                "clear wrong");
            run(mc, "/difficulty hard");
            check(mc.options.difficulty == 3, "difficulty failed");
            check(
                run(mc, "/seed").get(0).contains(Long.toString(mc.world.getSeed())), "seed wrong");
          });
      test(
          "help is coloured complete scroll-aware and emitted once without WE prefixes",
          () -> {
            scrolling.set(hud, true);
            var all = run(mc, "/help");
            check(all.size() > 20, "help still paginated");
            check(
                all.stream().filter(s -> s.contains("§b/tp§7")).count() == 1,
                "tp help missing or repeated");
            check(all.stream().anyMatch(s -> s.contains("/spawnpoint")), "new command missing");
            var detail = run(mc, "/help give");
            check(detail.stream().anyMatch(s -> s.contains("<targets>")), "give help stale");
            var we = run(mc, "//help");
            check(we.size() > 30, "WE help still paginated");
            check(we.stream().noneMatch(s -> s.contains("[WE]")), "WE help prefixes remain");
            check(
                we.stream().filter(s -> s.contains("World editing")).count() == 1,
                "WE help repeated");
            scrolling.set(hud, false);
            check(run(mc, "/help").size() == 7, "non-scrolling help not paginated");
            check(run(mc, "//help").size() == 6, "non-scrolling WE help not paginated");
            check(run(mc, "//help 6").get(0).contains("6/6"), "explicit help page lost");
            var invalid = ChatChecks.submit(mc, "/help -1");
            check(
                invalid.size() == 1 && invalid.get(0).contains("§c"),
                "invalid page printed unrelated help");
            for (String command :
                List.of(
                    "tp",
                    "teleport",
                    "give",
                    "clear",
                    "time",
                    "weather",
                    "kill",
                    "summon",
                    "ride",
                    "seed",
                    "difficulty",
                    "spawnpoint",
                    "setworldspawn",
                    "gamemode",
                    "warp")) run(mc, "/help " + command);
          });
      test(
          "master and aliases fail closed without changing world or inventory",
          () -> {
            allowed.enabled = false;
            CommandPermissions.preview(allowed);
            try {
              for (String command :
                  List.of(
                      "/tp @s 0 80 0",
                      "/teleport @s 0 80 0",
                      "/weather rain",
                      "/clear",
                      "/give @s 1 2",
                      "/spawnpoint",
                      "/setworldspawn",
                      "/difficulty easy")) {
                var output = ChatChecks.submit(mc, command);
                check(
                    output.size() == 1 && output.get(0).contains("disabled"),
                    "permission bypass: " + command + output);
              }
              run(mc, "/help tp");
              run(mc, "/seed");
            } finally {
              allowed.enabled = true;
              CommandPermissions.preview(allowed);
            }
          });
      test(
          "personal spawn persists through NBT and respawn preserves saved warps",
          () -> {
            run(mc, "/spawnpoint @s 8 80 10 90");
            check(((ForcedSpawn) mc.player).power$forcedSpawn(), "forced spawn missing");
            NbtCompound nbt = new NbtCompound();
            mc.player.writeNbt(nbt);
            ((ForcedSpawn) mc.player).power$forcedSpawn(false, 0);
            mc.player.readtNbt(nbt);
            check(
                ((ForcedSpawn) mc.player).power$forcedSpawn()
                    && ((ForcedSpawn) mc.player).power$spawnAngle() == 90,
                "forced spawn NBT lost");
            ((PlayerWarps) mc.player).spc$setWarpString("kept 1.25 80.0 3.5 ");
            mc.method_2122(false, 0);
            check(
                Math.abs(mc.player.x - 8.5) < .01
                    && Math.abs(mc.player.boundingBox.minY - 80.1) < .02,
                "respawn did not use command position");
            check(
                ((ForcedSpawn) mc.player).power$forcedSpawn() && mc.player.yaw == 90,
                "respawn state lost");
            check(
                ((PlayerWarps) mc.player).spc$getWarpString().equals("kept 1.25 80.0 3.5 "),
                "respawn erased warps");
            mc.player.method_506(spawn);
            check(
                !((ForcedSpawn) mc.player).power$forcedSpawn(),
                "bed spawn did not replace forced spawn");
            run(mc, "/setworldspawn 20 70 30");
            var p = mc.world.getSpawnPos();
            check(p.x == 20 && p.y == 70 && p.z == 30, "world spawn wrong");
          });
    } finally {
      if (mc.player.field_1595 != null) mc.player.method_1376(null);
      mc.player.inventory.main = main;
      mc.player.inventory.armor = armor;
      mc.player.inventory.setCursorStack(cursor);
      mc.player.method_1338(x, y, z, yaw, pitch);
      mc.player.health = health;
      mc.player.method_506(spawn);
      ((ForcedSpawn) mc.player).power$forcedSpawn(forcedBefore, angleBefore);
      ((PlayerWarps) mc.player).spc$setWarpString(warps);
      for (Object value : new ArrayList<>(mc.world.field_198))
        if (value instanceof Entity e && e != mc.player && !entities.contains(e)) e.markDead();
      ((local.luke.power.world.WorldCycles)mc.world.method_262()).power$daylightTime(time);
      mc.world.setSpawnPos(worldSpawn);
      mc.options.difficulty = difficulty;
      mc.options.save();
      properties.setRaining(rain);
      properties.setThundering(thunder);
      properties.setRainTime(rainTime);
      properties.getThunderTime(thunderTime);
      scrolling.set(hud, scrollBefore);
      CommandPermissions.preview(policy);
      cheatWorld.power$cheatsEnabled(cheatsBefore);
      mc.setScreen(null);
    }
    log("MODERN COMMAND FAILURES " + failures);
  }
}
