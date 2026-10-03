package local.luke.power.music_api.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.NetworkHandler;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(ClientNetworkHandler.class)
public abstract class ClientNetworkHandlerMixin extends NetworkHandler {

    @Redirect(
            method = "onEntitySpawn",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/block/Block;id:I",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    public int hauntedSands_onEntitySpawn(Block instance, EntitySpawnS2CPacket packet) {
        int blockId = packet.entityData;
        packet.entityData = 0;
        return blockId;
    }
}
