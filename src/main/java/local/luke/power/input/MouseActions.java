package local.luke.power.input;

import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Option;

/** Mouse bindings use the same press edge and modifier rules as keyboard bindings. */
public final class MouseActions {
  private MouseActions() {}
  public static void press(Minecraft mc, int code) {
    if (Bindings.matches(mc.options.inventoryKey, code)) mc.setScreen(new class_585(mc.player));
    else if (Bindings.matches(mc.options.chatKey, code)) mc.setScreen(new ChatScreen());
    else if (Bindings.matches(mc.options.dropKey, code)) mc.player.dropSelectedItem();
    else if (Bindings.matches(mc.options.fogKey, code))
      mc.options.setInt(Option.RENDER_DISTANCE, (Bindings.heldModifiers() & Chord.SHIFT) != 0 ? -1 : 1);
    for (KeyBinding key : mc.options.allKeys) {
      if (!Bindings.matches(key, code)) continue;
      switch (key.translationKey) {
        case "key.power_controls.hide_hud" -> mc.options.hideHud = !mc.options.hideHud;
        case "key.power_controls.debug_hud" -> mc.options.debugHud = !mc.options.debugHud;
        case "key.power_controls.third_person" -> mc.options.thirdPerson = !mc.options.thirdPerson;
        case "key.power_controls.cinematic_camera" -> mc.options.cinematicMode = !mc.options.cinematicMode;
        case "key.power_controls.toggle_fullscreen" -> mc.toggleFullscreen();
        default -> {}
      }
    }
    if (mc.currentScreen == null) Bindings.mousePress(code);
  }
}
