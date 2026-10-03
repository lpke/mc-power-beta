package com.github.telvarost.unitweakstelsaddons.mixin.shoveleffectivity;

import com.github.telvarost.unitweakstelsaddons.Config;
import com.google.common.collect.ObjectArrays;
import net.minecraft.block.Block;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ShovelItem.class)
class ShovelItemMixin extends ToolItem {
    @Shadow private static Block[] shovelEffectiveBlocks;

    public ShovelItemMixin(int i, int j, ToolMaterial arg, Block[] args) {
        super(i, j, arg, args);
    }

    @Inject(
            method = "<clinit>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/item/ShovelItem;shovelEffectiveBlocks:[Lnet/minecraft/block/Block;",
                    opcode = Opcodes.PUTSTATIC,
                    shift = At.Shift.AFTER
            )
    )
    private static void annoyanceFix_appendExtraBlocks(CallbackInfo ci) {

        ArrayList effectiveBlockList = new ArrayList<Block>();

        if (Config.config.SHOVELS_CONFIG.enableShovelsEffectiveOnSoulSand) {
            effectiveBlockList.add(Block.SOUL_SAND);
        }

        Object[] objectArray = effectiveBlockList.toArray();
        Block[] blockArray = new Block[objectArray.length];

        for (int objectIndex = 0; objectIndex < objectArray.length; objectIndex++) {
            blockArray[objectIndex] = (Block) objectArray[objectIndex];
        }

        if (0 < blockArray.length) {
            shovelEffectiveBlocks = ObjectArrays.concat(shovelEffectiveBlocks, blockArray, Block.class);
        }
    }
}