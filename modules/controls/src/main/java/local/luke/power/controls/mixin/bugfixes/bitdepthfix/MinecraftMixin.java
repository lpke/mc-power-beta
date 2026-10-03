package local.luke.power.controls.mixin.bugfixes.bitdepthfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.Minecraft;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create()V", remap = false), require = 0)
    public void bitDepthFix(Operation<Void> original) throws LWJGLException {
        if (ControlFeatures.BUGFIXES_CONFIG.bitDepthFix) {
            Display.create(new PixelFormat().withDepthBits(24));
        } else {
            Display.create();
        }
    }
}
