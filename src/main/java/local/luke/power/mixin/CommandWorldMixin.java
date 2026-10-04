package local.luke.power.mixin;

import java.util.UUID;
import local.luke.power.commands.CommandWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldProperties.class)
public abstract class CommandWorldMixin implements CommandWorld {
  @Unique private String power$commandId = UUID.randomUUID().toString();

  public String power$commandWorldId() {
    return power$commandId;
  }

  @Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
  private void power$read(NbtCompound tag, CallbackInfo ci) {
    String value = tag.getString("PowerBetaCommandWorldId");
    try {
      power$commandId = UUID.fromString(value).toString();
    } catch (IllegalArgumentException ignored) {
      /* Older worlds acquire an ID on their next save. */
    }
  }

  @Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("RETURN"))
  private void power$copy(WorldProperties other, CallbackInfo ci) {
    power$commandId = ((CommandWorld) other).power$commandWorldId();
  }

  @Inject(method = "updateProperties", at = @At("RETURN"))
  private void power$write(NbtCompound tag, NbtCompound player, CallbackInfo ci) {
    tag.putString("PowerBetaCommandWorldId", power$commandId);
  }
}
