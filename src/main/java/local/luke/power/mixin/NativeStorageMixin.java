package local.luke.power.mixin;
import local.luke.power.config.NativeStorage;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GameOptions.class, priority=800)
public class NativeStorageMixin {
  // Keep registration hooks and return injections, but never import options.txt.
  @Redirect(method="load",at=@At(value="INVOKE",target="Ljava/io/File;exists()Z"))
  private boolean power$skipSeparateOptions(java.io.File file) { return false; }
  // Let all key-registration hooks run before loading values, including early returns.
  @Inject(method="load",at=@At("RETURN")) private void power$load(CallbackInfo ci) { NativeStorage.load((GameOptions)(Object)this); }
  @Inject(method="save",at=@At("HEAD"),cancellable=true) private void power$save(CallbackInfo ci) { NativeStorage.save((GameOptions)(Object)this); ci.cancel(); }
}
