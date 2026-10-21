package local.luke.power.recipes.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.input.InventoryPanels;
import net.minecraft.class_524;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Both background texture and any native model/cook progress use this shifted origin. */
@Mixin(class_524.class)
public abstract class RecipeFurnaceBackgroundMixin {
  @ModifyExpressionValue(method="drawBackground",at=@At(value="FIELD",target="Lnet/minecraft/class_524;width:I"))
  private int power$backgroundOffset(int width){return width+2*InventoryPanels.offset(this);}
}
