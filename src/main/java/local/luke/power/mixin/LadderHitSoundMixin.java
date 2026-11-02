package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.LadderSounds;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SingleplayerInteractionManager.class, MultiplayerInteractionManager.class})
public abstract class LadderHitSoundMixin {
  @WrapOperation(method = "method_1721", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/sound/SoundManager;method_2015(Ljava/lang/String;FFFFF)V"))
  private void power$ladder(SoundManager manager, String sound, float x, float y, float z, float volume,
      float pitch, Operation<Void> original) {
    var mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    original.call(manager, LadderSounds.at(mc.world, x, y, z) ? "power_beta:block.ladder.hit" : sound, x, y, z, volume, pitch);
  }
}
