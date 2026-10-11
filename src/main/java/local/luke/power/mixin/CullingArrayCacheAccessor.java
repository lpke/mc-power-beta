package local.luke.power.mixin;

import net.minecraft.class_390;
import net.minecraft.class_43;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_390.class)
public interface CullingArrayCacheAccessor {
  @Accessor("field_1514") class_43[] power$loadedChunks();
  @Accessor("field_1511") class_43 power$emptyChunk();
}
