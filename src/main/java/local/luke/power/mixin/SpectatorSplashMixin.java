package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.input.PlayerMode;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class SpectatorSplashMixin {
  @WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$splash(World world, Entity entity, String sound, float volume, float pitch,
      Operation<Void> original) {
    if (sound.equals("random.splash") && entity instanceof PlayerMode mode && mode.power$spectator()) return;
    original.call(world, entity, sound, volume, pitch);
  }
}
