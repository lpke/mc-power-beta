package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.building.config.Config;
import local.luke.power.fastplace.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Native pair costs, prediction and held-stroke anchor ownership for every Beta slab material. */
final class SmartSlabs470Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null,
        "open a disposable singleplayer world with no menu");
    failures = 0; var p = mc.player; var saved = new NbtCompound(); p.write(saved);
    var inventory = p.inventory.main.clone(); int selected = p.inventory.selectedSlot;
    var config = Config.current().copy(); byte[] disk = Files.readAllBytes(PowerConfig.path());
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(p).toString();
    int x = ((int)Math.floor(p.x) >> 4) * 16 + 8, z = ((int)Math.floor(p.z) >> 4) * 16 + 8, y = 101;
    var blocks = new ArrayList<int[]>();
    try {
      check(mc.world.method_155(x - 4, y - 3, z - 4, x + 7, y + 5, z + 4), "slab fixture requires loaded terrain");
      for (int bx = x - 4; bx <= x + 7; bx++) for (int bz = z - 4; bz <= z + 4; bz++) for (int by = y - 3; by <= y + 5; by++)
        blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by,bz), mc.world.method_1778(bx, by, bz)});
      CreativeVehicleChecks.mode(mc, "SURVIVAL"); p.health = 20; p.dead = false;
      p.method_1340(x + 6.5, y + 3 + p.field_1631, z + .5);
      var settings = Config.current().copy(); settings.placement.setEnabled(true); settings.placement.slabMode = SlabMode.SMART;
      settings.placement.restrictionEnabled = false; settings.placement.attemptsPerTick = 1; settings.slabs.enabled = true; Config.preview(settings);
      test("SLABS470 native first placements match all slab anchors and material variants", () -> {
        for (int material = 0; material < 4; material++) for (int anchor : new int[]{1, 43, 44}) for (int face = 0; face < 6; face++) {
          clear(mc, blocks); FastPlace.release(); stack(mc, material);
          mc.world.method_154(x, y, z, anchor, anchor == 1 ? 0 : (material + 1) % 4);
          var target = PlacementTarget.at(mc.world, p.inventory.main[0], x, y, z, face);
          check(target != null, "missing prediction");
          boolean pair = face < 2 || anchor != 44;
          check(target.expectedBlock() == (pair ? 43 : 44), "first prediction ignored anchor/face");
          FastPlace.beginTick(mc, true, false, true);
          check(use(mc, x, y, z, face), "first native Smart placement failed material=" + material + " anchor=" + anchor + " face=" + face);
          var at = target.position(); check(mc.world.getBlockId(at.x(), at.y(), at.z()) == (pair ? 43 : 44), "actual shape differs from prediction");
          check(mc.world.method_1778(at.x(), at.y(), at.z()) == material, "slab material changed");
          check(p.inventory.main[0].count == (pair ? 62 : 63), "wrong initial slab cost");
          check(FastPlace.suppressVanilla(mc, 1), "initial operation did not retain held stroke");
        }
      });
      test("SLABS470 held native batch continues a lower row without completing it", () -> {
        clear(mc, blocks); FastPlace.release(); stack(mc, 2); mc.world.method_154(x, y, z, 44, 0);
        FastPlace.beginTick(mc, true, false, true); check(use(mc, x, y, z, 5), "initial lower row failed");
        aim(mc, x + 4.5, y + 1.5, z + .5, x + 1.99, y + .25, z + .5);
        FastPlace.beginTick(mc, true, false, true); FastPlace.placeBatch(mc);
        check(mc.world.getBlockId(x + 1, y, z) == 44 && mc.world.getBlockId(x + 2, y, z) == 44,
            "held batch failed to extend a single-slab row");
        check(p.inventory.main[0].count == 62, "held lower row consumed wrong count");
      });
      test("SLABS470 direction changes retain the initial anchor until release", () -> {
        for (int anchor : new int[]{1, 43, 44}) {
          clear(mc, blocks); FastPlace.release(); stack(mc, 3); mc.world.method_154(x, y, z, anchor, 0);
          FastPlace.beginTick(mc, true, false, true); check(use(mc, x, y, z, 5), "initial stroke failed");
          check(FastPlace.slabMode(mc.world, x + 1, y, z, 1) == SlabMode.DOUBLE, "upward change lost Double behavior");
          check(use(mc, x + 1, y, z, 1), "upward native operation failed");
          check(FastPlace.slabMode(mc.world, x + 1, y, z, 5) == (anchor == 44 ? SlabMode.CONTINUOUS : SlabMode.DOUBLE), "horizontal return replaced initial anchor");
          check(use(mc, x + 1, y, z, 5), "horizontal native continuation failed");
          check(mc.world.getBlockId(x + 2, y, z) == (anchor == 44 ? 44 : 43), "direction change selected wrong horizontal shape");
          FastPlace.release();
          check(FastPlace.slabMode(mc.world, x + 1, y, z, 5) == SlabMode.DOUBLE, "released stroke retained old lower anchor");
        }
      });
      test("SLABS470 retained choices and disabled Fast place preserve ordinary slab completion", () -> {
        clear(mc, blocks); FastPlace.release(); var current = Config.current().copy(); current.placement.enabled = false; Config.preview(current);
        stack(mc, 1); mc.world.method_154(x, y, z, 44, 1); mc.world.method_154(x - 1, y, z, 1, 0);
        check(use(mc, x - 1, y, z, 5) && mc.world.getBlockId(x, y, z) == 43, "Smart preference changed disabled Fast place completion");
        for (var choice : new SlabMode[]{SlabMode.CONTINUOUS, SlabMode.DOUBLE, SlabMode.MATCH_FIRST}) {
          current.placement.slabMode = choice; Config.preview(current);
          for (int face = 0; face < 6; face++) check(FastPlace.slabMode(mc.world, x, y, z, face) == choice, "saved mode behavior changed");
        }
        check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "slab fixture wrote preferences");
      });
    } finally {
      FastPlace.release(); Config.preview(config);
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      CreativeVehicleChecks.mode(mc, mode); p.read(saved); System.arraycopy(inventory, 0, p.inventory.main, 0, inventory.length); p.inventory.selectedSlot = selected;
    }
    log("SMART SLABS 470 FAILURES " + failures); check(failures == 0, "Smart slab failures=" + failures);
  }
  private static void stack(Minecraft mc, int material) { mc.player.inventory.selectedSlot = 0; mc.player.inventory.main[0] = new ItemStack(44, 64, material); }
  private static boolean use(Minecraft mc, int x, int y, int z, int face) { return mc.interactionManager.method_1713(mc.player, mc.world, mc.player.inventory.main[0], x, y, z, face); }
  private static void clear(Minecraft mc, List<int[]> blocks) { for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], 0, 0); }
  private static void aim(Minecraft mc, double px, double py, double pz, double x, double y, double z) {
    mc.player.method_1340(px, py, pz); mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
    double dx = x - px, dy = y - py, dz = z - pz;
    mc.player.yaw = mc.player.prevYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90);
    mc.player.pitch = mc.player.prevPitch = (float)-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
  }
}
