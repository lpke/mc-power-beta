package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.PowerBeta;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PowerBeta.class, remap = false)
public class Performance421TickCapture {
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/inventory/tools/InventoryAutomation;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$inventoryTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("inventoryTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/subtitles/Subtitles;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$subtitlesTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("subtitlesTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/chat/PlayerChat;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$chatTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("chatTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/audio/AudioController;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$audioTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("audioTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/light/LightOverlay;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$lightTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("lightTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/render/RenderControls;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$renderControlsTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("renderControlsTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/hud/InfoHud;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$hudTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("hudTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/sprint/SprintController;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$sprintTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("sprintTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/mining/VeinMining;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$veinTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("veinTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/status/TweakMessages;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$indicatorTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("indicatorTick", start); }
  }
  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Llocal/luke/power/debug/DebugKeys;tick(Lnet/minecraft/client/Minecraft;)V"))
  private static void power$debugTick(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("debugTick", start); }
  }
}
