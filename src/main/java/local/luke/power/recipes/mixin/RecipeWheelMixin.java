package local.luke.power.recipes.mixin;

import local.luke.power.recipes.RecipeBooks;
import local.luke.power.input.InventoryWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Screen.class,priority=1100)
public abstract class RecipeWheelMixin {
  @Shadow protected Minecraft minecraft;
  @Shadow public int width;
  @Shadow public int height;
  @Inject(method="onMouseEvent",at=@At("HEAD"),cancellable=true)
  private void power$bookWheel(CallbackInfo ci){
    if(Mouse.getEventDWheel()==0||minecraft.currentScreen!=(Object)this)return;
    int x=Mouse.getEventX()*width/minecraft.displayWidth,y=height-Mouse.getEventY()*height/minecraft.displayHeight-1;
    if(RecipeBooks.owns(this,x,y)){
      if(org.lwjgl.opengl.Display.isActive())RecipeBooks.book(this).scroll(x,y,Mouse.getEventDWheel());
      InventoryWheel.event(this,org.lwjgl.opengl.Display.isActive(),0,true);ci.cancel();
    }
  }
}
