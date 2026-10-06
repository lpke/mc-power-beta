package local.luke.power.controls.mixin.tweaks.morekeybinds;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapOperation(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.PUTFIELD))
    public void cancelSelectSlot(PlayerInventory instance, int value, Operation<Void> original) {
        // Do Nothing
    }

    // F1
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 59))
    public int modifyHideHudKeybind(int constant) {
        return local.luke.power.input.Bindings.eventCode(KeyBindingListener.hideHUD);
    }

    // Screenshot handling polls held state rather than comparing a keyboard event.
    @WrapOperation(method = "handleScreenshotKey", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z", remap = false))
    public boolean power$screenshot(int code, Operation<Boolean> original) {
        return local.luke.power.input.Bindings.down(KeyBindingListener.takeScreenshot);
    }

    // F3
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 61))
    public int modifyDebugHudKeybind(int constant) {
        return local.luke.power.input.Bindings.eventCode(KeyBindingListener.debugHud);
    }

    // F5
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 63))
    public int modifyThirdPersonKeybind(int constant) {
        return local.luke.power.input.Bindings.eventCode(KeyBindingListener.thirdPerson);
    }

    // F6
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 66))
    public int modifyCinematicCameraKeybind(int constant) {
        return local.luke.power.input.Bindings.eventCode(KeyBindingListener.cinematicCamera);
    }

    // F11
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 87))
    public int modifyToggleFullscreenKeybind(int constant) {
        return local.luke.power.input.FullscreenKey.eventCode(KeyBindingListener.toggleFullscreen);
    }
}
