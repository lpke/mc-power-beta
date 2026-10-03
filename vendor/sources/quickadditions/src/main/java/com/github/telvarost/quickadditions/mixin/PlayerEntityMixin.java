package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow public abstract boolean isSleeping();

    @Shadow public abstract void wakeUp(boolean bl, boolean bl2, boolean bl3);

    @Shadow public abstract boolean isFullyAsleep();

    @Inject(
            method = "swingHand",
            at = @At("HEAD"),
            cancellable = true
    )
    public void swingHand(CallbackInfo ci) {
        if (isSleeping() && isFullyAsleep() && Config.config.SLEEP_CONFIG.handSwingClickToExitBedsEnabled) {
            wakeUp(false, false, false);
        }
    }
}
