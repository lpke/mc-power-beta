package local.luke.creative.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.creative.*;
import local.luke.creative.command.CreativeCommands;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = Minecraft.class, priority = 1500)
public abstract class ClientMixin {
  @org.spongepowered.asm.mixin.Unique private local.luke.creative.api.GameMode lpke$respawnMode;

  @Inject(method = "method_2122", at = @At("HEAD"))
  private void lpke$beforeRespawn(boolean keep, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft) (Object) this;
    lpke$respawnMode =
        mc.level != null
                && !mc.level.isRemote
                && mc.player instanceof local.luke.creative.api.ModePlayer p
            ? p.lpke_mode()
            : null;
  }

  @Inject(method = "method_2122", at = @At("RETURN"))
  private void lpke$afterRespawn(boolean keep, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft) (Object) this;
    if (lpke$respawnMode != null && mc.player instanceof local.luke.creative.api.ModePlayer p)
      p.lpke_applyMode(lpke$respawnMode);
    lpke$respawnMode = null;
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void lpke$tick(CallbackInfo ci) {
    ClientRuntime.tick((Minecraft) (Object) this);
  }

  @Inject(method = "init", at = @At("RETURN"))
  private void lpke$commands(CallbackInfo ci) {
    CreativeCommands.register();
  }

  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
  private int lpke$scroll(int value) {
    return FlightController.scroll((Minecraft) (Object) this, value);
  }

  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
  private int lpke$keys(int key) {
    Minecraft mc = (Minecraft) (Object) this;
    if (ClientRuntime.local(mc)
        && Modes.spectator(mc.player)
        && (key == mc.options.inventoryKey.key || key == mc.options.dropKey.key)) return 0;
    return key;
  }

  @Inject(method = "processAttack", at = @At("HEAD"), cancellable = true)
  private void lpke$interact(int button, CallbackInfo ci) {
    if (Modes.spectator(((Minecraft) (Object) this).player)) ci.cancel();
  }

  @Inject(method = "processDigging", at = @At("HEAD"), cancellable = true)
  private void lpke$heldDig(int button, boolean held, CallbackInfo ci) {
    if (Modes.spectator(((Minecraft) (Object) this).player)) ci.cancel();
  }

  @Inject(method = "pickupHitBlock", at = @At("HEAD"), cancellable = true)
  private void lpke$pick(CallbackInfo ci) {
    if (Modes.spectator(((Minecraft) (Object) this).player)) ci.cancel();
  }
}
