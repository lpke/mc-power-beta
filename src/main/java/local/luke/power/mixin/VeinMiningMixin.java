package local.luke.power.mixin;

import local.luke.power.mining.VeinMining;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleplayerInteractionManager.class)
public abstract class VeinMiningMixin extends InteractionManager {
  protected VeinMiningMixin(Minecraft mc) { super(mc); }
  @Inject(method = {"method_1707", "method_1721"}, at = @At("HEAD"), cancellable = true)
  private void power$prepare(int x, int y, int z, int face, CallbackInfo ci) {
    if (VeinMining.busy()) { ci.cancel(); return; }
    VeinMining.prepare(minecraft, x, y, z);
  }
}
