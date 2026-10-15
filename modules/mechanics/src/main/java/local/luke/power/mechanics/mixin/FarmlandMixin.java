package local.luke.power.mechanics.mixin;

import local.luke.power.mechanics.Config;
import local.luke.power.rules.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Block;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(FarmlandBlock.class)
public abstract class FarmlandMixin extends Block {

    public FarmlandMixin(int i) {
        super(i, Material.SOIL);
    }

    @Inject(method = "onSteppedOn", at = @At("HEAD"), cancellable = true)
    public void powerMechanics_onSteppedOnByPlayer(World arg, int i, int j, int k, Entity arg2, CallbackInfo ci) {
        if (arg.isRemote) return;
        if (arg2 instanceof LivingEntity && !(arg2 instanceof PlayerEntity)
                && !WorldRules.enabled(arg.getProperties(), GameRule.MOB_CROP_TRAMPLING)) {
            ci.cancel();
            return;
        }
        if (arg2 instanceof PlayerEntity player) {
            var boots = player.inventory.armor[0];
            if (Config.config.MOB_CONFIG.disableTrampleFarmlandWhenBarefoot && boots == null
                    || Config.config.MOB_CONFIG.disableTrampleFarmlandWithLeatherBoots
                    && boots != null && boots.itemId == Item.LEATHER_BOOTS.id) ci.cancel();
        }
    }
}
