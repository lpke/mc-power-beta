package local.luke.power.mixin;

import local.luke.power.world.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldProperties.class)
public abstract class WorldDifficultyPropertiesMixin implements WorldDifficulty {
  @Unique private int power$difficulty = WorldDifficulty.DEFAULT;
  public int power$difficulty() { return power$difficulty; }
  public void power$difficulty(int value) { power$difficulty = WorldDifficulty.checked(value); }

  @Inject(method = "<init>(JLjava/lang/String;)V", at = @At("RETURN"))
  private void power$create(long seed, String name, CallbackInfo ci) {
    power$difficulty = WorldCreation.difficulty();
  }

  @Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
  private void power$read(NbtCompound tag, CallbackInfo ci) {
    if (tag.contains(WorldDifficulty.TAG)) {
      int value = tag.getInt(WorldDifficulty.TAG);
      power$difficulty = value >= 0 && value <= 3 ? value : WorldDifficulty.DEFAULT;
    }
  }

  @Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("RETURN"))
  private void power$copy(WorldProperties source, CallbackInfo ci) {
    power$difficulty = ((WorldDifficulty) source).power$difficulty();
  }

  @Inject(method = "updateProperties", at = @At("RETURN"))
  private void power$write(NbtCompound tag, NbtCompound player, CallbackInfo ci) {
    tag.putInt(WorldDifficulty.TAG, power$difficulty);
  }
}
