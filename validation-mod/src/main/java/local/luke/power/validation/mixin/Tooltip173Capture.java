package local.luke.power.validation.mixin;
import local.luke.power.validation.Release173Checks;
import net.minecraft.client.font.TextRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TextRenderer.class)
public class Tooltip173Capture {
  @Inject(method="drawWithShadow",at=@At("HEAD"))
  private void text(String text,int x,int y,int color,CallbackInfo ci){
    if(Release173Checks.capture && text.equals("Clear inventory")){
      Release173Checks.tooltip=++Release173Checks.event;Release173Checks.tooltipCount++;
      Release173Checks.tooltipDepth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      Release173Checks.tooltipLighting=GL11.glIsEnabled(GL11.GL_LIGHTING);
    }
  }
}
