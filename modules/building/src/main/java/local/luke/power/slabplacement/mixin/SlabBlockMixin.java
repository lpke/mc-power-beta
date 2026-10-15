package local.luke.power.slabplacement.mixin;

import local.luke.power.slabplacement.SlabPlacement;
import net.minecraft.class_553;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_553.class)
public class SlabBlockMixin {
  @Inject(method = "method_1611", at = @At("HEAD"), cancellable = true)
  private void power$explicitCompletion(World world, int x, int y, int z, CallbackInfo ci) {
    // Completion is handled before placement, with metadata and entity-collision checks.
    if (!world.isRemote && SlabPlacement.enabled()) ci.cancel();
  }
}
