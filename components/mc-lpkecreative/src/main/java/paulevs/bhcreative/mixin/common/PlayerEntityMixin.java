package paulevs.bhcreative.mixin.common;

import local.luke.creative.Modes;
import local.luke.creative.api.*;
import local.luke.creative.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.level.Level;
import net.minecraft.util.io.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import paulevs.bhcreative.BHCreative;
import paulevs.bhcreative.interfaces.CreativePlayer;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements CreativePlayer, ModePlayer {
  @Unique private boolean lpke$spectator;
  @Unique private GameMode lpke$previous = GameMode.CREATIVE;
  @Unique private float lpke$spectatorSpeed = .05f;

  public PlayerEntityMixin(Level level) {
    super(level);
  }

  @Override
  public boolean creative_isCreative() {
    return BHCreative.toBool(dataTracker.getByte(BHCreative.IS_CREATIVE_ID));
  }

  @Override
  public void creative_setCreative(boolean creative) {
    lpke_applyMode(creative ? GameMode.CREATIVE : GameMode.SURVIVAL);
  }

  @Override
  public boolean creative_isFlying() {
    return BHCreative.toBool(dataTracker.getByte(BHCreative.IS_FLYING_ID));
  }

  @Override
  public void creative_setFlying(boolean flying) {
    boolean next = lpke$spectator || flying && creative_isCreative();
    dataTracker.setData(BHCreative.IS_FLYING_ID, BHCreative.toByte(next));
    if (!next) velocityY = 0;
    fallDistance = 0;
  }

  @Override
  public GameMode lpke_mode() {
    return lpke$spectator
        ? GameMode.SPECTATOR
        : creative_isCreative() ? GameMode.CREATIVE : GameMode.SURVIVAL;
  }

  @Override
  public GameMode lpke_previousMode() {
    return lpke$previous;
  }

  @Override
  public void lpke_applyMode(GameMode mode) {
    GameMode old = lpke_mode();
    if (old == mode) return;
    if (lpke$spectator && !level.isRemote && !Modes.findExit((PlayerEntity) (Object) this)) return;
    lpke$previous = old;
    lpke$spectator = mode == GameMode.SPECTATOR;
    dataTracker.setData(BHCreative.IS_CREATIVE_ID, BHCreative.toByte(mode == GameMode.CREATIVE));
    immuneToFire = mode != GameMode.SURVIVAL;
    updateFromBB = lpke$spectator;
    creative_setFlying(lpke$spectator || mode == GameMode.CREATIVE && old == GameMode.SPECTATOR);
    velocityX = velocityY = velocityZ = 0;
    fallDistance = 0;
    fire = 0;
    if (lpke$spectator) {
      onGround = false;
      lpke$spectatorSpeed = .05f * Config.current().spectatorSpeed / 100;
    }
  }

  @Override
  public float lpke_spectatorSpeed() {
    return lpke$spectatorSpeed;
  }

  @Override
  public void lpke_spectatorSpeed(float speed) {
    lpke$spectatorSpeed = Float.isFinite(speed) ? Math.max(0, Math.min(.2f, speed)) : .05f;
  }

  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void lpke$damage(Entity source, int amount, CallbackInfoReturnable<Boolean> ci) {
    if (creative_isCreative() || lpke$spectator) ci.setReturnValue(false);
  }

  @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
  private void lpke$damage(int amount, CallbackInfo ci) {
    if (creative_isCreative() || lpke$spectator) ci.cancel();
  }

  @Inject(method = "writeCustomDataToTag", at = @At("TAIL"))
  private void lpke$write(CompoundTag tag, CallbackInfo ci) {
    tag.put("Creative", creative_isCreative());
    tag.put("Flying", creative_isFlying());
    tag.put("LpkeGameMode", lpke_mode().name());
    tag.put("LpkePreviousGameMode", lpke$previous.name());
    tag.put("LpkeSpectatorSpeed", lpke$spectatorSpeed);
  }

  @Inject(method = "readCustomDataFromTag", at = @At("TAIL"))
  private void lpke$read(CompoundTag tag, CallbackInfo ci) {
    GameMode mode = tag.getBoolean("Creative") ? GameMode.CREATIVE : GameMode.SURVIVAL;
    if (tag.containsKey("LpkeGameMode")) {
      try {
        mode = GameMode.valueOf(tag.getString("LpkeGameMode"));
      } catch (IllegalArgumentException ignored) {
      }
    }
    lpke$spectator = mode == GameMode.SPECTATOR;
    dataTracker.setData(BHCreative.IS_CREATIVE_ID, BHCreative.toByte(mode == GameMode.CREATIVE));
    immuneToFire = mode != GameMode.SURVIVAL;
    updateFromBB = lpke$spectator;
    creative_setFlying(lpke$spectator || mode == GameMode.CREATIVE && tag.getBoolean("Flying"));
    try {
      lpke$previous = GameMode.valueOf(tag.getString("LpkePreviousGameMode"));
    } catch (IllegalArgumentException ignored) {
      lpke$previous = mode == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE;
    }
    lpke_spectatorSpeed(
        tag.containsKey("LpkeSpectatorSpeed") ? tag.getFloat("LpkeSpectatorSpeed") : .05f);
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void lpke$tick(CallbackInfo ci) {
    if (lpke$spectator) {
      updateFromBB = true;
      onGround = false;
      air = 300;
      fire = 0;
    }
    if (creative_isCreative() || lpke$spectator) {
      immuneToFire = true;
      fallDistance = 0;
    }
    if (creative_isFlying() && !lpke$spectator && (isSleeping() || vehicle != null))
      creative_setFlying(false);
  }

  @Inject(method = "onCollisionFromEntity", at = @At("HEAD"), cancellable = true)
  private void lpke$pickup(Entity entity, CallbackInfo ci) {
    if (lpke$spectator) ci.cancel();
  }

  @Inject(
      method = "initDataTracker",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/living/LivingEntity;initDataTracker()V",
              shift = At.Shift.AFTER))
  private void lpke$track(CallbackInfo ci) {
    dataTracker.startTracking(BHCreative.IS_CREATIVE_ID, (byte) 0);
    dataTracker.startTracking(BHCreative.IS_FLYING_ID, (byte) 0);
  }
}
