package local.luke.power.compat.mixin.stackablebuckets;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import local.luke.power.compat.Config;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public abstract class ItemMixin {
    @ModifyReturnValue(method = "getMaxCount", at = @At("RETURN"))
    private int power$emptyBucketLimit(int original) {
        return (Object) this == Item.BUCKET && Config.config.STACK_SIZE_CONFIG.stackableEmptyBuckets
                ? 16 : original;
    }
}
