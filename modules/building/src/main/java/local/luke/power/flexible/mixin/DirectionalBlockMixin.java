package local.luke.power.flexible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.flexible.Orientation;
import net.minecraft.class_416;
import net.minecraft.class_421;
import net.minecraft.class_49;
import net.minecraft.class_510;
import net.minecraft.class_526;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({class_416.class, class_421.class, class_526.class, class_510.class, class_49.class})
public abstract class DirectionalBlockMixin {
  @WrapOperation(
      method = "method_1614",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_215(IIII)V"))
  private void orient(World w, int x, int y, int z, int meta, Operation<Void> original) {
    original.call(w, x, y, z, Orientation.metadata(w.getBlockId(x, y, z), x, y, z, meta));
  }
}
