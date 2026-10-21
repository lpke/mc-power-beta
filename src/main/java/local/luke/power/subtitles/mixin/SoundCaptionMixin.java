package local.luke.power.subtitles.mixin;

import local.luke.power.input.*;
import local.luke.power.subtitles.*;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class SoundCaptionMixin {
  @Inject(method = "method_2015", at = @At("HEAD"))
  private void power$positionalCaption(String id, float x, float y, float z, float volume, float pitch, CallbackInfo ci) {
    if (!Subtitles.enabled() || SubtitleForwarding.consume(id, x, y, z, volume, pitch)) return;
    Subtitles.positional((SoundManager)(Object)this, id, x, y, z, volume);
  }
  @Inject(method = "method_2009", at = @At("HEAD"))
  private void power$relativeCaption(String id, float volume, float pitch, CallbackInfo ci) {
    if (Subtitles.enabled()) Subtitles.relative((SoundManager)(Object)this, id, volume);
  }
  @Inject(method = "method_2013", at = @At("HEAD"))
  private void power$listener(LivingEntity entity, float delta, CallbackInfo ci) {
    if (!AudioListenerPose.interested() || entity == null || DetachedCamera.isActive()) return;
    float yaw = entity.prevYaw + (entity.yaw - entity.prevYaw) * delta;
    float angle = -yaw * .017453292F - 3.1415927F;
    AudioListenerPose.publish(entity.world, entity.prevX + (entity.x - entity.prevX) * delta,
        entity.prevY + (entity.y - entity.prevY) * delta, entity.prevZ + (entity.z - entity.prevZ) * delta,
        -MathHelper.sin(angle), -MathHelper.cos(angle));
  }
}
