package local.luke.power.validation.mixin;
import local.luke.power.validation.Release173Checks;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemRenderer.class)
public class Item173Capture {
  @Inject(method={"method_1487","method_1488"},at=@At("RETURN"))
  private void item(CallbackInfo ci){if(Release173Checks.capture)Release173Checks.lastItem=++Release173Checks.event;}
}
