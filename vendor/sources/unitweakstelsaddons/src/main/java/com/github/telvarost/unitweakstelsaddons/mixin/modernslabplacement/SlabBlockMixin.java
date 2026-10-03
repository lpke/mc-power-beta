package com.github.telvarost.unitweakstelsaddons.mixin.modernslabplacement;

import com.github.telvarost.unitweakstelsaddons.Config;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.SlabBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlabBlock.class)
public class SlabBlockMixin extends Block {

    public SlabBlockMixin(int i, boolean bl) {
        super(i, 6, Material.STONE);
    }

    @Inject(
            method = "onPlaced",
            at = @At("HEAD"),
            cancellable = true
    )
    public void uniTweaksTelsAddons_onPlaced(World arg, int i, int j, int k, CallbackInfo ci) {
        if (Config.config.slabPlacementFixesEnabled) {
            super.onPlaced(arg, i, j, k);
            ci.cancel();
        }
    }
}
