package local.luke.power.controls.mixin.bugfixes.lavawithoutsourcefix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.block.FlowingLiquidBlock;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.material.Material;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingLiquidBlock.class)
public class FlowingLiquidBlockMixin extends LiquidBlock {
    public FlowingLiquidBlockMixin(int i, Material arg) {
        super(i, arg);
    }

    @WrapOperation(
            method = "onTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/block/FlowingLiquidBlock;material:Lnet/minecraft/block/material/Material;",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 3
            )
    )
    private Material allowLavaToDissapear(FlowingLiquidBlock block, Operation<Material> original) {
        if (ControlFeatures.BUGFIXES_CONFIG.lavaWithoutSourceFix) {
            return Material.WATER;
        } else {
            return original.call(block);
        }
    }
}
