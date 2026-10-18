package local.luke.power.validation.mixin;

import local.luke.power.validation.Release252Checks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public class Armor252Callbacks {
  @Inject(method="onSlotClick",at=@At("RETURN"))
  private void power$callback(int slot,int button,boolean shift,PlayerEntity player,CallbackInfoReturnable<ItemStack> cir){
    if(Release252Checks.callbackHandler==this&&slot==Release252Checks.callbackSlot&&Release252Checks.callback!=null){
      var action=Release252Checks.callback;Release252Checks.callback=null;action.run();
    }
  }
}
