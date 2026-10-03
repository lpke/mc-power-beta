package local.luke.power.controls.mixin.tweaks.bedtweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SleepAttemptResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BedBlock.class)
public class BedBlockMixin {
    @WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;trySleep(III)Lnet/minecraft/entity/player/SleepAttemptResult;"))
    public SleepAttemptResult useinject(PlayerEntity player, int x, int y, int z, Operation<SleepAttemptResult> original) {
        if (ControlFeatures.TWEAKS_CONFIG.disableSleeping) {
            if (!player.world.isRemote) {
                player.sendMessage("Respawn Point Set");
                ((PlayerEntityAccessor) player).setRespawnPos(new Vec3i(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)));
                return SleepAttemptResult.OK;
            }
        }
        return original.call(player, x, y, z);
    }
}
