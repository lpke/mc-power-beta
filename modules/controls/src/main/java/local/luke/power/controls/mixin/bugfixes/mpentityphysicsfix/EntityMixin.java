package local.luke.power.controls.mixin.bugfixes.mpentityphysicsfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(Entity.class)
public class EntityMixin {
    @WrapOperation(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;x:D", opcode = Opcodes.PUTFIELD))
    private void fixX(Entity entity, double value, Operation<Void> original) {
        if (ControlFeatures.BUGFIXES_CONFIG.multiplayerEntityJitterFix) {
            if (!entity.world.isRemote || entity instanceof PlayerEntity || !(entity instanceof LivingEntity)) {
                entity.x = value;
            }
        } else {
            original.call(entity, value);
        }
    }

    @WrapOperation(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;y:D", opcode = Opcodes.PUTFIELD))
    private void fixY(Entity entity, double value, Operation<Void> original) {
        if (ControlFeatures.BUGFIXES_CONFIG.multiplayerEntityJitterFix) {
            if (!entity.world.isRemote || entity instanceof PlayerEntity || !(entity instanceof LivingEntity)) {
                entity.y = value;
            }
        } else {
            original.call(entity, value);
        }
    }

    @WrapOperation(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;z:D", opcode = Opcodes.PUTFIELD))
    private void fixZ(Entity entity, double value, Operation<Void> original) {
        if (ControlFeatures.BUGFIXES_CONFIG.multiplayerEntityJitterFix) {
            if (!entity.world.isRemote || entity instanceof PlayerEntity || !(entity instanceof LivingEntity)) {
                entity.z = value;
            }
        } else {
            original.call(entity, value);
        }
    }
}
