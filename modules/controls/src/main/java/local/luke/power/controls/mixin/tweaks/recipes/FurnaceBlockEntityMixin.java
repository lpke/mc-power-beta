package local.luke.power.controls.mixin.tweaks.recipes;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.tweaks.recipes.FuelLookup;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FurnaceBlockEntity.class)
public class FurnaceBlockEntityMixin extends BlockEntity {
    @Inject(method = "getFuelTime", at = @At(value = "HEAD"), cancellable = true)
    public void injectFuelValues(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (ControlFeatures.RECIPES_CONFIG.furnaceFuels) {
            if (stack != null) {
                FuelLookup.FuelLookupEntry entry = FuelLookup.lookup.get(stack.getItem());

                if (entry != null) {
                    if (entry.meta() == -1 || entry.meta() == stack.getDamage()) {
                        cir.setReturnValue(entry.fuelTime());
                    }
                }
            }
        }
    }
}
