package local.luke.power.autowalk.mixin;
import net.minecraft.class_196;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_196.class)
public interface CraftingPosition {
  @Accessor("field_711") int power$x();
  @Accessor("field_712") int power$y();
  @Accessor("field_713") int power$z();
}
