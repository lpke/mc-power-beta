package local.luke.power.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.ui.*;
import net.minecraft.client.gui.widget.EntryListWidget;

/** Match Done when leaving StationAPI's Edit menu with Escape. */
@Mixin(value = EditWorldScreen.class, remap = false)
public abstract class EditWorldEscapeMixin extends Screen implements WorldEditOwner {
  @Shadow @Final protected Screen parent;
  @Unique private final WorldEditControls power$controls = new WorldEditControls();
  public WorldEditControls power$worldControls() { return power$controls; }
  @Inject(method = "init", at = @At("TAIL"), remap = true)
  private void power$init(CallbackInfo ci) { power$controls.init(minecraft, width, height); Keyboard.enableRepeatEvents(true); }
  @WrapOperation(method = "render", at = @At(value = "INVOKE",
      target = "Lnet/modificationstation/stationapi/api/client/gui/screen/EditWorldScreen$EditButtonList;render(IIF)V", remap = true), remap = true)
  private void power$prepare(@org.spongepowered.asm.mixin.injection.Coerce Object list, int x, int y, float delta, Operation<Void> original) {
    power$controls.prepare((EditWorldScreen)(Object)this, (WorldEditButtons)list);
    original.call(list, x, y, delta);
  }
  @ModifyArg(method = "render", remap = true, at = @At(value = "INVOKE",
      target = "Lnet/modificationstation/stationapi/api/client/gui/screen/EditWorldScreen;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", remap = true), index = 1)
  private String power$title(String old) { return power$controls.title((EditWorldScreen)(Object)this); }
  @Inject(method = "render", at = @At("TAIL"), remap = true)
  private void power$render(int x, int y, float delta, CallbackInfo ci) { power$controls.render(x, y, delta); }
  @Override protected void mouseClicked(int x, int y, int button) {
    if (!power$controls.press(x, y, button)) super.mouseClicked(x, y, button);
  }
  @Override protected void mouseReleased(int x, int y, int button) {
    power$controls.release(button); super.mouseReleased(x, y, button);
  }
  @Override public void removed() { power$controls.cancel(); Keyboard.enableRepeatEvents(false); super.removed(); }

  @Override
  protected void keyPressed(char character, int key) {
    if (power$controls.key(character, key)) return;
    if (key == Keyboard.KEY_ESCAPE) minecraft.setScreen(parent);
    else super.keyPressed(character, key);
  }
}
