package local.luke.flexible.mixin;

import net.minecraft.class_533;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_533.class)
public interface BlockItemAccessor {
  @Accessor("field_2216")
  int flexible$blockId();
}
