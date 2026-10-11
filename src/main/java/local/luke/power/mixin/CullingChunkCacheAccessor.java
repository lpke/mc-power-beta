package local.luke.power.mixin;

import java.util.Map;
import net.minecraft.class_326;
import net.minecraft.class_43;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_326.class)
public interface CullingChunkCacheAccessor {
  @Accessor("field_1229") Map<Integer, class_43> power$loadedChunks();
  @Accessor("field_1226") class_43 power$emptyChunk();
}
