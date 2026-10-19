package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import local.luke.power.rules.RuleSet;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;

/** A short, reversible stone ray for real OS mouse presses. */
public final class CreativePressChecks {
  private static Session session;
  private CreativePressChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("physical-setup")) setup(mc);
    else if (action.equals("physical-state")) state(mc);
    else if (action.equals("physical-cleanup")) cleanup(mc);
    else throw new IllegalArgumentException("Use physical-setup, physical-state or physical-cleanup");
  }

  private static void setup(Minecraft mc) throws Exception {
    check(session == null, "clean up the previous physical press fixture first");
    check(!local.luke.power.input.DetachedCamera.isActive() && mc.player.field_1595 == null,
        "leave freecam and dismount before physical presses");
    Session next = new Session(mc);
    for (int i = 0; i < 3; i++) check(mc.world.getBlockId(next.x, next.y, next.z + i) == 0,
        "physical fixture needs three empty blocks in front of the player");
    session = next;
    try {
      AccessRules.of(mc.world.method_262()).allAllowed(true);
      CreativeVehicleChecks.mode(mc, "CREATIVE");
      mc.player.getClass().getMethod("creative_setFlying", boolean.class).invoke(mc.player, true);
      mc.setScreen(null); mc.field_2778 = true; mc.field_2807 = mc.player;
      mc.player.inventory.main[next.slot] = null;
      mc.player.method_1340(mc.player.x, next.y + .5, mc.player.z);
      mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
      mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 0;
      cooldown().setInt(mc, 0);
      for (int i = 0; i < 3; i++) mc.world.method_154(next.x, next.y, next.z + i, 1, 0);
      log("CREATIVE PHYSICAL ARMED stone=" + next.x + "," + next.y + "," + next.z
          + " through z=" + (next.z + 2) + "; send exactly two left press/release pairs less than 200 ms apart, then physical-state and physical-cleanup");
    } catch (Exception | Error error) { cleanup(mc); throw error; }
  }

  public static void nativePress(Minecraft mc, int button) {
    Session current = session;
    if (current == null || button != 0 || mc.world != current.world || mc.player != current.player) return;
    int index = current.presses++;
    if (index < current.times.length) {
      current.times[index] = Mouse.getEventNanoseconds();
      current.ticks[index] = ((Client253Access)mc).power$ticks();
      current.removed[index] = removed(current);
    }
  }

  private static void state(Minecraft mc) throws Exception {
    check(session != null && mc.world == session.world && mc.player == session.player, "no owned physical press fixture");
    Session current = session;
    log("CREATIVE PHYSICAL presses=" + current.presses + " removed=" + removed(current)
        + " ticks=" + java.util.Arrays.toString(current.ticks) + " eventNanos=" + java.util.Arrays.toString(current.times)
        + " removedAfterPress=" + java.util.Arrays.toString(current.removed));
    failures = 0;
    test("two rapid native mouse presses each break one Creative block", () -> {
      check(current.presses == 2, "expected exactly two native left presses, got " + current.presses);
      long interval = current.times[1] - current.times[0];
      check(interval > 0 && interval < 200_000_000L, "press pair was not rapid: " + interval + " ns");
      check(current.ticks[1] - current.ticks[0] < 5, "presses missed the five-tick cooldown window");
      check(current.removed[0] == 1 && current.removed[1] == 2 && removed(current) == 2,
          "native press dispatch skipped or doubled a break");
    });
    log("CREATIVE PHYSICAL failures=" + failures);
  }

  private static void cleanup(Minecraft mc) throws Exception {
    if (session == null) return;
    Session current = session;
    check(mc.world == current.world && mc.player == current.player, "return to the fixture world/player before cleanup");
    for (int i = 0; i < 3; i++) mc.world.method_154(current.x, current.y, current.z + i, 0, 0);
    System.arraycopy(current.inventory, 0, mc.player.inventory.main, 0, current.inventory.length);
    mc.player.inventory.selectedSlot = current.slot;
    CreativeVehicleChecks.mode(mc, current.mode);
    mc.player.getClass().getMethod("creative_setFlying", boolean.class).invoke(mc.player, current.flying);
    mc.player.method_1340(current.playerX, current.playerY, current.playerZ);
    mc.player.yaw = mc.player.prevYaw = current.yaw; mc.player.pitch = mc.player.prevPitch = current.pitch;
    mc.player.velocityX = current.vx; mc.player.velocityY = current.vy; mc.player.velocityZ = current.vz;
    AccessRules.restore(mc, current.rules); cooldown().setInt(mc, current.cooldown);
    mc.field_2778 = current.focus; mc.field_2807 = current.view; mc.setScreen(current.screen);
    session = null; log("CREATIVE PHYSICAL CLEANED");
  }

  private static int removed(Session current) {
    int count = 0;
    for (int i = 0; i < 3; i++) if (current.world.getBlockId(current.x, current.y, current.z + i) == 0) count++;
    return count;
  }
  private static Field cooldown() throws Exception {
    Field field = Minecraft.class.getDeclaredField("field_2787"); field.setAccessible(true); return field;
  }

  private static final class Session {
    final World world;
    final ClientPlayerEntity player;
    final Screen screen;
    final net.minecraft.entity.LivingEntity view;
    final RuleSet rules;
    final String mode;
    final ItemStack[] inventory;
    final boolean flying, focus;
    final int x, y, z, slot, cooldown;
    final double playerX, playerY, playerZ, vx, vy, vz;
    final float yaw, pitch;
    final long[] times = new long[8];
    final int[] ticks = new int[8], removed = new int[8];
    int presses;

    Session(Minecraft mc) throws Exception {
      world = mc.world; player = mc.player; screen = mc.currentScreen; view = mc.field_2807;
      rules = AccessRules.copy(mc); focus = mc.field_2778;
      mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(player).toString();
      flying = (boolean)player.getClass().getMethod("creative_isFlying").invoke(player);
      inventory = player.inventory.main.clone(); slot = player.inventory.selectedSlot; cooldown = cooldown().getInt(mc);
      playerX = player.x; playerY = player.y; playerZ = player.z;
      vx = player.velocityX; vy = player.velocityY; vz = player.velocityZ; yaw = player.yaw; pitch = player.pitch;
      x = (int)Math.floor(player.x); y = (int)Math.floor(player.y); z = (int)Math.floor(player.z) + 2;
      check(y >= 1 && y < 127, "move inside the world height before physical presses");
    }
  }
}
