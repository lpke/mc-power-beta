package local.luke.power.controls.mixin.bugfixes.droppeditemfix;

import local.luke.power.controls.ControlFeatures;
import net.modificationstation.stationapi.impl.client.arsenic.renderer.render.ArsenicItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ArsenicItemRenderer.class, priority = 900, remap = false)
public class ArsenicItemRendererMixin {
    @ModifyConstant(method = "renderVanilla", constant = @Constant(floatValue = 0.5F, ordinal = 0), require = 0, remap = false)
    float fixDroppedItemSize(float constant) {
        if (ControlFeatures.BUGFIXES_CONFIG.droppedItemSizeFix) {
            return 0.25F;
        }
        return constant;
    }
}
