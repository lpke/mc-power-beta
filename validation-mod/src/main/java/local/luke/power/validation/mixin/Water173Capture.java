package local.luke.power.validation.mixin;
import local.luke.power.validation.Release173Checks;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(World.class)
public class Water173Capture {
  @Inject(method="method_191",at=@At("HEAD"))
  private void sound(Entity entity,String sound,float volume,float pitch,CallbackInfo ci){
    if(Release173Checks.capture && entity==Release173Checks.splashEntity && sound.equals("random.splash"))Release173Checks.splashes++;
  }
}
