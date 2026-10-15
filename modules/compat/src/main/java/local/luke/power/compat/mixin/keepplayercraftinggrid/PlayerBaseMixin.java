package local.luke.power.compat.mixin.keepplayercraftinggrid;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerBaseMixin extends LivingEntity {

    @Shadow public ScreenHandler playerScreenHandler;

    public PlayerBaseMixin(World arg) {
        super(arg);
    }

    @Unique private static final String POWER_CRAFTING_INPUTS = "power_crafting_inputs";

    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void powerCompat$writeCraftingInputs(NbtCompound tag, CallbackInfo ci) {
        if (world.isRemote) return;
        var grid = ((PlayerScreenHandler) playerScreenHandler).craftingInput;
        var inputs = new NbtCompound();
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = grid.getStack(slot);
            if (stack != null) {
                NbtCompound data = stack.writeNbt(new NbtCompound());
                data.putInt("FullCount", stack.count);
                inputs.put("Slot" + slot, data);
            }
        }
        // Saving preserves occupied inputs even if the preference changed while open.
        // Native closure decides whether to retain or drop them; saving never moves items.
        tag.put(POWER_CRAFTING_INPUTS, inputs);
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    private void powerCompat$readCraftingInputs(NbtCompound tag, CallbackInfo ci) {
        if (world.isRemote) return;
        var grid = ((PlayerScreenHandler) playerScreenHandler).craftingInput;
        boolean nativeData = tag.contains(POWER_CRAFTING_INPUTS);
        var inputs = tag.getCompound(POWER_CRAFTING_INPUTS);
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = null;
            if (nativeData) {
                if (inputs.contains("Slot" + slot)) {
                    NbtCompound data = inputs.getCompound("Slot" + slot);
                    stack = new ItemStack(data);
                    stack.count = data.getInt("FullCount");
                }
            } else {
                int count = tag.getInt("ItemAmount_Slot" + slot);
                if (count > 0) stack = new ItemStack(tag.getInt("ItemID_Slot" + slot), count, tag.getInt("ItemDamage_Slot" + slot));
            }
            grid.setStack(slot, stack);
        }
    }
}
