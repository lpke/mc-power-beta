package local.luke.transport.mixin;

import local.luke.transport.BoatMotion;
import net.minecraft.class_113;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** class_113 is BoatEntity in Barn build 8; field_1594 is its passenger. */
@Mixin(class_113.class)
public abstract class BoatMixin extends Entity {
  @Unique private final BoatMotion transport$motion = new BoatMotion();

  protected BoatMixin(World world) {
    super(world);
  }

  @Inject(
      method = "tick",
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/class_113;field_1594:Lnet/minecraft/entity/Entity;",
              ordinal = 0))
  private void transport$steer(CallbackInfo ci) {
    if (world.isRemote || !local.luke.tweaks.config.Config.current().boatSteering) return;
    float forward = 0.0f;
    float strafe = 0.0f;
    if (field_1594 instanceof ClientPlayerEntity player && player.field_161 != null) {
      // BTA's local player supplies the negated forward and strafe input.
      forward = -player.field_161.field_2533;
      strafe = -player.field_161.field_2532;
    }
    transport$motion.step(velocityX, velocityZ, yaw, forward, strafe, field_1594 != null);
    velocityX = transport$motion.x;
    velocityZ = transport$motion.z;
    yaw = transport$motion.yaw;
  }

  @Redirect(
      method = "tick",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;velocityX:D"))
  private double transport$ignoreRiderVelocityX(Entity rider) {
    return world.isRemote || !local.luke.tweaks.config.Config.current().boatSteering
        ? rider.velocityX
        : 0.0;
  }

  @Redirect(
      method = "tick",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;velocityZ:D"))
  private double transport$ignoreRiderVelocityZ(Entity rider) {
    return world.isRemote || !local.luke.tweaks.config.Config.current().boatSteering
        ? rider.velocityZ
        : 0.0;
  }

  @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.4))
  private double transport$speedLimit(double vanilla) {
    return world.isRemote || !local.luke.tweaks.config.Config.current().boatSteering
        ? vanilla
        : BoatMotion.MAX_SPEED;
  }

  @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;atan2(DD)D"))
  private double transport$keepSteeringHeading(double z, double x) {
    return !world.isRemote
            && local.luke.tweaks.config.Config.current().boatSteering
            && field_1594 != null
        ? Math.toRadians(yaw)
        : Math.atan2(z, x);
  }

  @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.9900000095367432))
  private double transport$horizontalDrag(double vanilla) {
    return world.isRemote || !local.luke.tweaks.config.Config.current().boatSteering
        ? vanilla
        : 0.99;
  }

  @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.949999988079071))
  private double transport$verticalDrag(double vanilla) {
    return world.isRemote || !local.luke.tweaks.config.Config.current().boatSteering
        ? vanilla
        : 0.95;
  }

  // Preserve the collision branch and its injection points for UniTweaks, but
  // remove collision destruction even if its boatsDontBreak setting is off.
  @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.15, ordinal = 1))
  private double transport$surviveCollisions(double vanilla) {
    return world.isRemote || !local.luke.tweaks.config.Config.current().boatSteering
        ? vanilla
        : Double.MAX_VALUE;
  }
}
