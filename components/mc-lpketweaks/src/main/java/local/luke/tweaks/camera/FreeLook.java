package local.luke.tweaks.camera;

import local.luke.flexible.Compatibility;
import local.luke.flexible.Input;
import local.luke.tweaks.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Omnilook's input/camera separation, owned by LpkeTweaks. Never rotates the player. */
public final class FreeLook {
  // Keep the old options.txt identifier so existing key assignments survive migration.
  public static final KeyBinding KEY = new KeyBinding("key.omnilook.toggle", Keyboard.KEY_GRAVE);
  private static final LookAngles angles = new LookAngles();
  private static boolean active, wasDown, waitForRelease, previousThird, wasPerspectiveDown;
  private static Perspective perspective, configuredPerspective;
  private static boolean followedThird;
  private static ClientPlayerEntity owner;

  private FreeLook() {}

  public static boolean active() {
    return active;
  }

  public static boolean rendering() {
    return active;
  }

  public static float yaw(float value) {
    return rendering() ? angles.yaw(value) : value;
  }

  public static float pitch(float value) {
    return rendering() ? angles.pitch(value) : value;
  }

  public static void update(Minecraft mc) {
    boolean down = Input.down(KEY.code);
    boolean pressed = down && !wasDown;
    wasDown = down;
    boolean perspectiveDown = Input.down(Keyboard.KEY_F5);
    boolean perspectivePressed = perspectiveDown && !wasPerspectiveDown;
    wasPerspectiveDown = perspectiveDown;
    boolean eligible =
        Config.current().freeLook
            && mc.world != null
            && mc.player != null
            && !mc.player.dead
            && mc.player.health > 0
            && mc.player.world == mc.world
            && Float.isFinite(mc.player.yaw)
            && Float.isFinite(mc.player.pitch)
            && !mc.player.method_943()
            && mc.field_2807 == mc.player
            && mc.currentScreen == null
            && Display.isActive()
            && !Compatibility.freecam();
    if (!eligible || owner != null && owner != mc.player) {
      reset(mc);
      waitForRelease = down;
      return;
    }
    if (active
        && (perspectivePressed
            || configuredPerspective != Config.current().freeLookPerspective
            || followedThird != Config.current().freeLookFollowThirdPerson
            || mc.options.thirdPerson != (perspective == Perspective.THIRD_PERSON))) {
      // F5 is an explicit perspective choice; do not restore over it or reactivate while held.
      active = false;
      angles.clear();
      owner = null;
      waitForRelease = down;
      return;
    }
    if (!down) waitForRelease = false;
    if (waitForRelease) return;
    boolean wanted = Config.current().freeLookToggle ? (pressed ? !active : active) : down;
    if (wanted && !active) begin(mc);
    else if (!wanted && active) end(mc);
  }

  public static void begin(Minecraft mc) {
    if (active) return;
    angles.clear();
    owner = mc.player;
    previousThird = mc.options.thirdPerson;
    configuredPerspective = Config.current().freeLookPerspective;
    followedThird = Config.current().freeLookFollowThirdPerson;
    perspective =
        configuredPerspective == Perspective.FIRST_PERSON && followedThird && previousThird
            ? Perspective.THIRD_PERSON
            : configuredPerspective;
    mc.options.thirdPerson = perspective == Perspective.THIRD_PERSON;
    active = true;
  }

  private static void end(Minecraft mc) {
    active = false;
    mc.options.thirdPerson = previousThird;
    angles.clear();
    owner = null;
  }

  public static void reset(Minecraft mc) {
    if (active) mc.options.thirdPerson = previousThird;
    active = false;
    owner = null;
    angles.clear();
    waitForRelease = Input.down(KEY.code);
  }

  public static boolean turn(ClientPlayerEntity player, float dx, float dy) {
    if (!active) return true;
    angles.rotate(player.pitch, dx, dy);
    return false;
  }
}
