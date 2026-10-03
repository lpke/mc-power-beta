package local.luke.power.creative.inventory.mixin.common;

import local.luke.power.creative.Modes;
import local.luke.power.creative.api.*;
import local.luke.power.creative.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.level.Level;
import net.minecraft.util.io.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import local.luke.power.creative.inventory.CreativeInventory;
import local.luke.power.creative.inventory.interfaces.CreativePlayer;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements CreativePlayer, ModePlayer {
  @Unique private boolean power$spectator;
  @Unique private GameMode power$previous = GameMode.CREATIVE;
  @Unique private float power$spectatorSpeed = .05f;

  public PlayerEntityMixin(Level level) {
    super(level);
  }

  @Override
  public boolean creative_isCreative() {
    return CreativeInventory.toBool(dataTracker.getByte(CreativeInventory.IS_CREATIVE_ID));
  }

  @Override
  public void creative_setCreative(boolean creative) {
    power_applyMode(creative ? GameMode.CREATIVE : GameMode.SURVIVAL);
  }

  @Override
  public boolean creative_isFlying() {
    return CreativeInventory.toBool(dataTracker.getByte(CreativeInventory.IS_FLYING_ID));
  }

  @Override
  public void creative_setFlying(boolean flying) {
    boolean next = power$spectator || flying && creative_isCreative();
    dataTracker.setData(CreativeInventory.IS_FLYING_ID, CreativeInventory.toByte(next));
    if (!next) velocityY = 0;
    fallDistance = 0;
  }

  @Override
  public GameMode power_mode() {
    return power$spectator
        ? GameMode.SPECTATOR
        : creative_isCreative() ? GameMode.CREATIVE : GameMode.SURVIVAL;
  }

  @Override
  public GameMode power_previousMode() {
    return power$previous;
  }

  @Override
  public void power_applyMode(GameMode mode) {
    GameMode old = power_mode();
    if (old == mode) return;
    if (power$spectator && !level.isRemote && !Modes.findExit((PlayerEntity) (Object) this)) return;
    power$previous = old;
    power$spectator = mode == GameMode.SPECTATOR;
    dataTracker.setData(CreativeInventory.IS_CREATIVE_ID, CreativeInventory.toByte(mode == GameMode.CREATIVE));
    immuneToFire = mode != GameMode.SURVIVAL;
    updateFromBB = power$spectator;
    creative_setFlying(power$spectator || mode == GameMode.CREATIVE && old == GameMode.SPECTATOR);
    velocityX = velocityY = velocityZ = 0;
    fallDistance = 0;
    fire = 0;
    if (power$spectator) {
      onGround = false;
      power$spectatorSpeed = .05f * Config.current().spectatorSpeed / 100;
    }
  }

  @Override
  public float power_spectatorSpeed() {
    return power$spectatorSpeed;
  }

  @Override
  public void power_spectatorSpeed(float speed) {
    power$spectatorSpeed = Float.isFinite(speed) ? Math.max(0, Math.min(.2f, speed)) : .05f;
  }

  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void power$damage(Entity source, int amount, CallbackInfoReturnable<Boolean> ci) {
    if (creative_isCreative() || power$spectator) ci.setReturnValue(false);
  }

  @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
  private void power$damage(int amount, CallbackInfo ci) {
    if (creative_isCreative() || power$spectator) ci.cancel();
  }

  @Inject(method = "writeCustomDataToTag", at = @At("TAIL"))
  private void power$write(CompoundTag tag, CallbackInfo ci) {
    tag.put("Creative", creative_isCreative());
    tag.put("Flying", creative_isFlying());
    tag.put("PowerBetaGameMode", power_mode().name());
    tag.put("PowerBetaPreviousGameMode", power$previous.name());
    tag.put("PowerBetaSpectatorSpeed", power$spectatorSpeed);
  }

  @Inject(method = "readCustomDataFromTag", at = @At("TAIL"))
  private void power$read(CompoundTag tag, CallbackInfo ci) {
    for (String key : new String[]{"PowerBetaGameMode","PowerBetaPreviousGameMode"}) {
      String old = local.luke.power.storage.LegacyKeys.original(key);
      if (!tag.containsKey(key) && tag.containsKey(old)) tag.put(key,tag.getString(old));
    }
    String oldSpeed=local.luke.power.storage.LegacyKeys.original("PowerBetaSpectatorSpeed");
    if (!tag.containsKey("PowerBetaSpectatorSpeed") && tag.containsKey(oldSpeed))
      tag.put("PowerBetaSpectatorSpeed",tag.getFloat(oldSpeed));
    GameMode mode = tag.getBoolean("Creative") ? GameMode.CREATIVE : GameMode.SURVIVAL;
    if (tag.containsKey("PowerBetaGameMode")) {
      try {
        mode = GameMode.valueOf(tag.getString("PowerBetaGameMode"));
      } catch (IllegalArgumentException ignored) {
      }
    }
    power$spectator = mode == GameMode.SPECTATOR;
    dataTracker.setData(CreativeInventory.IS_CREATIVE_ID, CreativeInventory.toByte(mode == GameMode.CREATIVE));
    immuneToFire = mode != GameMode.SURVIVAL;
    updateFromBB = power$spectator;
    creative_setFlying(power$spectator || mode == GameMode.CREATIVE && tag.getBoolean("Flying"));
    try {
      power$previous = GameMode.valueOf(tag.getString("PowerBetaPreviousGameMode"));
    } catch (IllegalArgumentException ignored) {
      power$previous = mode == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE;
    }
    power_spectatorSpeed(
        tag.containsKey("PowerBetaSpectatorSpeed") ? tag.getFloat("PowerBetaSpectatorSpeed") : .05f);
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void power$tick(CallbackInfo ci) {
    if (power$spectator) {
      updateFromBB = true;
      onGround = false;
      air = 300;
      fire = 0;
    }
    if (creative_isCreative() || power$spectator) {
      immuneToFire = true;
      fallDistance = 0;
    }
    if (creative_isFlying() && !power$spectator && (isSleeping() || vehicle != null))
      creative_setFlying(false);
  }

  @Inject(method = "onCollisionFromEntity", at = @At("HEAD"), cancellable = true)
  private void power$pickup(Entity entity, CallbackInfo ci) {
    if (power$spectator) ci.cancel();
  }

  @Inject(
      method = "initDataTracker",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/living/LivingEntity;initDataTracker()V",
              shift = At.Shift.AFTER))
  private void power$track(CallbackInfo ci) {
    dataTracker.startTracking(CreativeInventory.IS_CREATIVE_ID, (byte) 0);
    dataTracker.startTracking(CreativeInventory.IS_FLYING_ID, (byte) 0);
  }
}
