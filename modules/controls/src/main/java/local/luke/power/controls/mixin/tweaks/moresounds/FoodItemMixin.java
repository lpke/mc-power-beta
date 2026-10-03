package local.luke.power.controls.mixin.tweaks.moresounds;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(FoodItem.class)
public class FoodItemMixin {
    @Unique
    private static final Random random = new Random();

    @Inject(method = "use", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack;count:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    public void burpOnEat(ItemStack stack, World world, PlayerEntity user, CallbackInfoReturnable<ItemStack> cir) {
        // A failed use must remain silent. FoodItem consumes exactly one item on success.
        if (ControlFeatures.FEATURES_CONFIG.eatingSounds)
            world.playSound(user, "power_controls:random.eat", 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        if (ControlFeatures.FEATURES_CONFIG.burpingSounds && random.nextInt(10) > 3)
            world.playSound(user, "power_controls:random.burp", 0.5F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }
}
