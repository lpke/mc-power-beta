package local.luke.power.environment.mixin.server;

import local.luke.power.environment.Config;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @ModifyConstant(
            method = "handlePlayerAction",
            constant = @Constant(intValue = 16)
    )
    public int powerEnvironment_handlePlayerAction(int constant) {
        return Config.config.SPAWN_PROTECTION_RADIUS;
    }

    @ModifyConstant(
            method = "onPlayerInteractBlock",
            constant = @Constant(intValue = 16)
    )
    public int powerEnvironment_onPlayerInteractBlock(int constant) {
        return Config.config.SPAWN_PROTECTION_RADIUS;
    }
}
