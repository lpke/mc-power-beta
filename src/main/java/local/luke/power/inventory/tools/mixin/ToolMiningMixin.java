package local.luke.power.inventory.tools.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.inventory.tools.*;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SingleplayerInteractionManager.class, priority = 1100)
public abstract class ToolMiningMixin extends InteractionManager {
  protected ToolMiningMixin(Minecraft mc) { super(mc); }
  @Inject(method = {"method_1707", "method_1721"}, at = @At("HEAD"), cancellable = true)
  private void power$tool(int x, int y, int z, int face, CallbackInfo ci) {
    if (!AutoToolSwitch.prepare(minecraft, x, y, z)) ci.cancel();
  }
  @WrapMethod(method = "method_1716")
  private boolean power$broken(int x, int y, int z, int face, Operation<Boolean> original) {
    var action = minecraft.player == null ? null : Restock.before(minecraft, minecraft.player.inventory.getSelectedItem());
    boolean result = original.call(x, y, z, face);
    if (result) Restock.after(minecraft, action);
    return result;
  }
}
