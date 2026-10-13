package local.luke.power.mixin;

import com.matthewperiut.retroauth.profile.GameProfile;
import com.matthewperiut.retroauth.profile.provider.MineToolsProfileProvider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import local.luke.power.skin.SkinProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MineToolsProfileProvider.class, remap = false)
public abstract class SkinProfileFallbackMixin {
  @Inject(method = "get", at = @At("RETURN"), cancellable = true)
  private void power$completeProfile(String name, CallbackInfoReturnable<Future<GameProfile>> cir) {
    // This pinned provider uses CompletableFuture. Validate completion without waiting;
    // an incomplete response lets SkinService try Mojang through its existing fallback.
    if (cir.getReturnValue() instanceof CompletableFuture<GameProfile> future)
      cir.setReturnValue(future.thenApply(profile -> {
        if (profile == null || !SkinProfiles.hasTextureData(profile.getTextureValue()))
          throw new IllegalStateException("Incomplete texture profile; trying the next provider");
        return profile;
      }));
  }
}
