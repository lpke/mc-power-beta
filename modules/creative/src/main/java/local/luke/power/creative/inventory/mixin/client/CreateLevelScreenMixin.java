package local.luke.power.creative.inventory.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.CreateLevelScreen;
import net.minecraft.client.gui.widgets.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateLevelScreen.class)
public abstract class CreateLevelScreenMixin extends Screen {
	@Unique private boolean cheats = false;
	
	@SuppressWarnings("unchecked")
	@Inject(method = "init", at = @At("TAIL"))
	private void creative_addButtons(CallbackInfo info) {
		Button cancelButton = (Button) this.buttons.get(1);
		this.buttons.add(1, new Button(2, this.width / 2 - 100, cancelButton.y, creative_getButtonName()));
		cancelButton.y = this.height / 4 + 144 + 12;
	}
	
	@Inject(method = "buttonClicked", at = @At("TAIL"))
	protected void creative_buttonClicked(Button button, CallbackInfo info) {
		if (button.id == 2) {
			cheats = !cheats;
			button.text = creative_getButtonName();
		}
		else if (button.id == 0) {
			if (minecraft.player != null) {
				((local.luke.power.permissions.CheatWorld) minecraft.level.getProperties()).power$cheatsEnabled(cheats);
			}
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void creative_cycleBack(int x, int y, int mouseButton, CallbackInfo info) {
		if (mouseButton != 1) return;
		for (Object entry : this.buttons) {
			Button button = (Button) entry;
			if (button.id == 2 && button.isMouseOver(minecraft, x, y)) {
				// There are two modes, so either direction toggles to the other.
				this.buttonClicked(button);
				minecraft.soundHelper.playSound("random.click", 1.0F, 1.0F);
				info.cancel();
				return;
			}
		}
	}
	
	@Unique
	private String creative_getButtonName() {
		return "Cheats: " + (cheats ? "On" : "Off");
	}
}
