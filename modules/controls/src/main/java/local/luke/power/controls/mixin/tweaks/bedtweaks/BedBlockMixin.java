package local.luke.power.controls.mixin.tweaks.bedtweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SleepAttemptResult;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BedBlock.class)
public class BedBlockMixin {
    @WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;trySleep(III)Lnet/minecraft/entity/player/SleepAttemptResult;"))
    public SleepAttemptResult useinject(PlayerEntity player, int x, int y, int z, Operation<SleepAttemptResult> original) {
        if (player.world.isRemote) return original.call(player, x, y, z);
        boolean sleep = local.luke.power.rules.WorldRules.enabled(player.world.getProperties(), local.luke.power.rules.GameRule.ALLOW_SLEEP);
        SleepAttemptResult result = sleep ? original.call(player, x, y, z) : SleepAttemptResult.OTHER_PROBLEM;
        if (!sleep || result == SleepAttemptResult.NOT_POSSIBLE_NOW || result == SleepAttemptResult.OK) {
            player.sendMessage("Respawn Point Set");
            ((PlayerEntityAccessor) player).setRespawnPos(new Vec3i(x, y, z));
            // Respawn-only use must not mark an unoccupied bed as occupied or show a night-only error.
            return result == SleepAttemptResult.OK ? result : SleepAttemptResult.OTHER_PROBLEM;
        }
        return result;
    }
}
