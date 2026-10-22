package local.luke.power.mixin;

import local.luke.power.input.PlayerMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class SpectatorInventoryMixin {
  @Inject(method="method_509", at=@At("HEAD"), cancellable=true)
  private void power$noSpectatorDrops(ItemStack stack,boolean random,CallbackInfo ci) {
    if ((Object)this instanceof PlayerMode mode && mode.power$spectator()) ci.cancel();
  }
  @Inject(method="writeNbt", at=@At("RETURN"))
  private void power$saveCursor(NbtCompound tag,CallbackInfo ci) {
    var player=(PlayerEntity)(Object)this;
    if (player.world.isRemote) return;
    tag.values().removeIf(value -> "PowerBetaSpectatorCursor".equals(((net.minecraft.nbt.NbtElement)value).getKey()));
    // Closing Spectator inventory retains the cursor instead of dropping it.
    // Keep it safe even if mode access is revoked before the next inventory visit.
    // Use the existing full-count/native-NBT codec for the already owned cursor slot.
    if (player.inventory.getCursorStack()==null) return;
    try { tag.putString("PowerBetaSpectatorCursor",local.luke.power.death.DeathItems.encode(player.inventory.getCursorStack())); }
    catch(java.io.IOException e) { throw new IllegalStateException("Could not preserve held Spectator item",e); }
  }
  @Inject(method="readtNbt", at=@At("RETURN"))
  private void power$loadCursor(NbtCompound tag,CallbackInfo ci) {
    var player=(PlayerEntity)(Object)this;
    if (player.world.isRemote || !tag.contains("PowerBetaSpectatorCursor")) return;
    try { player.inventory.setCursorStack(local.luke.power.death.DeathItems.decode(tag.getString("PowerBetaSpectatorCursor"))); }
    catch(java.io.IOException e) { throw new IllegalStateException("Saved Spectator cursor item is invalid; original data retained",e); }
  }
}
