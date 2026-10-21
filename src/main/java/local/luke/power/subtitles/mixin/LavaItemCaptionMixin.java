package local.luke.power.subtitles.mixin;

import local.luke.power.subtitles.CaptionCalls;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_142.class)
public abstract class LavaItemCaptionMixin {
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$caption(World world, Entity source, String sound, float volume, float pitch, Operation<Void> original) {
    CaptionCalls.entity(sound.equals("random.fizz") ? "subtitles.block.lava.extinguish" : null, world, source, sound, volume, pitch, original);
  }
}
