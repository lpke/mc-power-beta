package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.util.Random;
import local.luke.power.audio.*;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** A successful native lava hit supplies the sound, without changing damage or ignition. */
@Mixin(Entity.class)
public abstract class LavaBurnSoundMixin {
  @Unique private static final Random power$burnRandom = new Random();
  @WrapOperation(method = "method_1332", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$lavaBurn(Entity entity, Entity attacker, int damage, Operation<Boolean> original) {
    int before = entity instanceof ItemLavaSoundState item ? item.power$itemSoundHealth() : 0;
    boolean accepted = original.call(entity, attacker, damage);
    boolean result = accepted;
    if (!AudioConfig.current().lavaBurnSounds || !FluidSounds.splashOwner(entity)) return result;
    if (entity instanceof ItemLavaSoundState item) {
      int health = item.power$itemSoundHealth();
      accepted = accepted || health < before;
      if (health > 0 && entity.field_1645 % 10 != 0) return result;
    }
    if (accepted) FluidSounds.entitySound(entity, "entity.generic.burn", .4f, 2 + power$burnRandom.nextFloat() * .4f);
    return result;
  }
}
