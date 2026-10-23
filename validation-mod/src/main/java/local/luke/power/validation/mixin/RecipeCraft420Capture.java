package local.luke.power.validation.mixin;

import local.luke.power.validation.Recipe402Checks;
import net.minecraft.class_557;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_557.class)
public abstract class RecipeCraft420Capture {
  @Shadow private Inventory field_2366;
  @Shadow private PlayerEntity field_2367;
  @Inject(method = "onCrafted", at = @At("TAIL"))
  private void validation$nativeCraft(ItemStack result, CallbackInfo ci) {
    Recipe402Checks.craftCallback(field_2366, field_2367, result);
    local.luke.power.validation.Recipe441Checks.callback(field_2366, field_2367, result);
  }
}
