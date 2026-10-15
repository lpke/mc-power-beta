package local.luke.power.mixin;

import local.luke.power.input.PlayerMode;
import local.luke.power.rules.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class SleepRulesMixin {
  @Inject(method = "method_268", at = @At("HEAD"), cancellable = true)
  private void power$sleep(CallbackInfoReturnable<Boolean> cir) {
    World world = (World)(Object)this;
    if (world.isRemote) return;
    int eligible = 0, sleeping = 0;
    for (Object entry : world.field_200) {
      PlayerEntity player = (PlayerEntity)entry;
      if (!player.isAlive() || player instanceof PlayerMode mode && mode.power$spectator()) continue;
      eligible++;
      if (player.method_504()) sleeping++;
    }
    int percentage = WorldRules.of(world.method_262()).number(GameRule.PLAYERS_SLEEPING_PERCENTAGE);
    cir.setReturnValue(SleepThreshold.reached(eligible, sleeping, percentage));
  }
}
