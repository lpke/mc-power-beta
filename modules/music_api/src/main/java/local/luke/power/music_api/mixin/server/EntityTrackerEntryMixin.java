package local.luke.power.music_api.mixin.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.entity.EntityTrackerEntry;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.SERVER)
@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {

    @Shadow public Entity currentTrackedEntity;

    @Inject(
            method = "createAddEntityPacket",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/FallingBlockEntity;blockId:I",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            ),
            cancellable = true
    )
    private void hauntedSands_createAddEntityPacket(CallbackInfoReturnable<Packet> cir) {
        FallingBlockEntity var3 = (FallingBlockEntity)this.currentTrackedEntity;
        if (var3.blockId != Block.GRAVEL.id) {
            cir.setReturnValue(new EntitySpawnS2CPacket(this.currentTrackedEntity, 70, var3.blockId));
        }
    }
}
