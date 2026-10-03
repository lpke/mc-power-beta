package local.luke.power.mechanics.mixin.server;

import local.luke.power.mechanics.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.NetworkHandler;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.SERVER)
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayerPacketHandlerMixin extends NetworkHandler implements CommandOutput {

    @WrapOperation(
            method = "handleUpdateSign",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;length()I",
                    ordinal = 0
            )
    )
    public int powerMechanics_onUpdateSignCheckLength(String instance, Operation<Integer> original) {
        if (Config.config.INTERACTIVE_BLOCK_CONFIG.enableColorSignsWithDye && instance.contains("§")) {
            return original.call(instance) - 2;
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(
            method = "handleUpdateSign",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;length()I",
                    ordinal = 1
            )
    )
    public int powerMechanics_onUpdateSignCheckCharacters(String instance, Operation<Integer> original) {
        if (Config.config.INTERACTIVE_BLOCK_CONFIG.enableColorSignsWithDye && instance.contains("§")) {
            return 0;
        } else {
            return original.call(instance);
        }
    }
}
