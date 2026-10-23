package local.luke.power.subtitles.mixin;

import local.luke.power.subtitles.CaptionCalls;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_385.class)
public abstract class LavaCaptionMixin {
  @WrapOperation(method = "method_1221", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_150(DDDLjava/lang/String;FF)V"))
  private void power$caption(World world, double x, double y, double z, String sound, float volume, float pitch, Operation<Void> original) {
    local.luke.power.audio.SoundCategoryScope.run("blocks", () ->
        CaptionCalls.position("subtitles.block.lava.extinguish", world, x, y, z, sound, volume, pitch, original));
  }
}
