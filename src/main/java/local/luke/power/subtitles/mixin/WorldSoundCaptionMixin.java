package local.luke.power.subtitles.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.SoundCaptionContext;
import local.luke.power.subtitles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldSoundCaptionMixin {
  @Shadow private Minecraft field_1814;
  @Shadow private World field_1805;
  @Inject(method = "method_1154", at = @At("HEAD"))
  private void power$worldCaption(String id, double x, double y, double z, float volume, float pitch, CallbackInfo ci) {
    if (Subtitles.enabled()) Subtitles.positional(field_1805, field_1814.soundManager, id, x, y, z, volume);
  }
  @WrapOperation(method = "method_1154", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundManager;method_2015(Ljava/lang/String;FFFFF)V"))
  private void power$forward(SoundManager manager, String id, float x, float y, float z, float volume, float pitch, Operation<Void> original) {
    if (!Subtitles.enabled()) { original.call(manager, id, x, y, z, volume, pitch); return; }
    try (var ignored = SubtitleForwarding.push(id, x, y, z, volume, pitch)) { original.call(manager, id, x, y, z, volume, pitch); }
  }
  @WrapOperation(method = "method_1152", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundManager;method_2015(Ljava/lang/String;FFFFF)V"))
  private void power$blockBreak(SoundManager manager, String id, float x, float y, float z, float volume, float pitch, Operation<Void> original) {
    try (var ignored = SoundCaptionContext.push("subtitles.block.generic.break")) { original.call(manager, id, x, y, z, volume, pitch); }
  }
  @WrapOperation(method = "method_1152", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_150(DDDLjava/lang/String;FF)V"))
  private void power$worldEvent(World world, double x, double y, double z, String id, float volume, float pitch, Operation<Void> original,
      PlayerEntity player, int event, int eventX, int eventY, int eventZ, int data) {
    String caption = switch (event) {
      case 1000, 1002 -> "subtitles.block.dispenser.dispense";
      case 1001 -> "subtitles.block.dispenser.fail";
      case 1004 -> "subtitles.block.fire.extinguish";
      default -> null;
    };
    try (var ignored = SoundCaptionContext.push(caption)) { original.call(world, x, y, z, id, volume, pitch); }
  }
}
