package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.gameplay.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.visual.*;
import net.minecraft.class_142;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

/** Repeatable dense-item workload, including the native collision and explosion paths. */
final class Release151Checks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("save")) {
      var s = GameplayConfig.copy(); s.flintIgnitesTnt = true; s.fastItemCollisions = false; GameplayConfig.save(s);
      log("PASS saved 1.5.1 settings"); return;
    }
    if (action.equals("saved")) {
      check(GameplayConfig.current().flintIgnitesTnt && !GameplayConfig.current().fastItemCollisions, "new settings failed restart");
      log("PASS new settings survived restart"); return;
    }
    if (action.startsWith("sphere ")) { Release151SphereChecks.run(mc, action.substring(7)); return; }
    if (action.equals("text") || action.equals("light")) { Release151RenderChecks.run(mc, action); return; }
    if (action.equals("check")) { Release151GameplayChecks.run(mc); return; }
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
    CreativeVehicleChecks.mode(mc, "CREATIVE");
    mc.player.method_1340(0, 102, -6);
    for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) mc.world.method_214(x, z);
    var original = GameplayConfig.copy();
    List<Entity> items = new ArrayList<>();
    try {
      for (int n : new int[]{0, 1024, 4096, 8192}) {
        var s = GameplayConfig.copy(); s.mergeItems = false; s.explosionProofItems = true; s.fastItemCollisions = !action.equals("slow"); GameplayConfig.preview(s);
        for (int x = -3; x <= 5; x++) for (int z = -3; z <= 5; z++) mc.world.method_229(x, 99, z, Block.STONE.id);
        for (int i = 0; i < n; i++) {
          var item = new class_142(mc.world, 1 + (i % 16) * .04, 100.2, 1 + (i / 16 % 16) * .04,
              new ItemStack(Block.COBBLESTONE.id, 1, 0));
          item.velocityX = item.velocityY = item.velocityZ = 0; item.field_566 = 30000;
          mc.world.method_210(item); items.add(item);
        }
        double[] ticks = new double[12];
        for (int i = 0; i < ticks.length; i++) {
          long start = System.nanoTime(); mc.world.method_227(); ticks[i] = (System.nanoTime() - start) / 1e6;
        }
        Arrays.sort(ticks);
        long start = System.nanoTime();
        for (int i = 0; i < 4; i++) new net.minecraft.class_60(mc.world, null, 1, 101, 1, 4).method_1195();
        double explosions = (System.nanoTime() - start) / 4e6;
        check(Release150Checks.count(items) == n, "protected explosion lost items");
        s.mergeItems = true; s.mergeThreshold = 256; GameplayConfig.preview(s);
        int passes = 0; double max = 0;
        while (Release150Checks.alive(items) >= 256 && passes++ < 200) {
          start = System.nanoTime(); ItemMerging.merge(mc.world, mc); max = Math.max(max, (System.nanoTime() - start) / 1e6);
        }
        check(Release150Checks.count(items) == n, "merging lost items");
        log("BENCH items=" + n + " tickMedianMs=" + ticks[6] + " tickMaxMs=" + ticks[11]
            + " explosionMs=" + explosions + " mergePasses=" + passes + " mergeMaxMs=" + max
            + " remaining=" + Release150Checks.alive(items));
        items.forEach(Entity::markDead); mc.world.method_227(); items.clear();
      }
    } finally { items.forEach(Entity::markDead); GameplayConfig.preview(original); }
  }
}
