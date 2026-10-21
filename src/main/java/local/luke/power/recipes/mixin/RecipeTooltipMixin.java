package local.luke.power.recipes.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.input.InventoryPanels;
import net.modificationstation.stationapi.api.client.event.gui.screen.container.TooltipRenderEvent;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;

/** StationAPI subtracts the unshifted origin from an already translated container matrix. */
@Mixin(targets="net.modificationstation.stationapi.impl.client.gui.screen.container.CustomTooltipRendererImpl",remap=false)
public abstract class RecipeTooltipMixin {
  @WrapMethod(method="renderCustomTooltips")
  private static void power$tooltipOrigin(TooltipRenderEvent event,Operation<Void> original) {
    int offset=event.container==null?0:InventoryPanels.offset(event.container);
    if(offset==0){original.call(event);return;}
    GL11.glPushMatrix();
    try{GL11.glTranslatef(-offset,0,0);original.call(event);}finally{GL11.glPopMatrix();}
  }
}
