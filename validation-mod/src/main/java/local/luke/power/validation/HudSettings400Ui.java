package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Reads shared geometry, then sends actual native screen presses and keys. */
final class HudSettings400Ui {
  static final String OFF = "power_hud:hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.OFFHAND_POSITION_CONFIG.";
  private HudSettings400Ui() {}
  static Object get(Object target, String name) throws Exception { return Editor230Checks.get(target, name); }
  static Object call(Object target, String name) throws Exception { return invoke(target, name, new Class<?>[0]); }
  static Object invoke(Object target, String name, Class<?>[] types, Object... arguments) throws Exception {
    return Editor230Checks.invoke(target, name, types, arguments);
  }
  static int number(Object target, String name) throws Exception { return ((Number)call(target, name)).intValue(); }
  static void set(Object target, String name, Object value) throws Exception {
    Field field = field(target.getClass(), name); field.set(target, value);
  }
  static Field field(Class<?> owner, String name) throws Exception {
    for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
      try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
      catch (NoSuchFieldException absent) { }
    }
    throw new NoSuchFieldException(name);
  }
  static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key((char)0, key); }
  static void open(Minecraft mc, Screen screen, int width, int height) { mc.setScreen(screen); screen.init(mc, width, height); }
  static SettingsDraft draft(HudEditorScreen screen) throws Exception { return (SettingsDraft)get(screen, "draft"); }
  static void refresh(HudEditorScreen screen) throws Exception { call(screen, "afterEdit"); }
  static Map<String, JsonElement> values(ConfigSession session) {
    Map<String, JsonElement> values = new LinkedHashMap<>(); session.settings().forEach(s -> values.put(s.id, s.value.deepCopy())); return values;
  }
  static Setting setting(ConfigSession session, String id) { return session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(); }

  static List<String> listed(PowerOptionsScreen screen, List<String> ids) throws Exception {
    set(screen, "conflictIds", ids); set(screen, "changedOnly", false); set(screen, "showDisabled", true);
    ((Set<?>)get(screen, "collapsed")).clear(); UiChecks.search(screen, ""); set(screen, "scroll", 0d); call(screen, "layout");
    List<String> result = new ArrayList<>();
    for (Object row : (List<?>)get(screen, "rows")) { Setting s = (Setting)call(row, "setting"); if (s != null) result.add(s.id); }
    return result;
  }
  static Object row(PowerOptionsScreen screen, String id) throws Exception {
    listed(screen, List.of(id));
    for (Object row : (List<?>)get(screen, "rows")) { Setting s = (Setting)call(row, "setting"); if (s != null && s.id.equals(id)) return row; }
    throw new AssertionError("native Options row is absent: " + id);
  }
  static int optionY(PowerOptionsScreen screen, Object row) throws Exception {
    int span = number(screen, "span"), offset = span < 340 ? 16 : 3;
    return number(screen, "top") + number(row, "y") + offset + 8;
  }
  static void optionReset(PowerOptionsScreen screen, String id) throws Exception {
    Object row = row(screen, id); click(screen, number(screen, "right") - 24, optionY(screen, row), 0);
  }
  static void optionControl(PowerOptionsScreen screen, String id) throws Exception {
    Object row = row(screen, id); Setting s = (Setting)call(row, "setting");
    int left = (int)invoke(screen, "mainControlLeft", new Class<?>[]{row.getClass()}, row);
    int right = (int)invoke(screen, "controlEnd", new Class<?>[]{Setting.class}, s);
    click(screen, (left + right) / 2, optionY(screen, row), 0);
  }
  static void optionAction(PowerOptionsScreen screen, int index) throws Exception {
    int x = (int)invoke(screen, "actionX", new Class<?>[]{int.class}, index);
    click(screen, x + 8, screen.height - 18, 0);
  }
  static HudEditorScreen hud(Minecraft mc, PowerOptionsScreen options) throws Exception {
    optionControl(options, HudEditorSettings.ENTRY);
    check(mc.currentScreen instanceof HudEditorScreen, "native HUD editor action did not open its screen");
    var hud = (HudEditorScreen)mc.currentScreen; hud.init(mc, options.width, options.height); return hud;
  }
  static void select(HudEditorScreen screen, String id) throws Exception {
    List<String> ids = (List<String>)call(screen, "selectionIds"); check(ids.contains(id), "missing HUD selector entry " + id);
    click(screen, number(screen, "left") + 8, 38, 0);
    check((boolean)get(screen, "selecting"), "HUD selector did not open");
    int current = (int)get(screen, "popupIndex"), target = ids.indexOf(id);
    for (int i = 0; i < Math.floorMod(target - current, ids.size()); i++) key(screen, Keyboard.KEY_DOWN);
    key(screen, Keyboard.KEY_RETURN); check(get(screen, "selected").equals(id), "HUD selector key action missed " + id);
  }
  static int expose(HudEditorScreen screen, Setting s) throws Exception {
    ((Set<?>)get(screen, "collapsed")).clear(); set(screen, "settingsHidden", false);
    invoke(screen, "revealControl", new Class<?>[]{Setting.class}, s);
    return (int)invoke(screen, "controlY", new Class<?>[]{Setting.class}, s);
  }
  static void edit(HudEditorScreen screen, Setting s, JsonElement wanted) throws Exception {
    if (s.value.equals(wanted)) return;
    int y = expose(screen, s), x = (int)invoke(screen, "controlX", new Class<?>[]{Setting.class}, s) + 8;
    if (s.kind == Setting.Kind.BOOLEAN) click(screen, x, y + 8, 0);
    else if (s.kind == Setting.Kind.CHOICE) {
      for (int i = 0; i < s.choices.size() && !s.value.equals(wanted); i++) click(screen, x, y + 8, 0);
    } else {
      click(screen, x, y + 8, 1); Object inline = get(screen, "inline");
      check((boolean)call(inline, "active"), "numeric exact entry did not open for " + s.id);
      ((TextInput)get(inline, "input")).setText(wanted.getAsString()); key(screen, Keyboard.KEY_RETURN);
      check(!(boolean)call(inline, "active"), "numeric exact entry did not commit for " + s.id);
    }
    check(s.value.equals(wanted), "native HUD control missed " + s.id + ", wanted " + wanted + " got " + s.value);
  }
  static void reset(HudEditorScreen screen, Setting s) throws Exception {
    int y = expose(screen, s); click(screen, number(screen, "resetX") + 8, y + 8, 0);
  }
  static void footer(HudEditorScreen screen, String label) throws Exception {
    for (Object button : (List<?>)call(screen, "footer")) if (call(button, "label").equals(label)) {
      click(screen, number(button, "x") + number(button, "width") / 2, screen.height - 19, 0); return;
    }
    throw new AssertionError("missing HUD footer action " + label);
  }
  static void resetChoice(HudEditorScreen screen, String label) throws Exception {
    for (Object button : (List<?>)call(screen, "resetButtons")) if (call(button, "label").equals(label)) {
      check((boolean)call(button, "enabled"), "HUD reset action is unexpectedly disabled: " + label);
      click(screen, number(button, "x") + number(button, "width") / 2, number(button, "y") + 8, 0); return;
    }
    throw new AssertionError("missing native reset choice " + label);
  }
  static void toggle(HudEditorScreen screen, String id) throws Exception {
    Object toolbar = call(screen, "toolbar");
    for (Object button : (List<?>)call(toolbar, "toggles")) if (call(button, "id").equals(id)) {
      click(screen, number(button, "x") + 5, number(button, "y") + 8, 0); return;
    }
    throw new AssertionError("missing native HUD preview toggle " + id);
  }
  static Object element(HudEditorScreen screen, String id) throws Exception {
    for (Object element : (List<?>)call(get(screen, "preview"), "elements")) if (call(element, "id").equals(id)) return element;
    return null;
  }
  static JsonElement different(Setting s, JsonElement expected) {
    if (s.kind == Setting.Kind.BOOLEAN) return new JsonPrimitive(!expected.getAsBoolean());
    if (s.kind == Setting.Kind.KEY) return new JsonPrimitive(Keyboard.KEY_F18);
    if (s.kind == Setting.Kind.CHOICE) return new JsonPrimitive((expected.getAsInt() + 1) % s.choices.size());
    double next = expected.getAsDouble() + Math.max(1, s.step); if (next > s.max) next = expected.getAsDouble() - Math.max(1, s.step);
    return s.kind == Setting.Kind.INTEGER ? new JsonPrimitive((int)next) : new JsonPrimitive(next);
  }
  static void capture(Minecraft mc, Screen screen, Path path, int width, int height) throws Exception {
    Files.createDirectories(path.getParent()); screen.init(mc, width, height);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try { SettingsSnapshot.render(mc, screen, width, height, path); }
    finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
}
