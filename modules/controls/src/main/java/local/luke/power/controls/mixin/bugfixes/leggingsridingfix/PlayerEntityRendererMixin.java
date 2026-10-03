package local.luke.power.controls.mixin.bugfixes.leggingsridingfix;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer {
    @Shadow
    private BipedEntityModel armor2;

    public PlayerEntityRendererMixin(EntityModel arg, float f) {
        super(arg, f);
    }

    @Inject(method = "render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V", at = @At("HEAD"))
    public void fixLeggings(PlayerEntity player, double e, double f, double g, float h, float par6, CallbackInfo ci) {
        if (ControlFeatures.BUGFIXES_CONFIG.leggingsWhenRidingFix) {
            ItemStack stack = player.inventory.armor[1];
            if (stack != null) {
                if (stack.getItem() instanceof ArmorItem) {
                    this.armor2.riding = this.model.riding;
                }
            }
        }
    }
}
