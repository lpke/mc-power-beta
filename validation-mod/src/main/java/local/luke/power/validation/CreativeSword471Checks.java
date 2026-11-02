package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.offhand.Offhand;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Native mouse press, held mining, final break and entity attack regression checks. */
public final class CreativeSword471Checks {
  private CreativeSword471Checks() {}

  public static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && mc.currentScreen == null,
        "load a disposable singleplayer world");
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0
        && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "focus the muted clone and release mouse buttons");
    Field cooldown = Minecraft.class.getDeclaredField("field_2787"); cooldown.setAccessible(true);
    int oldCooldown = cooldown.getInt(mc), oldSwingTicks = mc.player.handSwingTicks;
    boolean oldSwing = mc.player.handSwinging;
    failures = 0;
    try (var s = new OffhandActions400State(mc)) {
      for (int sword : new int[]{267, 268, 272, 276, 283}) for (int block : new int[]{1, 31, 46}) {
        test("CREATIVE SWORD471 sword=" + sword + " block=" + block + " press/hold/final", () -> {
          s.reset(); CreativeVehicleChecks.mode(mc, "CREATIVE");
          s.hands(OffhandActions400State.tagged(sword, 1, 7, "creative-sword-471"), null);
          s.nativeState.scene(block, 1); String[] before = s.items();
          mc.player.handSwinging = false; mc.player.handSwingTicks = 0; cooldown.setInt(mc, 0);
          press(s);
          check(mc.player.handSwinging && mc.player.handSwingTicks == -1, "Creative sword click lost its native swing");
          check(mc.world.getBlockId(s.nativeState.x, s.nativeState.y, s.nativeState.z) == block,
              "Creative sword press broke a block");
          for (int tick = 0; tick < 30; tick++) ((Client253Access)(Object)mc).power$heldMining(0, true);
          check(mc.world.getBlockId(s.nativeState.x, s.nativeState.y, s.nativeState.z) == block,
              "Creative sword hold broke a block");
          check(!mc.interactionManager.method_1716(s.nativeState.x, s.nativeState.y, s.nativeState.z, 1),
              "Creative sword final callback reported a block break");
          check(mc.world.getBlockId(s.nativeState.x, s.nativeState.y, s.nativeState.z) == block
              && Arrays.equals(before, s.items()), "Creative sword final callback changed terrain or native item bytes");
        });
      }
      test("CREATIVE SWORD471 switching to sword clears pending mining", () -> {
        s.reset(); s.hands(new ItemStack(274, 1, 0), null); s.nativeState.scene(1, 1);
        mc.interactionManager.method_1707(s.nativeState.x, s.nativeState.y, s.nativeState.z, 1);
        mc.interactionManager.method_1721(s.nativeState.x, s.nativeState.y, s.nativeState.z, 1);
        mc.interactionManager.method_1721(s.nativeState.x, s.nativeState.y, s.nativeState.z, 1);
        Field progress = net.minecraft.SingleplayerInteractionManager.class.getDeclaredField("field_2184"); progress.setAccessible(true);
        check(progress.getFloat(mc.interactionManager) > 0, "fixture did not establish native mining progress");
        CreativeVehicleChecks.mode(mc, "CREATIVE"); s.hands(new ItemStack(267, 1, 0), null);
        ((Client253Access)(Object)mc).power$heldMining(0, true);
        check(progress.getFloat(mc.interactionManager) == 0
            && mc.world.getBlockId(s.nativeState.x, s.nativeState.y, s.nativeState.z) == 1,
            "switching to Creative sword retained mining progress or broke the pending target");
      });
      for (int main : new int[]{0, 274}) {
        test("CREATIVE SWORD471 offhand sword leaves main-hand mining main=" + main, () -> {
          s.reset(); CreativeVehicleChecks.mode(mc, "CREATIVE");
          ItemStack sword = OffhandActions400State.tagged(267, 1, 7, "offhand-sword-471");
          s.hands(main == 0 ? null : new ItemStack(main, 1, 0), sword);
          String original = AutoTools400State.bytes(sword); s.nativeState.scene(1, 1); cooldown.setInt(mc, 0); press(s);
          check(mc.world.getBlockId(s.nativeState.x, s.nativeState.y, s.nativeState.z) == 0,
              "offhand sword blocked ordinary main-hand mining");
          check(Offhand.get(mc.player.inventory) == sword && original.equals(AutoTools400State.bytes(sword)),
              "main-hand mining changed offhand sword data");
        });
      }
      test("CREATIVE SWORD471 native entity attack and swing", () -> {
        s.reset(); CreativeVehicleChecks.mode(mc, "CREATIVE");
        s.hands(OffhandActions400State.tagged(267, 1, 7, "entity-sword-471"), null);
        Probe target = new Probe(mc.world); s.owned.add(target); mc.field_2823 = new class_27(target);
        String[] before = s.items(); mc.player.handSwinging = false; cooldown.setInt(mc, 0); press(s);
        check(target.damage > 0 && target.attacker == mc.player && mc.player.handSwinging,
            "Creative sword stopped native entity attack or swing");
        check(Arrays.equals(before, s.items()), "Creative entity attack changed protected sword data");
      });
      test("CREATIVE SWORD471 Survival retains native mining and wear", () -> {
        s.reset(); ItemStack sword = OffhandActions400State.tagged(267, 1, 7, "survival-sword-471");
        s.hands(sword, null); s.nativeState.scene(1, 1);
        check(mc.interactionManager.method_1716(s.nativeState.x, s.nativeState.y, s.nativeState.z, 1)
            && mc.world.getBlockId(s.nativeState.x, s.nativeState.y, s.nativeState.z) == 0
            && sword.getDamage() == 9, "Survival sword mining or native two-point wear changed");
        s.nativeState.scene(31, 1); cooldown.setInt(mc, 0); press(s);
        check(mc.world.getBlockId(s.nativeState.x, s.nativeState.y, s.nativeState.z) == 0,
            "Survival sword lost native instant block mining");
      });
    } finally {
      cooldown.setInt(mc, oldCooldown); mc.player.handSwinging = oldSwing; mc.player.handSwingTicks = oldSwingTicks;
    }
    log("CREATIVE SWORD471 checks complete failures=" + failures);
  }

  private static void press(OffhandActions400State s) {
    Set<Entity> before = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Object entity : s.mc.world.field_198) before.add((Entity)entity);
    ((Client253Access)(Object)s.mc).power$use(0);
    for (Object entity : s.mc.world.field_198) if (!before.contains(entity)) s.owned.add((Entity)entity);
  }
  private static final class Probe extends Entity {
    int damage; Entity attacker;
    Probe(World world) { super(world); }
    protected void method_1310() {}
    public void writeNbt(NbtCompound nbt) {}
    public void readtNbt(NbtCompound nbt) {}
    public boolean damage(Entity source, int amount) { attacker = source; damage += amount; return true; }
  }
}
