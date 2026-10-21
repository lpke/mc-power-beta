package local.luke.power.offhand.mixin;

import local.luke.power.offhand.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class OffhandPlayerDataMixin implements OffhandDataOwner {
  @Unique private NbtCompound power$offhandDocument;
  public NbtCompound power$offhandDocument() { return power$offhandDocument; }
  public void power$offhandDocument(NbtCompound document) { power$offhandDocument=document; }
  @Inject(method="readtNbt", at=@At("HEAD"))
  private void power$readOffhand(NbtCompound tag, CallbackInfo ci) {
    try {
      OffhandData.validatePlayer(tag);
      Offhand.set(((PlayerEntity)(Object)this).inventory, OffhandData.read(tag));
      power$offhandDocument = tag.contains(OffhandData.TAG) ? OffhandData.copy(tag.getCompound(OffhandData.TAG)) : null;
    } catch (java.io.IOException failure) { throw new IllegalStateException("Unsafe offhand data; original world data retained", failure); }
  }
  @Inject(method="writeNbt", at=@At("RETURN"))
  private void power$writeOffhand(NbtCompound tag, CallbackInfo ci) {
    try {
      OffhandData.write(tag, Offhand.get(((PlayerEntity)(Object)this).inventory), power$offhandDocument);
      if (tag.contains(OffhandData.TAG)) power$offhandDocument = OffhandData.copy(tag.getCompound(OffhandData.TAG));
    } catch (java.io.IOException failure) { throw new IllegalStateException("Could not protect offhand item; player save stopped", failure); }
  }
}
