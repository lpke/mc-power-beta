package local.luke.power.client_fixes.mixin.client.misc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.stat.PlayerStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import local.luke.power.client_fixes.Config;

@Mixin(PlayerStats.class)
public class PlayerStatsMixin {

    @WrapOperation(
            method = "deserialize",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"
            )
    )
    private static boolean deserialize(String instance, Object string, Operation<Boolean> original) {
        boolean checksumMismatch = original.call(instance, string);

        checksumMismatch = checksumMismatch || Config.config.disableStatsChecksumVerification;

        return checksumMismatch;
    }
}
