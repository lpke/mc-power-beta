package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.rules.*;
import net.minecraft.block.Material;
import net.minecraft.class_142;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lava immunity leaves fire, explosions, despawning and server authority independent. */
@Mixin(Entity.class)
public abstract class LavaItemsMixin {
  @Unique private boolean power$protectItem() {
    Entity entity = (Entity)(Object)this;
    return entity instanceof class_142 && entity.world != null && !entity.world.isRemote
        && !WorldRules.enabled(entity.world.method_262(), GameRule.LAVA_DESTROYS_ITEMS);
  }

  @Inject(method = "method_1332", at = @At("HEAD"), cancellable = true)
  private void power$lava(CallbackInfo ci) {
    if (power$protectItem()) {
      ((Entity)(Object)this).fire = 0;
      ci.cancel();
    }
  }

  @WrapOperation(method = "move", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_225(Lnet/minecraft/util/math/Box;)Z"))
  private boolean power$lavaContact(World world, Box box, Operation<Boolean> original) {
    // Skip both contact damage and ignition, so leaving lava cannot carry a fire timer.
    return original.call(world, box) && !(power$protectItem() && world.method_169(box, Material.LAVA));
  }
}
