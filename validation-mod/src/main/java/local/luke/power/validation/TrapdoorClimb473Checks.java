package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** The installed 26.3 OPEN + ladder-below + equal-FACING predicate, using native Beta movement. */
final class TrapdoorClimb473Checks {
  private TrapdoorClimb473Checks() {}
  static void run(Minecraft mc, String action) throws Exception {
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "focus and mute a disposable client");
    if (action.equals("capture")) { capture(mc); return; }
    check(action.equals("native") && mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.field_1595 == null,
        "use trapdoor-climb-473 native in a disposable singleplayer world outside vehicles");
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path());
    var old = GameplayConfig.copy(); var p = mc.player; var input = p.field_161; var previous = mc.currentScreen;
    var saved = new net.minecraft.nbt.NbtCompound(); p.write(saved);
    String mode = ((local.luke.power.input.PlayerMode)p).power$spectator() ? "SPECTATOR"
        : ((local.luke.power.input.PlayerMode)p).power$creative() ? "CREATIVE" : "SURVIVAL";
    int x = (int)Math.floor(p.x) + 5, y = 112, z = (int)Math.floor(p.z) + 5;
    check(mc.world.method_155(x - 2, y - 2, z - 2, x + 2, y + 4, z + 2), "load the fixture area first");
    List<int[]> blocks = new ArrayList<>();
    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -2; dy <= 4; dy++) {
      check(mc.world.method_1777(x + dx, y + dy, z + dz) == null, "fixture overlaps a block entity");
      blocks.add(new int[]{x + dx, y + dy, z + dz, mc.world.getBlockId(x + dx, y + dy, z + dz), mc.world.method_1778(x + dx, y + dy, z + dz)});
    }
    int predicates = 0;
    try {
      mc.setScreen(null); CreativeVehicleChecks.mode(mc, "SURVIVAL"); p.field_161 = new net.minecraft.class_41();
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[1] == y - 2 ? 1 : 0, 0);
      for (int dy = -1; dy <= 2; dy++) for (int[] side : new int[][]{{0,1},{0,-1},{1,0},{-1,0}}) mc.world.method_154(x + side[0], y + dy, z + side[1], 1, 0);
      LivingEntity mob = new LivingEntity(mc.world) {};
      for (boolean enabled : new boolean[]{false, true}) {
        var settings = GameplayConfig.copy(); settings.modernTrapdoorClimbing = enabled; GameplayConfig.preview(settings);
        for (int trap = 0; trap < 8; trap++) for (int ladder = 0; ladder < 8; ladder++) {
          mc.world.method_154(x, y, z, 96, trap); mc.world.method_154(x, y - 1, z, 65, ladder);
          p.method_1341(x + .5, y + .25, z + .5, 0, 0); mob.method_1340(x + .5, y + .25, z + .5);
          boolean expected = enabled && (trap & 4) != 0 && ladder == (trap & 3) + 2;
          check(p.method_932() == expected && mob.method_932() == expected, "climb predicate enabled=" + enabled + " trap=" + trap + " ladder=" + ladder);
          predicates++;
        }
        mc.world.method_154(x, y, z, 65, 2); p.method_1341(x + .5, y + .25, z + .5, 0, 0);
        check(p.method_932(), "toggle changed ordinary ladders");
      }
      var enabled = GameplayConfig.copy(); enabled.modernTrapdoorClimbing = true; GameplayConfig.preview(enabled);
      mc.world.method_154(x, y, z, 96, 4);
      for (int below : new int[]{0, 1, 96}) {
        mc.world.method_154(x, y - 1, z, below, 4); check(!p.method_932(), "non-ladder below became climbable");
      }
      mc.world.method_154(x, y - 1, z, 65, 2);
      mc.world.isRemote = true;
      try { check(!p.method_932(), "multiplayer acquired a local climb rule"); } finally { mc.world.isRemote = false; }
      for (int orientation = 0; orientation < 4; orientation++) {
        mc.world.method_154(x, y, z, 96, orientation | 4); mc.world.method_154(x, y - 1, z, 65, orientation + 2);
        double[] trap = fall(mc, x, y, z, false), sneak = fall(mc, x, y, z, true);
        mc.world.method_154(x, y, z, 65, orientation + 2);
        double[] ladder = fall(mc, x, y, z, false), ladderSneak = fall(mc, x, y, z, true);
        check(Math.abs(trap[0] - ladder[0]) < 1e-9 && Math.abs(trap[1] - ladder[1]) < 1e-9, "trapdoor changed ladder fall limits");
        check(Math.abs(sneak[0] - ladderSneak[0]) < 1e-9 && Math.abs(sneak[1] - ladderSneak[1]) < 1e-9, "trapdoor changed native sneak holding");
        mc.world.method_154(x, y, z, 96, orientation | 4);
        float yaw = new float[]{0, 180, -90, 90}[orientation];
        p.field_161.field_2536 = false; p.method_1341(x + .5, y + .05, z + .5, yaw, 0); p.velocityX = p.velocityY = p.velocityZ = 0;
        double feet = p.boundingBox.minY;
        for (int tick = 0; tick < 8; tick++) p.method_945(0, 1);
        check(p.boundingBox.minY - feet > .4, "native collision climbing did not rise at orientation " + orientation);
        log("TRAPDOOR473 native climb orientation=" + orientation + " rise=" + (p.boundingBox.minY - feet));
      }
      boolean[] placement = new boolean[6];
      for (boolean choice : new boolean[]{false, true}) {
        var settings = GameplayConfig.copy(); settings.modernTrapdoorClimbing = choice; GameplayConfig.preview(settings);
        for (int face = 0; face < 6; face++) {
          boolean allowed = Block.field_1913.method_1561(mc.world, x, y, z, face);
          // Existing unsupported-placement preferences remain authoritative.
          if (!choice) placement[face] = allowed;
          else check(allowed == placement[face], "climbing changed existing placement eligibility on face " + face);
        }
        for (int face = 2; face < 6; face++) {
          Block.field_1913.method_1606(mc.world, x, y, z, face);
          check(mc.world.method_1778(x, y, z) == face - 2, "native placement orientation changed");
        }
      }
      log("TRAPDOOR473 predicates=" + predicates + " movement/placement PASS");
    } finally {
      mc.world.isRemote = false; GameplayConfig.preview(old); p.field_161 = input;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      p.read(saved); CreativeVehicleChecks.mode(mc, mode); mc.setScreen(previous);
    }
    check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "trapdoor fixture changed saved settings");
  }
  private static double[] fall(Minecraft mc, int x, int y, int z, boolean sneak) {
    var p = mc.player; p.field_161.field_2536 = sneak; p.method_1341(x + .5, y + .6, z + .5, 0, 0);
    p.velocityX = p.velocityZ = 0; p.velocityY = -1; double feet = p.boundingBox.minY;
    p.method_945(0, 0); return new double[]{p.boundingBox.minY - feet, p.velocityY};
  }
  private static void capture(Minecraft mc) throws Exception {
    var previous = mc.currentScreen; var options = new PowerOptionsScreen(previous); mc.setScreen(options);
    int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    Path report = Path.of("power-beta-data/reports/trapdoor-climb-473"); Files.createDirectories(report);
    try {
      for (int[] size : new int[][]{{320, 240}, {854, 480}}) {
        options.init(mc, size[0], size[1]); UiChecks.field(options, "page", "Gameplay"); UiChecks.search(options, "Modern trapdoor climbing");
        check(find(options.session(), "gameplay.modernTrapdoorClimbing").page.equals("Gameplay"), "missing Gameplay setting");
        boolean visible = false;
        for (Object row : (List<?>)UiChecks.field(options, "rows")) {
          var setting = (Setting)UiChecks.call(row, "setting");
          visible |= setting != null && setting.id.equals("gameplay.modernTrapdoorClimbing");
        }
        check(visible, "trapdoor setting missing from search results");
        SettingsSnapshot.render(mc, options, size[0], size[1], report.resolve("setting-" + size[0] + ".png"));
      }
      for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, "gameplay.modernTrapdoorClimbing", new com.google.gson.JsonPrimitive(false)).getAsBoolean() == (base == DefaultBases.Base.POWER_BETA), "wrong trapdoor default for " + base);
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(matrix); mc.setScreen(previous);
    }
    log("TRAPDOOR473 settings/defaults capture PASS");
  }
}
