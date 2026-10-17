package local.luke.power.validation.mixin;

import local.luke.power.validation.Release240Checks;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextRenderer.class)
public abstract class TitleTextCapture {
  @Inject(method = "draw(Ljava/lang/String;III)V", at = @At("HEAD"))
  private void power$tag(String text,int x,int y,int color,CallbackInfo ci) {
    local.luke.power.validation.Release241Checks.captureMobTag(text,x,y);
    if (local.luke.power.validation.Release241Checks.captureTags && text.equals("2")) local.luke.power.validation.Release241Checks.tags++;
  }
  @Inject(method = "drawWithShadow", at = @At("HEAD"))
  private void power$title(String text,int x,int y,int color,CallbackInfo ci) {
    if (Release240Checks.captureTitle && Release240Checks.titleText.size()<256) Release240Checks.titleText.add(text);
  }
}
