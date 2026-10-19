package local.luke.power.controls.mixin.tweaks.moresounds;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract Item getItem();

    @Unique
    private static final Random random = new Random();

    @Inject(method = "damage", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/item/ItemStack;count:I", ordinal = 0, shift = At.Shift.BEFORE))
    public void playSoundOnBreak(int amount, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;
        boolean enabled = this.getItem() instanceof ArmorItem
                ? ControlFeatures.FEATURES_CONFIG.armorBreakSounds
                : ControlFeatures.FEATURES_CONFIG.toolBreakSounds;
        if (enabled)
            player.world.playSound(player, "power_controls:random.break", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }
}
