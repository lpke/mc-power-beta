package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import local.luke.power.input.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.subtitles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

/** Exercises installed native sound hooks while both user-facing audio volumes remain zero. */
public final class Subtitles400Checks {
  private Subtitles400Checks() {}

  public static void run(Minecraft mc, String phase) throws Exception {
    check(phase.equals("native"), "expected native");
    check(mc.world != null && mc.player != null && mc.field_2807 == mc.player, "load a disposable world with the player camera first");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute this disposable clone before subtitle checks");
    check(!DetachedCamera.isActive(), "exit freecam before subtitle checks");
    failures = 0;
    SubtitleSettings original = SubtitleConfig.copy();
    float yaw = mc.player.yaw, prevYaw = mc.player.prevYaw;
    double prevX = mc.player.prevX, prevY = mc.player.prevY, prevZ = mc.player.prevZ;
    boolean hud = mc.options.hideHud, started = SoundManagerAccessor.power$started();
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object cameraPose = camera.getClass().getMethod("getCameraPosition").invoke(camera);
    Object savedPose = cameraPose.getClass().getMethod("clone").invoke(cameraPose);
    try {
      var enabled = SubtitleConfig.copy(); enabled.enabled = true; enabled.displaySeconds = 3; SubtitleConfig.preview(enabled);
      test("muted registered native sounds caption without initializing audio or advancing sound RNG", () -> {
        reset(mc);
        Object pool = ((SoundManagerAccessor)mc.soundManager).power$sounds();
        byte[] before = randomState(pool, mc.soundManager);
        sound(mc, "step.stone", mc.player.x + 1, mc.player.y, mc.player.z, 1);
        check(Subtitles.pending() == 1, "muted native sound was not admitted");
        check(keys(mc).equals(Set.of("subtitles.block.generic.footsteps")), "native footsteps caption missing");
        check(Arrays.equals(before, randomState(pool, mc.soundManager)), "caption admission advanced native sound RNG");
        check(SoundManagerAccessor.power$started() == started, "caption admission initialized audio");
      });
      test("zero-volume, negative-volume, unknown and UI sounds have no captions", () -> {
        reset(mc);
        sound(mc, "step.stone", mc.player.x, mc.player.y, mc.player.z, 0);
        sound(mc, "step.stone", mc.player.x, mc.player.y, mc.player.z, -1);
        sound(mc, "power_beta:unregistered.subtitle.fixture", mc.player.x, mc.player.y, mc.player.z, 1);
        sound(mc, "random.click", mc.player.x, mc.player.y, mc.player.z, 1);
        mc.soundManager.method_2009("random.click", 1, 1);
        check(Subtitles.pending() == 0 && keys(mc).isEmpty(), "excluded native sound entered caption state");
      });
      test("native WorldRenderer forwarding admits a nearby sound exactly once", () -> {
        reset(mc);
        mc.worldRenderer.method_1154("step.stone", mc.player.x + 1, mc.player.y, mc.player.z, 1, 1);
        check(Subtitles.pending() == 1, "WorldRenderer and SoundManager both admitted the same sound");
        check(keys(mc).equals(Set.of("subtitles.block.generic.footsteps")), "forwarded cue missing");
      });
      test("native world-event context captions dispenser clicks while ordinary UI clicks remain silent", () -> {
        reset(mc);
        mc.worldRenderer.method_1152(mc.player, 1000, (int)Math.floor(mc.player.x), (int)Math.floor(mc.player.y), (int)Math.floor(mc.player.z), 0);
        check(keys(mc).equals(Set.of("subtitles.block.dispenser.dispense")), "world-event semantic caption not installed");
      });
      test("one-use semantic scope does not rename a second native sound", () -> {
        reset(mc);
        try (var scope = SoundCaptionContext.push("subtitles.block.generic.place")) {
          sound(mc, "step.stone", mc.player.x, mc.player.y, mc.player.z, 1);
          sound(mc, "step.stone", mc.player.x + 1, mc.player.y, mc.player.z, 1);
        }
        check(keys(mc).equals(Set.of("subtitles.block.generic.place", "subtitles.block.generic.footsteps")), "semantic scope leaked into another sound");
      });
      test("relative native portal cues are visible without direction arrows", () -> {
        reset(mc); mc.soundManager.method_2009("portal.travel", 1, 1);
        var visible = visible(mc);
        check(visible.size() == 1 && visible.get(0).key().equals("subtitles.block.portal.travel") && visible.get(0).arrow() == 0, "relative portal cue changed direction or identity");
      });
      test("native muted listener retains double interpolation and the native yaw basis", () -> {
        mc.player.prevX = mc.player.x - .125; mc.player.prevY = mc.player.y - .375; mc.player.prevZ = mc.player.z + .25;
        for (float angle : new float[]{0, 90, 180, 270}) {
          reset(mc); mc.player.yaw = angle; mc.player.prevYaw = angle - 10;
          mc.soundManager.method_2013(mc.player, .25F);
          var listener = listener();
          check(listener != null && Math.abs(listener.x() - (mc.player.prevX + (mc.player.x - mc.player.prevX) * .25)) < 1e-9
              && Math.abs(listener.y() - (mc.player.prevY + (mc.player.y - mc.player.prevY) * .25)) < 1e-9
              && Math.abs(listener.z() - (mc.player.prevZ + (mc.player.z - mc.player.prevZ) * .25)) < 1e-9, "native listener position lost precision");
          basis(listener, angle - 7.5F);
          arrows(listener);
        }
      });
      test("detached muted listener owns captions before the physical-player distance gate", () -> {
        reset(mc); FreecamPreviewChecks.active(true);
        try {
          for (float angle : new float[]{0, 90, 180, 270}) {
            double x = mc.player.x + 100.125, y = mc.player.y + .375, z = mc.player.z - .25;
            FreecamPreviewChecks.pose(x, y, z, 70, angle);
            Subtitles.clear(); Subtitles.tick(mc); mc.soundManager.method_2013(mc.player, .5F);
            var listener = listener();
            check(listener != null && listener.x() == x && listener.y() == y && listener.z() == z, "root listener replaced detached coordinates");
            basis(listener, angle - 180F); arrows(listener);
            mc.worldRenderer.method_1154("step.stone", x + 1, y, z, 1, 1);
            check(Subtitles.pending() == 1 && keys(mc).equals(Set.of("subtitles.block.generic.footsteps")), "physical-player range gate hid a nearby detached cue");
          }
        } finally { FreecamPreviewChecks.active(false); }
      });
      test("disabled and departed contexts cannot revive pending captions", () -> {
        reset(mc); sound(mc, "step.stone", mc.player.x, mc.player.y, mc.player.z, 1);
        var disabled = SubtitleConfig.copy(); disabled.enabled = false; SubtitleConfig.preview(disabled);
        sound(mc, "step.stone", mc.player.x, mc.player.y, mc.player.z, 1);
        check(Subtitles.pending() == 0 && state().positionCount() == 0, "disabled captions retained pending/history work");
        disabled.enabled = true; SubtitleConfig.preview(disabled); reset(mc);
        check(keys(mc).isEmpty(), "enabling revived an old caption");
        Subtitles.positional(new Object(), mc.soundManager, "step.stone", mc.player.x, mc.player.y, mc.player.z, 1);
        check(Subtitles.pending() == 0, "wrong-world cue was admitted");
        sound(mc, "step.stone", mc.player.x, mc.player.y, mc.player.z, 1); Subtitles.leave();
        Subtitles.tick(mc); mc.soundManager.method_2013(mc.player, 1);
        check(Subtitles.pending() == 0 && keys(mc).isEmpty(), "world departure retained a pending cue");
      });
      test("native sound bursts obey pending and position caps", () -> {
        reset(mc); long dropped = Subtitles.dropped();
        for (int i = 0; i < 2000; i++) sound(mc, "step.stone", mc.player.x + i / 1000D, mc.player.y, mc.player.z, 1);
        check(Subtitles.pending() <= 1024 && Subtitles.dropped() > dropped, "pending queue did not bound a burst");
        check(!keys(mc).isEmpty() && state().positionCount() <= SubtitleState.MAX_POSITIONS, "position queue lost its cap or newest nearby cue");
      });
      check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0 && SoundManagerAccessor.power$started() == started, "subtitle fixture changed audio output state");
    } finally {
      FreecamPreviewChecks.active(false);
      camera.getClass().getMethod("setCameraPositionAndRotation", double.class, double.class, double.class, float.class, float.class, float.class)
          .invoke(camera, number(savedPose, "x"), number(savedPose, "y"), number(savedPose, "z"),
              (float)number(savedPose, "pitch"), (float)number(savedPose, "yaw"), (float)number(savedPose, "roll"));
      mc.player.yaw = yaw; mc.player.prevYaw = prevYaw;
      mc.player.prevX = prevX; mc.player.prevY = prevY; mc.player.prevZ = prevZ; mc.options.hideHud = hud;
      SubtitleConfig.preview(original); Subtitles.clear(); Subtitles.tick(mc); mc.soundManager.method_2013(mc.player, 1);
    }
    log("SUBTITLES400 NATIVE failures=" + failures + " audioStarted=" + started);
  }

  private static void reset(Minecraft mc) {
    Subtitles.clear(); Subtitles.tick(mc); mc.soundManager.method_2013(mc.player, 1);
  }
  private static void sound(Minecraft mc, String id, double x, double y, double z, float volume) {
    mc.soundManager.method_2015(id, (float)x, (float)y, (float)z, volume, 1);
  }
  private static List<SubtitleState.Visible> visible(Minecraft mc) throws Exception {
    Subtitles.tick(mc);
    return state().visible(System.nanoTime() / 1_000_000, SubtitleConfig.current().displaySeconds / 3, listener());
  }
  private static Set<String> keys(Minecraft mc) throws Exception {
    var keys = new HashSet<String>(); for (var cue : visible(mc)) keys.add(cue.key()); return keys;
  }
  private static Object field(String name) throws Exception {
    Field field = Subtitles.class.getDeclaredField(name); field.setAccessible(true); return field.get(null);
  }
  private static SubtitleState state() throws Exception { return (SubtitleState)field("STATE"); }
  private static SubtitleState.Listener listener() throws Exception { return (SubtitleState.Listener)field("listener"); }
  private static double number(Object object, String field) throws Exception { return ((Number)object.getClass().getField(field).get(object)).doubleValue(); }
  private static void basis(SubtitleState.Listener listener, float yaw) {
    float radians = -yaw * .017453292F - 3.1415927F;
    check(listener.forwardX() == -MathHelper.sin(radians) && listener.forwardZ() == -MathHelper.cos(radians), "caption basis differs from native audio");
  }
  private static void arrows(SubtitleState.Listener listener) {
    double fx = listener.forwardX(), fz = listener.forwardZ();
    check(SubtitleState.arrow(listener, listener.x() + fx * 5, listener.y(), listener.z() + fz * 5) == 0, "ahead cue gained an arrow");
    check(SubtitleState.arrow(listener, listener.x() - fz * 5, listener.y(), listener.z() + fx * 5) == '>', "right cue points left");
    check(SubtitleState.arrow(listener, listener.x() + fz * 5, listener.y(), listener.z() - fx * 5) == '<', "left cue points right");
    check(SubtitleState.arrow(listener, listener.x(), listener.y() + 5, listener.z()) == 0, "vertical cue gained a horizontal arrow");
  }
  private static byte[] randomState(Object... owners) throws Exception {
    var bytes = new ByteArrayOutputStream();
    try (var output = new ObjectOutputStream(bytes)) {
      for (Object owner : owners) for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass())
        for (Field field : type.getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers()) && Random.class.isAssignableFrom(field.getType())) {
          field.setAccessible(true); output.writeObject(field.get(owner));
        }
    }
    return bytes.toByteArray();
  }
}
