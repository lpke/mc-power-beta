package local.luke.autowalk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public final class AutoWalk {
  public static final KeyBinding KEY = new KeyBinding("Auto-walk (toggle)", Keyboard.KEY_R);
  private static final WalkToggle TOGGLE = new WalkToggle();
  private static Minecraft client;
  private static Object world;
  private static Object player;

  private AutoWalk() {}

  public static void tick(Minecraft minecraft) {
    client = minecraft;
    if (world != minecraft.world || player != minecraft.player) {
      TOGGLE.stop();
      world = minecraft.world;
      player = minecraft.player;
    }
    TOGGLE.update(local.luke.power.input.Bindings.down(KEY), canWalk());
  }

  private static boolean isKeyDown(int code) {
    if (code < 0) {
      int button = code + 100;
      return Mouse.isCreated()
          && button >= 0
          && button < Mouse.getButtonCount()
          && Mouse.isButtonDown(button);
    }
    return Keyboard.isCreated()
        && code > Keyboard.KEY_NONE
        && code < Keyboard.KEYBOARD_SIZE
        && Keyboard.isKeyDown(code);
  }

  private static boolean canWalk() {
    return local.luke.tweaks.config.Config.current().autoWalk
        && client != null
        && client.world != null
        && client.player != null
        && client.currentScreen == null
        && Display.isActive();
  }

  public static boolean isWalking() {
    if (!canWalk()) {
      TOGGLE.stop();
    }
    return TOGGLE.isActive();
  }

  public static void stop() {
    TOGGLE.stop();
  }
}
