package local.luke.power.mixin;

import local.luke.power.PowerBeta;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientMixin {
  @Inject(method = "method_2120", at = @At("HEAD"), cancellable = true)
  private void power$validateWorld(String folder, String name, long seed, CallbackInfo ci) {
    if (!local.luke.power.world.WorldLoadGuard.allow(folder)) ci.cancel();
  }
  @Inject(method = "method_2115", at = @At("HEAD"))
  private void power$leaveWorld(net.minecraft.world.World world, String message,
      net.minecraft.entity.player.PlayerEntity player, CallbackInfo ci) {
    if (world == null) power$resetTweaks();
  }
  @Inject(method = "stop", at = @At("HEAD"))
  private void power$quit(CallbackInfo ci) {
    power$resetTweaks();
    try { local.luke.power.storage.PowerConfig.flushPending(); }
    catch (java.io.IOException e) { PowerBeta.LOG.error("Could not save inactive tweaks before shutdown", e); }
  }
  @org.spongepowered.asm.mixin.Unique
  private void power$resetTweaks() {
    try { local.luke.power.input.TweakLifecycle.reset(); }
    catch (RuntimeException e) { PowerBeta.LOG.error("Could not clear every active tweak", e); }
  }
  @Inject(method = "method_2139", at = @At("HEAD"))
  private void power$dimensionMusic(CallbackInfo ci) {
    local.luke.power.audio.AudioController.changingDimension();
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void power$bindings(CallbackInfo ci) {
    Minecraft mc = (Minecraft)(Object)this;
    local.luke.power.input.KeyConfig.load();
    if (mc.options != null) local.luke.power.input.Bindings.register(mc.options.allKeys);
  }
  @Inject(method = "tick", at = @At("TAIL"))
  private void power$tick(CallbackInfo ci) {
    PowerBeta.tick((Minecraft) (Object) this);
  }
}
