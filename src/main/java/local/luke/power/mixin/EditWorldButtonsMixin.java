package local.luke.power.mixin;

import java.util.List;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ButtonWidget;
import local.luke.power.ui.*;
import net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen;
import net.modificationstation.stationapi.api.client.gui.widget.ButtonWidgetAttachedContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen$EditButtonList", remap = false)
public abstract class EditWorldButtonsMixin implements WorldEditButtons {
  @Shadow @Final protected List<ButtonWidgetAttachedContext> editButtons;
  @Unique private WorldEditControls power$controls;
  public List<ButtonWidgetAttachedContext> power$editButtons() { return editButtons; }
  public void power$renameOwner(WorldEditControls controls) { power$controls = controls; }
  @WrapOperation(method = "renderEntry", remap = true, at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/gui/widget/ButtonWidget;render(Lnet/minecraft/client/Minecraft;II)V", remap = true))
  private void power$hideRename(ButtonWidget button, Minecraft mc, int x, int y, Operation<Void> original) {
    if (power$controls == null || !power$controls.hides(button)) original.call(button, mc, x, y);
  }
  @Inject(method = "entryClicked", at = @At("HEAD"), cancellable = true, remap = true)
  private void power$inlineOwnsPress(int index, boolean twice, CallbackInfo ci) {
    if (power$controls != null && power$controls.ownsPress()) ci.cancel();
  }
}
