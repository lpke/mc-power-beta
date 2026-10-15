package local.luke.power.compat.mixin.stackablebuckets;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import local.luke.power.compat.Config;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public abstract class ItemMixin {
    @ModifyReturnValue(method = "getMaxCount", at = @At("RETURN"))
    private int power$stackLimit(int original) {
        var settings = Config.config.STACK_SIZE_CONFIG;
        if ((Object) this == Item.BUCKET && settings.stackableEmptyBuckets) return 16;
        if ((Object) this == Item.WOODEN_DOOR && settings.enableModernWoodDoorStackSize) return 64;
        if ((Object) this == Item.IRON_DOOR && settings.enableModernIronDoorStackSize) return 64;
        if ((Object) this == Item.SIGN && settings.enableModernSignStackSize) return 16;
        return original;
    }
}
