package local.luke.power.mixin;

import net.minecraft.class_42;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_42.class)
public interface RenderBlockViewAccessor {
  @Accessor("field_169") World power$world();
}
