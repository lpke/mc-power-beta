package local.luke.power.mixin;

import local.luke.power.world.WorldDifficulty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ClientDifficultyMixin {
  @Shadow public World world;

  @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;difficulty:I"))
  private int power$worldDifficulty(GameOptions options) {
    return world == null || world.isRemote ? options.difficulty
        : ((WorldDifficulty) world.method_262()).power$difficulty();
  }

  @Inject(method = "method_2115", at = @At("HEAD"))
  private void power$loadDifficulty(World next, String message, PlayerEntity player, CallbackInfo ci) {
    if (next != null && !next.isRemote)
      next.field_213 = ((WorldDifficulty) next.method_262()).power$difficulty();
  }
}
