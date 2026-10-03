package local.luke.power.mixin;
import local.luke.power.PowerBeta;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public class ClientMixin {
  @Inject(method="tick",at=@At("TAIL")) private void power$tick(CallbackInfo ci) { PowerBeta.tick((Minecraft)(Object)this); }
}
