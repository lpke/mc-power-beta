package com.github.telvarost.quickadditions.mixin.server;

import com.github.telvarost.quickadditions.Config;
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
    public int quickAdditions_handlePlayerAction(int constant) {
        return Config.config.SPAWN_PROTECTION_RADIUS;
    }

    @ModifyConstant(
            method = "onPlayerInteractBlock",
            constant = @Constant(intValue = 16)
    )
    public int quickAdditions_onPlayerInteractBlock(int constant) {
        return Config.config.SPAWN_PROTECTION_RADIUS;
    }
}
