package local.luke.power.controls.mixin.bugfixes.bowheldfix;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "renderMore(Lnet/minecraft/entity/player/PlayerEntity;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.BEFORE, ordinal = 1))
    protected void changeBowRendering(PlayerEntity entity, float f, CallbackInfo ci) {
        if (ControlFeatures.BUGFIXES_CONFIG.bowHeldFix) {
            ItemStack itemStack = entity.getHand();
            if (itemStack != null && itemStack.getItem() instanceof BowItem) {
                GL11.glTranslatef(0.0F, -0.5F, 0.0F);
            }
        }
    }
}
