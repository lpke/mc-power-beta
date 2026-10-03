package local.luke.building.validation;

import static local.luke.building.validation.ValidationRun.*;

import local.luke.power.fastplace.*;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.client.Minecraft;

/** Slow and physical-input checks, explicitly requested through the lab command file. */
public final class FinalChecks {
  private static final Region LARGE = Region.between(new Pos(0, 110, 0), new Pos(63, 125, 63));
  private static int phase;
  private static long before, started;
  private static SlabMode slabMode;

  public static void largeEdit(Minecraft mc) {
    WorldEditor.leaveWorld();
    var editor = WorldEditor.editor(mc);
    before = hash(editor);
    started = System.currentTimeMillis();
    editor.select(LARGE);
    WorldEditor.command(mc, "//set glass");
    phase = 1;
    log("LARGE EDIT START volume=" + LARGE.volume());
  }

  private static long hash(Editor editor) {
    long hash = 0xcbf29ce484222325L;
    for (Pos p : LARGE) {
      BlockValue value = editor.engine.read(p);
      hash = (hash ^ (value.id * 16L + value.meta)) * 0x100000001b3L;
    }
    return hash;
  }

  public static void tick(Minecraft mc) {
    if (phase == 0) return;
    var editor = WorldEditor.editor(mc);
    if (editor.engine.busy()) return;
    if (phase == 1) {
      for (Pos p : LARGE) check(editor.engine.read(p).id == 20, "large edit glass at " + p);
      log("PASS large edit 65536 blocks elapsedMs=" + (System.currentTimeMillis() - started));
      WorldEditor.command(mc, "//undo");
      phase = 2;
    } else {
      phase = 0;
      check(hash(editor) == before, "large undo original block and metadata hash");
      log(
          "PASS large undo exact original hash elapsedMs="
              + (System.currentTimeMillis() - started));
      phase = 0;
    }
  }

  public static void prepareSlabs(Minecraft mc, String mode) throws Exception {
    reset(mc);
    WorldEditor.leaveWorld();
    creative(mc, true);
    Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer")
        .getMethod("creative_setFlying", boolean.class)
        .invoke(mc.player, true);
    slabMode = SlabMode.valueOf(mode);
    var settings = FastPlace.settings();
    settings.setEnabled(true);
    settings.slabMode = slabMode;
    FastPlace.apply(settings);
    stack(mc, 44, 0);
    aim(mc, .5, 104.7, 1.5, .5, 100.99, .5);
    log("SLAB INPUT READY " + slabMode);
  }

  public static void slabResult(Minecraft mc) {
    int first = mc.world.getBlockId(0, 101, 0);
    int second = mc.world.getBlockId(0, 102, 0);
    log(
        "SLAB OBSERVED first="
            + first
            + " second="
            + second
            + " xyz="
            + mc.player.x
            + ","
            + mc.player.y
            + ","
            + mc.player.z
            + " hit="
            + mc.field_2823);
    check(first == (slabMode == SlabMode.MATCH_FIRST ? 44 : 43), "physical first slab layer");
    if (slabMode == SlabMode.CONTINUOUS) check(second == 43, "held climbs to second full layer");
    if (slabMode == SlabMode.MATCH_FIRST) check(second == 0, "held respects first half and layer");
    log("PASS physical held-click slabs " + slabMode + " first=" + first + " second=" + second);
  }
}
