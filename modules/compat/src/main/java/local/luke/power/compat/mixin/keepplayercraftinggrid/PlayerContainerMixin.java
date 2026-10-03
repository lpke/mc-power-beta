package local.luke.power.compat.mixin.keepplayercraftinggrid;

import local.luke.power.compat.Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerScreenHandler.class)
public abstract class PlayerContainerMixin extends ScreenHandler {

    @Inject(method = "onClosed", at = @At("HEAD"), cancellable = true)
    public void powerCompat_onClosed(PlayerEntity arg, CallbackInfo ci) {
        if (Config.config.allowCraftingInventorySlots)
        {
            super.onClosed(arg);
            ci.cancel();
        }
    }

    public boolean canUse(PlayerEntity arg) {
        return true;
    }
}
