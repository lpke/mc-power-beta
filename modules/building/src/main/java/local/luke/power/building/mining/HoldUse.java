package local.luke.power.building.mining;

import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.opengl.Display;

/** Temporary native use input. Menus and detached views immediately release it. */
public final class HoldUse {
  public static final KeyBinding KEY = new KeyBinding("Hold use (toggle)", 0);
  private static boolean active;
  private static Minecraft client;
  private static Object world, player;
  private HoldUse() {}
  public static void tick(Minecraft mc) {
    if (client != mc) {
      client = mc; UseInput.register(HoldUse::active);
      Bindings.onKeyPress("building.holdUse",HoldUse::press);
      Bindings.onMousePress("building.holdUse",HoldUse::press);
    }
    if (world != mc.world || player != mc.player) { stop(); world = mc.world; player = mc.player; }
    if(!eligible())stop();
  }
  private static boolean eligible() {
    return client != null && client.world != null && !client.world.isRemote && client.player != null
        && client.world == world && client.player == player && client.player.health > 0 && !client.player.dead
        && client.currentScreen == null && client.field_2778 && Display.isActive()
        && !InteractionState.carryingContainer && !MovementOwnership.cameraControlsMovement()
        && !DetachedCamera.isActive()
        && (!(client.player instanceof PlayerMode mode) || !mode.power$spectator());
  }
  private static void press(int key) {
    if(!eligible()||!Bindings.matches(KEY,key))return;
    if(active)stop();
    else {active=true;UseInput.press();}
  }
  public static boolean active() { if (!eligible()) stop(); return active; }
  public static void stop() {
    boolean wasActive=active;active=false;UseInput.cancelPress();
    if(wasActive&&(!org.lwjgl.input.Mouse.isCreated()||!org.lwjgl.input.Mouse.isButtonDown(1)))local.luke.power.fastplace.FastPlace.release();
  }
}
