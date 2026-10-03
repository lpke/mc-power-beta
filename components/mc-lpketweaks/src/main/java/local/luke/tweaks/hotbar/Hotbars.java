package local.luke.tweaks.hotbar;

import local.luke.flexible.Compatibility;
import local.luke.flexible.Input;
import local.luke.tweaks.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public final class Hotbars {
  public static final KeyBinding BASE = new KeyBinding("lpketweaks.hotbar.base", 0);
  public static final KeyBinding SCROLL = new KeyBinding("lpketweaks.hotbar.scroll", 0);
  public static final KeyBinding[] ROWS = {
    new KeyBinding("lpketweaks.hotbar.row1", 0),
    new KeyBinding("lpketweaks.hotbar.row2", 0),
    new KeyBinding("lpketweaks.hotbar.row3", 0)
  };
  public static final KeyBinding[] ALL = {BASE, SCROLL, ROWS[0], ROWS[1], ROWS[2]};
  private static final boolean[] held = new boolean[3];
  private static boolean scrolling, scrollDown, blocked;
  private static int row;
  private static ClientPlayerEntity owner;

  private Hotbars() {}

  public static boolean eligible(Minecraft mc) {
    return mc != null
        && mc.world != null
        && !mc.world.isRemote
        && mc.player != null
        && mc.player.world == mc.world
        && mc.player.health > 0
        && !mc.player.dead
        && !mc.player.field_1642
        && mc.field_2807 == mc.player
        && mc.currentScreen == null
        && mc.field_2778
        && Display.isActive()
        && !Compatibility.freecam()
        && mc.player.container == mc.player.playerContainer
        && mc.player.inventory.getCursorStack() == null;
  }

  public static void tick(Minecraft mc) {
    HotbarTransaction.tick();
    boolean down = local.luke.power.input.Bindings.down(SCROLL);
    boolean eligible = eligible(mc);
    if (!eligible || owner != null && owner != mc.player) {
      scrolling = false;
      owner = null;
      blocked = down;
      for (int i = 0; i < 3; i++) held[i] = rowDown(i);
      scrollDown = down;
      return;
    }
    if (!down && !scrollDown) blocked = false;
    HotbarSettings settings = Config.current().hotbar;
    if (settings.scroll && down && !scrollDown && !blocked) {
      scrolling = true;
      owner = mc.player;
      row = settings.selectedRow;
    }
    boolean consumedScroll = scrolling;
    if (scrolling && !down) {
      if (settings.scroll && !blocked) {
        swap(mc, row);
        if (settings.rememberRow && settings.selectedRow != row)
          try {
            Config.update(s -> s.hotbar.selectedRow = row);
          } catch (java.io.IOException e) {
            Config.LOG.warn("Could not save selected hotbar row", e);
          }
      }
      scrolling = false;
      owner = null;
    }
    if (!settings.scroll) {
      scrolling = false;
      owner = null;
      blocked = down;
    }
    scrollDown = down;
    boolean performed = false;
    for (int i = 0; i < 3; i++) {
      boolean pressed = rowDown(i);
      if (settings.swap && !consumedScroll && !down && !performed && pressed && !held[i]) {
        swap(mc, i);
        performed = true;
      }
      held[i] = pressed;
    }
  }

  private static boolean rowDown(int i) {
    return local.luke.power.input.Bindings.down(ROWS[i])
        || Config.current().hotbar.numberRowKeys
            && local.luke.power.input.Bindings.down(BASE)
            && Input.down(Keyboard.KEY_1 + i);
  }

  public static void swap(Minecraft mc, int row) {
    if (!eligible(mc)) return;
    HotbarTransaction.run(mc, row);
  }

  public static int wheel(Minecraft mc, int delta) {
    if (delta == 0
        || !eligible(mc)
        || !Config.current().hotbar.scroll
        || !local.luke.power.input.Bindings.down(SCROLL)
        || blocked) return delta;
    if (!scrolling) {
      scrolling = true;
      owner = mc.player;
      row = Config.current().hotbar.selectedRow;
    }
    scrollDown = true;
    row = InventoryRows.scroll(row, delta, Config.current().hotbar.reverseScroll);
    return 0;
  }

  private static int boundRow(int code) {
    for (int i = 0; i < 3; i++)
      if (code != 0
          && (code == local.luke.power.input.Bindings.eventCode(ROWS[i])
              || Config.current().hotbar.numberRowKeys
                  && local.luke.power.input.Bindings.down(BASE)
                  && code == Keyboard.KEY_1 + i)) return i;
    return -1;
  }

  private static boolean event(Minecraft mc, int code, boolean down) {
    if (!eligible(mc) || !Config.current().hotbar.swap) return false;
    int i = boundRow(code);
    if (i < 0) return false;
    if (down && !held[i] && !scrolling && !local.luke.power.input.Bindings.down(SCROLL)) swap(mc, i);
    held[i] = down;
    return true;
  }

  public static boolean consumesKey(Minecraft mc, int code) {
    return eligible(mc) && Config.current().hotbar.swap && boundRow(code) >= 0;
  }

  public static int key(Minecraft mc, int code) {
    return event(mc, code, Keyboard.getEventKeyState()) ? 0 : code;
  }

  public static int mouse(Minecraft mc, int button) {
    if (button < 0 || !eligible(mc)) return button;
    boolean row = event(mc, button - 100, Mouse.getEventButtonState());
    return row || consumesMouse(mc, button) ? -1 : button;
  }

  public static boolean consumesMouse(Minecraft mc, int button) {
    if (button < 0 || !eligible(mc)) return false;
    int code = button - 100;
    var settings = Config.current().hotbar;
    return settings.swap && (code == local.luke.power.input.Bindings.eventCode(BASE) || boundRow(code) >= 0)
        || settings.scroll && code == local.luke.power.input.Bindings.eventCode(SCROLL);
  }

  public static boolean showSwap(Minecraft mc) {
    return eligible(mc)
        && Config.current().hotbar.overlay
        && Config.current().hotbar.swap
        && local.luke.power.input.Bindings.down(BASE);
  }

  public static boolean showScroll(Minecraft mc) {
    return eligible(mc) && Config.current().hotbar.overlay && scrolling && local.luke.power.input.Bindings.down(SCROLL);
  }

  public static int row() {
    return row;
  }
}
