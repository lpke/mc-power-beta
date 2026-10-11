package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.death.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class KeptInventoryMixin implements KeptPlayer {
  @Unique private KeptInventory power$kept;
  public KeptInventory power$keptInventory() { return power$kept; }
  public void power$keptInventory(KeptInventory value) { power$kept = value; }

  @WrapOperation(method = "method_938", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;method_509(Lnet/minecraft/item/ItemStack;Z)V"))
  private void power$noDeathApple(PlayerEntity player, ItemStack item, boolean random, Operation<Void> original) {
    if (!KeepInventory.kept(player)) original.call(player, item, random);
  }
  @Inject(method = "writeNbt", at = @At("RETURN"))
  private void power$saveKept(NbtCompound tag, CallbackInfo ci) {
    tag.putByteArray("PowerBetaKeptInventory", power$kept == null ? new byte[0] : power$kept.bytes());
  }
  @Inject(method = "readtNbt", at = @At("RETURN"))
  private void power$loadKept(NbtCompound tag, CallbackInfo ci) {
    byte[] bytes = tag.getByteArray("PowerBetaKeptInventory");
    if (bytes.length == 0) { power$kept = null; return; }
    try { power$kept = KeptInventory.read(bytes); power$kept.restore((PlayerEntity)(Object)this); }
    catch (java.io.IOException e) { throw new IllegalStateException("Saved kept inventory is unsafe; original world data retained", e); }
  }
}
