package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.Setting;
import local.luke.power.map.MapConfig;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** The native HUD and editor share stamina pixels, but retain separate gameplay and draft state. */
final class Release300StaminaHudChecks {
  private static final String NATIVE = "power_hud:hudpositions.", SIMPLE = NATIVE + "SIMPLE_HUD_POSITION_CONFIG.",
      STAMINA = NATIVE + "ADVANCED_HUD_POSITIONS_CONFIG.STAMINA_POSITION_CONFIG.";
  private static final Path REPORT = Path.of("power-beta-data/reports/release-300/stamina-hud");
  private static Visual visual;
  private Release300StaminaHudChecks() {}

  static void run(Minecraft mc, SprintState state) throws Exception {
    var original = SprintConfig.copy(); var map = MapConfig.copy(); boolean auto = MenuPreferences.current().autoApply;
    Screen previous = mc.currentScreen;
    Object config = Class.forName("local.luke.power.hud.Config").getField("hudpositions").get(null);
    Object simple = get(config, "SIMPLE_HUD_POSITION_CONFIG"), advanced = get(config, "ADVANCED_HUD_POSITIONS_CONFIG");
    Object stamina = get(advanced, "STAMINA_POSITION_CONFIG"), hotbar = get(advanced, "HOTBAR_POSITION_CONFIG"), armor = get(advanced, "ARMOR_POSITION_CONFIG");
    Class<?> memory = Class.forName("local.luke.power.ui.HudEditorMemory");
    var scrolls = new HashMap<>((Map<?, ?>)field(memory, "scrolls").get(null));
    var collapsed = new HashSet<>((Set<?>)field(memory, "collapsed").get(null)); Object selected = field(memory, "selected").get(null);
    try (var hud = new Fields(mc.inGameHud); var global = new Fields(config); var combined = new Fields(simple);
        var separate = new Fields(stamina); var bar = new Fields(hotbar); var armorFields = new Fields(armor)) {
      MenuPreferences.current().autoApply = false;
      var quiet = MapConfig.copy(); quiet.minimap = quiet.worldMap = quiet.worldWaypoints = false; MapConfig.preview(quiet);
      test("native stamina visibility and anchors follow combined/separate layouts independently of the hotbar", () -> layouts(mc, state, config, simple, stamina, hotbar, armor));
      test("stamina preview retains identical partial sprites with gameplay disabled and isolates drag/Undo/Discard", () -> editor(mc, state));
    } finally {
      MenuPreferences.current().autoApply = auto; SprintConfig.preview(original); MapConfig.preview(map); mc.setScreen(previous);
      restore((Map<?, ?>)field(memory, "scrolls").get(null), scrolls); restore((Collection<?>)field(memory, "collapsed").get(null), collapsed);
      field(memory, "selected").set(null, selected);
    }
  }

  private static void layouts(Minecraft mc, SprintState state, Object config, Object simple, Object stamina, Object hotbar, Object armor) throws Exception {
    mc.setScreen(null); var settings = SprintConfig.copy(); settings.stamina = true; settings.capacity = 100; SprintConfig.preview(settings); state.stamina = 45;
    set(armor, "enableVisibility", false); set(hotbar, "enableVisibility", false);
    set(config, "putStatusBarIconsBelowHotbar", false);
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight); int width = size.method_1857(), height = size.method_1858();
    var layout = (StatusHudLayout)mc.inGameHud;
    for (int mode = 0; mode <= 2; mode++) for (int anchor = 0; anchor <= 2; anchor++) {
      enumeration(config, "hudPositioningSystem", mode);
      for (Object position : List.of(simple, stamina)) {
        enumeration(position, "horizontalPosition", anchor); enumeration(position, "verticalPosition", 2);
        set(position, "horizontalPositionOffset", 11); set(position, "verticalPositionOffset", 23); set(position, "enableVisibility", true);
      }
      nativeHud(mc, width, height);
      int x = NativeHudLayout.baseX(width, NativeHudLayout.STAMINA, 0)
          + NativeHudLayout.xOffset(width, NativeHudLayout.STAMINA, mode, anchor, 0) + (mode == 0 ? 0 : 11);
      int y = NativeHudLayout.baseY(height, NativeHudLayout.STAMINA)
          + NativeHudLayout.yOffset(height, NativeHudLayout.STAMINA, mode, 2, false, false) + (mode == 0 ? 0 : 23);
      check(layout.power$staminaX() == x && layout.power$staminaY() == y && layout.power$staminaVisible(), "native stamina geometry differs for mode=" + mode + " anchor=" + anchor);
      if (mode != 0) {
        Object position = mode == 1 ? simple : stamina; set(position, "enableVisibility", false); nativeHud(mc, width, height);
        check(!layout.power$staminaVisible() && Release300SprintPolishChecks.draw(mc, size) == 0, "hidden stamina emitted fragments in layout " + mode);
        set(position, "enableVisibility", true); nativeHud(mc, width, height);
      }
    }
    enumeration(config, "hudPositioningSystem", 2); enumeration(stamina, "horizontalPosition", 0);
    set(stamina, "enableVisibility", true); nativeHud(mc, width, height);
    check(!layout.power$hotbarVisible() && layout.power$staminaVisible() && Release300SprintPolishChecks.draw(mc, size) > 0,
        "separate stamina still depends on hotbar visibility");
    enumeration(config, "hudPositioningSystem", 1); set(simple, "enableVisibility", false); set(stamina, "enableVisibility", true); nativeHud(mc, width, height);
    check(!layout.power$staminaVisible(), "separate visibility bypassed the combined HUD switch");
  }
  private static void nativeHud(Minecraft mc, int width, int height) throws Exception { canvas(mc, width, height, () -> mc.inGameHud.render(0, false, -1, -1)); }

  private static void editor(Minecraft mc, SprintState state) throws Exception {
    var settings = SprintConfig.copy(); settings.stamina = false; settings.capacity = 100; SprintConfig.preview(settings); state.stamina = 100;
    var options = new PowerOptionsScreen(null); mc.setScreen(options);
    var parent = find(options.session(), STAMINA + "horizontalPositionOffset").value.deepCopy();
    try {
      find(options.session(), "maps.minimap").value = new JsonPrimitive(false);
      var hud = new HudEditorScreen(options, STAMINA + "horizontalPositionOffset"); mc.setScreen(hud); hud.init(mc, 640, 360);
      set(hud, "background", false); set(hud, "snapping", false);
      var draft = (SettingsDraft)get(hud, "draft");
      draft.get(HudEditorSettings.ENTRY).value = new JsonPrimitive(2);
      for (String part : List.of("HOTBAR", "HEARTS", "ARMOR", "OXYGEN", "OVERLAY_MESSAGE"))
        draft.get(NATIVE + "ADVANCED_HUD_POSITIONS_CONFIG." + part + "_POSITION_CONFIG.enableVisibility").value = new JsonPrimitive(false);
      for (Setting value : draft.settings()) if (value.id.matches("infoHud\\.clusters\\.[0-3]\\.enabled")) value.value = new JsonPrimitive(false);
      for (String suffix : List.of("enableVisibility", "horizontalPosition", "verticalPosition", "horizontalPositionOffset", "verticalPositionOffset"))
        draft.get(STAMINA + suffix).value = suffix.equals("enableVisibility") ? new JsonPrimitive(true)
            : new JsonPrimitive(suffix.equals("verticalPosition") ? 2 : suffix.equals("horizontalPositionOffset") ? 100 : suffix.equals("verticalPositionOffset") ? 80 : 0);
      call(hud, "afterEdit"); call(hud, "select", new Class<?>[]{String.class}, "native.5");
      Object preview = get(hud, "preview"); set(preview, "showChat", false); set(preview, "showToast", false); set(preview, "showTweaks", false); set(preview, "suppressDebug", true);
      canvas(mc, 480, 270, () -> call(preview, "draw", new Class<?>[]{UiScreen.class, Minecraft.class, int.class, int.class, int.class, String.class, boolean.class, float.class}, hud, mc, 480, 270, 0, "native.5", false, 0f));
      Object element = element(preview, "native.5");
      check(number(element, "x") == 100 && number(element, "y") == 80 && number(element, "width") == 81 && number(element, "height") == 9, "stamina preview anchor/hit bounds differ from its native nine-pixel sprite");
      var actual = crop(mc, 480, 270, 100, 80, 81, 9);
      canvas(mc, 480, 270, () -> SprintStaminaHud.draw(100, 80, 13, settings.restartStamina));
      check(Arrays.equals(actual, crop(mc, 480, 270, 100, 80, 81, 9)), "feature-off/full-reserve preview differs from the native partial stamina sprite");
      check(!SprintConfig.current().stamina && state.stamina == 100, "preview changed gameplay stamina or its enablement");
      canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
      element = element(preview, "native.5"); int[] pointer = pointer(hud, element);
      var before = draft.snapshot();
      click(hud, pointer[0], pointer[1]); check(get(hud, "dragging") != null, "selected stamina preview failed to own a drag");
      int dx = Math.max(5, (int)Math.round(24 * number(hud, "scale"))), dy = Math.max(3, (int)Math.round(12 * number(hud, "scale")));
      call(hud, "move", new Class<?>[]{int.class, int.class}, pointer[0] + dx, pointer[1] + dy);
      ((ScreenInput)(Object)hud).power$release(pointer[0] + dx, pointer[1] + dy, 0);
      check(draft.get(STAMINA + "horizontalPositionOffset").value.getAsInt() > 100 && draft.get(STAMINA + "verticalPositionOffset").value.getAsInt() > 80, "native stamina drag did not update its dedicated offsets");
      check(find(options.session(), STAMINA + "horizontalPositionOffset").value.equals(parent), "stamina drag escaped into the parent session");
      call(hud, "undo"); check(draft.snapshot().equals(before), "Undo did not restore the entire stamina drag boundary");
      offsetResponse(mc, hud, draft, preview);
      canvas(mc, 640, 360, () -> hud.render(-1, -1, 0)); capture(mc, "stamina-editor-separate");
      draft.get(STAMINA + "enableVisibility").value = new JsonPrimitive(false); call(hud, "afterEdit");
      canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
      check(((List<?>)call(preview, "elements")).stream().noneMatch(value -> {
        try { return call(value, "id").equals("native.5"); } catch (Exception failure) { throw new RuntimeException(failure); }
      }), "disabled stamina preview retained native content");
      draft.get(HudEditorSettings.ENTRY).value = new JsonPrimitive(1); draft.get(SIMPLE + "enableVisibility").value = new JsonPrimitive(true);
      call(hud, "afterEdit"); canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
      check(get(hud, "selected").equals("combined") && element(preview, "combined") != null, "combined HUD failed to retain the stamina selection");
      capture(mc, "stamina-editor-combined");
      for (Object button : (List<?>)call(hud, "footer")) if (call(button, "label").equals("Cancel")) { click(hud, (int)number(button, "x") + 8, hud.height - 19); break; }
      check((boolean)call(get(hud, "exit"), "active"), "dirty stamina editor Cancel skipped its prompt");
      click(hud, hud.width / 2, hud.height / 2 + 18);
      check(mc.currentScreen == options && find(options.session(), STAMINA + "horizontalPositionOffset").value.equals(parent), "Discard published stamina editor offsets");
    } finally { options.session().discard(); mc.setScreen(null); }
  }

  private static void offsetResponse(Minecraft mc, HudEditorScreen hud, SettingsDraft draft, Object preview) throws Exception {
    String armor = NATIVE + "ADVANCED_HUD_POSITIONS_CONFIG.ARMOR_POSITION_CONFIG.";
    for (String prefix : List.of(STAMINA, armor)) {
      draft.get(prefix + "enableVisibility").value = new JsonPrimitive(true);
      draft.get(prefix + "horizontalPosition").value = new JsonPrimitive(1); draft.get(prefix + "verticalPosition").value = new JsonPrimitive(0);
      draft.get(prefix + "horizontalPositionOffset").value = new JsonPrimitive(0); draft.get(prefix + "verticalPositionOffset").value = new JsonPrimitive(0);
    }
    draft.get(NATIVE + "putStatusBarIconsBelowHotbar").value = new JsonPrimitive(false);
    call(hud, "afterEdit"); canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
    double start = number(element(preview, "native.5"), "y");
    ((ScreenInput)(Object)hud).power$key((char)0, Keyboard.KEY_UP);
    canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
    check(number(element(preview, "native.5"), "y") == start - 1, "Up one pixel remained inside the armor collision dead zone");
    call(hud, "undo"); canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
    int[] p = pointer(hud, element(preview, "native.5")); click(hud, p[0], p[1]);
    check(get(hud, "dragging") != null, "default armor-adjacent stamina did not own a drag");
    int dy = Math.max(1, (int)Math.round(10 * number(hud, "scale")));
    call(hud, "move", new Class<?>[]{int.class, int.class}, p[0], p[1] - dy); ((ScreenInput)(Object)hud).power$release(p[0], p[1] - dy, 0);
    int offset = draft.get(STAMINA + "verticalPositionOffset").value.getAsInt();
    canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
    check(offset < 0 && Math.abs(offset + 10) <= 2 && number(element(preview, "native.5"), "y") == start + offset,
        "upward drag failed to move directly from the armor-aware anchor");
    draft.get(STAMINA + "horizontalPositionOffset").value = new JsonPrimitive(100); call(hud, "afterEdit");
    canvas(mc, 640, 360, () -> hud.render(-1, -1, 0));
    check(number(element(preview, "native.5"), "y") == start + offset, "horizontal movement jumped vertically after leaving armor overlap");
  }

  /** Short-lived visual setup; all changes stay in memory and Restore owns their original values. */
  static void visual(Minecraft mc, String action) throws Exception {
    if (action.equals("restore")) { if (visual != null) { visual.close(); visual = null; } log("STAMINA VISUAL restored"); return; }
    if (action.equals("show")) {
      check(visual == null, "restore the active stamina visual fixture first");
      check(mc.world != null && !mc.world.isRemote && mc.player != null && !local.luke.power.input.DetachedCamera.isActive(), "open a disposable singleplayer world outside freecam");
      var armed = new Visual(mc);
      try { armed.show(); visual = armed; } catch (Exception failure) { armed.close(); throw failure; }
      log("STAMINA VISUAL armed at 55%, restart 40%; restore before leaving this world"); return;
    }
    check(visual != null && visual.mc == mc && visual.world == mc.world, "run stamina visual show first");
    if (action.equals("pause")) mc.setScreen(new net.minecraft.class_525());
    else if (action.equals("editor")) {
      if (visual.options != null) visual.options.session().discard();
      visual.options = new PowerOptionsScreen(null); mc.setScreen(visual.options);
      mc.setScreen(new HudEditorScreen(visual.options, STAMINA + "horizontalPositionOffset"));
    } else throw new IllegalArgumentException("Use stamina visual show|pause|editor|restore");
  }

  private static final class Visual implements AutoCloseable {
    final Minecraft mc; final Object world; final Screen screen; final SprintSettings settings; final RuleSet rules;
    final String mode; final boolean flying, hidden, paused; final int override; final Fields state, global, stamina; PowerOptionsScreen options;
    final Object config, position;
    Visual(Minecraft mc) throws Exception {
      this.mc = mc; world = mc.world; screen = mc.currentScreen; settings = SprintConfig.copy(); rules = AccessRules.copy(mc);
      mode = (String)call(call(mc.player, "power_mode"), "name"); flying = (boolean)call(mc.player, "creative_isFlying");
      override = (int)call(mc.player, "power_flightOverride"); hidden = mc.options.hideHud; paused = mc.paused;
      config = Class.forName("local.luke.power.hud.Config").getField("hudpositions").get(null);
      position = get(get(config, "ADVANCED_HUD_POSITIONS_CONFIG"), "STAMINA_POSITION_CONFIG");
      state = new Fields(((SprintPlayer)mc.player).power$sprint()); global = new Fields(config); stamina = new Fields(position);
    }
    void show() throws Exception {
      var enabled = rules.copy(); enabled.set(GameRule.ALLOW_SPRINTING, true); AccessRules.restore(mc, enabled);
      CreativeVehicleChecks.mode(mc, "SURVIVAL"); call(mc.player, "power_flightOverride", new Class<?>[]{int.class}, 0); call(mc.player, "creative_setFlying", new Class<?>[]{boolean.class}, false);
      var shown = SprintConfig.copy(); shown.stamina = true; shown.capacity = 100; shown.restartStamina = 40;
      shown.drain = shown.recharge = shown.rechargeStill = .1; shown.jumpCost = 0; SprintConfig.preview(shown);
      var reserve = ((SprintPlayer)mc.player).power$sprint(); reserve.stamina = 55; reserve.exhausted = reserve.sprinting = false; reserve.rechargeTicks = Integer.MAX_VALUE;
      enumeration(config, "hudPositioningSystem", 2); set(position, "enableVisibility", true);
      enumeration(position, "horizontalPosition", 1); enumeration(position, "verticalPosition", 0);
      set(position, "horizontalPositionOffset", 0); set(position, "verticalPositionOffset", 0);
      mc.options.hideHud = false; mc.paused = false; mc.setScreen(null);
    }
    public void close() throws Exception {
      check(mc.world == world, "restore the stamina visual fixture in its original disposable world");
      if (options != null) options.session().discard();
      CreativeVehicleChecks.mode(mc, mode); call(mc.player, "power_flightOverride", new Class<?>[]{int.class}, override); call(mc.player, "creative_setFlying", new Class<?>[]{boolean.class}, flying);
      state.close(); stamina.close(); global.close(); AccessRules.restore(mc, rules); SprintConfig.preview(settings);
      mc.options.hideHud = hidden; mc.setScreen(screen); mc.paused = paused;
    }
  }

  private static Object element(Object preview, String id) throws Exception {
    for (Object item : (List<?>)call(preview, "elements")) if (call(item, "id").equals(id)) return item;
    throw new AssertionError("missing preview element " + id);
  }
  private static int[] pointer(HudEditorScreen hud, Object element) throws Exception {
    double scale = number(hud, "scale"); return new int[]{(int)Math.round(number(hud, "canvasX") + (number(element, "x") + number(element, "width") / 2) * scale),
        (int)Math.round(number(hud, "canvasY") + (number(element, "y") + number(element, "height") / 2) * scale)};
  }
  private static void canvas(Minecraft mc, int width, int height, Check action) throws Exception {
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight); GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glClearColor(.1f, .1f, .1f, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, width, height, 0, 1000, 3000);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000);
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glColor4f(1, 1, 1, 1);
      action.run(); GL11.glFinish();
    } finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(mode); }
  }
  private static int[] crop(Minecraft mc, int width, int height, int x, int y, int w, int h) {
    int left = (int)Math.round(x * mc.displayWidth / (double)width), bottom = (int)Math.round((height - y - h) * mc.displayHeight / (double)height);
    int span = (int)Math.round(w * mc.displayWidth / (double)width), rows = (int)Math.round(h * mc.displayHeight / (double)height);
    ByteBuffer buffer = BufferUtils.createByteBuffer(span * rows * 4);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    try {
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0); GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
      GL11.glReadPixels(left, bottom, span, rows, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    } finally { GL11.glPopClientAttrib(); }
    int[] result = new int[span * rows];
    for (int i = 0; i < result.length; i++) result[i] = (buffer.get(i * 4) & 255) << 16 | (buffer.get(i * 4 + 1) & 255) << 8 | buffer.get(i * 4 + 2) & 255;
    return result;
  }
  private static void capture(Minecraft mc, String name) throws Exception {
    int[] pixels = crop(mc, 1, 1, 0, 0, 1, 1); var image = new BufferedImage(mc.displayWidth, mc.displayHeight, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < mc.displayHeight; y++) for (int x = 0; x < mc.displayWidth; x++) image.setRGB(x, mc.displayHeight - 1 - y, pixels[y * mc.displayWidth + x]);
    Files.createDirectories(REPORT); ImageIO.write(image, "PNG", REPORT.resolve(name + ".png").toFile());
  }
  private static void click(Screen screen, int x, int y) { ((ScreenInput)(Object)screen).power$click(x, y, 0); }
  private static Field field(Class<?> type, String name) throws Exception { for (; type != null; type = type.getSuperclass()) try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException ignored) {} throw new NoSuchFieldException(name); }
  private static Object get(Object target, String name) throws Exception { return field(target.getClass(), name).get(target); }
  private static void set(Object target, String name, Object value) throws Exception { field(target.getClass(), name).set(target, value); }
  private static void enumeration(Object target, String name, int value) throws Exception { Field f = field(target.getClass(), name); f.set(target, f.getType().getEnumConstants()[value]); }
  private static Object call(Object target, String name, Class<?>[] types, Object... values) throws Exception { return Editor230Checks.invoke(target, name, types, values); }
  private static Object call(Object target, String name) throws Exception { return call(target, name, new Class<?>[0]); }
  private static double number(Object target, String name) throws Exception { return ((Number)call(target, name)).doubleValue(); }
  @SuppressWarnings({"rawtypes", "unchecked"}) private static void restore(Map target, Map values) { target.clear(); target.putAll(values); }
  @SuppressWarnings({"rawtypes", "unchecked"}) private static void restore(Collection target, Collection values) { target.clear(); target.addAll(values); }
  private static final class Fields implements AutoCloseable {
    final Object target; final Map<Field, Object> values = new LinkedHashMap<>();
    Fields(Object target) throws Exception { this.target = target; for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) for (Field f : type.getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) { f.setAccessible(true); values.put(f, f.get(target)); } }
    public void close() throws Exception { for (var value : values.entrySet()) value.getKey().set(target, value.getValue()); }
  }
}
