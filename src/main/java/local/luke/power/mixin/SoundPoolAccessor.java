package local.luke.power.mixin;
import java.util.*;
import net.minecraft.class_266;
import net.minecraft.class_267;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(class_266.class)
public interface SoundPoolAccessor {
  @Accessor("field_1089") Map<String,List<class_267>> power$groups();
  @Accessor("field_1090") List<class_267> power$tracks();
}
