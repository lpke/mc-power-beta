package local.luke.power.building.camera;

import local.luke.power.flexible.Compatibility;
import local.luke.power.flexible.Input;
import local.luke.power.building.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Free look's input/camera separation, owned by BuildingFeatures. Never rotates the player. */
public final class FreeLook {
  // Stable identifier shared with Power Beta's binding registry.
  public static final KeyBinding KEY = new KeyBinding("key.powerbeta.free_look", Keyboard.KEY_NONE);
  private static final LookAngles angles = new LookAngles();
  private static boolean active, wasDown, waitForRelease, previousThird;
  private static Perspective perspective, configuredPerspective;
  private static boolean followedThird;
  private static ClientPlayerEntity owner;
  private static boolean detachedOwner;
  static { local.luke.power.input.CameraView.look(value -> yaw((float)value), value -> pitch((float)value)); }

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
    boolean down = local.luke.power.input.Bindings.down(KEY);
    boolean pressed = down && !wasDown;
    wasDown = down;
    boolean eligible =
        mc.world != null
            && mc.player != null
            && !mc.player.dead
            && mc.player.health > 0
            && mc.player.world == mc.world
            && Float.isFinite(mc.player.yaw)
            && Float.isFinite(mc.player.pitch)
            && !mc.player.method_943()
            && mc.field_2807 == mc.player
            && mc.currentScreen == null
            && Display.isActive();
    if (!eligible || owner != null && (owner != mc.player || detachedOwner != Compatibility.freecam())) {
      reset(mc);
      waitForRelease = down;
      return;
    }
    if (active
        && (configuredPerspective != Config.current().freeLookPerspective
            || followedThird != Config.current().freeLookFollowThirdPerson)) {
      active = false;
      angles.clear();
      owner = null;
      waitForRelease = down;
      return;
    }
    if (active && mc.options.thirdPerson != (perspective == Perspective.THIRD_PERSON)) {
      // Keep the same render angles, and honour the explicit view choice on release.
      perspective = mc.options.thirdPerson ? Perspective.THIRD_PERSON : Perspective.FIRST_PERSON;
      previousThird = mc.options.thirdPerson;
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
    detachedOwner = Compatibility.freecam();
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
    waitForRelease = local.luke.power.input.Bindings.down(KEY);
  }

  public static boolean turn(ClientPlayerEntity player, float dx, float dy) {
    if (!active) return true;
    angles.rotate(detachedOwner ? (float)local.luke.power.input.DetachedCamera.pitch() : player.pitch, dx, dy);
    return false;
  }
}
