package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import net.minecraft.class_113;
import net.minecraft.class_142;
import net.minecraft.class_549;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

final class CreativeVehicleChecks {
  static void mode(Minecraft mc, String name) throws Exception {
    Class type = Class.forName("local.luke.power.creative.api.GameMode");
    Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_applyMode", type)
        .invoke(mc.player, Enum.valueOf(type, name));
  }
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    String before = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    var inventory = mc.player.inventory.main.clone();
    int slot = mc.player.inventory.selectedSlot;
    List<Entity> created = new ArrayList<>();
    try {
      mc.setScreen(null); mc.player.inventory.selectedSlot = 0; mc.player.inventory.main[0] = null;
      for (int type = -1; type < 3; type++) {
        final int kind = type;
        test("one creative click destroys vehicle " + type + " with no item or inventory drops", () -> {
          mode(mc, "CREATIVE");
          Entity vehicle = vehicle(mc, kind); created.add(vehicle);
          if (vehicle instanceof class_549 cart) for (int i = 0; i < cart.size(); i++)
            cart.setStack(i, new ItemStack(i == 0 ? 257 : 264, i == 0 ? 1 : 64, i == 0 ? 37 : 0));
          int drops = drops(mc);
          mc.interactionManager.attackEntity(mc.player, vehicle);
          check(vehicle.dead, "vehicle survived"); check(drops(mc) == drops, "vehicle dropped items");
          mc.interactionManager.attackEntity(mc.player, vehicle);
          check(drops(mc) == drops, "repeat attack dropped items");
        });
      }
      test("vehicle removal safely dismounts its passenger", () -> {
        mode(mc, "CREATIVE");
        Entity boat = vehicle(mc, -1), rider = vehicle(mc, 0);
        created.add(boat); created.add(rider); rider.method_1376(boat);
        int drops = drops(mc); mc.interactionManager.attackEntity(mc.player, boat);
        check(boat.dead && !rider.dead && rider.field_1595 == null, "passenger was removed or left mounted");
        check(drops(mc) == drops, "occupied boat dropped items");
      });
      test("survival still needs multiple hits and drops its vehicle", () -> {
        mode(mc, "SURVIVAL"); Entity boat = vehicle(mc, -1); created.add(boat);
        int drops = drops(mc); mc.interactionManager.attackEntity(mc.player, boat);
        check(!boat.dead, "survival removed boat immediately");
        for (int i = 0; i < 10 && !boat.dead; i++) mc.interactionManager.attackEntity(mc.player, boat);
        check(boat.dead && drops(mc) > drops, "survival drops lost");
      });
      test("spectator and remote worlds cannot use creative deletion", () -> {
        Entity boat = vehicle(mc, -1); created.add(boat);
        mode(mc, "SPECTATOR"); mc.interactionManager.attackEntity(mc.player, boat); check(!boat.dead, "spectator deleted boat");
        mode(mc, "CREATIVE"); mc.world.isRemote = true;
        try { mc.interactionManager.attackEntity(mc.player, boat); check(!boat.dead, "remote deletion"); }
        finally { mc.world.isRemote = false; }
      });
    } finally {
      mc.world.isRemote = false;
      for (Entity e : created) { if (e instanceof class_549 c) for (int i = 0; i < c.size(); i++) c.setStack(i, null); e.markDead(); }
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.selectedSlot = slot; mode(mc, before);
    }
    log("CREATIVE VEHICLE FAILURES " + failures);
  }
  private static Entity vehicle(Minecraft mc, int type) {
    Entity e = type < 0 ? new class_113(mc.world, 30, 110, 30) : new class_549(mc.world, 30, 110, 30, type);
    mc.world.method_210(e); return e;
  }
  private static int drops(Minecraft mc) { return mc.world.method_175(class_142.class, Box.getOrCreate(27, 107, 27, 34, 114, 34)).size(); }
}
