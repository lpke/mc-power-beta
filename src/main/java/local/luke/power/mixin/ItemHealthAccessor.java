package local.luke.power.mixin;

import net.minecraft.class_142;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_142.class)
public interface ItemHealthAccessor {
  @Accessor("field_569") int power$health();
  @Accessor("field_569") void power$health(int health);
}
