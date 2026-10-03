package local.luke.worldedit.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccess {
  @Accessor("field_1636")
  void worldedit$fallDistance(float value);
}
