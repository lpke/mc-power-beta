package local.luke.power.creative.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.creative.*;
import local.luke.power.creative.command.CreativeCommands;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = Minecraft.class, priority = 1500)
public abstract class ClientMixin {
  @org.spongepowered.asm.mixin.Unique private local.luke.power.creative.api.GameMode power$respawnMode;

  @Inject(method = "method_2122", at = @At("HEAD"))
  private void power$beforeRespawn(boolean keep, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft) (Object) this;
    power$respawnMode =
        mc.level != null
                && !mc.level.isRemote
                && mc.player instanceof local.luke.power.creative.api.ModePlayer p
            ? p.power_mode()
            : null;
  }

  @Inject(method = "method_2122", at = @At("RETURN"))
  private void power$afterRespawn(boolean keep, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft) (Object) this;
    if (power$respawnMode != null && mc.player instanceof local.luke.power.creative.api.ModePlayer p)
      p.power_applyMode(power$respawnMode);
    power$respawnMode = null;
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void power$tick(CallbackInfo ci) {
    ClientRuntime.tick((Minecraft) (Object) this);
  }

  @Inject(method = "init", at = @At("RETURN"))
  private void power$commands(CallbackInfo ci) {
    CreativeCommands.register();
  }

  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
  private int power$scroll(int value) {
    return FlightController.scroll((Minecraft) (Object) this, value);
  }

  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
  private int power$keys(int key) {
    Minecraft mc = (Minecraft) (Object) this;
    if (ClientRuntime.local(mc)
        && Modes.spectator(mc.player)
        && (key == mc.options.inventoryKey.key || key == mc.options.dropKey.key)) return 0;
    return key;
  }

  @Inject(method = "processAttack", at = @At("HEAD"), cancellable = true)
  private void power$interact(int button, CallbackInfo ci) {
    if (Modes.spectator(((Minecraft) (Object) this).player)) ci.cancel();
  }

  @Inject(method = "processDigging", at = @At("HEAD"), cancellable = true)
  private void power$heldDig(int button, boolean held, CallbackInfo ci) {
    if (Modes.spectator(((Minecraft) (Object) this).player)) ci.cancel();
  }

  @Inject(method = "pickupHitBlock", at = @At("HEAD"), cancellable = true)
  private void power$pick(CallbackInfo ci) {
    if (Modes.spectator(((Minecraft) (Object) this).player)) ci.cancel();
  }
}
