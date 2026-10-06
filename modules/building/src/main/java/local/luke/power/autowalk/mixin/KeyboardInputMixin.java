package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.AutoWalk;
import local.luke.power.autowalk.WalkToggle;
import local.luke.power.autowalk.InventoryMovement;
import local.luke.power.input.Bindings;
import local.luke.power.input.MovementScreens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import net.minecraft.class_41;
import net.minecraft.class_413;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Barn build 8 leaves KeyboardInput and its movement fields unnamed. */
@Mixin(value = class_413.class, priority = 1100)
public abstract class KeyboardInputMixin extends class_41 {
  @Shadow private boolean[] field_1661;
  @Shadow private GameOptions field_1662;

  @Inject(method = "method_1942", at = @At("HEAD"))
  private void updateHeldMovement(PlayerEntity player, CallbackInfo ci) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    boolean inInventory = InventoryMovement.allows(mc, mc.currentScreen);
    if (mc.currentScreen == null && !local.luke.power.building.config.Config.current().inventoryWhileMoving
        && Bindings.modifiers().isEmpty() && !Bindings.hasMouseBindings()) return;
    boolean active = mc.player == player && Display.isActive() && (mc.currentScreen == null || inInventory);
    field_1661[0] = active && power$held(field_1662.forwardKey, inInventory);
    field_1661[1] = active && power$held(field_1662.backKey, inInventory);
    field_1661[2] = active && power$held(field_1662.leftKey, inInventory);
    field_1661[3] = active && power$held(field_1662.rightKey, inInventory);
    field_1661[4] = active && power$held(field_1662.jumpKey, inInventory);
    field_1661[5] = active && !inInventory && Bindings.down(field_1662.sneakKey);
  }

  @org.spongepowered.asm.mixin.Unique
  private static boolean power$held(Object key, boolean inventory) {
    return inventory ? MovementScreens.keyboardDown(key) : Bindings.down(key);
  }

  @Inject(method = "method_1941", at = @At("HEAD"))
  private void cancelOnManualMovement(int keyCode, boolean pressed, CallbackInfo ci) {
    if (pressed && (keyCode == local.luke.power.input.Bindings.eventCode(field_1662.forwardKey) || keyCode == local.luke.power.input.Bindings.eventCode(field_1662.backKey))) {
      AutoWalk.stop();
    }
  }

  @Inject(method = "method_1942", at = @At("TAIL"))
  private void applyAutoWalk(PlayerEntity player, CallbackInfo ci) {
    if (AutoWalk.isWalking()) {
      // forward input, physical forward key, sneak state; preserve vanilla strafing/jumping.
      field_2533 = WalkToggle.forwardInput(field_2533, field_1661[0], field_2536);
    }
  }
}
