package local.luke.power.mixin;
import local.luke.power.input.Binding;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.*;
@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements Binding {
  @Shadow public String translationKey;
  @Shadow public int code;
  public String power$id() { return translationKey; }
  public int power$code() { return code; }
}
