package net.danygames2014.gambac.mixin;

import net.minecraft.client.Minecraft;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void makeDisplayCurrent(CallbackInfo ci) {
        try {
            Display.makeCurrent();
            Display.update();
        } catch (LWJGLException e) {
            System.err.println("Error while making the Display current");
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

// Testing of a bug that made 2 windows exist at the same time if the game crashed on startup
//    @Inject(method = "init", at = @At(value = "TAIL"))
//    public void aa(CallbackInfo ci){
//        throw new RuntimeException("e");
//    }
}
