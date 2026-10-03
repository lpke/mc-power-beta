package local.luke.power.controls.mixin.tweaks.disabledimensionload;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @WrapOperation(method = "loadWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;running:Z"))
    public boolean disableDimensionLoad(MinecraftServer server, Operation<Boolean> original, @Local ServerWorld serverWorld, @Local(ordinal = 1) int worldId) {
        for (int i = 0; i < ControlFeatures.GENERAL_CONFIG.disabledDimensions.length; i++) {
            if (ControlFeatures.GENERAL_CONFIG.disabledDimensions[i] == worldId) {
                return false;
            }
        }

        return original.call(server);
    }
}
