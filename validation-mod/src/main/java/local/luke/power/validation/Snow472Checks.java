package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.gameplay.*;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native snow placement and bucket-ray comparisons in a disposable world. */
final class Snow472Checks {
  private Snow472Checks() {}
  static void run(Minecraft mc, String phase) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute disposable client");
    if (phase.equals("capture")) { capture(mc); return; }
    check(phase.equals("native"), "expected capture or native"); failures = 0;
    var settings = GameplayConfig.copy(); var main = mc.player.inventory.main; int selected = mc.player.inventory.selectedSlot;
    double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
    float yaw = mc.player.yaw, pitch = mc.player.pitch;
    int x = (int)Math.floor(px) + 6, y = Math.max(108, Math.min(120, (int)Math.floor(py) + 6)), z = (int)Math.floor(pz) + 6;
    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 3; dy++)
      check(mc.world.getBlockId(x + dx, y + dy, z + dz) == 0, "snow fixture needs an empty elevated area");
    try {
      mc.player.inventory.main = new ItemStack[36]; mc.player.inventory.selectedSlot = 0;
      var enabled = GameplayConfig.copy(); enabled.modernSnowPlacement = true; GameplayConfig.preview(enabled);
      test("SNOW472 block items replace clicked snow without adding layers", () -> {
        for (boolean fix : new boolean[]{false, true}) for (int metadata : new int[]{0, 3, 7}) for (int face = 0; face < 6; face++) {
          var choice = GameplayConfig.copy(); choice.modernSnowPlacement = fix; GameplayConfig.preview(choice);
          reset(mc, x, y, z); snow(mc, x, y, z, metadata);
          ItemStack held = DeathChecks.tagged(4, 5, 0, "native-snow-placement");
          String identity = DeathItems.encode(held); mc.player.inventory.main[0] = held;
          check(place(mc, held, x, y, z, face), "snow replacement refused on face " + face);
          check(mc.world.getBlockId(x, y, z) == 4 && held.count == 4, "snow replacement/count");
          held.count = 5; check(identity.equals(DeathItems.encode(held)), "native damage/NBT changed");
        }
        GameplayConfig.preview(enabled);
      });
      test("SNOW472 replacement preserves a torch's clicked face", () -> {
        reset(mc, x, y, z); snow(mc, x, y, z, 0); mc.world.method_154(x - 1, y, z, 1, 0);
        var held = new ItemStack(50, 5, 0); check(place(mc, held, x, y, z, 5), "torch placement refused");
        check(mc.world.getBlockId(x, y, z) == 50 && mc.world.method_1778(x, y, z) == 1, "torch ignored clicked east face");
      });
      test("SNOW472 snow items never gain stacking or layering", () -> {
        for (boolean fix : new boolean[]{false, true}) {
          var choice = GameplayConfig.copy(); choice.modernSnowPlacement = fix; GameplayConfig.preview(choice);
          reset(mc, x, y, z); snow(mc, x, y, z, 0); var held = new ItemStack(78, 5, 0);
          place(mc, held, x, y, z, 1);
          check(mc.world.getBlockId(x, y, z) == 78 && mc.world.method_1778(x, y, z) == 0
              && held.count == 5 && mc.world.getBlockId(x, y + 1, z) == 0, "snow layering or extra consumption introduced");
        }
        GameplayConfig.preview(enabled);
      });
      test("SNOW472 buckets replace clicked snow only with the fix enabled", () -> {
        for (boolean fix : new boolean[]{false, true}) for (int filled : new int[]{326, 327}) {
          var choice = GameplayConfig.copy(); choice.modernSnowPlacement = fix; GameplayConfig.preview(choice);
          reset(mc, x, y, z); snow(mc, x, y, z, 0);
          mc.player.method_1340(x + .5, y + 3, z + .5); mc.player.prevX = mc.player.x; mc.player.prevY = mc.player.y; mc.player.prevZ = mc.player.z;
          mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 90;
          var held = new ItemStack(filled, 1, 0); mc.player.inventory.main[0] = held;
          ItemStack returned = held.method_698(mc.world, mc.player);
          check(returned != null && returned.itemId == 325 && returned.count == 1, "native bucket did not return one empty");
          int targetY = fix ? y : y + 1, block = mc.world.getBlockId(x, targetY, z);
          check(block == (filled == 326 ? 8 : 10) || block == (filled == 326 ? 9 : 11), "wrong bucket target for fix=" + fix);
          check(fix ? mc.world.getBlockId(x, y + 1, z) == 0 : mc.world.getBlockId(x, y, z) == 78, "bucket changed both snow and adjacent block");
        }
        GameplayConfig.preview(enabled);
      });
      test("SNOW472 Nether water keeps native evaporation and never pre-deletes snow", () -> {
        boolean hell = mc.world.dimension.field_2176;
        try {
          reset(mc, x, y, z); snow(mc, x, y, z, 0); mc.world.dimension.field_2176 = true;
          mc.player.method_1340(x + .5, y + 3, z + .5); mc.player.prevX = mc.player.x; mc.player.prevY = mc.player.y; mc.player.prevZ = mc.player.z;
          mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 90;
          var held = new ItemStack(326, 1, 0); mc.player.inventory.main[0] = held;
          var returned = held.method_698(mc.world, mc.player);
          check(returned.itemId == 325 && mc.world.getBlockId(x, y, z) == 78
              && mc.world.getBlockId(x, y + 1, z) == 0, "Nether water bypassed evaporation or deleted snow");
        } finally { mc.world.dimension.field_2176 = hell; }
      });
    } finally {
      reset(mc, x, y, z);
      for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) mc.world.method_154(x + dx, y - 1, z + dz, 0, 0);
      GameplayConfig.preview(settings); mc.player.inventory.main = main; mc.player.inventory.selectedSlot = selected;
      mc.player.method_1340(px, py, pz); mc.player.prevX = px; mc.player.prevY = py; mc.player.prevZ = pz;
      mc.player.yaw = mc.player.prevYaw = yaw; mc.player.pitch = mc.player.prevPitch = pitch;
    }
    log("SNOW472 failures=" + failures); check(failures == 0, "snow placement failures=" + failures);
  }
  private static boolean place(Minecraft mc, ItemStack held, int x, int y, int z, int face) {
    mc.player.inventory.main[0] = held;
    return held.method_701(mc.player, mc.world, x, y, z, face);
  }
  private static void snow(Minecraft mc, int x, int y, int z, int metadata) { mc.world.method_154(x, y, z, 78, metadata); }
  private static void reset(Minecraft mc, int x, int y, int z) {
    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
      for (int dy = 0; dy <= 3; dy++) mc.world.method_154(x + dx, y + dy, z + dz, 0, 0);
      mc.world.method_154(x + dx, y - 1, z + dz, 1, 0);
    }
  }
  private static void capture(Minecraft mc) throws Exception {
    var previous = mc.currentScreen; var screen = new PowerOptionsScreen(previous); screen.init(mc, 854, 480);
    Setting setting = screen.session().settings().stream().filter(s -> s.id.equals("gameplay.modernSnowPlacement")).findFirst().orElseThrow();
    field(screen, "page", "Fixes"); ((Set<?>)field(screen, "collapsed")).clear(); field(screen, "scroll", 0d); call(screen, "layout");
    for (Object row : (List<?>)field(screen, "rows")) if (call(row, "setting") == setting) { field(screen, "scroll", Math.max(0, ((Number)call(row, "y")).doubleValue() - 28)); break; }
    call(screen, "layout"); Path report = Path.of("power-beta-data/reports/snow-472"); Files.createDirectories(report);
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try { SettingsSnapshot.render(mc, screen, 854, 480, report.resolve("setting-854.png")); }
    finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(mode); mc.setScreen(previous); }
    for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, setting.id, setting.powerBetaDefault).getAsBoolean() == (base == DefaultBases.Base.POWER_BETA), "snow default for " + base);
    log("SNOW472 settings/defaults capture PASS");
  }
}
