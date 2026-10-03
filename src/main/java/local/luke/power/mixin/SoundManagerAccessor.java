package local.luke.power.mixin;
import net.minecraft.class_266;
import net.minecraft.client.sound.SoundManager;
import paulscode.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
  @Accessor("soundSystem") static SoundSystem power$system(){throw new AssertionError();}
  @Accessor("field_2673") static boolean power$started(){throw new AssertionError();}
  @Accessor("field_2668") class_266 power$sounds();
  @Accessor("field_2669") class_266 power$records();
  @Accessor("field_2670") class_266 power$music();
  @Accessor("field_2675") void power$countdown(int value);
}
