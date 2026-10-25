package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.gameplay.ModernSwimming;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class SwimmingLadderMixin {
  @WrapOperation(method="method_945", at=@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;method_1373()Z"))
  private boolean power$ladderSneak(LivingEntity entity, Operation<Boolean> original) {
    if (entity instanceof ClientPlayerEntity player && !player.world.isRemote && ModernSwimming.crouched(player)) {
      Minecraft mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
      // A low ceiling owns the body pose, but only the Sneak key holds a ladder.
      if (mc != null && mc.player == player && mc.world == player.world)
        return player.field_161 != null && player.field_161.field_2536;
    }
    return original.call(entity);
  }
}
