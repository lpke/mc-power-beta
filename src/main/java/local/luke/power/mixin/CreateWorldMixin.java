package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.world.*;
import net.minecraft.class_180;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Run the init callback after the creative module has added the cheats choice.
@Mixin(value = class_180.class, priority = 1200)
public abstract class CreateWorldMixin extends Screen {
  @Unique private int power$difficulty = WorldDifficulty.DEFAULT;
  @Unique private static final String[] POWER_DIFFICULTIES = {"Peaceful", "Easy", "Normal", "Hard"};

  @Inject(method = "init", at = @At("TAIL"))
  private void power$choices(CallbackInfo ci) {
    for (int i = 0; i < buttons.size(); i++) {
      ButtonWidget cheats = (ButtonWidget) buttons.get(i);
      if (cheats.id != 2) continue;
      buttons.set(i, new ButtonWidget(2, width / 2 - 100, cheats.y, 88, 20, cheats.text));
      buttons.add(new ButtonWidget(3, width / 2 - 8, cheats.y, 108, 20, power$difficultyLabel()));
      break;
    }
  }

  @Unique private String power$difficultyLabel() { return "Difficulty: " + POWER_DIFFICULTIES[power$difficulty]; }

  @Inject(method = "buttonClicked", at = @At("HEAD"))
  private void power$choose(ButtonWidget button, CallbackInfo ci) {
    if (button.id == 3 && button.active) {
      power$difficulty = (power$difficulty + 1) % 4;
      button.text = power$difficultyLabel();
    }
  }

  @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void power$reverse(int x, int y, int mouseButton, CallbackInfo ci) {
    if (mouseButton != 1) return;
    for (Object entry : buttons) {
      ButtonWidget button = (ButtonWidget) entry;
      if (button.id == 3 && button.isMouseOver(minecraft, x, y)) {
        power$difficulty = (power$difficulty + 3) % 4;
        button.text = power$difficultyLabel();
        minecraft.soundManager.method_2009("random.click", 1, 1);
        ci.cancel();
        return;
      }
    }
  }

  @WrapOperation(method = "buttonClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;method_2120(Ljava/lang/String;Ljava/lang/String;J)V"))
  private void power$create(Minecraft mc, String folder, String name, long seed, Operation<Void> original) {
    WorldCreation.withDifficulty(power$difficulty, () -> original.call(mc, folder, name, seed));
  }

  @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_180;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"), index = 3)
  private int power$title(int y) { return 20; }
}
