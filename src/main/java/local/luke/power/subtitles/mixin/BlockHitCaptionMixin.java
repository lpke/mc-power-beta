package local.luke.power.subtitles.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.SoundCaptionContext;
import net.minecraft.*;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SingleplayerInteractionManager.class, MultiplayerInteractionManager.class})
public abstract class BlockHitCaptionMixin {
  @WrapOperation(method = "method_1721", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundManager;method_2015(Ljava/lang/String;FFFFF)V"))
  private void power$hit(SoundManager sound, String id, float x, float y, float z, float volume, float pitch, Operation<Void> original) {
    try (var ignored = SoundCaptionContext.push("subtitles.block.generic.hit")) { original.call(sound, id, x, y, z, volume, pitch); }
  }
}
