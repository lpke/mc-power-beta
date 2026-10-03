package local.luke.power.mixin;
import java.util.*;
import net.minecraft.class_525;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Repositions existing buttons; native save/quit and photo actions keep their original handlers. */
@Mixin(value=class_525.class,priority=800)
public class PauseMixin extends Screen {
  @Inject(method="init",at=@At("TAIL"))
  private void power$layout(CallbackInfo ci){
    int top=height/4+8;ButtonWidget photo=null,options=null;
    for(Object object:buttons){ButtonWidget b=(ButtonWidget)object;
      switch(b.id){case 4->{b.x=width/2-100;b.y=top;}case 5->{b.x=width/2-100;b.y=top+24;}case 6->{b.x=width/2+2;b.y=top+24;}case 0->{b.x=width/2-100;b.y=top+48;options=b;}case 20->{b.x=width/2+2;b.y=top+48;photo=b;}case 1->{b.x=width/2-100;b.y=top+84;}default->{}}
    }
    if(photo!=null&&options!=null){((ButtonAccessor)photo).power$width(98);((ButtonAccessor)options).power$width(98);photo.text="Photo mode...";}
  }
}
