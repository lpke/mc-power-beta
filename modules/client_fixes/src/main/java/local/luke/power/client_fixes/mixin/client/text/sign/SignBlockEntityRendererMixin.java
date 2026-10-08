package local.luke.power.client_fixes.mixin.client.text.sign;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.ModHelper;
import local.luke.power.client_fixes.mixinterface.TextFieldWidgetAccessor;
import local.luke.power.client_fixes.mixinterface.SignBlockEntityAccessor;

@Mixin(SignBlockEntityRenderer.class)
public class SignBlockEntityRendererMixin {
    @Unique
    private SignBlockEntity sign;

    @Inject(method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDF)V", at = @At("HEAD"))
    private void onRender(SignBlockEntity sign, double x, double y, double z, float var8, CallbackInfo ci) {
        this.sign = sign;
    }

    @Redirect(method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/block/entity/SignBlockEntity;texts:[Ljava/lang/String;", args = "array=get"))
    private String getSignText(String[] signText, int i) {
        return ((TextFieldWidgetAccessor) ((SignBlockEntityAccessor) sign).getTextFields()[i]).getDisplayText();
    }
}
