package local.luke.power.creative;

import local.luke.power.creative.config.Config;
import local.luke.power.input.Binding;
import local.luke.power.input.Bindings;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/** Defer only the plain F3/F4 mode-picker gesture; all other bindings stay immediate. */
public final class DebugKey {
  private static final DebugKeyGesture gesture = new DebugKeyGesture();
  private static Object world;

  private DebugKey() {}

  private static Binding defaultBinding(Minecraft mc) {
    if (Keys.MODIFIER.key != Keyboard.KEY_F3
        || Keys.PICKER.key != Keyboard.KEY_F4
        || Bindings.modifiers("power_creative.modifier") != 0
        || Bindings.modifiers("power_creative.picker") != 0
        || Bindings.modifiers("key.power_controls.debug_hud") != 0) return null;
    for (Object key : mc.options.keyBindings)
      if (key instanceof Binding b && b.power$id().equals("key.power_controls.debug_hud"))
        return b.power$code() == Keyboard.KEY_F3 ? b : null;
    return null;
  }

  public static void tick(Minecraft mc) {
    if (world != mc.level) {
      world = mc.level;
      cancel();
    }
    if (gesture.pending() && (!ClientRuntime.active(mc) || defaultBinding(mc) == null
        || Bindings.eventCode(Keys.MODIFIER) != Keyboard.KEY_F3
        || Bindings.eventCode(Keys.PICKER) != Keyboard.KEY_F4)) cancel();
  }

  public static void event(Minecraft mc, int key, boolean down, boolean repeat) {
    if (local.luke.power.input.DebugInput.managesOverlay()) { gesture.cancel(); return; }
    tick(mc);
    if (key == Keyboard.KEY_F4 && down && !repeat) gesture.cycle();
    if (key != Keyboard.KEY_F3) return;
    if (!down) {
      if (gesture.release()) mc.options.debugHud = !mc.options.debugHud;
      return;
    }
    if (repeat || !ClientRuntime.active(mc)) return;
    Binding debug = defaultBinding(mc);
    if (debug != null && Bindings.matches(debug, key)
        && Bindings.eventCode(Keys.MODIFIER) == Keyboard.KEY_F3
        && Bindings.eventCode(Keys.PICKER) == Keyboard.KEY_F4)
      gesture.press(Keyboard.isKeyDown(Keyboard.KEY_F4));
  }

  public static boolean deferred() { return gesture.pending(); }
  public static void cancel() { gesture.cancel(); }
}
