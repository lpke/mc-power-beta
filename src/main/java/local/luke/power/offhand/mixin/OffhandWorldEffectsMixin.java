package local.luke.power.offhand.mixin;

import local.luke.power.offhand.OffhandUse;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Effects are observed only during the synchronous native use callback. */
@Mixin(World.class)
public class OffhandWorldEffectsMixin {
  @Inject(method="method_210", at=@At("RETURN"))
  private void power$spawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValue()) OffhandUse.effect((World)(Object)this);
  }
  @Inject(method="method_229", at=@At("RETURN"))
  private void power$block(int x,int y,int z,int id,CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValue()) OffhandUse.effect((World)(Object)this);
  }
  @Inject(method="method_201", at=@At("RETURN"))
  private void power$state(int x,int y,int z,int id,int data,CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValue()) OffhandUse.effect((World)(Object)this);
  }
  @Inject(method="method_150", at=@At("HEAD"))
  private void power$evaporation(double x,double y,double z,String sound,float volume,float pitch,CallbackInfo ci) {
    if (sound.equals("random.fizz")) OffhandUse.effect((World)(Object)this);
  }
}
