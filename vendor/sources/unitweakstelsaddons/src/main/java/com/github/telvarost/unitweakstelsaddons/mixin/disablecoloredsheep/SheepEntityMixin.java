package com.github.telvarost.unitweakstelsaddons.mixin.disablecoloredsheep;

import com.github.telvarost.unitweakstelsaddons.Config;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SheepEntity.class)
public abstract class SheepEntityMixin extends AnimalEntity {

    public SheepEntityMixin(World arg) {
        super(arg);
        this.texture = "/mob/sheep.png";
        this.setBoundingBoxSpacing(0.9F, 1.3F);
    }

    @ModifyReturnValue(
            method = "generateDefaultColor",
            at = @At("RETURN")
    )
    private static int betaTweaks_generateDefaultColor(int original) {
        if (Config.config.disableColoredSheepSpawning) {
            return 0;
        } else {
            return original;
        }
    }
}
