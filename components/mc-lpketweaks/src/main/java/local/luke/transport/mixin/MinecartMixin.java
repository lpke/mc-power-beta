package local.luke.transport.mixin;

import net.minecraft.class_549;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BTA advances minecart physics in two 0.4-block steps per game tick. Reuse Beta's rail following,
 * collision and inventory code rather than replace entity classes, registrations or save formats.
 * The re-entry is bounded to one.
 */
@Mixin(class_549.class)
public abstract class MinecartMixin extends Entity {
  @Shadow public int field_2272; // damage animation
  @Shadow public int field_2273; // time since hit
  @Shadow public double field_2277; // furnace push X
  @Shadow public double field_2278; // furnace push Z
  @Unique private boolean transport$secondStep;

  protected MinecartMixin(World world) {
    super(world);
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void transport$keepAnimationAtNormalRate(CallbackInfo ci) {
    if (transport$secondStep) {
      // tick() decrements these immediately; BTA does that only once.
      if (field_2272 > 0 && field_2272 < Integer.MAX_VALUE) field_2272++;
      if (field_2273 > 0 && field_2273 < Integer.MAX_VALUE) field_2273++;
    }
  }

  @Inject(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/class_549;method_1340(DDD)V",
              ordinal = 2,
              shift = At.Shift.AFTER))
  private void transport$pushFromInside(CallbackInfo ci) {
    if (!world.isRemote
        && local.luke.tweaks.config.Config.current().fastMinecarts
        && field_1594 instanceof PlayerEntity
        && Math.hypot(velocityX, velocityZ) < 0.01f) {
      velocityX += field_1594.velocityX * 0.05;
      velocityZ += field_1594.velocityZ * 0.05;
    }
  }

  @Inject(
      method = "tick",
      at = @At(value = "FIELD", target = "Lnet/minecraft/class_549;field_2275:I", ordinal = 1))
  private void transport$followFurnaceMotion(CallbackInfo ci) {
    // BTA follows the new rail direction, including after a reversal. Beta
    // otherwise clears the furnace's push when it opposes current motion.
    if (!world.isRemote
        && local.luke.tweaks.config.Config.current().fastMinecarts
        && Math.hypot(field_2277, field_2278) > 0.01
        && Math.hypot(velocityX, velocityZ) > 0.001) {
      field_2277 = velocityX;
      field_2278 = velocityZ;
    }
  }

  @ModifyVariable(method = "method_1353", at = @At("STORE"), ordinal = 6)
  private double transport$removeVanillaCollisionGate(double value) {
    // BTA removes Beta's position-dependent early return when carts meet.
    return world.isRemote || !local.luke.tweaks.config.Config.current().fastMinecarts ? value : 0.0;
  }

  @Inject(method = "method_1353", at = @At("HEAD"), cancellable = true)
  private void transport$ignoreUnpushableEntities(Entity other, CallbackInfo ci) {
    if (!world.isRemote
        && local.luke.tweaks.config.Config.current().fastMinecarts
        && !other.method_1380()) ci.cancel();
  }

  @Inject(method = "tick", at = @At("RETURN"))
  private void transport$advanceSecondStep(CallbackInfo ci) {
    if (world.isRemote
        || !local.luke.tweaks.config.Config.current().fastMinecarts
        || dead
        || transport$secondStep) return;
    double startX = prevX;
    double startY = prevY;
    double startZ = prevZ;
    transport$secondStep = true;
    try {
      tick();
    } finally {
      transport$secondStep = false;
      // The renderer must interpolate over the entire tick, not half of it.
      prevX = startX;
      prevY = startY;
      prevZ = startZ;
    }
  }
}
