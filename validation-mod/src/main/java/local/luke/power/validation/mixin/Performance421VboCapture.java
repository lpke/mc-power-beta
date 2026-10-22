package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.nio.ByteBuffer;
import local.luke.power.validation.Performance420Checks;
import net.mine_diver.smoothbeta.client.render.VboPool;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = VboPool.class, remap = false)
public class Performance421VboCapture {
  @WrapMethod(method = "bufferData")
  private void power$upload(ByteBuffer data, VboPool.Pos pos, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(data, pos); }
    finally { Performance420Checks.measured("vboUpload", start); }
  }
  @WrapMethod(method = "compactRanges")
  private void power$compact(Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(); }
    finally { Performance420Checks.measured("vboCompact", start); }
  }
  @WrapMethod(method = "expandVbo")
  private void power$grow(int capacity, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(capacity); }
    finally { Performance420Checks.measured("vboGrow", start); }
  }
}
