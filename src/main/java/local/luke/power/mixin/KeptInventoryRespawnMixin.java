package local.luke.power.mixin;

import local.luke.power.commands.CommandContext;
import local.luke.power.death.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class KeptInventoryRespawnMixin {
  @Unique private PlayerEntity power$previous;
  @Unique private KeptInventory power$saved;
  @Unique private String power$world;
  @Inject(method = "method_2122", at = @At("HEAD"))
  private void power$beforeRespawn(boolean unused, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft)(Object)this;
    power$previous = null; power$saved = null;
    if (mc.player == null || mc.world==null || mc.world.isRemote) return;
    power$previous=mc.player;power$world=CommandContext.world(mc);
    if(!KeepInventory.kept(mc.player))return;
    power$saved = ((KeptPlayer)mc.player).power$keptInventory();
    try { if (!power$saved.matches(mc.player)) throw new java.io.IOException("Kept inventory changed before respawn"); }
    catch (java.io.IOException e) { throw new IllegalStateException("Kept inventory ownership changed; respawn stopped", e); }
  }
  @Inject(method = "method_2122", at = @At("RETURN"))
  private void power$afterRespawn(boolean unused, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft)(Object)this;
    if (power$previous != null && !power$world.equals(CommandContext.world(mc))) throw new IllegalStateException("Kept inventory world changed");
    if(power$previous!=null&&mc.player!=null&&mc.player!=power$previous) {
      var document=((local.luke.power.offhand.OffhandDataOwner)power$previous).power$offhandDocument();
      try { ((local.luke.power.offhand.OffhandDataOwner)mc.player).power$offhandDocument(document==null?null:local.luke.power.offhand.OffhandData.copy(document)); }
      catch(java.io.IOException failure) { throw new IllegalStateException("Offhand document could not survive respawn",failure); }
    }
    KeepInventory.transfer(mc, power$previous, power$saved); power$previous = null; power$saved = null;
  }
}
