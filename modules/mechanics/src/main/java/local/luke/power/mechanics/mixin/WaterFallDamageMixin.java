package local.luke.power.mechanics.mixin;

import local.luke.power.mechanics.Config;
import local.luke.power.mechanics.WaterContact;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WaterFallDamageMixin {
    @Shadow protected float fallDistance;
    @Unique private double power$fallStartX, power$fallStartY, power$fallStartZ;
    @Unique private boolean power$moving;
    @Unique private final WaterContact.Water power$water = this::power$waterHeight;

    @WrapMethod(method = "move")
    private void power$rememberFallStart(double dx, double dy, double dz, Operation<Void> original) {
        Entity entity = (Entity)(Object)this;
        if (!Config.config.MOB_CONFIG.waterNegatesFallDamage || entity.world.isRemote
                || !(entity instanceof LivingEntity)) {
            original.call(dx, dy, dz);
            return;
        }
        Box box = entity.boundingBox;
        double oldX = power$fallStartX, oldY = power$fallStartY, oldZ = power$fallStartZ;
        boolean wasMoving = power$moving;
        power$fallStartX = (box.minX + box.maxX) / 2;
        power$fallStartY = box.minY;
        power$fallStartZ = (box.minZ + box.maxZ) / 2;
        power$moving = true;
        try {
            original.call(dx, dy, dz);
        } finally {
            power$moving = wasMoving;
            power$fallStartX = oldX; power$fallStartY = oldY; power$fallStartZ = oldZ;
        }
    }

    @Inject(method = "fall", at = @At("HEAD"))
    private void power$resetWaterFall(double dy, boolean onGround, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (!Config.config.MOB_CONFIG.waterNegatesFallDamage || entity.world.isRemote
                || !(entity instanceof LivingEntity) || fallDistance <= 0) return;
        // A dry boat keeps its rider out of the water, as in modern Java.
        if (entity.vehicle instanceof BoatEntity && !power$boatSubmerged(entity.vehicle.boundingBox)) return;
        Box box = entity.boundingBox;
        if (WaterContact.touches(power$water, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
                || power$moving && WaterContact.crosses(power$water, power$fallStartX, power$fallStartY, power$fallStartZ,
                (box.minX + box.maxX) / 2, box.minY, (box.minZ + box.maxZ) / 2)) {
            fallDistance = 0;
        }
    }

    @Unique
    private boolean power$boatSubmerged(Box box) {
        double top = box.maxY + 0.001;
        for (int x = (int)Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int)Math.floor(box.maxY); y < Math.ceil(top); y++) {
                for (int z = (int)Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    double height = power$waterHeight(x, y, z);
                    if (height > 0 && y + height > top) return true;
                }
            }
        }
        return false;
    }

    @Unique
    private double power$waterHeight(int x, int y, int z) {
        Entity entity = (Entity)(Object)this;
        if (y < 0 || y >= 128 || entity.world.getMaterial(x, y, z) != Material.WATER) return 0;
        return WaterContact.height(entity.world.getBlockMeta(x, y, z),
                entity.world.getMaterial(x, y + 1, z) == Material.WATER);
    }
}
