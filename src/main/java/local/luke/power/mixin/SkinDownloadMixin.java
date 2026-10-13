package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.matthewperiut.retroauth.skin.SkinService;
import local.luke.power.skin.SkinProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SkinService.class, remap = false)
public abstract class SkinDownloadMixin {
  @ModifyExpressionValue(method = "updatePlayer", at = @At(value = "INVOKE",
      target = "Lcom/matthewperiut/retroauth/profile/PlayerProfile;getSkinUrl()Ljava/lang/String;"))
  private String power$remoteSkin(String url) { return SkinProfiles.downloadUrl(url); }
}
