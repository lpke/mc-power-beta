package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.commands.api.*;
import local.luke.power.commands.util.*;
import local.luke.power.death.*;
import local.luke.power.input.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.MenuPreferences;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;

/** Live command origins and their deliberately physical or literal exceptions. */
final class YCommand448Checks {
  private YCommand448Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.dimensionId == 0 && !DetachedCamera.isActive()
        && !WorldEditor.editing() && mc.player.field_1595 == null,
        "open an idle, unmounted disposable singleplayer Overworld outside freecam");
    check(org.lwjgl.opengl.Display.isActive() && mc.options.musicVolume == 0 && mc.options.soundVolume == 0,
        "focus and mute the disposable client");
    failures = 0;
    var owner = mc.player; var camera = mc.field_2807;
    var rules = AccessRules.copy(mc);
    Object editor = field(WorldEditor.class, "editor").get(null), world = field(WorldEditor.class, "world").get(null);
    Object feetPreview = field(MenuPreferences.class, "feetYPreview").get(null);
    Object creativeSettings = Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null);
    Field flightPreference = creativeSettings.getClass().getField("flight");
    boolean flight = flightPreference.getBoolean(creativeSettings);
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var probe = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
    int x = ((int)Math.floor(owner.x) >> 4) * 16 + 8;
    int z = ((int)Math.floor(owner.z) >> 4) * 16 + 8;
    check(mc.world.method_239(x, 100, z), "command fixture needs an already loaded chunk");
    try (var detached = new StaticFields(DetachedCamera.class)) {
      mc.player = probe; mc.field_2807 = probe;
      // Mode entry normally restores Creative flight through the settings writer.
      // Supply that preference in memory so this fixture never queues a save.
      flightPreference.setBoolean(creativeSettings, true);
      AccessRules.restore(mc, RuleSet.cheats());
      field(WorldEditor.class, "editor").set(null, null); field(WorldEditor.class, "world").set(null, null);
      for (String mode : List.of("SURVIVAL", "CREATIVE", "SPECTATOR")) {
        CreativeVehicleChecks.mode(mc, mode);
        for (boolean feet : new boolean[]{false, true}) {
          MenuPreferences.previewFeetY(feet);
          for (String motion : List.of("ground", "falling", "flying")) {
            final String label = mode + " feet=" + feet + " " + motion;
            test("YCOMMAND448 reported parsers, selectors and WorldEdit " + label, () -> {
              DetachedCamera.register(() -> false);
              reset(probe, x, z, motion);
              double expected = mode.equals("SPECTATOR")
                  ? probe.y - probe.field_1631 + HeadGeometry.BASE_ABOVE_FEET
                  : feet ? probe.boundingBox.minY : probe.y;
              origins(mc, probe.x, expected, probe.z);
            });
            test("YCOMMAND448 relative teleport uses reported origin " + label, () -> {
              DetachedCamera.register(() -> false); reset(probe, x, z, motion);
              double expected = mode.equals("SPECTATOR")
                  ? probe.y - probe.field_1631 + HeadGeometry.BASE_ABOVE_FEET
                  : feet ? probe.boundingBox.minY : probe.y;
              command(mc, "tp ~ ~2 ~");
              near(probe.boundingBox.minY, expected + 2, "relative teleport destination feet");
            });
          }
          test("YCOMMAND448 detached commands use head-base for " + mode + " feet=" + feet, () -> {
            reset(probe, x, z, "falling");
            DetachedCamera.register(() -> true, () -> x + 2.25, () -> 104.05,
                () -> z + 3.75, () -> 0, () -> 180);
            origins(mc, x + 2.25, HeadGeometry.baseY(104.05), z + 3.75);
            command(mc, "tp ~ ~ ~");
            near(probe.boundingBox.minY, HeadGeometry.baseY(104.05), "detached relative teleport feet");
          });
        }
      }
      DetachedCamera.register(() -> false); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      for (boolean feet : new boolean[]{false, true}) {
        MenuPreferences.previewFeetY(feet);
        test("YCOMMAND448 explicit world coordinates retain literal Y feet=" + feet, () -> {
          reset(probe, x, z, "ground");
          var absolute = new PosParse(probe, 0, new String[]{"12", "83", "-17"});
          near(absolute.y, 83, "literal PosParse Y");
          var spawn = SpawnPosition.parse(probe, new String[]{"12", "83", "-17"}, 0);
          check(spawn.y == 83, "literal spawn Y changed");
          command(mc, "tp " + (x + .5) + " 83 " + (z + .5));
          near(probe.boundingBox.minY, 83, "literal teleport feet");
          selection(mc, "//pos1 " + x + ",83," + z);
          check(WorldEditor.editor(mc).pos1.equals(new Pos(x, 83, z)), "literal selection reprojected Y");
        });
        test("YCOMMAND448 entity teleport and navigation retain body feet=" + feet, () -> {
          reset(probe, x, z, "ground"); double bodyY = probe.boundingBox.minY;
          command(mc, "tp @s @s"); near(probe.boundingBox.minY, bodyY, "entity destination body feet");
          var navigation = new NavigationActions(mc, WorldEditor.editor(mc));
          Method method = NavigationActions.class.getDeclaredMethod("feet"); method.setAccessible(true);
          Pos origin = (Pos)method.invoke(navigation);
          check(origin.y() == (int)Math.floor(bodyY + .00001), "navigation shifted with report preference");
          var other = new ClientPlayerEntity(mc, mc.world, mc.session, probe.dimensionId);
          reset(other, x, z, "ground");
          near(CommandPosition.of(other).y(), other.boundingBox.minY, "other player command fallback feet");
        });
        test("YCOMMAND448 stored death coordinates and legacy warps retain literal Y feet=" + feet, () -> {
          var record = new DeathRecord(); record.id = "448"; record.x = -12; record.y = 83;
          record.z = 17; record.dimension = 0; record.cause = "coordinate fixture";
          Method describe = DeathCommands.class.getDeclaredMethod("describe", DeathRecord.class); describe.setAccessible(true);
          check(describe.invoke(null, record).toString().contains("-12, 83, 17"), "death report reprojected stored feet");
          String legacy = "home:12.25,83.75,-17.5;opaque legacy payload";
          ((PlayerWarps)probe).spc$setWarpString(legacy);
          var tag = new NbtCompound(); probe.write(tag);
          check(tag.getString("warps").equals(legacy), "warp save rewrote legacy coordinates");
          probe.read(tag);
          check(((PlayerWarps)probe).spc$getWarpString().equals(legacy), "warp load rewrote legacy coordinates");
        });
      }
      test("YCOMMAND448 preserves durable preferences", () ->
          check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "command fixture wrote preferences"));
    } finally {
      mc.player = owner; mc.field_2807 = camera; AccessRules.restore(mc, rules);
      field(WorldEditor.class, "editor").set(null, editor); field(WorldEditor.class, "world").set(null, world);
      field(MenuPreferences.class, "feetYPreview").set(null, feetPreview);
      flightPreference.setBoolean(creativeSettings, flight);
    }
    log("YCOMMAND448 FAILURES " + failures);
    check(failures == 0, "Y command 448 failures=" + failures);
  }

  private static void origins(Minecraft mc, double x, double y, double z) throws Exception {
    var p = mc.player; double bodyY = p.boundingBox.minY;
    var pose = CommandPosition.of(p);
    near(pose.x(), x, "command X"); near(pose.y(), y, "command Y"); near(pose.z(), z, "command Z");
    var plain = new PosParse(p); near(plain.y, y, "default PosParse Y");
    var relative = new PosParse(p, 0, new String[]{"~", "~2", "~"});
    check(relative.valid, "relative PosParse rejected"); near(relative.y, y + 2, "relative PosParse Y");
    var local = CommandNumbers.position(pose.x(), pose.y(), pose.z(), 0, 0,
        new String[]{"^", "^2", "^"}, 0);
    near(local[1], y + 2, "local coordinate Y");
    if (p.dimensionId == 0) {
      check(SpawnPosition.parse(p, new String[0], 0).y == (int)Math.floor(y), "default spawn ignored reported Y");
      check(SpawnPosition.parse(p, new String[]{"~", "~2", "~"}, 0).y == (int)Math.floor(y + 2),
          "relative spawn ignored reported Y");
    }
    var source = new SharedCommandSource(p);
    double distance = Math.sqrt(Math.pow(p.x - x, 2) + Math.pow(bodyY - y, 2) + Math.pow(p.z - z, 2));
    String filter = "@s[distance=" + Math.max(0, distance - .00001) + ".." + (distance + .00001) + "]";
    check(EntityTargets.resolve(source, filter, false).equals(List.of(p)), "selector origin ignored reported pose");
    check(EntityTargets.resolve(source, "@s[x=" + p.x + ",y=" + bodyY + ",z=" + p.z + ",distance=0]", false)
        .equals(List.of(p)), "explicit selector body origin changed");
    selection(mc, "//pos1");
    var editor = WorldEditor.editor(mc);
    Pos origin = new Pos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
    check(editor.pos1.equals(origin), "default WorldEdit selection ignored reported pose");
    selection(mc, "//pos2 ~,~2,~");
    check(editor.pos2.equals(origin.add(0, 2, 0)), "relative WorldEdit selection ignored reported pose");
    // Copy reads one existing block and changes only this isolated editor's clipboard.
    editor.pos1 = editor.pos2 = new Pos((int)Math.floor(p.x), 100, (int)Math.floor(p.z));
    selection(mc, "//copy");
    for (int ticks = 0; editor.engine.busy() && ticks < 8; ticks++) editor.engine.tick(64);
    check(!editor.engine.busy() && origin.equals(editor.clipboardOrigin), "clipboard anchor ignored reported pose");
    near(p.boundingBox.minY, bodyY, "report/query/selection/copy moved physical feet");
  }

  private static void reset(ClientPlayerEntity p, int x, int z, String motion) throws Exception {
    p.spacingXZ = .6f; p.spacingY = 1.8f; p.field_1631 = 1.62f; p.field_1640 = 0;
    p.method_1341(x + .5, 100.25, z + .5, 0, 0);
    // Flight override avoids changing the user's persistent flight preference.
    Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_flightOverride", int.class)
        .invoke(p, motion.equals("flying") ? 1 : 0);
    Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer")
        .getMethod("creative_setFlying", boolean.class).invoke(p, motion.equals("flying"));
    p.velocityX = p.velocityZ = 0; p.velocityY = motion.equals("falling") ? -.5 : 0;
    p.field_1623 = motion.equals("ground");
    if (motion.equals("flying")) check(((PlayerMode)p).power$flying(), "fixture failed to enter flight");
  }

  private static void command(Minecraft mc, String text) throws Exception {
    var output = new ArrayList<String>();
    var source = new SharedCommandSource(mc.player) {
      @Override public void sendFeedback(String line) { output.add(line); }
    };
    output.addAll(ChatChecks.record(() -> check(RetroChatUtil.handleCommand(source, text, true), "unhandled " + text)));
    check(output.stream().noneMatch(line -> line.startsWith("§c")), text + " failed: " + output);
  }

  private static void selection(Minecraft mc, String text) throws Exception {
    var output = ChatChecks.record(() -> check(WorldEditor.command(mc, text), "unhandled " + text));
    check(output.stream().noneMatch(line -> line.startsWith("§c")), text + " failed: " + output);
  }

  private static void near(double actual, double expected, String label) {
    check(Math.abs(actual - expected) < 2e-6, label + ": " + actual + " expected " + expected);
  }

  private static Field field(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
  }

  private static final class StaticFields implements AutoCloseable {
    final Map<Field, Object> values = new LinkedHashMap<>();
    StaticFields(Class<?> type) throws Exception {
      for (Field field : type.getDeclaredFields()) if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
        field.setAccessible(true); values.put(field, field.get(null));
      }
    }
    public void close() throws Exception { for (var entry : values.entrySet()) entry.getKey().set(null, entry.getValue()); }
  }
}
