package local.luke.power.controls.mixin.bugfixes.droppeditemfix;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ItemRenderer.class, priority = 1100)
public class ItemRendererMixin {
    @ModifyConstant(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V", constant = @Constant(floatValue = 0.5F, ordinal = 0), require = 0)
    float fixDroppedItemSize(float constant) {
        if (ControlFeatures.BUGFIXES_CONFIG.droppedItemSizeFix) {
            return 0.25F;
        }
        return constant;
    }
}
