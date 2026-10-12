package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.gameplay.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public final class Release151SphereChecks {
  private static int remaining, total, peakItems, peakTnt;
  private static final List<Double> ticks = new ArrayList<>(), renders = new ArrayList<>();
  private static String scenario;
  public static boolean measuring() { return remaining > 0; }
  public static void tick(double millis) { if (measuring()) ticks.add(millis); }
  public static void render(double millis) { if (measuring()) renders.add(millis); }
  public static void observe(Minecraft mc) {
    if (!measuring()) return;
    int items = 0, tnt = 0;
    for (Object e : mc.world.method_291()) {
      if (e instanceof class_142 item && !item.dead) items++;
      if (e instanceof class_365 entity && !entity.dead) tnt++;
    }
    peakItems = Math.max(peakItems, items); peakTnt = Math.max(peakTnt, tnt);
    if (--remaining == 0) try {
      log("SPHERE " + scenario + " ticks=" + total + " peakItems=" + peakItems + " peakTnt=" + peakTnt
          + " remainingItems=" + items + " remainingTnt=" + tnt + " tick=" + summary(ticks) + " render=" + summary(renders));
      log("SPHERE DONE " + scenario);
    } catch (Exception e) { throw new RuntimeException(e); }
  }
  private static String summary(List<Double> values) {
    if (values.isEmpty()) return "none";
    Collections.sort(values);
    return "median:" + values.get(values.size()/2) + ",p95:" + values.get((int)(values.size()*.95)) + ",max:" + values.get(values.size()-1);
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("setup")) {
      ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
      CreativeVehicleChecks.mode(mc, "CREATIVE"); mc.setScreen(null);
      for (Object e : new ArrayList<>(mc.world.method_291())) if (e instanceof class_142 || e instanceof class_365) ((Entity)e).markDead();
      for (int x = -17; x <= 17; x++) for (int z = -17; z <= 17; z++)
        for (int y = 83; y <= 107; y++) mc.world.method_154(x, y, z, y < 96 ? Block.DIRT.id : 0, 0);
      mc.player.method_1340(.5, 96, .5); mc.player.yaw = 0; mc.player.pitch = 5;
      var s = GameplayConfig.copy(); s.limitTnt = true; s.tntPerTick = 4; s.tntMillis = 8;
      s.explosionProofItems = true; s.mergeItems = true; s.mergeThreshold = 256; GameplayConfig.preview(s);
      ChatChecks.submit(mc, "//sphere tnt 5");
      return;
    }
    scenario = action;
    var s = GameplayConfig.copy(); s.mergeItems = !action.contains("unmerged");
    s.limitTnt = !action.contains("unlimited"); s.fastItemCollisions = !action.contains("slow");
    s.explosionProofItems = !action.contains("unprotected"); GameplayConfig.preview(s);
    var v = VisualConfig.copy(); v.hideEntities = action.contains("hidden"); v.hideItems = true; VisualConfig.preview(v);
    mc.setScreen(null);
    // Stand next to the sphere at ground level and prime its nearest surface.
    mc.player.method_1340(.5, 96, -7.5); mc.player.yaw = 0; mc.player.pitch = 0;
    boolean ignited = false;
    for (int y = 96; y <= 101 && !ignited; y++) for (int z = -6; z <= 0 && !ignited; z++)
      if (mc.world.getBlockId(0, y, z) == Block.field_1887.id) {
        mc.world.method_229(0, y, z, 0); Block.field_1887.method_1612(mc.world, 0, y, z, 1); ignited = true;
      }
    check(ignited, "WorldEdit sphere did not finish");
    remaining = total = 600; peakItems = peakTnt = 0; ticks.clear(); renders.clear();
    log("SPHERE START " + scenario);
  }
}
