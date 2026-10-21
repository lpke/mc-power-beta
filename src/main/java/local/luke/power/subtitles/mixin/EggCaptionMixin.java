package local.luke.power.subtitles.mixin;

import local.luke.power.subtitles.CaptionCalls;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_280.class)
public abstract class EggCaptionMixin {
  @WrapOperation(method = "method_451", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$caption(World world, Entity source, String sound, float volume, float pitch, Operation<Void> original) {
    CaptionCalls.entity("subtitles.entity.egg.throw", world, source, sound, volume, pitch, original);
  }
}
