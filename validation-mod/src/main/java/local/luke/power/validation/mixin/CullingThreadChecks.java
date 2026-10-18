package local.luke.power.validation.mixin;

import local.luke.power.validation.Validation;
import net.minecraft.class_43;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class CullingThreadChecks {
  @Inject(method="method_214",at=@At("HEAD"))
  private void power$workerRead(int x,int z,CallbackInfoReturnable<class_43> cir) {
    if(local.luke.power.validation.Release252Checks.checkingUnloaded
        && !Thread.currentThread().getName().equals("CullThread")) local.luke.power.validation.Release252Checks.chunkRequests++;
    if(Thread.currentThread().getName().equals("CullThread")) Validation.backgroundChunkReads.incrementAndGet();
  }
}
