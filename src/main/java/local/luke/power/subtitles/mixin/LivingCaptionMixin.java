package local.luke.power.subtitles.mixin;

import local.luke.power.subtitles.CaptionCalls;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingCaptionMixin {
  @WrapOperation(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V", ordinal = 0))
  private void power$death(World world, Entity source, String sound, float volume, float pitch, Operation<Void> original) {
    CaptionCalls.entity(source instanceof PlayerEntity ? "player-death" : "death", world, source, sound, volume, pitch, original);
  }
  @WrapOperation(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V", ordinal = 1))
  private void power$hurt(World world, Entity source, String sound, float volume, float pitch, Operation<Void> original) {
    CaptionCalls.entity(source instanceof PlayerEntity ? "player-hurt" : "hurt", world, source, sound, volume, pitch, original);
  }
  @WrapOperation(method = "method_1313", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V", ordinal = 0))
  private void power$networkHurt(World world, Entity source, String sound, float volume, float pitch, Operation<Void> original) {
    CaptionCalls.entity(source instanceof PlayerEntity ? "player-hurt" : "hurt", world, source, sound, volume, pitch, original);
  }
  @WrapOperation(method = "method_1313", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V", ordinal = 1))
  private void power$networkDeath(World world, Entity source, String sound, float volume, float pitch, Operation<Void> original) {
    CaptionCalls.entity(source instanceof PlayerEntity ? "player-death" : "death", world, source, sound, volume, pitch, original);
  }
  @WrapOperation(method = "method_1389", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$landing(World world, Entity source, String sound, float volume, float pitch, Operation<Void> original) {
    CaptionCalls.entity("subtitles.block.generic.fall", world, source, sound, volume, pitch, original);
  }
}
