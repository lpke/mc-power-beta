package local.luke.power.client_fixes.mixin.client.text.sign;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.packet.play.UpdateSignPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import local.luke.power.client_fixes.ModHelper;
import local.luke.power.client_fixes.mixinterface.SignBlockEntityAccessor;

@Mixin(ClientNetworkHandler.class)
public class ClientNetworkHandlerMixin {
    @Inject(method = "handleUpdateSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;markDirty()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onHandleSignUpdate(UpdateSignPacket packet, CallbackInfo ci, BlockEntity blockEntity, SignBlockEntity sign, int var4) {
        TextFieldWidget[] textFields = ((SignBlockEntityAccessor) sign).getTextFields();
        for (int i = 0; i < packet.text.length; i++) {
            textFields[i].setText(packet.text[i]);
        }
    }
}
