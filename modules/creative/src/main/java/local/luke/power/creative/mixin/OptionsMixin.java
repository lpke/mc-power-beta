package local.luke.power.creative.mixin;

import java.util.*;
import local.luke.power.creative.Keys;
import net.minecraft.client.options.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameOptions.class)
public abstract class OptionsMixin {
  @Shadow public KeyBinding[] keyBindings;

  @Inject(method = "load", at = @At("HEAD"))
  private void power$keys(CallbackInfo ci) {
    List<KeyBinding> keys = new ArrayList<>(Arrays.asList(keyBindings));
    for (KeyBinding key : Keys.ALL) if (!keys.contains(key)) keys.add(key);
    keyBindings = keys.toArray(KeyBinding[]::new);
  }
}
