package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import local.luke.power.input.InventoryPanels;
import local.luke.power.input.InventoryTextInput;
import local.luke.power.ui.TextInput;
import local.luke.power.validation.mixin.ScreenInput;
import net.glasslauncher.mods.gcapi3.impl.screen.widget.ExtensibleTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.opengl.Display;

/** Actual native/config widgets and recipe pointer routes use the same live preference. */
final class TextInputs444Checks {
  private TextInputs444Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.player != null && mc.world != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(mc.player instanceof local.luke.power.input.PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "use Survival for the native recipe-search fixture");
    Object config = Class.forName("local.luke.power.controls.ControlFeatures").getField("USER_INTERFACE_CONFIG").get(null);
    var preference = config.getClass().getField("clearTextFieldWithRightClick"); Object previous = preference.get(config);
    failures = 0;
    try (var state = new Recipe402Checks.State(mc)) {
      test("TEXT444 native widget obeys live preference, bounds, enablement and maximum length", () -> {
        state.reset(640);
        var input = new TextFieldWidget(mc.currentScreen, mc.textRenderer, 10, 10, 100, 18, "Native");
        input.setMaxLength(8); input.setFocused(true);
        preference.set(config, false); input.mouseClicked(14, 14, 1);
        check(input.getText().equals("Native"), "native disabled preference cleared text");
        preference.set(config, true); input.mouseClicked(114, 14, 1);
        check(input.getText().equals("Native"), "native outside click cleared text");
        input.enabled = false; input.mouseClicked(14, 14, 1);
        check(input.getText().equals("Native"), "native disabled widget cleared text");
        input.enabled = true; input.mouseClicked(14, 14, 1);
        check(input.getText().isEmpty(), "native enabled preference did not clear");
        input.setFocused(true);
        for (char c : "abcdefghijk".toCharArray()) input.keyPressed(c, 0);
        check(input.getText().equals("abcdefgh"), "native maximum length changed after clearing");
      });
      test("TEXT444 gcapi widget keeps validation, disabled ownership and change callback", () -> {
        var input = new ExtensibleTextFieldWidget(mc.textRenderer); input.setXYWH(10, 10, 100, 18);
        input.setMaxLength(8); input.setText("Config");
        int[] updates = {0}; input.setTextUpdatedListener(() -> updates[0]++);
        preference.set(config, false); input.mouseClicked(14, 14, 1);
        check(input.getText().equals("Config") && updates[0] == 0, "gcapi Off altered text or callback");
        preference.set(config, true); input.setEnabled(false); input.mouseClicked(14, 14, 1);
        check(input.getText().equals("Config") && updates[0] == 0, "disabled gcapi field accepted right-click");
        input.setEnabled(true); input.mouseClicked(14, 14, 1);
        check(input.isSelected() && input.getText().isEmpty() && updates[0] == 1, "gcapi clear lost focus or callback");
        check(input.getCursorMax() == 0 && input.getCursorMin() == 0 && input.getMaxLength() == 8, "gcapi clear changed limits or retained selection");
      });
      for (int width : new int[]{640, 320}) test("TEXT444 recipe search owns right-click and respects modal input at width " + width, () -> {
        state.reset(width); var book = state.book(); var screen = mc.currentScreen;
        var input = (TextInput)field(book, "search"); input.setText("diamond"); input.selectAll(); book.tick();
        var bounds = book.layout().search(); int x = bounds.x() + 4, y = bounds.y() + 4;
        String inventory = state.snapshot();
        preference.set(config, false); ((ScreenInput)screen).power$click(x, y, 1);
        check(input.text().equals("diamond") && InventoryTextInput.active(screen), "recipe Off cleared text or lost pointer ownership");
        preference.set(config, true); Object modal = new Object(); InventoryPanels.modal(screen, modal, true);
        try { check(!book.mouse(x, y, 1) && input.text().equals("diamond"), "recipe accepted input through another modal"); }
        finally { InventoryPanels.modal(screen, modal, false); }
        ((ScreenInput)screen).power$click(x, y, 1);
        check(input.text().isEmpty() && input.focused && InventoryTextInput.active(screen), "recipe right-click did not clear and retain typing ownership");
        check(state.snapshot().equals(inventory), "search right-click changed native inventory");
      });
    } finally { preference.set(config, previous); }
    log("TEXT444 failures=" + failures); check(failures == 0, "text-inputs-444 failures=" + failures);
  }
}
