package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.rules.*;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
public class PortalRulesMixin {
  @ModifyExpressionValue(method = "method_937", at = @At(value = "FIELD",
      target = "Lnet/minecraft/entity/player/ClientPlayerEntity;field_512:Z", opcode = 180))
  private boolean power$entry(boolean inPortal) {
    ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
    boolean allowed = player.world.isRemote || player.dimensionId == -1
        || WorldRules.enabled(player.world.method_262(), GameRule.ALLOW_ENTERING_NETHER_USING_PORTALS);
    if (!allowed) ((PortalContactAccessor) player).power$portalContact(false);
    return inPortal && allowed;
  }
}
