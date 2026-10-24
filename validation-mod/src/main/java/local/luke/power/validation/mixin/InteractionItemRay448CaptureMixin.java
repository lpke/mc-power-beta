package local.luke.power.validation.mixin;

import local.luke.power.validation.CameraTarget448Checks;
import net.minecraft.class_27;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class InteractionItemRay448CaptureMixin {
  @Inject(method="method_160",at=@At("HEAD"),cancellable=true)
  private void validation$blockRay(Vec3d start,Vec3d end,CallbackInfoReturnable<class_27> cir) {
    if(CameraTarget448Checks.capture(1,start))cir.setReturnValue(null);
  }

  @Inject(method="method_161",at=@At("HEAD"),cancellable=true)
  private void validation$itemRay(Vec3d start,Vec3d end,boolean liquids,CallbackInfoReturnable<class_27> cir) {
    if(CameraTarget448Checks.capture(2,start))cir.setReturnValue(null);
  }
}
