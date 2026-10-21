package local.luke.power.subtitles;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.input.SoundCaptionContext;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class CaptionCalls {
  private CaptionCalls() {}
  public static void entity(String context, World world, Entity entity, String sound, float volume, float pitch, Operation<Void> original) {
    if (!SoundCaptionContext.enabled()) { original.call(world, entity, sound, volume, pitch); return; }
    try (var ignored = SoundCaptionContext.push(context)) { original.call(world, entity, sound, volume, pitch); }
  }
  public static void position(String context, World world, double x, double y, double z, String sound, float volume, float pitch, Operation<Void> original) {
    if (!SoundCaptionContext.enabled()) { original.call(world, x, y, z, sound, volume, pitch); return; }
    try (var ignored = SoundCaptionContext.push(context)) { original.call(world, x, y, z, sound, volume, pitch); }
  }
}
