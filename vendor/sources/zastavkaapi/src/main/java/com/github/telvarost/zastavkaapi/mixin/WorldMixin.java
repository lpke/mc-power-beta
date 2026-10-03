package com.github.telvarost.zastavkaapi.mixin;

import com.github.telvarost.zastavkaapi.ZastavkaHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(World.class)
public abstract class WorldMixin {

    @Inject(
            method = "getEntityCollisions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;addIntersectingBoundingBox(Lnet/minecraft/world/World;IIILnet/minecraft/util/math/Box;Ljava/util/ArrayList;)V"
            )
    )
    public void zastavkaApi_getEntityCollisions(Entity entity, Box box, CallbackInfoReturnable<List> cir) {
        ZastavkaHelper.collisionEntity = entity;
    }
}
