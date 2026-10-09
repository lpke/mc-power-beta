package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class PortalInventoryMixin {
  @Shadow protected Minecraft minecraft;

  private boolean power$keepInventory() {
    return VisualConfig.current().inventoryInPortals
        && minecraft.currentScreen != null
        && minecraft.currentScreen.getClass() == class_585.class
        && minecraft.player != null
        && ((class_585) minecraft.currentScreen).container == minecraft.player.playerContainer;
  }

  @WrapOperation(method = "method_937", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
  private void power$keepPortalInventory(Minecraft mc, Screen screen, Operation<Void> original) {
    if (!power$keepInventory()) original.call(mc, screen);
  }

  // The first cooldown write starts travel. Close before StationAPI transfers the player,
  // so normal cursor/crafting cleanup runs once in the source world, at its original position.
  @Inject(method = "method_937", at = @At(value = "FIELD",
      target = "Lnet/minecraft/entity/player/ClientPlayerEntity;field_511:I", opcode = 181, ordinal = 0))
  private void power$closeBeforeTravel(CallbackInfo ci) {
    if (power$keepInventory()) minecraft.player.closeScreen();
  }
}
