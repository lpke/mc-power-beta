package local.luke.power.autowalk.mixin;

import java.util.Arrays;
import local.luke.power.autowalk.AutoWalk;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
  @Shadow public KeyBinding[] allKeys;

  @Inject(method = "load", at = @At("HEAD"))
  private void registerKey(CallbackInfo ci) {
    for (KeyBinding key : allKeys) {
      if (key == AutoWalk.KEY) return;
    }
    allKeys = Arrays.copyOf(allKeys, allKeys.length + 1);
    allKeys[allKeys.length - 1] = AutoWalk.KEY;
  }
}
