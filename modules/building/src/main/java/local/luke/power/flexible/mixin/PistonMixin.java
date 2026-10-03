package local.luke.power.flexible.mixin;

import local.luke.power.flexible.Orientation;
import net.minecraft.class_223;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_223.class)
public abstract class PistonMixin {
  @Inject(method = "method_761", at = @At("RETURN"), cancellable = true)
  private static void orient(
      World w, int x, int y, int z, PlayerEntity player, CallbackInfoReturnable<Integer> cir) {
    cir.setReturnValue(Orientation.metadata(w.getBlockId(x, y, z), x, y, z, cir.getReturnValue()));
  }
}
