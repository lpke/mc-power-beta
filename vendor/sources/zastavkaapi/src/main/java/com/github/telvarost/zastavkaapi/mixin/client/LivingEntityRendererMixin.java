package com.github.telvarost.zastavkaapi.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends EntityRenderer {

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V"
            ),
            remap = false
    )
    protected void zastavkaApi_glTranslatef(LivingEntity livingEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
        if (0 < livingEntity.zastavkaApi_getFrozenTicks()) {
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;bindTexture(Lnet/minecraft/entity/LivingEntity;IF)Z"
            )
    )
    protected boolean zastavkaApi_bindTexture(LivingEntityRenderer instance, LivingEntity mob, int layer, float tickDelta, Operation<Boolean> original) {
        if (0 < mob.zastavkaApi_getFrozenTicks()) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            return false;
        } else {
            return original.call(instance, mob, layer, tickDelta);
        }
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(FFFFFF)V"
            )
    )
    protected void zastavkaApi_render(EntityModel instance, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float scale, Operation<Void> original, LivingEntity livingEntity, double d, double e, double f, float g, float h) {
        if (0 < livingEntity.zastavkaApi_getFrozenTicks()) {
            original.call(instance, livingEntity.walkAnimationProgress, livingEntity.lastWalkAnimationSpeed, (float)livingEntity.age, headYaw, headPitch, scale);
        } else {
            original.call(instance, limbAngle, limbDistance, animationProgress, headYaw, headPitch, scale);
        }
    }
}
