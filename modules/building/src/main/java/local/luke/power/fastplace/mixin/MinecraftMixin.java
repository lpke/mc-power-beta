package local.luke.power.fastplace.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.fastplace.FastPlace;
import local.luke.power.fastplace.PlacementTarget;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void fastplace$input(CallbackInfo ci) {
    FastPlace.tickInput((Minecraft) (Object) this);
  }

  @Inject(method = "tick", at = @At("TAIL"))
  private void fastplace$place(CallbackInfo ci) {
    FastPlace.placeBatch((Minecraft) (Object) this);
  }

  @Inject(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
  private void fastplace$release(CallbackInfo ci) {
    if (Mouse.getEventButton() == 1 && !Mouse.getEventButtonState()) FastPlace.release();
  }

  @Inject(method = "method_2107", at = @At("HEAD"), cancellable = true)
  private void fastplace$preventRepeat(int button, CallbackInfo ci) {
    if (FastPlace.suppressVanilla((Minecraft) (Object) this, button)) ci.cancel();
  }

  @WrapOperation(
      method = "method_2107",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/InteractionManager;method_1713(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)Z"))
  private boolean fastplace$first(
      InteractionManager manager,
      PlayerEntity player,
      World world,
      ItemStack stack,
      int x,
      int y,
      int z,
      int face,
      Operation<Boolean> original) {
    Minecraft mc = (Minecraft) (Object) this;
    PlacementTarget target =
        FastPlace.eligible(mc, stack) ? PlacementTarget.at(world, stack, x, y, z, face) : null;
    int slot = player.inventory.selectedSlot;
    float yaw = player.yaw;
    boolean result = original.call(manager, player, world, stack, x, y, z, face);
    FastPlace.firstPlacement(mc, stack, slot, face, yaw, target, result);
    return result;
  }

  @Inject(method = "setScreen", at = @At("HEAD"))
  private void fastplace$screen(Screen screen, CallbackInfo ci) {
    if (screen != null) FastPlace.interrupt();
  }

  @Inject(method = "method_2115", at = @At("HEAD"))
  private void fastplace$world(World world, String message, PlayerEntity player, CallbackInfo ci) {
    FastPlace.interrupt();
  }
}
