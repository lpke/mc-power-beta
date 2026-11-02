package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.ByteBuffer;
import local.luke.power.validation.Smoothness470Checks;
import net.mine_diver.smoothbeta.client.render.VboPool;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=VboPool.class,remap=false)
public class Smoothness470VboCapture {
  @WrapMethod(method="bufferData") private void upload(ByteBuffer data,VboPool.Pos pos,Operation<Void> original){long start=Smoothness470Checks.start();try{original.call(data,pos);}finally{Smoothness470Checks.measured(Smoothness470Checks.UPLOAD,start);}}
  @WrapMethod(method="compactRanges") private void compact(Operation<Void> original){long start=Smoothness470Checks.start();try{original.call();}finally{Smoothness470Checks.measured(Smoothness470Checks.COMPACT,start);}}
  @WrapMethod(method="expandVbo") private void grow(int capacity,Operation<Void> original){long start=Smoothness470Checks.start();try{original.call(capacity);}finally{Smoothness470Checks.measured(Smoothness470Checks.GROW,start);}}
}
