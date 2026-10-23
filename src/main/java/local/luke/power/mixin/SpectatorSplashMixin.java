package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.input.PlayerMode;
import local.luke.power.audio.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class SpectatorSplashMixin implements FluidSoundContact {
  @Shadow protected boolean field_1649;
  @Unique private boolean power$audioWet;
  @Unique private boolean power$contactReady;
  @Unique private static final java.util.Random power$splashRandom = new java.util.Random();
  @Override public boolean power$audioInWater() { return power$audioWet; }

  @Inject(method = "baseTick", at = @At("HEAD"))
  private void power$modernContact(CallbackInfo ci) {
    Entity entity = (Entity)(Object)this;
    if (!(entity instanceof net.minecraft.entity.player.ClientPlayerEntity)) return;
    var settings = AudioConfig.current();
    if (!(settings.waterSplashSounds || settings.swimmingSounds) || !FluidSounds.localPlayer(entity)) {
      power$audioWet = power$contactReady = false; return;
    }
    boolean wet = FluidSounds.bodyInWater(entity);
    if (power$contactReady && wet && !power$audioWet && !field_1649 && settings.waterSplashSounds) {
      float volume = FluidSoundMath.volume(entity.velocityX, entity.velocityY, entity.velocityZ, .2f);
      entity.world.method_150(entity.x, entity.boundingBox.minY, entity.z, FluidSoundMath.splash(volume), volume,
          1 + (power$splashRandom.nextFloat() - power$splashRandom.nextFloat()) * .4f);
    }
    power$audioWet = wet;
    power$contactReady = true;
  }
  @WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$splash(World world, Entity entity, String sound, float volume, float pitch,
      Operation<Void> original) {
    if (sound.equals("random.splash") && entity instanceof PlayerMode mode && mode.power$spectator()) return;
    if (sound.equals("random.splash") && AudioConfig.current().waterSplashSounds && FluidSounds.localPlayer(entity))
      return;
    original.call(world, entity, sound, volume, pitch);
  }
}
