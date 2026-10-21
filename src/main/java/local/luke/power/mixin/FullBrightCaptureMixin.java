package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.RenderControls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Isometric captures draw the world outside Minecraft's normal render entry point. */
@Mixin(targets = "local.luke.power.capture.IsometricScreenshotRenderer", remap = false)
@Pseudo
public class FullBrightCaptureMixin {
  @WrapMethod(method = "doRender")
  private void power$capture(Operation<Void> original) {
    var mc = (net.minecraft.client.Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    RenderControls.beginRender();
    try {
      local.luke.power.render.HeldLighting.beginCapture(mc, 0);
      original.call();
    }
    // Preparation runs before the capture method's own error handler. Keep its
    // normal failure behavior for allocation or native-render failures here too.
    catch (OutOfMemoryError failure) { mc.inGameHud.addChatMessage("Out of memory. Reduce render distance and try again."); }
    catch (Throwable failure) { failure.printStackTrace(); }
    finally {
      local.luke.power.render.HeldLighting.endCapture();
      RenderControls.endRender();
    }
  }
}
