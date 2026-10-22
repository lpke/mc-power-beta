package local.luke.power.debug.mixin;

import net.modificationstation.stationapi.api.client.resource.ReloadScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ReloadScreenManager.class, remap = false)
public interface ReloadManagerAccess {
  @Invoker("onFinish") static void power$finish() { throw new AssertionError(); }
}
