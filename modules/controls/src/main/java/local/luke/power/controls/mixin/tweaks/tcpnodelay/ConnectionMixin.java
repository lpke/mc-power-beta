package local.luke.power.controls.mixin.tweaks.tcpnodelay;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.network.Connection;
import net.minecraft.network.NetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Socket;
import java.net.SocketException;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/net/Socket;setTrafficClass(I)V", shift = At.Shift.AFTER))
    public void enableTcpNodelay(Socket socket, String string, NetworkHandler networkHandler, CallbackInfo ci) throws SocketException {
        if (ControlFeatures.GENERAL_CONFIG.tcpNoDelay) {
            socket.setTcpNoDelay(true);
        }
    }
}
