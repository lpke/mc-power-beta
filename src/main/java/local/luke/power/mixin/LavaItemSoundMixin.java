package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.*;
import net.minecraft.class_142;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_142.class)
public abstract class LavaItemSoundMixin implements ItemLavaSoundState {
  @Shadow private int field_569;
  @Override public int power$itemSoundHealth() { return field_569; }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$nativeBounce(World world, Entity entity, String sound, float volume, float pitch, Operation<Void> original) {
    if (sound.equals("random.fizz") && AudioConfig.current().lavaBurnSounds && FluidSounds.localWorld(world)) return;
    SoundCategoryScope.run("ambient", () -> original.call(world, entity, sound, volume, pitch));
  }
}
