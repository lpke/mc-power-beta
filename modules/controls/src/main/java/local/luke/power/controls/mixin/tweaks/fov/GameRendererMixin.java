package local.luke.power.controls.mixin.tweaks.fov;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener;
import local.luke.power.controls.util.CompatHelper;
import local.luke.power.controls.util.ModOptions;
import local.luke.power.controls.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    private Minecraft client;

    @Shadow
    private float viewDistance;

    @Shadow
    protected abstract float getFov(float tickDelta);

    @Unique
    public float fov = 70F;

    @Unique
    float fovZoom = 0F;

    @Unique
    boolean zoomedIn = false;

    @Unique
    public float getFovMultiplier(float partialTicks, boolean isHand, float originalValue) {
        fov = isHand ? originalValue : originalValue + ModOptions.getFovOffsetInDegrees();

        if (local.luke.power.input.Bindings.down(KeyBindingListener.zoom) && client.currentScreen == null) {
            if (!zoomedIn) {
                ModOptions.zoomFovOffset = 0;
                fovZoom = 0F;
                zoomedIn = true;
            }

            if (!isHand) {
                fov /= 4F;

                fovZoom += ModOptions.zoomFovOffset * 5;
                ModOptions.zoomFovOffset = 0;

                fovZoom = Util.clamp(fovZoom, 5F - fov, 130F - fov);
            } else {
                fov -= 50F;
            }

            fov += fovZoom;
        } else {
            zoomedIn = false;
        }

        // Mod Compat
        for (BiFunction<Float, Float, Float> modFovModifier : CompatHelper.fovMethods) {
            fov = modFovModifier.apply(fov, partialTicks);
        }

        return fov;
    }

    @ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/option/GameOptions;cinematicMode:Z"))
    public boolean smoothCameraWhenZooming(boolean original) {
        if (!true) {
            return original;
        }
        
        return original || local.luke.power.input.Bindings.down(KeyBindingListener.zoom);
    }

    @Unique private boolean power$perspectivePending;
    @Unique private float power$aspect, power$near, power$far;

    // Resolve FOV after the camera transform has supplied its actual world position.
    // Photo mode's orthographic projection bypasses this call and remains untouched.
    @WrapOperation(method = "renderWorld(FI)V", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/util/glu/GLU;gluPerspective(FFFF)V", remap = false))
    private void power$deferPerspective(float fov, float aspect, float near, float far, Operation<Void> original) {
        power$perspectivePending = true;
        power$aspect = aspect; power$near = near; power$far = far;
    }

    @WrapMethod(method = "renderWorld(FI)V")
    private void power$cameraPerspective(float delta, int eye, Operation<Void> original) {
        power$perspectivePending = false;
        try {
            original.call(delta, eye);
            if (power$perspectivePending) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GLU.gluPerspective(getFovMultiplier(delta, false, getFov(delta)), power$aspect, power$near, power$far);
            }
        } finally {
            power$perspectivePending = false;
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    @Inject(method = "renderFirstPersonHand", at = @At(value = "HEAD"))
    public void adjustHandFov(float f, int i, CallbackInfo ci) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(getFovMultiplier(f, true, this.getFov(f)), (float) client.displayWidth / (float) client.displayHeight, 0.05F, viewDistance * 2.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
