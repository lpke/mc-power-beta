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
public abstract class CommandWorldMixin
    implements CommandWorld, local.luke.power.permissions.CheatWorld, local.luke.power.world.WorldInventory {
  @Unique private boolean power$cheats;
  @Unique private boolean power$keepInventory;

  public boolean power$keepInventory() { return power$keepInventory && power$cheatsEnabled(); }
  public void power$keepInventory(boolean enabled) { power$keepInventory = enabled && power$cheatsEnabled(); }

  public boolean power$cheatsEnabled() {
    return power$cheats;
  }

  public void power$cheatsEnabled(boolean enabled) {
    power$cheats = enabled;
    if (!enabled) power$keepInventory = false;
  }

  @Unique private String power$commandId = UUID.randomUUID().toString();

  public String power$commandWorldId() {
    return power$commandId;
  }

  @Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
  private void power$read(NbtCompound tag, CallbackInfo ci) {
    power$cheats = tag.getBoolean(TAG);
    power$keepInventory = power$cheats && tag.getBoolean("PowerBetaKeepInventory");
    String value = tag.getString("PowerBetaCommandWorldId");
    try {
      power$commandId = UUID.fromString(value).toString();
    } catch (IllegalArgumentException ignored) {
      /* New worlds receive the generated ID on save. */
    }
  }

  @Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("RETURN"))
  private void power$copy(WorldProperties other, CallbackInfo ci) {
    power$commandId = ((CommandWorld) other).power$commandWorldId();
    power$cheats = ((local.luke.power.permissions.CheatWorld) other).power$cheatsEnabled();
    power$keepInventory = power$cheats && ((local.luke.power.world.WorldInventory) other).power$keepInventory();
  }

  @Inject(method = "updateProperties", at = @At("RETURN"))
  private void power$write(NbtCompound tag, NbtCompound player, CallbackInfo ci) {
    tag.putString("PowerBetaCommandWorldId", power$commandId);
    tag.putBoolean(TAG, power$cheats);
    tag.putBoolean("PowerBetaKeepInventory", power$keepInventory());
  }
}
