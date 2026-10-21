package local.luke.power.mixin;

import local.luke.power.world.SavedWorldMenu;
import net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EditWorldScreen.class, remap = false)
public abstract class SavedWorldMenuMixin {
  @Inject(method = "init", at = @At("TAIL"), remap = true)
  private void power$worldTools(CallbackInfo ci) { SavedWorldMenu.filterScreen((EditWorldScreen)(Object)this); }
}
