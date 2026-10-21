package local.luke.power.recipes.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.recipes.*;
import local.luke.power.input.InventoryPanels;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ContainerScreen.class,priority=2500)
public abstract class RecipeContainerMixin extends Screen implements RecipeBookHost {
  @Unique private RecipeBookOverlay power$book;
  @Shadow protected abstract void drawBackground(float delta);
  @Override public RecipeBookOverlay power$recipeBook(){
    if(power$book==null){var context=RecipeContext.of((ContainerScreen)(Object)this);if(context!=null)power$book=new RecipeBookOverlay(this,minecraft,context,false);}
    return power$book;
  }
  @Inject(method="init",at=@At("TAIL"))
  private void power$init(CallbackInfo ci){var book=power$recipeBook();if(book!=null)book.tick();}
  @Inject(method="tick",at=@At("TAIL"))
  private void power$tick(CallbackInfo ci){var book=power$recipeBook();if(book!=null)book.tick();}
  @ModifyExpressionValue(method={"render","isPointOverSlot","mouseClicked"},at=@At(value="FIELD",target="Lnet/minecraft/client/gui/screen/container/ContainerScreen;width:I"))
  private int power$nativeOffset(int width){return width+2*InventoryPanels.offset(this);}
  @ModifyExpressionValue(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerInventory;getCursorStack()Lnet/minecraft/item/ItemStack;",ordinal=0))
  private ItemStack power$deferCarried(ItemStack stack){var book=power$recipeBook();return book!=null&&book.rendersCarried()?null:stack;}
  @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/container/ContainerScreen;drawBackground(F)V",shift=At.Shift.AFTER))
  private void power$buttonBehindItems(int x,int y,float delta,CallbackInfo ci){var book=power$recipeBook();if(book!=null)book.renderInventoryButton(x,y);}
  @WrapMethod(method="render")
  private void power$render(int x,int y,float delta,Operation<Void> original){var book=power$recipeBook();
    if(book!=null&&book.replacesNative()){
      renderBackground();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
      try{drawBackground(delta);}finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }else original.call(x,y,delta);
    if(book!=null)book.render(x,y);
  }
  // Dispatch before inventory gesture guards reject clicks in registered panel bounds.
  @WrapMethod(method="mouseClicked")
  private void power$mouse(int x,int y,int button,Operation<Void> original){var book=power$recipeBook();if(book!=null&&book.mouse(x,y,button))power$cancelBookGestures();else original.call(x,y,button);}
  @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
  private void power$key(char c,int key,CallbackInfo ci){var book=power$recipeBook();if(book!=null&&book.key(c,key)){power$cancelBookGestures();ci.cancel();}}
  @Unique private void power$cancelBookGestures(){
    if((Object)this instanceof local.luke.power.input.InventoryDrag drag)drag.power$cancelItemDrag();
    if((Object)this instanceof local.luke.power.input.CreativePaint paint)paint.power$cancelCreativePaint();
  }
  @Inject(method="removed",at=@At("HEAD"))
  private void power$close(CallbackInfo ci){if(power$book!=null)power$book.removed();InventoryPanels.remove(this);}
}
