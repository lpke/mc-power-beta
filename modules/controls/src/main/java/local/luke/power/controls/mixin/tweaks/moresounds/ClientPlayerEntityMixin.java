package local.luke.power.controls.mixin.tweaks.moresounds;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.input.SoundCaptionContext;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {


    @Inject(method = "openChestScreen", at = @At(value = "TAIL"))
    public void chestOpeningSound(Inventory inventory, CallbackInfo ci) {

        if (ControlFeatures.FEATURES_CONFIG.moreSoundsChests != local.luke.power.controls.tweaks.moresounds.ChestSoundsEnum.NONE) {
            PlayerEntity player = (PlayerEntity.class.cast(this));
            try (var ignored = SoundCaptionContext.push("subtitles.block.chest.open")) {
                player.world.playSound(player, ControlFeatures.FEATURES_CONFIG.moreSoundsChests.getOpenSound(), 0.3F, 1.0F);
            }
        }
    }

    @Inject(method = "closeHandledScreen", at = @At(value = "HEAD"))
    public void chestClosingSound(CallbackInfo ci) {
        if (ControlFeatures.FEATURES_CONFIG.moreSoundsChests != local.luke.power.controls.tweaks.moresounds.ChestSoundsEnum.NONE) {
            PlayerEntity player = (PlayerEntity.class.cast(this));
            if (player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                try (var ignored = SoundCaptionContext.push("subtitles.block.chest.close")) {
                    player.world.playSound(player, ControlFeatures.FEATURES_CONFIG.moreSoundsChests.getCloseSound(), 0.3F, 1.0F);
                }
            }
        }
    }
}
