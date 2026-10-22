package local.luke.power.building.hotbar;

import local.luke.power.flexible.Compatibility;
import local.luke.power.flexible.Input;
import local.luke.power.building.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public final class Hotbars {
  public static final KeyBinding BASE = new KeyBinding("power_building.hotbar.base", 0);
  public static final KeyBinding SCROLL = new KeyBinding("power_building.hotbar.scroll", 0);
  public static final KeyBinding[] ROWS = {
    new KeyBinding("power_building.hotbar.row1", 0),
    new KeyBinding("power_building.hotbar.row2", 0),
    new KeyBinding("power_building.hotbar.row3", 0)
  };
  public static final KeyBinding[] ALL = {BASE, SCROLL, ROWS[0], ROWS[1], ROWS[2]};
  private static final boolean[] held = new boolean[3];
  private static boolean scrolling, scrollDown, blocked;
  private static int row;
  private static ClientPlayerEntity owner;
  private static long debugGeneration;

  private Hotbars() {}

  public static boolean eligible(Minecraft mc) {
    return mc != null
        && mc.world != null
        && !mc.world.isRemote
        && ((local.luke.power.input.WorldIdentity)mc.world.method_262()).power$inventoryReady()
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
    boolean down = local.luke.power.input.Bindings.down(SCROLL);
    int rowMask = 0;
    for (int i = 0; i < 3; i++) if (rowDown(i)) rowMask |= 1 << i;
    debugBoundary();
    // Debug may claim a fresh physical press before its queued key event. Suppression
    // must cancel a preview instead of committing it as an ordinary key release.
    if (debugClaimed()) cancelPending();
    HotbarTransaction.tick(mc);
    boolean eligible = eligible(mc);
    if (!eligible || owner != null && owner != mc.player) {
      scrolling = false;
      owner = null;
      blocked = down;
      for (int i = 0; i < 3; i++) held[i] = (rowMask & 1 << i) != 0;
      scrollDown = down;
      return;
    }
    if (!down && !scrollDown) blocked = false;
    HotbarSettings settings = Config.current().hotbar;
    if (down && !scrollDown && !blocked) {
      scrolling = true;
      owner = mc.player;
      row = settings.selectedRow;
    }
    if (scrolling && down) HotbarTransaction.preview(mc, row);
    boolean consumedScroll = scrolling;
    if (scrolling && !down) {
      if (!blocked) {
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

    scrollDown = down;
    boolean performed = false;
    for (int i = 0; i < 3; i++) {
      boolean pressed = (rowMask & 1 << i) != 0;
      if (!consumedScroll && !down && !performed && pressed && !held[i]) {
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
            && local.luke.power.input.DebugInput.allows(BASE.translationKey, Keyboard.KEY_1 + i, Input.down(Keyboard.KEY_1 + i))
            && Input.down(Keyboard.KEY_1 + i);
  }

  private static boolean debugClaimed() {
    for (KeyBinding binding : ALL)
      if (binding.code != 0 && local.luke.power.input.DebugInput.claimed(binding.code)) return true;
    if (Config.current().hotbar.numberRowKeys && Input.down(BASE.code))
      for (int i = 0; i < 3; i++) if (local.luke.power.input.DebugInput.claimed(Keyboard.KEY_1 + i)) return true;
    return false;
  }

  private static void debugBoundary() {
    long generation = local.luke.power.input.DebugInput.entryGeneration();
    if (debugGeneration == generation) return;
    debugGeneration = generation;
    // A held row preview is a gesture, unlike movement held before Debug.
    if (scrolling) cancelPending();
    else if (Input.down(SCROLL.code)) scrollDown = blocked = true;
  }

  public static void swap(Minecraft mc, int row) {
    if (!eligible(mc)) return;
    HotbarTransaction.run(mc, row);
  }

  public static void cancelPending() {
    HotbarTransaction.cancel();
    scrolling = false;
    owner = null;
    scrollDown = blocked = Input.down(SCROLL.code);
    for (int i = 0; i < 3; i++) held[i] = Input.down(ROWS[i].code)
        || Config.current().hotbar.numberRowKeys && Input.down(BASE.code) && Input.down(Keyboard.KEY_1 + i);
  }

  public static int wheel(Minecraft mc, int delta) {
    boolean down = local.luke.power.input.Bindings.down(SCROLL);
    debugBoundary();
    if (delta == 0
        || !eligible(mc)
        || !down
        || blocked) return delta;
    if (!scrolling) {
      scrolling = true;
      owner = mc.player;
      row = Config.current().hotbar.selectedRow;
    }
    scrollDown = true;
    row = InventoryRows.scroll(row, delta, Config.current().hotbar.reverseScroll);
    HotbarTransaction.preview(mc, row);
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
    if (!eligible(mc)) return false;
    int i = boundRow(code);
    if (i < 0) return false;
    if (down && !held[i] && !scrolling && !local.luke.power.input.Bindings.down(SCROLL)) swap(mc, i);
    held[i] = down;
    return true;
  }

  public static boolean consumesKey(Minecraft mc, int code) {
    return eligible(mc) && boundRow(code) >= 0;
  }

  public static int key(Minecraft mc, int code) {
    debugBoundary();
    return event(mc, code, Keyboard.getEventKeyState()) ? 0 : code;
  }

  public static int mouse(Minecraft mc, int button) {
    debugBoundary();
    if (button < 0 || !eligible(mc)) return button;
    boolean row = event(mc, button - 100, Mouse.getEventButtonState());
    return row || consumesMouse(mc, button) ? -1 : button;
  }

  public static boolean consumesMouse(Minecraft mc, int button) {
    if (button < 0 || !eligible(mc)) return false;
    int code = button - 100;
    return (code == local.luke.power.input.Bindings.eventCode(BASE) || boundRow(code) >= 0)
        || code == local.luke.power.input.Bindings.eventCode(SCROLL);
  }

  public static boolean showSwap(Minecraft mc) {
    return eligible(mc)
        && Config.current().hotbar.overlay
        && local.luke.power.input.Bindings.down(BASE);
  }

  public static boolean showScroll(Minecraft mc) {
    return eligible(mc) && Config.current().hotbar.overlay && scrolling && local.luke.power.input.Bindings.down(SCROLL);
  }

  public static int row() {
    return row;
  }
}
