package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import local.luke.power.input.ClickMining;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;

/** Exercise both native attack entry points across air gaps and unrelated delays. */
final class Release300MiningAirChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "load a disposable world first");
    var previous = mc.currentScreen; var hit = mc.field_2823; boolean focus = mc.field_2778;
    var rules = AccessRules.copy(mc); String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    Object config = Class.forName("local.luke.power.building.config.Config").getMethod("current").invoke(null);
    Field enabled = config.getClass().getField("clickMining"); boolean preference = enabled.getBoolean(config), inverted = ClickMining.inverted();
    Field cooldown = Minecraft.class.getDeclaredField("field_2787"); cooldown.setAccessible(true); int originalDelay = cooldown.getInt(mc);
    var inventory = mc.player.inventory.main.clone(); int slot = mc.player.inventory.selectedSlot;
    int x = (int)Math.floor(mc.player.x) + 3, y = Math.min(123, (int)Math.floor(mc.player.y) + 3), z = (int)Math.floor(mc.player.z);
    check(mc.world.getBlockId(x, y, z) == 0, "fixture needs an air column above the player");
    var input = (Client253Access)mc;
    try {
      mc.setScreen(null); mc.field_2778 = true; AccessRules.of(mc.world.method_262()).allAllowed(true);
      CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.player.inventory.main[slot] = null; ClickMining.reset();
      test("air clicks keep vanilla delay when disabled, remove only that penalty when enabled, and respect temporary inversion", () -> {
        for (boolean setting : new boolean[]{false, true}) for (boolean temporary : new boolean[]{false, true}) {
          enabled.setBoolean(config, setting); ClickMining.reset(); if (temporary) ClickMining.toggle();
          cooldown.setInt(mc, 0); mc.field_2823 = null; input.power$use(0);
          check(cooldown.getInt(mc) == (setting != temporary ? 0 : 10), "wrong air penalty for saved=" + setting + " inverted=" + temporary);
        }
      });
      test("a valid target mines immediately after air, preserving unrelated native cooldowns and multiplayer air penalties", () -> {
        enabled.setBoolean(config, true); ClickMining.reset(); cooldown.setInt(mc, 0); mc.field_2823 = null; input.power$use(0);
        mc.world.method_154(x, y, z, 3, 0);
        mc.field_2823 = new class_27(x, y, z, 1, Vec3d.createCached(x + .5, y + 1, z + .5));
        // Native target selection gets its own setup tick. Damage/drop handling stays native.
        for (int i = 0; i < 100 && mc.world.getBlockId(x, y, z) != 0; i++) input.power$heldMining(0, true);
        check(mc.world.getBlockId(x, y, z) == 0, "air penalty blocked native held mining");
        cooldown.setInt(mc, 7); mc.field_2823 = null; input.power$use(0);
        check(cooldown.getInt(mc) == 7, "continuous mining erased a pre-existing delay");
        cooldown.setInt(mc, 0); mc.world.isRemote = true;
        try { input.power$use(0); check(cooldown.getInt(mc) == 10, "multiplayer air delay changed"); }
        finally { mc.world.isRemote = false; }
      });
    } finally {
      mc.world.method_154(x, y, z, 0, 0); System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.selectedSlot = slot; enabled.setBoolean(config, preference); ClickMining.reset(); if (inverted) ClickMining.toggle();
      CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules); mc.field_2823 = hit; mc.field_2778 = focus;
      cooldown.setInt(mc, originalDelay); mc.interactionManager.method_1705(); mc.setScreen(previous);
    }
    log("RELEASE300 MINING AIR failures=" + failures);
  }
}
