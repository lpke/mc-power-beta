package local.luke.building.validation.mixin;

import net.minecraft.SingleplayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SingleplayerInteractionManager.class)
public interface MiningAccessor {
  @Accessor("field_2187")
  int delay();
}
