package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import local.luke.power.render.CullingRendererAccess;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;

/** Native world setup racing only immutable worker reads, including unregistered subclasses. */
public final class Culling421Checks {
  public static final AtomicInteger backgroundRendererReads = new AtomicInteger();
  public static class InheritedSign extends SignBlockEntity {}
  public static class MissingRenderer extends BlockEntity {}
  private Culling421Checks() {}

  public static void run(Minecraft mc) throws Exception {
    check(mc.world != null, "open a disposable world");
    check(backgroundRendererReads.get() == 0, "culling worker called the mutating native renderer resolver");
    check(backgroundChunkReads.get() == 0, "culling worker called the native chunk loader");
    var nativeDispatcher = BlockEntityRenderDispatcher.INSTANCE;
    nativeDispatcher.method_1273(mc.world);
    var snapshots = (CullingRendererAccess)nativeDispatcher;
    var source = (Map<?, ?>)UiChecks.field(nativeDispatcher, "field_1564");
    int size = source.size();
    var failure = new AtomicReference<Throwable>();
    var worker = new Thread(() -> {
      try {
        for (int i = 0; i < 20000; i++) {
          check(snapshots.power$hasRenderer(InheritedSign.class), "inherited sign renderer missing");
          check(!snapshots.power$hasRenderer(MissingRenderer.class), "missing renderer became visible");
        }
      } catch (Throwable error) { failure.set(error); }
    }, "PowerBeta culling validation");
    worker.start();
    for (int i = 0; i < 2000; i++) nativeDispatcher.method_1273(mc.world);
    worker.join(5000);
    check(!worker.isAlive() && failure.get() == null, "snapshot race failed: " + failure.get());
    check(source.size() == size && !source.containsKey(InheritedSign.class)
        && !source.containsKey(MissingRenderer.class), "worker resolved into native registry");
    check(nativeDispatcher.method_1275(InheritedSign.class) != null
        && nativeDispatcher.method_1275(MissingRenderer.class) == null, "native resolution differs");
    check(snapshots.power$hasRenderer(InheritedSign.class)
        && !snapshots.power$hasRenderer(MissingRenderer.class), "native discoveries were not published");
    log("CULLING 421 PASS: native setup, inherited/absent renderers, immutable worker reads, loaded terrain only");
  }
}
