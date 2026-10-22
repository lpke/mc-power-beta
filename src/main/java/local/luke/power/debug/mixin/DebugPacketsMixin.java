package local.luke.power.debug.mixin;

import local.luke.power.debug.*;

import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Packet.class)
public abstract class DebugPacketsMixin {
  @Inject(method="method_801",at=@At("HEAD"))
  private static void power$sent(Packet packet,java.io.DataOutputStream stream,CallbackInfo ci) {
    if(DebugPerformance.networkEnabled())DebugPerformance.packet(true,packet.size()+1);
  }
  @Inject(method="method_803",at=@At("RETURN"))
  private static void power$received(java.io.DataInputStream stream,boolean server,CallbackInfoReturnable<Packet> cir) {
    if(DebugPerformance.networkEnabled() && cir.getReturnValue()!=null)DebugPerformance.packet(false,cir.getReturnValue().size()+1);
  }
}
