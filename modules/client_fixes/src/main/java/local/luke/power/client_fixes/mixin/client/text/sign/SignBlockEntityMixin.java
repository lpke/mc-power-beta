package local.luke.power.client_fixes.mixin.client.text.sign;

import lombok.Getter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.mixin.client.MinecraftAccessor;
import local.luke.power.client_fixes.mixinterface.SignBlockEntityAccessor;

@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityAccessor {
    @Shadow
    public String[] texts;

    @Unique
    @Getter
    private final TextFieldWidget[] textFields = new TextFieldWidget[4];

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        for (int i = 0; i < texts.length; i++) {
            TextFieldWidget textField = textFields[i] = new TextFieldWidget(null, MinecraftAccessor.getInstance().textRenderer, -1, -1, -1, -1, texts[i]);
            textField.setMaxLength(15);
        }
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void onReadNbt(CallbackInfo ci) {
        for (int i = 0; i < texts.length; i++) {
            textFields[i].setText(texts[i]);
        }
    }

    @Redirect(method = "writeNbt", at = @At(value = "FIELD", target = "Lnet/minecraft/block/entity/SignBlockEntity;texts:[Ljava/lang/String;", args = "array=get"))
    private String getSignText(String[] signText, int i) {
        return textFields[i].getText();
    }
}
