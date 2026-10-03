package local.luke.power.gold.mixin;

import local.luke.power.gold.Config;
import net.minecraft.item.ToolMaterial;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ToolMaterial.class)
public class ToolMaterialMixin {

    @Shadow @Final private float miningSpeed;

    @Shadow @Final private int itemDurability;

    @Inject(at = @At("HEAD"), method = "getMiningLevel", cancellable = true)
    public void powerGold_getMiningLevel(CallbackInfoReturnable<Integer> cir) {
        if (  (Config.config.enableGoldPickaxeSilkTouch)
           && (12.0F == this.miningSpeed)
           && (32 == this.itemDurability)
        ) {
            cir.setReturnValue(2);
        }
    }
}
