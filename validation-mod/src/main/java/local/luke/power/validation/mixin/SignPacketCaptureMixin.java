package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.validation.SignEditingChecks;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignEditScreen.class)
public class SignPacketCaptureMixin {
  @WrapOperation(method = "removed", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/network/ClientNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"))
  private void validation$signPacket(ClientNetworkHandler handler, Packet packet, Operation<Void> original) {
    if (!SignEditingChecks.capture(packet)) original.call(handler, packet);
  }
}
