package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import local.luke.power.audio.LadderSounds;
import net.minecraft.block.Block;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldRenderer.class)
public abstract class LadderBreakSoundMixin {
  @Shadow private World field_1805;
  @WrapOperation(method = "method_1152", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/sound/SoundManager;method_2015(Ljava/lang/String;FFFFF)V"))
  private void power$ladder(SoundManager manager, String sound, float x, float y, float z, float volume,
      float pitch, Operation<Void> original, @Local Block block) {
    original.call(manager, LadderSounds.enabled(field_1805) && block == Block.field_1854
        ? "power_beta:block.ladder.break" : sound, x, y, z, volume, pitch);
  }
}
