package com.github.telvarost.unitweakstelsaddons.mixin.vehicleloginlogoutfix;

import com.github.telvarost.unitweakstelsaddons.Config;
import com.github.telvarost.unitweakstelsaddons.VehicleHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.entity.player.PlayerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(World.class)
public class WorldMixin {

    @Shadow public boolean isRemote;

    @Shadow public List entities;

    @Inject(
            method = "spawnEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;notifyEntityAdded(Lnet/minecraft/entity/Entity;)V"
            )
    )
    public void spawnEntity(Entity arg, CallbackInfoReturnable<Boolean> cir) {
        if (  (Config.config.vehicleLogoutLoginFixesEnabled)
           && (VehicleHelper.isVehicleSaved)
           && (!this.isRemote)
        ) {
            /** - Find saved vehicle if on single player */
            for (int entityIndex = 0; entityIndex < this.entities.size(); entityIndex++) {
                Entity entityToCheck = (Entity) this.entities.get(entityIndex);

                if (  (entityToCheck.getClass().equals(VehicleHelper.savedVehicleClass))
                   && (1 > Math.abs(entityToCheck.x - VehicleHelper.savedVehicleX))
                   && (1 > Math.abs(entityToCheck.y - VehicleHelper.savedVehicleY))
                   && (1 > Math.abs(entityToCheck.z - VehicleHelper.savedVehicleZ))
                ) {
                    PlayerEntity player = PlayerHelper.getPlayerFromGame();
                    if (null != player) {
                        player.setVehicle(entityToCheck);
                    }
                    VehicleHelper.isVehicleSaved = false;
                }
            }
        }
    }
}
