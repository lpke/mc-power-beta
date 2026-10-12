package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.permissions.CheatWorld;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

final class Release151GameplayChecks {
  static void run(Minecraft mc) throws Exception {
    var before = GameplayConfig.copy();
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
    CreativeVehicleChecks.mode(mc, "CREATIVE");
    mc.player.method_1340(0, 102, -6);
    for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) mc.world.method_214(x, z);
    try {
      test("collision setting is live, default on and in Fixes", () -> {
        var setting = find(SettingsRegistry.open(mc), "gameplay.fastItemCollisions");
        check(setting.page.equals("Fixes") && setting.defaultValue.getAsBoolean(), "wrong defaults/page");
      });
      test("flint ignition modes, off state and multiplayer authority", () -> ignition(mc));
      test("collision equivalence, live toggles, movement and removal", () -> collisions(mc));
      test("distant merge sweeps preserve changing native item data", () -> merging(mc));
    } finally { GameplayConfig.preview(before); }
  }
  private static void ignition(Minecraft mc) throws Exception {
    ItemStack old = mc.player.inventory.main[mc.player.inventory.selectedSlot];
    List<Entity> before = new ArrayList<>(mc.world.method_291());
    try {
      for (String mode : List.of("SURVIVAL", "CREATIVE", "SPECTATOR")) {
        CreativeVehicleChecks.mode(mc, mode);
        for (boolean enabled : new boolean[]{false, true}) {
          var s = GameplayConfig.copy(); s.flintIgnitesTnt = enabled; GameplayConfig.preview(s);
          mc.world.method_229(3, 105, 3, Block.field_1887.id); mc.world.method_229(3, 106, 3, 0);
          var flint = new ItemStack(Item.field_472, 1); mc.player.inventory.main[mc.player.inventory.selectedSlot] = flint;
          int count = tnt(mc);
          mc.interactionManager.method_1713(mc.player, mc.world, flint, 3, 105, 3, 1);
          boolean expected = enabled && !mode.equals("SPECTATOR");
          check(tnt(mc) - count == (expected ? 1 : 0), "wrong ignition " + mode + "/" + enabled);
          check(mc.world.getBlockId(3, 105, 3) == (expected ? 0 : Block.field_1887.id), "wrong block " + mode);
          if (expected) check(flint.getDamage() == (mode.equals("CREATIVE") ? 0 : 1), "wrong durability");
          mc.world.method_229(3, 105, 3, 0); mc.world.method_229(3, 106, 3, 0);
        }
      }
      CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var s = GameplayConfig.copy(); s.flintIgnitesTnt = true; GameplayConfig.preview(s);
      mc.world.method_229(3, 105, 3, Block.field_1887.id);
      mc.world.isRemote = true;
      try { check(!Block.field_1887.method_1608(mc.world, 3, 105, 3, mc.player), "client primed server TNT"); }
      finally { mc.world.isRemote = false; mc.world.method_229(3, 105, 3, 0); }
    } finally {
      mc.player.inventory.main[mc.player.inventory.selectedSlot] = old;
      for (Object entry : new ArrayList<>(mc.world.method_291())) if (entry instanceof class_365 entity && !before.contains(entity)) entity.markDead();
      CreativeVehicleChecks.mode(mc, "CREATIVE");
    }
  }
  private static int tnt(Minecraft mc) { return (int)mc.world.method_291().stream().filter(e -> e instanceof class_365 t && !t.dead).count(); }
  private static void merging(Minecraft mc) throws Exception {
    var settings = GameplayConfig.copy(); settings.mergeItems = true; settings.mergeThreshold = 16; GameplayConfig.preview(settings);
    mc.world.method_214(2, 0); mc.world.method_214(3, 0);
    List<Entity> created = new ArrayList<>();
    var sweep = ((ItemMerging.Access)mc.world).power$itemSweep(); sweep.clear();
    try {
      for (int i = 0; i < 16; i++) created.add(Release150Checks.item(mc, 4, 1, 0, 40, 110, 0));
      // More than one visit budget separates the initial target from the last item.
      for (int i = 0; i < 6000; i++) created.add(Release150Checks.item(mc, 4, 64, 0, 60, 110, 0));
      var tail = Release150Checks.item(mc, 4, 1, 0, 40, 110, 0); created.add(tail);
      var target = (class_142)created.get(0);
      for (int i = 0; i < 20 && target.field_564.count < 16; i++) ItemMerging.merge(mc.world, mc);
      check(target.field_564.count == 16 && !tail.dead, "distant pile or resumable budget failed");
      DeathChecks.tag(target.field_564).putString("changed", "between passes");
      for (int i = 0; i < 20; i++) ItemMerging.merge(mc.world, mc);
      check(target.field_564.count == 16 && tail.field_564.count == 1 && !tail.dead, "stale NBT identity merged different items");
      check(Release150Checks.count(created) == 6000 * 64 + 17, "resumable merge changed total count");
      sweep.clear();
      settings.mergeItems = false; GameplayConfig.preview(settings);
      check(ItemMerging.merge(mc.world, mc) == 0, "disabled merge did work");
      settings.mergeItems = true; GameplayConfig.preview(settings); mc.world.isRemote = true;
      try { check(ItemMerging.merge(mc.world, mc) == 0, "multiplayer merged items"); }
      finally { mc.world.isRemote = false; }
    } finally { created.forEach(Entity::markDead); sweep.clear(); }
  }
  private static List<String> boxes(Minecraft mc, Entity entity, Box box, boolean fast) {
    var s = GameplayConfig.copy(); s.fastItemCollisions = fast; GameplayConfig.preview(s);
    List<String> result = new ArrayList<>();
    for (Object entry : mc.world.method_190(entity, box)) {
      Box b = (Box)entry; result.add(b.minX + ":" + b.minY + ":" + b.minZ + ":" + b.maxX + ":" + b.maxY + ":" + b.maxZ);
    }
    Collections.sort(result); return result;
  }
  private static void collisions(Minecraft mc) {
    var settings = GameplayConfig.copy(); settings.fastItemCollisions = false; GameplayConfig.preview(settings);
    List<Entity> created = new ArrayList<>();
    var item = new class_142(mc.world, 4, 105, 4, new ItemStack(Block.STONE.id, 1, 0));
    var tnt = new class_365(mc.world, 4, 105, 4); tnt.field_1390 = 80;
    var boat = new class_113(mc.world, 4.5, 105, 4);
    // A subclass with a collision box must remain in the index.
    var custom = new class_142(mc.world, 4, 105, 4, new ItemStack(Block.STONE.id, 1, 0)) {
      @Override public Box method_1381() { return boundingBox; }
    };
    created.addAll(List.of(item, tnt, boat, custom));
    try {
      created.forEach(mc.world::method_210);
      List<Entity> indexed = new ArrayList<>();
      ((CollisionCandidates.ChunkAccess)mc.world.method_214(0, 0)).power$queryColliders(item, Box.create(2, 102, 2, 7, 108, 7), indexed, 0);
      check(indexed.contains(boat) && indexed.contains(custom) && !indexed.contains(tnt), "collision index bypassed or incomplete");
      for (int stage = 0; stage < 3; stage++) {
        for (Entity entity : List.of(item, tnt, boat, custom, mc.player)) {
          Box box = Box.create(2, 102, 2, 7, 108, 7);
          check(boxes(mc, entity, box, false).equals(boxes(mc, entity, box, true)), "collision mismatch " + stage + "/" + entity.getClass());
        }
        if (stage == 0) { boat.method_1340(20, 105, 4); mc.world.method_193(boat, false); }
        else { custom.markDead(); mc.world.method_227(); }
      }
    } finally { created.forEach(Entity::markDead); }
  }
}
