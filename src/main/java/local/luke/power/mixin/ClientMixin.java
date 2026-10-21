package local.luke.power.mixin;

import local.luke.power.PowerBeta;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientMixin {
  @Inject(method = "run", remap = false, at = @At("HEAD"))
  private void power$inventoryThread(CallbackInfo ci) {
    local.luke.power.inventory.InventoryActions.initializeGameThread();
    local.luke.power.inventory.tools.SavedCreativeHotbars.initialize();
    local.luke.power.inventory.tools.SortingLayoutPresets.initialize();
    local.luke.power.subtitles.Subtitles.initialize();
  }
  @Inject(method = "run", remap = false, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V", shift = At.Shift.AFTER, remap = false))
  private void power$inventoryFocus(CallbackInfo ci) {
    if (!org.lwjgl.opengl.Display.isActive()) {
      local.luke.power.inventory.NativeInventoryPlan.cancel();
      local.luke.power.inventory.tools.InventoryAutomation.clear();
    }
  }
  @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
  private net.minecraft.client.gui.screen.Screen power$firstSetup(net.minecraft.client.gui.screen.Screen screen) {
    local.luke.power.input.InventoryWheel.clear();
    local.luke.power.input.InventoryTextInput.clear();
    local.luke.power.inventory.InventoryActions.cancel();
    Minecraft mc = (Minecraft)(Object)this;
    return screen instanceof net.minecraft.client.gui.screen.TitleScreen && mc.world == null
        && local.luke.power.storage.PowerConfig.problem().isEmpty()
        && local.luke.power.config.DefaultBases.needsSetup()
        ? new local.luke.power.ui.DefaultsSetupScreen(screen) : screen;
  }
  @Inject(method = "method_2120", at = @At("HEAD"), cancellable = true)
  private void power$validateWorld(String folder, String name, long seed, CallbackInfo ci) {
    if (!local.luke.power.world.WorldLoadGuard.allow(folder)) ci.cancel();
  }
  @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "method_2115")
  private void power$inventoryWorld(net.minecraft.world.World world, String message,
      net.minecraft.entity.player.PlayerEntity player, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
    local.luke.power.input.InventoryWheel.clear();
    local.luke.power.inventory.InventoryActions.cancel();
    local.luke.power.world.WorldCompatibility.clear();
    if (world == null) power$resetTweaks();
    // Capture replaces native loading with a cancellable HEAD injection. Wrap its
    // completed path too, so successful loads always establish the durability proof.
    original.call(world, message, player);
    Minecraft mc = (Minecraft)(Object)this;
    try { local.luke.power.world.WorldCompatibility.confirm(mc); }
    catch (Exception failure) {
      PowerBeta.LOG.error("World compatibility verification failed", failure);
      local.luke.power.world.WorldLoadGuard.block(failure.getMessage());
      local.luke.power.world.WorldLoadGuard.tick(mc);
    }
  }
  @Inject(method = "stop", at = @At("HEAD"))
  private void power$quit(CallbackInfo ci) {
    local.luke.power.map.Maps.leave();
    power$resetTweaks();
    local.luke.power.inventory.NativeInventoryPlan.cancel();
    try { local.luke.power.storage.JournalQueue.awaitQueued(); }
    catch (java.io.IOException e) { PowerBeta.LOG.error("Could not finish inventory recovery writes before shutdown", e); }
    try { local.luke.power.storage.PowerConfig.flushPending(); }
    catch (java.io.IOException e) { PowerBeta.LOG.error("Could not save inactive tweaks before shutdown", e); }
  }
  @org.spongepowered.asm.mixin.Unique
  private void power$resetTweaks() {
    local.luke.power.input.InventoryWheel.clear();
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
    local.luke.power.inventory.NativeInventoryPlan.tick(mc);
    local.luke.power.input.InventoryWheel.context(mc.currentScreen, org.lwjgl.opengl.Display.isActive());
    local.luke.power.input.KeyConfig.load();
    if (mc.options != null) local.luke.power.input.Bindings.register(mc.options.allKeys);
  }
  @Inject(method = "tick", at = @At("TAIL"))
  private void power$tick(CallbackInfo ci) {
    PowerBeta.tick((Minecraft) (Object) this);
  }
}
