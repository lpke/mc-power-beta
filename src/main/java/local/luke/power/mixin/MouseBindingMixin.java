package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.class_381;
import net.minecraft.class_483;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MouseBindingMixin {
  @Shadow private int ticksPlayed;
  @Shadow private int field_2798;
  @Shadow private void method_2107(int button) { throw new AssertionError(); }
  @Unique private long power$mouseEvent = Long.MIN_VALUE;
  @Unique private boolean power$mouseConsumed;
  @Unique private boolean power$automaticUse;
  @Unique private boolean power$automaticAttack;

  @Inject(method="tick",at=@At(value="INVOKE",target="Lorg/lwjgl/input/Mouse;isButtonDown(I)Z",ordinal=0,remap=false))
  private void power$initialUse(CallbackInfo ci) {
    if(!UseInput.consumePress())return;
    Minecraft mc=(Minecraft)(Object)this;
    local.luke.power.offhand.OffhandUse.newPress(mc);
    method_2107(1);
    field_2798=ticksPlayed;power$automaticUse=true;
  }

  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
  private int power$mousePress(Operation<Integer> original) {
    int button = original.call();
    long event = Mouse.getEventNanoseconds();
    if (event != power$mouseEvent) {
      power$mouseEvent = event;
      power$mouseConsumed = false;
      Minecraft mc = (Minecraft)(Object)this;
      if (button == 1 && Mouse.getEventButtonState()) local.luke.power.offhand.OffhandUse.newPress(mc);
      if (button >= 0 && Mouse.getEventButtonState() && mc.field_2778 && mc.currentScreen == null
          && mc.player != null && Bindings.claimsMouse(button - 100)) {
        power$mouseConsumed = true;
        MouseActions.press(mc, button - 100);
      }
    }
    return power$mouseConsumed ? -1 : button;
  }

  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z", remap = false))
  private boolean power$heldMouse(int button, Operation<Boolean> original) {
    Minecraft mc = (Minecraft)(Object)this;
    if(DebugInput.suppressNative(button-100))return false;
    if (button == 0) {
      boolean automatic = AttackInput.held();
      // Grabbing the pointer postpones native repeats until a physical press.
      // A new automatic press must open that same native repeat/swing path.
      if (automatic && !power$automaticAttack) field_2798 = ticksPlayed - 100;
      power$automaticAttack = automatic;
      if (automatic) return true;
    }
    if (button == 1) {
      boolean automatic = UseInput.held();
      if (automatic && !power$automaticUse) field_2798 = ticksPlayed - 100;
      power$automaticUse = automatic;
    }
    if (button == 1 && mc.player instanceof PlayerMode mode && mode.power$creative()) {
      var held = mc.player.inventory.getSelectedItem();
      // The event path still handles every press. Creative keeps the item, so
      // Beta's separate held-button path would otherwise place another vehicle.
      if (!local.luke.power.offhand.Offhand.enabled(mc.player)
          && held != null && (held.getItem() instanceof class_483 || held.getItem() instanceof class_381)
          || local.luke.power.offhand.OffhandUse.heldVehicle(mc)) return false;
    }
    return button == 1 && power$automaticUse || !Bindings.claimsMouse(button - 100) && original.call(button);
  }
}
