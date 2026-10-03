package local.luke.power.controls.mixin.tweaks.fencejump;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity {
    public PlayerEntityMixin(World world) {
        super(world);
    }

    @Inject(method = "jump", at = @At("TAIL"))
    public void increaseJumpHeight(CallbackInfo ci) {
        if (ControlFeatures.GAMEPLAY_CONFIG.fenceJumping) {
            this.velocityY += 0.002F;
        }
    }
}
