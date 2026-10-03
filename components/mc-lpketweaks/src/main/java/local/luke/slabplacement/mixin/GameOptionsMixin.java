package local.luke.slabplacement.mixin;

import java.util.Arrays;
import local.luke.slabplacement.Keys;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
  @Shadow public KeyBinding[] allKeys;

  @Inject(method = "load", at = @At("HEAD"))
  private void register(CallbackInfo ci) {
    for (KeyBinding key : Keys.ALL) {
      if (Arrays.asList(allKeys).contains(key)) continue;
      allKeys = Arrays.copyOf(allKeys, allKeys.length + 1);
      allKeys[allKeys.length - 1] = key;
    }
  }
}
