package local.luke.power.mixin;

import local.luke.power.input.Bindings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_413;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_413.class)
public class MovementBindingMixin {
  @Shadow private boolean[] field_1661;
  @Shadow private GameOptions field_1662;

  @Inject(method = "method_1942", at = @At("HEAD"))
  private void power$movement(PlayerEntity player, CallbackInfo ci) {
    if (Bindings.modifiers().isEmpty() && !Bindings.hasMouseBindings()) return;
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    boolean active = mc.player == player && mc.currentScreen == null && Display.isActive();
    // Re-evaluate on modifier press/release too, so movement can never stay latched.
    field_1661[0] = active && Bindings.down(field_1662.forwardKey);
    field_1661[1] = active && Bindings.down(field_1662.backKey);
    field_1661[2] = active && Bindings.down(field_1662.leftKey);
    field_1661[3] = active && Bindings.down(field_1662.rightKey);
    field_1661[4] = active && Bindings.down(field_1662.jumpKey);
    field_1661[5] = active && Bindings.down(field_1662.sneakKey);
  }
}
