package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.rules.*;
import local.luke.power.world.*;
import net.minecraft.class_180;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_180.class)
public abstract class CreateWorldMixin extends Screen {
  @Shadow private TextFieldWidget field_632, field_633;
  @Shadow private void method_608() { throw new AssertionError(); }
  @Unique private int power$difficulty = WorldDifficulty.DEFAULT;
  @Unique private CreationRules power$draft;
  @Unique private String power$name, power$seed;
  @Unique private static final String[] POWER_DIFFICULTIES = {"Peaceful", "Easy", "Normal", "Hard"};

  @Inject(method = "init", at = @At("HEAD"))
  private void power$rememberFields(CallbackInfo ci) {
    if (field_632 != null) { power$name = field_632.getText(); power$seed = field_633.getText(); }
  }
  @Inject(method = "init", at = @At("TAIL"))
  private void power$choices(CallbackInfo ci) {
    if (power$draft == null) power$draft = new CreationRules(RulePresets.survival(), RulePresets.cheats());
    if (power$name != null) { field_632.setText(power$name); field_633.setText(power$seed); method_608(); }
    int left = width / 2 - 132;
    for (int i = 0; i < buttons.size(); i++) {
      ButtonWidget button = (ButtonWidget) buttons.get(i);
      if (button.id == 0 || button.id == 1)
        buttons.set(i, new ButtonWidget(button.id, left + button.id * 134, height - 28, 130, 20, button.text));
    }
    buttons.add(new ButtonWidget(2, left, height - 80, 130, 20, power$rulesLabel()));
    buttons.add(new ButtonWidget(3, left + 134, height - 80, 130, 20, power$difficultyLabel()));
    if (power$draft.preset() == CreationRules.Preset.CUSTOM) {
      buttons.add(new ButtonWidget(4, left, height - 56, 130, 20, "Edit game rules"));
      buttons.add(new ButtonWidget(5, left + 134, height - 56, 130, 20, power$modeLabel()));
    }
  }
  @Unique private String power$rulesLabel() {
    return "Game rules: " + switch (power$draft.preset()) { case SURVIVAL -> "Survival"; case CHEATS -> "Cheats"; case CUSTOM -> "Custom"; };
  }
  @Unique private String power$difficultyLabel() { return "Difficulty: " + POWER_DIFFICULTIES[power$difficulty]; }
  @Unique private String power$modeLabel() { String mode = power$draft.mode(); return "Start: " + Character.toUpperCase(mode.charAt(0)) + mode.substring(1); }
  @Unique private void power$cycle(int id, int direction) {
    if (id == 2) power$draft.cycle(direction);
    if (id == 3) power$difficulty = Math.floorMod(power$difficulty + direction, 4);
    if (id == 5) power$draft.cycleMode(direction);
    init();
  }
  @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
  private void power$choose(ButtonWidget button, CallbackInfo ci) {
    if (!button.active) return;
    if (button.id == 0 && !power$draft.problem().isEmpty()) { ci.cancel(); return; }
    if (button.id == 2 || button.id == 3 || button.id == 5) { power$cycle(button.id, 1); ci.cancel(); }
    if (button.id == 4) {
      local.luke.power.ui.PowerOptionsScreen.openCreationRules(minecraft, this, power$draft); ci.cancel();
    }
  }
  @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void power$reverse(int x, int y, int mouseButton, CallbackInfo ci) {
    if (mouseButton != 1) return;
    for (Object entry : buttons) {
      ButtonWidget button = (ButtonWidget) entry;
      if ((button.id == 2 || button.id == 3 || button.id == 5) && button.isMouseOver(minecraft, x, y)) {
        power$cycle(button.id, -1); minecraft.soundManager.method_2009("random.click", 1, 1); ci.cancel(); return;
      }
    }
  }
  @Inject(method = "render", at = @At("HEAD"))
  private void power$validDraft(int x, int y, float delta, CallbackInfo ci) {
    for (Object entry : buttons) {
      ButtonWidget button = (ButtonWidget) entry;
      if (button.id == 0) button.active = !field_632.getText().isBlank() && power$draft.problem().isEmpty();
    }
  }
  @Inject(method = "render", at = @At("TAIL"))
  private void power$problem(int x, int y, float delta, CallbackInfo ci) {
    if (!power$draft.problem().isEmpty()) drawCenteredTextWithShadow(textRenderer, power$draft.problem(), width / 2, height - 91, 0xff8888);
  }
  @WrapOperation(method = "buttonClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;method_2120(Ljava/lang/String;Ljava/lang/String;J)V"))
  private void power$create(Minecraft mc, String folder, String name, long seed, Operation<Void> original) {
    WorldCreation.withRules(power$difficulty, power$draft.rules(), () -> original.call(mc, folder, name, seed));
    if (mc.world != null && mc.player != null && !mc.world.isRemote) {
      try {
        Class<?> modes = Class.forName("local.luke.power.creative.Modes"), type = Class.forName("local.luke.power.creative.api.GameMode");
        Object mode = java.util.Arrays.stream(type.getEnumConstants()).filter(v -> ((Enum<?>) v).name().equalsIgnoreCase(power$draft.mode())).findFirst().orElseThrow();
        modes.getMethod("change", Minecraft.class, type, boolean.class).invoke(null, mc, mode, false);
      } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not set the starting game mode", e); }
    }
  }
  @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_180;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"), index = 3)
  private int power$title(int y) { return 20; }
}
