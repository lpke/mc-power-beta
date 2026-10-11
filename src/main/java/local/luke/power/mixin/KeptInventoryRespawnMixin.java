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
    if (mc.player == null || !KeepInventory.kept(mc.player)) return;
    power$previous = mc.player; power$saved = ((KeptPlayer)mc.player).power$keptInventory(); power$world = CommandContext.world(mc);
    try { if (!power$saved.matches(mc.player)) throw new java.io.IOException("Kept inventory changed before respawn"); }
    catch (java.io.IOException e) { throw new IllegalStateException("Kept inventory ownership changed; respawn stopped", e); }
  }
  @Inject(method = "method_2122", at = @At("RETURN"))
  private void power$afterRespawn(boolean unused, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft)(Object)this;
    if (power$saved != null && !power$world.equals(CommandContext.world(mc))) throw new IllegalStateException("Kept inventory world changed");
    KeepInventory.transfer(mc, power$previous, power$saved); power$previous = null; power$saved = null;
  }
}
