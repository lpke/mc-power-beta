package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.bow.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.UseInput;
import local.luke.power.inventory.HandUse;
import local.luke.power.offhand.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Actual native use in a focused, muted disposable clone; restores all fixture state. */
public final class CreativeBow471Checks {
  private enum Ammo { NONE, OPPOSITE, STORAGE }
  private CreativeBow471Checks() {}

  public static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && mc.currentScreen == null,
        "load a disposable singleplayer world");
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0 && !Mouse.isButtonDown(1),
        "focus the muted clone and release Use");
    GameplaySettings settings = GameplayConfig.copy();
    Field input = UseInput.class.getDeclaredField("held"); input.setAccessible(true);
    BooleanSupplier held = (BooleanSupplier)input.get(null);
    failures = 0;
    try (var s = new OffhandActions400State(mc)) {
      UseInput.register(() -> true);
      for (boolean modern : new boolean[]{false, true}) for (boolean off : new boolean[]{false, true})
        for (Ammo source : Ammo.values()) for (int count : source == Ammo.NONE ? new int[]{0} : new int[]{1, 117}) {
          test("CREATIVE BOW471 modern=" + modern + " offhand=" + off + " ammo=" + source + " count=" + count, () -> {
            s.reset(); mode(modern); CreativeVehicleChecks.mode(mc, "CREATIVE");
            ItemStack bow = OffhandActions400State.tagged(261, 1, 383, "creative-bow-471");
            ItemStack ammo = source == Ammo.NONE ? null : OffhandActions400State.tagged(262, count, 5, "creative-ammo-471");
            s.hands(off ? source == Ammo.OPPOSITE ? ammo : null : bow,
                off ? bow : source == Ammo.OPPOSITE ? ammo : null);
            if (source == Ammo.STORAGE) mc.player.inventory.main[35] = ammo;
            String[] before = s.items();
            s.use();
            check(Arrays.equals(before, s.items()), "starting Creative use changed native item data/count");
            finish(s, bow, modern);
            check(s.arrows() == 1 && Arrays.equals(before, s.items()), "Creative shot failed or consumed native items");
            check(Offhand.stack(mc.player, off ? HandUse.Hand.OFF : HandUse.Hand.MAIN) == bow,
                "Creative use replaced the owned bow");
            check(source == Ammo.NONE || (source == Ammo.STORAGE ? mc.player.inventory.main[35]
                : Offhand.stack(mc.player, off ? HandUse.Hand.MAIN : HandUse.Hand.OFF)) == ammo,
                "Creative use replaced the owned ammunition");
            class_410 arrow = arrow(s);
            check(((ModernArrowData)(Object)arrow).power$isModernArrow() == modern, "wrong native/modern arrow simulation");
            check(!arrow.field_1574, "Creative arrow can create survival pickup ammunition");
            if (!modern) nativeFlight(s, arrow);
            NbtCompound nbt = new NbtCompound(); arrow.writeNbt(nbt);
            check(!nbt.getBoolean("player"), "native save restored pickup ammunition");
            nbt.putByte("inGround", (byte)1); nbt.putByte("shake", (byte)0);
            class_410 restored = new class_410(mc.world); restored.readtNbt(nbt); s.owned.add(restored);
            CreativeVehicleChecks.mode(mc, "SURVIVAL");
            restored.method_1354(mc.player);
            check(!restored.field_1574 && !restored.dead && Arrays.equals(before, s.items()),
                "saved Creative projectile added survival ammunition on collision");
          });
        }
      for (boolean modern : new boolean[]{false, true}) for (boolean off : new boolean[]{false, true}) {
        test("CREATIVE BOW471 survival gate modern=" + modern + " offhand=" + off, () -> {
          s.reset(); mode(modern);
          ItemStack bow = OffhandActions400State.tagged(261, 1, 12, "survival-bow-471");
          s.hands(off ? null : bow, off ? bow : null);
          String[] empty = s.items(); s.use(); ModernBow.release(mc); s.nativeState.finish();
          check(s.arrows() == 0 && ModernBow.hand(mc.player) == null && Arrays.equals(empty, s.items()),
              "Survival fired without ammunition");
          ItemStack ammo = OffhandActions400State.tagged(262, 4, 5, "survival-ammo-471");
          s.hands(off ? ammo : bow, off ? bow : ammo); String[] before = s.items();
          s.use(); finish(s, bow, modern);
          ItemStack remaining = Offhand.stack(mc.player, off ? HandUse.Hand.MAIN : HandUse.Hand.OFF);
          ItemStack used = Offhand.stack(mc.player, off ? HandUse.Hand.OFF : HandUse.Hand.MAIN);
          check(s.arrows() == 1 && remaining.count == 3 && used.getDamage() == (modern ? 13 : 12),
              "Survival ammo/durability changed");
          check(arrow(s).field_1574, "Survival arrow lost native pickup permission");
          ItemStack expected = OffhandActions400State.tagged(262, 3, 5, "survival-ammo-471");
          check(AutoTools400State.bytes(expected).equals(AutoTools400State.bytes(remaining)), "Survival consumed ammunition NBT");
          s.unchanged(before, 0, 45);
        });
        test("CREATIVE BOW471 remote authority modern=" + modern + " offhand=" + off, () -> {
          s.reset(); mode(modern); CreativeVehicleChecks.mode(mc, "CREATIVE");
          ItemStack bow = new ItemStack(261, 1, 12), ammo = new ItemStack(262, 4, 0);
          s.hands(off ? null : bow, off ? bow : null); mc.player.inventory.main[35] = ammo;
          mc.world.isRemote = true;
          try (HandUse ignored = HandUse.begin(mc.player.inventory, off ? HandUse.Hand.OFF : HandUse.Hand.MAIN, false)) {
            bow.method_698(mc.world, mc.player);
          } finally { mc.world.isRemote = false; }
          check(ammo.count == 3 && bow.getDamage() == 12 && ModernBow.hand(mc.player) == null && s.arrows() == 0,
              "Creative adaptation replaced the remote native item path");
        });
      }
      test("CREATIVE BOW471 main hand without offhand feature", () -> {
        s.reset(); mode(false); CreativeVehicleChecks.mode(mc, "CREATIVE");
        var disabled = new OffhandSettings(); disabled.enabled = false; OffhandConfig.preview(disabled);
        s.hands(OffhandActions400State.tagged(261, 1, 383, "offhand-disabled"), null);
        String[] before = s.items(); s.use();
        check(s.arrows() == 1 && Arrays.equals(before, s.items()), "Creative native shot depends on enabling offhand");
      });
    } finally {
      ModernBow.cancel(); GameplayConfig.preview(settings); UseInput.register(held);
    }
    log("CREATIVE BOW471 checks complete failures=" + failures);
  }

  private static void mode(boolean modern) {
    ModernBow.cancel(); var settings = GameplayConfig.copy(); settings.modernBowHandling = modern; GameplayConfig.preview(settings);
  }
  private static void finish(OffhandActions400State s, ItemStack bow, boolean modern) throws Exception {
    if (!modern) {
      check(ModernBow.hand(s.mc.player) == null && s.arrows() == 1, "native Beta shot waited for a draw/release");
      return;
    }
    check(ModernBow.using(s.mc.player, bow) && s.arrows() == 0, "modern shot did not wait for release");
    for (int t = ModernBow.ticks(s.mc.player, bow); t < 20; t++) ModernBow.tick(s.mc);
    Set<Entity> before = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Object entity : s.mc.world.field_198) before.add((Entity)entity);
    ModernBow.release(s.mc); s.nativeState.finish();
    for (Object entity : s.mc.world.field_198) if (!before.contains(entity)) s.owned.add((Entity)entity);
  }
  private static class_410 arrow(OffhandActions400State s) {
    return (class_410)s.owned.stream().filter(e -> e instanceof class_410 && !e.dead).findFirst().orElseThrow();
  }
  private static void nativeFlight(OffhandActions400State s, class_410 arrow) {
    double launch = Math.sqrt(arrow.velocityX * arrow.velocityX + arrow.velocityY * arrow.velocityY + arrow.velocityZ * arrow.velocityZ);
    check(launch > 1.3 && launch < 1.7, "native Beta 1.5 launch speed changed");
    arrow.method_1340(s.nativeState.x + .5, 140, s.nativeState.z + .5);
    arrow.velocityX = arrow.velocityZ = 0; arrow.velocityY = 1.5;
    arrow.tick();
    check(Math.abs(arrow.y - 141.5) < 1e-9 && Math.abs(arrow.velocityY - (1.5 * (double).99f - (double).03f)) < 1e-9,
        "Creative shot changed native Beta air inertia/gravity");
  }
}
