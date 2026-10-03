package local.luke.fakesneak.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.fakesneak.FakeSneak;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class EntityMixin {
  @WrapMethod(method = "move")
  private void protectEdge(double dx, double dy, double dz, Operation<Void> original) {
    Entity e = (Entity) (Object) this;
    if (FakeSneak.protects(e, dy)) {
      var clipped = FakeSneak.clip(e, dx, dz);
      dx = clipped.x();
      dz = clipped.z();
    }
    original.call(dx, dy, dz);
  }
}
