package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import net.minecraft.client.InteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=InteractionManager.class,priority=2300)
public class Smoothness470PlaceCapture {
  @WrapMethod(method="method_1713") private boolean measure(PlayerEntity player,World world,ItemStack stack,int x,int y,int z,int face,Operation<Boolean> original){long start=Smoothness470Checks.start();try{return original.call(player,world,stack,x,y,z,face);}finally{Smoothness470Checks.measured(Smoothness470Checks.PLACE,start);}}
}
