package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.input.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.StatusHudLayout;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;

/** Native mode ownership and HUD draws; no gameplay travel or durable settings writes. */
final class Release300SprintPolishChecks {
  private Release300SprintPolishChecks() {}
  private static final Class<?>[] NONE = new Class<?>[0];
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.health > 0, "open a disposable singleplayer world with a living player");
    check(Display.isActive() && !DetachedCamera.isActive() && mc.player.field_1595 == null, "use an unmounted player outside freecam and focus the disposable client");
    check(!mc.player.method_1334() && !mc.player.method_1335() && !mc.player.method_1373(), "stand outside water/lava and stop sneaking before sprint checks");
    failures = 0;
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    Screen previous = mc.currentScreen;
    var originalSettings = SprintConfig.copy(); var rules = AccessRules.copy(mc);
    Class<?> config = Class.forName("local.luke.power.creative.config.Config"), flight = Class.forName("local.luke.power.creative.FlightController"), runtime = Class.forName("local.luke.power.creative.ClientRuntime");
    Object originalCreative = call(stat(config, "current"), "copy"), creative = call(originalCreative, "copy");
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    String originalMode = (String)call(call(mc.player, "power_mode"), "name");
    boolean originalFlying = (boolean)call(mc.player, "creative_isFlying");
    int originalOverride = (int)call(mc.player, "power_flightOverride");
    var state = ((SprintPlayer)mc.player).power$sprint();
    Field keyField = Keyboard.class.getDeclaredField("keyDownBuffer"); keyField.setAccessible(true);
    var keys = (ByteBuffer)keyField.get(null); byte[] savedKeys = new byte[keys.capacity()];
    for (int i = 0; i < savedKeys.length; i++) savedKeys[i] = keys.get(i);
    var modifiers = Bindings.modifiers(); Map<KeyBinding, Integer> keyCodes = new LinkedHashMap<>();
    Object[] bindings = (Object[])field(Bindings.class, "registered").get(null);
    var originalInput = mc.player.field_161; var input = new net.minecraft.class_41();
    try (var playerFields = new Fields(mc.player); var options = new Fields(mc.options); var minecraft = new Fields(mc);
        var sprintState = new Fields(state); var sprintController = new Fields(SprintController.class);
        var flightFields = new Fields(flight); var latch = new Fields(field(flight, "sprint").get(null));
        var runtimeFields = new Fields(runtime); var cameraFields = new Fields(camera); var view = new ViewState()) {
      try {
        mc.setScreen(null); mc.paused = false; mc.field_2778 = true; mc.field_2807 = mc.player; mc.player.field_161 = input;
        for (Object object : bindings) if (object instanceof Binding binding && (binding.power$id().equals("key.forward")
            || binding.power$id().equals("powerbeta.sprint") || binding.power$id().equals("power_creative.sprint"))) {
          KeyBinding key = (KeyBinding)object; keyCodes.put(key, key.code); key.code = binding.power$id().equals("key.forward") ? 17 : 29;
        }
        Bindings.configure(Map.of()); for (int i = 0; i < savedKeys.length; i++) keys.put(i, (byte)0);
        var allowed = rules.copy();
        for (GameRule rule : List.of(GameRule.ALLOW_SPRINTING, GameRule.ALLOW_FLIGHT, GameRule.ALLOW_CREATIVE_MODE, GameRule.ALLOW_SPECTATOR_MODE)) allowed.set(rule, true);
        AccessRules.restore(mc, allowed);
        set(creative, "flight", true); set(creative, "sprintToggle", true); set(creative, "sprintMultiplier", 200); set(creative, "landStopsFlight", false);
        invoke(config, "preview", new Class<?>[]{creative.getClass()}, creative);
        var settings = SprintConfig.copy(); settings.stamina = true; settings.activation = 1; settings.doubleTap = false;
        settings.noSprintInCombat = false; settings.jumpBoost = false; settings.capacity = 100; settings.drain = 10; settings.jumpCost = 4;
        SprintConfig.preview(settings);
        invoke(runtime, "tick", new Class<?>[]{Minecraft.class}, mc);
        test("actual native modes charge stamina only for grounded Survival and preserve exhausted reserves elsewhere", () -> modes(mc, state, keys, input));
        test("ground, Creative and Spectator share dynamic-FOV strength while freecam keeps its own preference", () -> fov(mc, state));
        test("zero-stamina Creative takeoff hands published ground sprint to flight before ground cancellation", () -> handoff(mc, state, keys, input, flight));
        test("native stamina HUD draws in gameplay/chat, skips disabled/freecam states and restores GL state", () -> hud(mc, state));
      } finally {
        // Restore native tracker flags before the surrounding field snapshots close.
        CreativeVehicleChecks.mode(mc, originalMode); call(mc.player, "power_flightOverride", new Class<?>[]{int.class}, originalOverride);
        call(mc.player, "creative_setFlying", new Class<?>[]{boolean.class}, originalFlying);
      }
    } finally {
      for (int i = 0; i < savedKeys.length; i++) keys.put(i, savedKeys[i]);
      for (var e : keyCodes.entrySet()) e.getKey().code = e.getValue(); Bindings.configure(modifiers);
      mc.player.field_161 = originalInput; SprintConfig.preview(originalSettings); AccessRules.restore(mc, rules);
      invoke(config, "preview", new Class<?>[]{originalCreative.getClass()}, originalCreative); mc.setScreen(previous);
    }
    test("sprint polish fixture leaves durable preferences unchanged", () -> check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "sprint fixture wrote global settings"));
    log("RELEASE300 SPRINT-POLISH failures=" + failures);
  }

  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object call(Object target, String name) throws Exception { return call(target, name, NONE); }
  private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception { return Editor230Checks.invoke(target, name, types, args); }
  private static Object invoke(Class<?> type, String name, Class<?>[] types, Object... args) throws Exception {
    Method m = type.getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(null, args);
  }
  private static Object stat(Class<?> type, String name) throws Exception { return invoke(type, name, NONE); }
  private static void set(Object target, String name, Object value) throws Exception { field(target.getClass(), name).set(target, value); }
  private static void mode(Minecraft mc, String mode, boolean flying) throws Exception {
    FreecamPreviewChecks.active(false); mc.setScreen(null); CreativeVehicleChecks.mode(mc, mode);
    call(mc.player, "power_flightOverride", new Class<?>[]{int.class}, flying ? 1 : 0);
    call(mc.player, "creative_setFlying", new Class<?>[]{boolean.class}, flying);
    mc.player.field_1623 = !flying; mc.player.field_1624 = false;
  }
  private static void modes(Minecraft mc, SprintState state, ByteBuffer keys, net.minecraft.class_41 input) throws Exception {
    input.field_2533 = 1; keys.put(17, (byte)1); keys.put(29, (byte)1);
    for (String mode : List.of("CREATIVE", "CREATIVE-FLIGHT", "SURVIVAL-FLIGHT", "SPECTATOR", "FREECAM")) {
      mode(mc, mode.startsWith("CREATIVE") ? "CREATIVE" : mode.equals("SPECTATOR") ? "SPECTATOR" : "SURVIVAL", mode.endsWith("FLIGHT") || mode.equals("SPECTATOR"));
      if (mode.equals("FREECAM")) FreecamPreviewChecks.active(true);
      check(!SprintController.usesStamina(mc.player), "native stamina owner is wrong for " + mode);
      state.stamina = 0; state.exhausted = true; state.rechargeTicks = 17; state.combatTicks = 0;
      SprintController.movement(mc.player); SprintController.jumped(mc.player);
      check(state.stamina == 0 && state.exhausted && state.rechargeTicks == 17, "native " + mode + " changed saved Survival reserve/exhaustion/delay");
      if (mode.equals("CREATIVE")) check(state.sprinting, "zero reserve blocks Creative ground sprint");
    }
    mode(mc, "SURVIVAL", false);
    check(SprintController.usesStamina(mc.player), "grounded Survival does not own stamina");
    SprintController.movement(mc.player);
    check(!state.sprinting && state.stamina == 0 && state.exhausted && state.rechargeTicks == 16, "Survival did not resume its saved exhausted delay");
    state.stamina = 6; state.exhausted = false; state.rechargeTicks = 0;
    SprintController.movement(mc.player); SprintController.jumped(mc.player);
    check(state.sprinting && Math.abs(state.stamina - 1.5) < 1e-9, "actual Survival movement/jump did not charge its stamina");
  }
  private static void resetFov() throws Exception { field(SprintController.class, "previousFov").setFloat(null, 1); field(SprintController.class, "fov").setFloat(null, 1); }
  private static void fov(Minecraft mc, SprintState state) throws Exception {
    var settings = SprintConfig.copy(); settings.speed = 200; settings.dynamicFov = true; settings.fovStrength = 40;
    settings.creativeFov = settings.spectatorFov = false; settings.freecamFov = true; SprintConfig.preview(settings);
    for (String mode : List.of("SURVIVAL", "CREATIVE", "SPECTATOR")) {
      mode(mc, mode, !mode.equals("SURVIVAL")); state.sprinting = mode.equals("SURVIVAL");
      SprintView.publish(SprintView.Kind.CREATIVE, true, 2); SprintView.publish(SprintView.Kind.SPECTATOR, true, 2);
      resetFov(); SprintController.tick(mc);
      check(Math.abs(SprintController.fov(1) - 1.1) < 1e-6 && Math.abs(SprintView.fov(.5f) - 1.05) < 1e-6, "shared 40% DFOV strength differs for " + mode);
      settings.dynamicFov = false; settings.creativeFov = settings.spectatorFov = true; SprintConfig.preview(settings);
      resetFov(); SprintController.tick(mc); check(SprintController.fov(1) == 1, "retained per-mode flag bypassed shared DFOV switch");
      settings.dynamicFov = true; settings.creativeFov = settings.spectatorFov = false; SprintConfig.preview(settings);
    }
    mode(mc, "SURVIVAL", false); FreecamPreviewChecks.active(true); settings.dynamicFov = false; settings.fovStrength = 0; SprintConfig.preview(settings);
    SprintView.publish(SprintView.Kind.FREECAM, true, 2); resetFov(); SprintController.tick(mc);
    check(Math.abs(SprintController.fov(1) - 1.25) < 1e-6, "freecam incorrectly shares ground/flight strength");
    settings.freecamFov = false; SprintConfig.preview(settings); resetFov(); SprintController.tick(mc);
    check(SprintController.fov(1) == 1, "freecam FOV ignored its independent preference"); FreecamPreviewChecks.active(false);
  }
  private static void handoff(Minecraft mc, SprintState state, ByteBuffer keys, net.minecraft.class_41 input, Class<?> flight) throws Exception {
    mode(mc, "CREATIVE", false); var settings = SprintConfig.copy(); settings.activation = 1; settings.speed = 130; SprintConfig.preview(settings);
    invoke(flight, "reset", NONE); SprintController.reset(); input.field_2533 = 1; keys.put(17, (byte)1); keys.put(29, (byte)0);
    state.stamina = 0; state.exhausted = true; state.rechargeTicks = 11; SprintController.movement(mc.player);
    keys.put(29, (byte)1); SprintController.movement(mc.player);
    check(SprintView.active(SprintView.Kind.GROUND), "Creative ground sprint never published before takeoff");
    call(mc.player, "power_flightOverride", new Class<?>[]{int.class}, 1); call(mc.player, "creative_setFlying", new Class<?>[]{boolean.class}, true);
    SprintController.tick(mc); invoke(flight, "tick", new Class<?>[]{Minecraft.class}, mc);
    check(!SprintView.active(SprintView.Kind.GROUND) && SprintView.active(SprintView.Kind.CREATIVE), "takeoff lost sprint handoff or retained ground sprint");
    check(state.stamina == 0 && state.exhausted && state.rechargeTicks == 11, "handoff changed saved Survival stamina");
    keys.put(29, (byte)0); invoke(flight, "tick", new Class<?>[]{Minecraft.class}, mc);
    check(SprintView.active(SprintView.Kind.CREATIVE), "release lost inherited flight Toggle");
    keys.put(17, (byte)0); invoke(flight, "tick", new Class<?>[]{Minecraft.class}, mc);
    check(!SprintView.active(SprintView.Kind.CREATIVE), "movement stop retained inherited flight Toggle");
  }

  private static void hud(Minecraft mc, SprintState state) throws Exception {
    check(GLContext.getCapabilities().OpenGL15, "native HUD fixture needs OpenGL 1.5 occlusion queries");
    mode(mc, "SURVIVAL", false); var settings = SprintConfig.copy(); settings.stamina = true; settings.capacity = 100; SprintConfig.preview(settings);
    state.stamina = 45; mc.options.hideHud = false;
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    try (var layout = new Fields(mc.inGameHud)) {
      check(mc.inGameHud instanceof StatusHudLayout, "native HUD does not expose shared status layout");
      set(mc.inGameHud, "scaledWidth", size.method_1857()); set(mc.inGameHud, "scaledHeight", size.method_1858());
      set(mc.inGameHud, "hotbarVisibility", true); set(mc.inGameHud, "xOffsetHotbar", 0); set(mc.inGameHud, "yOffsetHotbar", 0);
      check(draw(mc, size) > 0, "eligible Survival stamina icons submitted no fragments");
      mc.setScreen(new ChatScreen()); check(draw(mc, size) > 0, "chat hid Survival stamina icons");
      mc.currentScreen = new Screen() {}; check(draw(mc, size) == 0, "ordinary menu rendered stamina icons"); mc.setScreen(null);
      settings.stamina = false; SprintConfig.preview(settings); check(draw(mc, size) == 0, "disabled stamina submitted HUD fragments");
      settings.stamina = true; SprintConfig.preview(settings); FreecamPreviewChecks.active(true);
      check(draw(mc, size) == 0, "freecam rendered Survival stamina icons"); FreecamPreviewChecks.active(false);
      mode(mc, "CREATIVE", false); check(draw(mc, size) == 0, "Creative ground rendered Survival stamina icons");
      mode(mc, "SURVIVAL", false); state.stamina = 100; check(draw(mc, size) == 0, "full stamina retained an unnecessary icon row");
    }
  }
  private static int draw(Minecraft mc, class_564 size) throws Exception {
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE), query = GL15.glGenQueries();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight); GL11.glDisable(GL11.GL_SCISSOR_TEST); GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, size.method_1857(), size.method_1858(), 0, -1, 1);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_LIGHTING); GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glEnable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_BLEND); GL11.glDepthMask(false); GL11.glColor4f(.2f, .3f, .4f, .5f);
      var before = glState(); GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, query);
      try { SprintStaminaHud.render(mc); } finally { GL15.glEndQuery(GL15.GL_SAMPLES_PASSED); }
      int samples = GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT);
      check(before.equals(glState()), "stamina icon draw changed GL enable/blend/depth/color state");
      check(GL11.glGetError() == GL11.GL_NO_ERROR, "stamina icon draw left a GL error"); return samples;
    } finally {
      GL15.glDeleteQueries(query); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
  }
  private static List<Object> glState() {
    var values = new ArrayList<Object>();
    for (int capability : new int[]{GL11.GL_TEXTURE_2D, GL11.GL_LIGHTING, GL11.GL_DEPTH_TEST, GL11.GL_CULL_FACE, GL11.GL_BLEND, GL11.GL_ALPHA_TEST}) values.add(GL11.glIsEnabled(capability));
    values.add(GL11.glGetInteger(GL11.GL_BLEND_SRC)); values.add(GL11.glGetInteger(GL11.GL_BLEND_DST)); values.add(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
    FloatBuffer color = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color); for (int i = 0; i < 4; i++) values.add(color.get(i));
    return values;
  }
  private static final class Fields implements AutoCloseable {
    final Object target; final Map<Field, Object> values = new LinkedHashMap<>();
    Fields(Object object) throws Exception {
      boolean statics = object instanceof Class<?>; target = statics ? null : object;
      for (Class<?> type = statics ? (Class<?>)object : object.getClass(); type != null; type = type.getSuperclass()) for (Field f : type.getDeclaredFields())
        if (!Modifier.isFinal(f.getModifiers()) && Modifier.isStatic(f.getModifiers()) == statics) { f.setAccessible(true); values.put(f, f.get(target)); }
    }
    public void close() throws Exception { for (var e : values.entrySet()) e.getKey().set(target, e.getValue()); }
  }
  private static final class ViewState implements AutoCloseable {
    final double[] ratios = ((double[])field(SprintView.class, "ratios").get(null)).clone();
    final boolean[] active = ((boolean[])field(SprintView.class, "active").get(null)).clone();
    final Object provider = field(SprintView.class, "fov").get(null);
    ViewState() throws Exception {}
    public void close() throws Exception {
      System.arraycopy(ratios, 0, field(SprintView.class, "ratios").get(null), 0, ratios.length);
      System.arraycopy(active, 0, field(SprintView.class, "active").get(null), 0, active.length); field(SprintView.class, "fov").set(null, provider);
    }
  }
}
