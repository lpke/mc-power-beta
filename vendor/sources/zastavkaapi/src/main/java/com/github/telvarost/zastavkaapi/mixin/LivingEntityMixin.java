package com.github.telvarost.zastavkaapi.mixin;

import com.github.telvarost.zastavkaapi.interfaces.FrozenInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements FrozenInterface {
    @Shadow
    public float swingAnimationProgress;
    @Shadow
    public float lastSwingAnimationProgress;
    @Shadow
    public float walkAnimationProgress;
    @Shadow
    public float walkAnimationSpeed;
    @Shadow
    public float lastWalkAnimationSpeed;
    @Shadow
    public float bodyYaw;
    @Shadow
    public float lastBodyYaw;
    @Shadow
    public int deathTime;

    @Shadow public float damagedSwingDir;

    @Shadow public abstract void tickMovement();

    @Unique
    float swingAnimationProgressHeld;
    @Unique
    float walkAnimationProgressHeld;
    @Unique
    float walkAnimationSpeedHeld;
    @Unique
    float bodyYawHeld;
    @Unique
    float yawHeld;
    @Unique
    float pitchHeld;
    @Unique
    int deathTimeHeld;

    @Unique
    int delayCancelMovementTicks = 3;
    @Unique
    int _frozenTicks = 0;

    public LivingEntityMixin(World world) {
        super(world);
    }

    @Override
    public int zastavkaApi_getFrozenTicks() {
        return _frozenTicks;
    }

    @Override
    public void zastavkaApi_setFrozenTicks(int frozenTicks) {
        /** - If the entity is not frozen delay freeze movement effect by 3 ticks */
        if (0 >= _frozenTicks) {
            delayCancelMovementTicks = 3;
        }

        _frozenTicks = frozenTicks;
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void zastavkaApi_writeCustomDataToTag(NbtCompound tag, CallbackInfo info) {
        tag.putInt("Frozen", _frozenTicks);
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void zastavkaApi_readCustomDataFromTag(NbtCompound tag, CallbackInfo info) {
        _frozenTicks = tag.getInt("Frozen");
    }

    @Inject(method = "damage", at = @At("HEAD"))
    public void damage(Entity damageSource, int amount, CallbackInfoReturnable<Boolean> cir) {
        if (0 < this.zastavkaApi_getFrozenTicks()) {
            delayCancelMovementTicks = 3;

            if (this.isOnFire() && !(damageSource instanceof PlayerEntity)) {
                this.zastavkaApi_setFrozenTicks(0);
            }
        }
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;tickMovement()V"
            ),
            cancellable = true
    )
    public void zastavkaApi_tickMovement(CallbackInfo ci) {
        if ((0 < this.zastavkaApi_getFrozenTicks()) && this.isAlive()) {
            if (!this.onGround && !this.isWet()) {
                this.tickMovement();
            } else {
                velocityX = 0.0F;
                velocityZ = 0.0F;
            }

            horizontalSpeed = 0.0F;
            damagedSwingDir = 0.0F;

            if (0 < delayCancelMovementTicks) {
                delayCancelMovementTicks--;
                swingAnimationProgressHeld = lastSwingAnimationProgress;
                walkAnimationProgressHeld = walkAnimationProgress;
                walkAnimationSpeedHeld = lastWalkAnimationSpeed;
                bodyYawHeld = lastBodyYaw;
                yawHeld = prevYaw;
                pitchHeld = prevPitch;
                deathTimeHeld = deathTime;
            } else {
                _frozenTicks--;
                swingAnimationProgress = swingAnimationProgressHeld;
                lastSwingAnimationProgress = swingAnimationProgressHeld;
                walkAnimationProgress = walkAnimationProgressHeld;
                walkAnimationSpeed = walkAnimationSpeedHeld;
                lastWalkAnimationSpeed = walkAnimationSpeedHeld;
                bodyYaw = bodyYawHeld;
                lastBodyYaw = bodyYawHeld;
                yaw = yawHeld;
                prevYaw = yawHeld;
                pitch = pitchHeld;
                prevPitch = pitchHeld;
                deathTime = deathTimeHeld;
                ci.cancel();
            }
        }
    }
}
