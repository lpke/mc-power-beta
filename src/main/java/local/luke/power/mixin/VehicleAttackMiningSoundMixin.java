package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.*;
import local.luke.power.input.PlayerMode;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Modern mining cancellation resets charge only when there was a native mining action. */
@Mixin(SingleplayerInteractionManager.class)
public abstract class VehicleAttackMiningSoundMixin extends InteractionManager {
  @Unique private boolean power$vehicleSoundMining;
  protected VehicleAttackMiningSoundMixin(Minecraft mc) { super(mc); }

  @WrapOperation(method = {"method_1707", "method_1721"}, at = @At(value = "INVOKE",
      target = "Lnet/minecraft/block/Block;method_1582(Lnet/minecraft/entity/player/PlayerEntity;)F"))
  private float power$mining(Block block, PlayerEntity player, Operation<Float> original) {
    float progress = original.call(block, player);
    if (AudioConfig.current().vehicleAttackSounds && !player.world.isRemote
        && !(player instanceof PlayerMode mode && (mode.power$creative() || mode.power$spectator())))
      power$vehicleSoundMining = progress < 1;
    return progress;
  }

  @WrapOperation(method = "method_1721", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;getBlockId(III)I"))
  private int power$gone(World world, int x, int y, int z, Operation<Integer> original) {
    int block = original.call(world, x, y, z);
    if (block == 0) power$vehicleSoundMining = false;
    return block;
  }

  @Inject(method = "method_1716", at = @At("RETURN"))
  private void power$finished(int x, int y, int z, int face, CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValueZ()) power$vehicleSoundMining = false;
  }

  @Inject(method = "method_1705", at = @At("HEAD"))
  private void power$abort(CallbackInfo ci) {
    if (!power$vehicleSoundMining) return;
    power$vehicleSoundMining = false;
    VehicleAttackSounds.reset(minecraft.player);
  }
}
