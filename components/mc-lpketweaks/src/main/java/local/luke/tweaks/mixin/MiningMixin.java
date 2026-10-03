package local.luke.tweaks.mixin;

import net.minecraft.SingleplayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Click Mining Forever's singleplayer delay reset; native damage and drops remain unchanged. */
@Mixin(SingleplayerInteractionManager.class)
public abstract class MiningMixin {
  @Shadow private int field_2187;

  @Inject(method = "method_1721", at = @At("TAIL"))
  private void lpke$clickMining(int x, int y, int z, int face, CallbackInfo ci) {
    if (local.luke.tweaks.config.Config.current().clickMining && field_2187 > 0) field_2187 = 0;
  }
}
