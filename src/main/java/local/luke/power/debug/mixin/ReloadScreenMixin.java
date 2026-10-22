package local.luke.power.debug.mixin;

import local.luke.power.debug.DebugReload;
import net.minecraft.client.gui.screen.Screen;
import net.modificationstation.stationapi.api.client.resource.ReloadScreenManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Preserve StationAPI's reload executor and failure screen, remove its timed logo stages. */
@Mixin(targets = "net.modificationstation.stationapi.api.client.resource.ReloadScreen", remap = false)
public abstract class ReloadScreenMixin extends Screen {
  @Shadow @Final private Screen parent;
  @Shadow @Final private Runnable done;
  @Unique private long power$start, power$finished;
  @Inject(method = "isReloadStarted", at = @At("HEAD"), cancellable = true)
  private void power$startImmediately(CallbackInfoReturnable<Boolean> cir) {
    if (DebugReload.requested()) cir.setReturnValue(true);
  }
  @Inject(method = {"render", "method_118"}, at = @At("HEAD"), cancellable = true, require = 1)
  private void power$compactReload(int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (!DebugReload.requested()) return;
    long now = System.nanoTime();
    if (power$start == 0) power$start = now;
    var reload = ReloadScreenManager.getCurrentReload();
    if (power$finished == 0 && ReloadScreenManager.isReloadComplete()) {
      try { if (reload.isPresent()) reload.get().throwException(); }
      catch (RuntimeException error) { DebugReload.finish(); return; }
      power$finished = now;
    }
    if (parent != null) parent.render(mouseX, mouseY, delta);
    double alpha = power$finished == 0 ? Math.min(1, (now - power$start) / 120_000_000.0)
        : Math.max(0, 1 - (now - power$finished) / 160_000_000.0);
    fill(0, 0, width, height, (int)(alpha * 230) << 24);
    int color = (int)(alpha * 255) << 24 | 0xffffff;
    if(alpha>0) {
      org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
      try {
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawCenteredTextWithShadow(textRenderer, "Reloading textures and resources...", width / 2, height / 2 - 14, color);
      }finally{org.lwjgl.opengl.GL11.glPopAttrib();}
    }
    int barWidth = Math.max(20, Math.min(240, width - 40));
    int left = (width - barWidth) / 2, top = height / 2 + 5;
    fill(left, top, left + barWidth, top + 4, (int)(alpha * 110) << 24 | 0xffffff);
    double progress = power$finished != 0 ? 1 : reload.map(value -> (double)value.getProgress()).orElse(0.0);
    fill(left, top, left + (int)(barWidth * Math.max(0, Math.min(1, progress))), top + 4, color);
    ci.cancel();
    if (power$finished != 0 && alpha == 0) {
      DebugReload.finish(); ReloadManagerAccess.power$finish(); done.run();
      local.luke.power.debug.DebugActions.message(minecraft,"Reloaded textures and resources");
    }
  }
}
