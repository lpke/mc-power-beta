package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Native dropdown geometry, retained drag ownership, isolated edits and complete tooltip inventory. */
final class Dropdown450Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/dropdowns-450");
  private static final Class<?> TYPE;
  static {
    try { TYPE = Class.forName("local.luke.power.ui.SettingsDropdown"); }
    catch (ClassNotFoundException e) { throw new ExceptionInInitializerError(e); }
  }
  private Dropdown450Checks() {}

  private static final class Host extends UiScreen {
    final Object popup;
    int pointerX = -1000, pointerY = -1000;
    Host() throws Exception { var constructor = TYPE.getDeclaredConstructor(); constructor.setAccessible(true); popup = constructor.newInstance(); }
    public void render(int mx, int my, float delta) {
      renderBackground(); titleBar("Dropdown review");
      try { call(popup, "render", new Class<?>[]{UiScreen.class, int.class, int.class}, this, pointerX, pointerY); }
      catch (Exception e) { throw new IllegalStateException(e); }
    }
  }
  static void capture(Minecraft mc) throws Exception {
    Files.createDirectories(REPORT);
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    var previous = mc.currentScreen;
    var options = new PowerOptionsScreen(previous);
    try {
      mc.currentScreen = options;
      for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
        options.init(mc, size[0], size[1]);
        var setting = options.session().settings().stream().filter(v -> v.id.equals("maps.caveMode")).findFirst().orElseThrow();
        Dropdown450Screens.openSetting(options, setting);
        Object popup = UiChecks.field(options, "choices");
        int mx = integer(popup, "x") + 12;
        int my = integer(popup, "y") + integer(popup, "defaultIndex") * 20 - (int)(double)field(popup, "scroll") + 8;
        SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("options-default-" + size[0] + ".png"), mx, my);
        int selected = integer(popup, "selected"), defaultIndex = integer(popup, "defaultIndex");
        int hovered = (selected + 1) % setting.choices.size();
        if (hovered == defaultIndex && defaultIndex != selected) hovered = (hovered + 1) % setting.choices.size();
        int hoverY = integer(popup, "y") + hovered * 20 - (int)(double)field(popup, "scroll") + 8;
        SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("options-hover-" + size[0] + ".png"), mx, hoverY);
        call(popup, "close", new Class<?>[]{});
        setting = options.session().settings().stream().filter(v -> v.id.equals("maps.middleClickAction")).findFirst().orElseThrow();
        Dropdown450Screens.openSetting(options, setting);
        SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("options-actions-" + size[0] + ".png"));
      }
    } finally {
      options.removed(); mc.currentScreen = previous;
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    log("DROPDOWN450 staged captures=" + REPORT);
  }

  static void run(Minecraft mc) throws Exception {
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    failures = 0; Files.createDirectories(REPORT);
    var document = PowerConfig.document(); byte[] saved = Files.readAllBytes(PowerConfig.path());
    var base = DefaultBases.current();
    var current = DefaultBases.class.getDeclaredField("current"); current.setAccessible(true);
    var host = new Host(); host.init(mc, 320, 240);
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      test("DROPDOWN450 every live choice has a valid selected-base default and tooltip inventory", () -> {
        var inventory = new ArrayList<Map<String, Object>>();
        for (Setting setting : SettingsRegistry.open(mc).settings()) {
          if (!(boolean)call(null, "accepts", new Class<?>[]{Setting.class}, setting)) continue;
          var options = new ArrayList<Map<String, Object>>();
          for (int i = 0; i < setting.choices.size(); i++) {
            String tip = Tooltips.choice(setting, i), label = setting.choices.get(i);
            check(tip.length() <= 230, setting.id + " tooltip is too long: " + label);
            check(tip.isBlank() || !tip.equalsIgnoreCase(label), setting.id + " tooltip only repeats the label");
            options.add(Map.of("label", label, "tooltip", tip, "omitted", tip.isBlank()));
          }
          var defaults = new LinkedHashMap<String, Integer>();
          for (var selectedBase : DefaultBases.Base.values()) {
            current.set(null, selectedBase); open(host, setting);
            int reset = integer(host.popup, "defaultIndex");
            check(reset >= 0 && reset < setting.choices.size(), "invalid default: " + setting.id + " / " + selectedBase.id);
            check(reset == index(setting, DefaultBases.value(selectedBase, setting.id, setting.powerBetaDefault)),
                "dropdown default disagrees with reset: " + setting.id);
            defaults.put(selectedBase.id, reset);
          }
          inventory.add(Map.of("id", setting.id, "label", setting.label, "settingTooltip", setting.description,
              "options", options, "defaults", defaults));
        }
        Files.writeString(REPORT.resolve("tooltip-inventory.json"), Catalog.JSON.toJson(inventory));
        check(inventory.size() >= 25, "choice inventory missed live backends");
      });
      current.set(null, base);
      test("DROPDOWN450 world-map sidebar retains the dropdown scrollbar until its originating release", () -> Dropdown450Screens.checkSidebar(mc));
      test("DROPDOWN450 F3 presets retain their isolated draft and keyboard selection", () -> Dropdown450Screens.checkF3(mc));
      test("DROPDOWN450 selecting, cancelling and Boolean semantic choices preserve value types", () -> {
        Setting setting = choice(); var before = setting.value.deepCopy(); var edits = new AtomicInteger();
        open(host, setting, edits::incrementAndGet);
        key(host, Keyboard.KEY_DOWN); key(host, Keyboard.KEY_ESCAPE);
        check(setting.value.equals(before) && edits.get() == 0, "Escape changed a value");
        open(host, setting, edits::incrementAndGet);
        key(host, Keyboard.KEY_END); key(host, Keyboard.KEY_RETURN);
        check(setting.value.getAsInt() == setting.choices.size() - 1 && edits.get() == 1, "Enter did not apply one edit");
        Setting toggle = new Setting("creative.sprintToggle", "creative", "Creative", "", "Sprint", "",
            Setting.Kind.BOOLEAN, new JsonPrimitive(false), new JsonPrimitive(true), 0, 1, 1, List.of("Hold", "Toggle"), false);
        open(host, toggle); key(host, Keyboard.KEY_END); key(host, Keyboard.KEY_RETURN);
        check(toggle.value.getAsJsonPrimitive().isBoolean() && toggle.value.getAsBoolean(), "semantic choice corrupted Boolean storage");
      });
      test("DROPDOWN450 overflow wheel, keyboard and final release coordinates share row geometry", () -> {
        var labels = java.util.stream.IntStream.range(0, 80).mapToObj(i -> "Choice " + i).toList();
        var accepted = new AtomicInteger(-1);
        generic(host, labels, 0, 30, 160, accepted::set);
        call(host.popup, "wheel", new Class<?>[]{int.class}, -120);
        check((double)field(host.popup, "scroll") == 40, "wheel did not scroll only the popup");
        field(host.popup, "scroll", 20.9d);
        press(host, integer(host.popup, "x") + 12, integer(host.popup, "y"));
        check(accepted.get() == 1, "fractional scrolling selected a different row from the one drawn");
        generic(host, labels, 0, 30, 160, accepted::set);
        int x = integer(host.popup, "x") + integer(host.popup, "width") - 5, y = integer(host.popup, "y");
        press(host, x, y + 3);
        Object bar = field(host.popup, "bar");
        check((boolean)field(bar, "dragging"), "scrollbar did not retain the press");
        call(host.popup, "release", new Class<?>[]{int.class, int.class}, y + 60, -1);
        call(host.popup, "release", new Class<?>[]{int.class, int.class}, y + 60, 1);
        check((boolean)field(bar, "dragging"), "motion or another button ended the drag");
        call(host.popup, "release", new Class<?>[]{int.class, int.class}, y + integer(host.popup, "rows") * 20, 0);
        check(!(boolean)field(bar, "dragging"), "originating release retained the drag");
        check((double)field(host.popup, "scroll") > 1000, "quick drag ignored its final release coordinates");
        key(host, Keyboard.KEY_HOME); check((double)field(host.popup, "scroll") == 0, "Home did not reveal the first choice");
        key(host, Keyboard.KEY_END); check(integer(host.popup, "focus") == 79, "End lost the final choice");
        key(host, Keyboard.KEY_ESCAPE);
      });
      test("DROPDOWN450 small and large menus stay on screen with delayed item tooltips", () -> {
        for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
          host.init(mc, size[0], size[1]); open(host, choice());
          int x = integer(host.popup, "x"), y = integer(host.popup, "y"), width = integer(host.popup, "width"), rows = integer(host.popup, "rows");
          check(x >= 0 && y >= 0 && x + width <= size[0] && y + rows * 20 <= size[1], "popup escaped the screen");
          host.pointerX = x + 14; host.pointerY = y + 8;
          SettingsSnapshot.render(mc, host, size[0], size[1], REPORT.resolve("choices-" + size[0] + ".png"));
          Object hover = field(host.popup, "hover");
          check((int)field(hover, "ticks") == 0, "tooltip appeared before the hover delay");
          for (int i = 0; i < 13; i++) call(host.popup, "tick", new Class<?>[]{});
          SettingsSnapshot.render(mc, host, size[0], size[1], REPORT.resolve("tooltip-" + size[0] + ".png"));
          check((int)field(hover, "ticks") == 13, "tooltip delay did not use ticks");
          var labels = java.util.stream.IntStream.range(0, 80).mapToObj(i -> "Choice " + i).toList();
          generic(host, labels, 0, size[1] - 32, 180, i -> {});
          host.pointerX = host.pointerY = -1000;
          SettingsSnapshot.render(mc, host, size[0], size[1], REPORT.resolve("overflow-" + size[0] + ".png"));
        }
      });
      check(document.equals(PowerConfig.document()) && Arrays.equals(saved, Files.readAllBytes(PowerConfig.path())),
          "dropdown checks changed saved settings");
    } finally {
      current.set(null, base); call(host.popup, "close", new Class<?>[]{}); host.removed();
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode);
    }
    log("DROPDOWN450 failures=" + failures + " reports=" + REPORT);
    check(failures == 0, "dropdown-450 failures=" + failures);
  }
  private static int index(Setting setting, com.google.gson.JsonElement value) { return setting.kind == Setting.Kind.BOOLEAN ? value.getAsBoolean() ? 1 : 0 : value.getAsInt(); }
  private static Setting choice() { return new Setting("maps.caveMode", "maps", "Map", "", "Cave mode", "",
      Setting.Kind.CHOICE, new JsonPrimitive(0), new JsonPrimitive(1), 0, 2, 1, List.of("Surface", "Automatic", "Manual"), false); }
  private static void open(Host host, Setting setting) throws Exception { open(host, setting, () -> {}); }
  private static void open(Host host, Setting setting, Runnable changed) throws Exception {
    call(host.popup, "open", new Class<?>[]{Setting.class, int.class, int.class, int.class, int.class, int.class, Runnable.class},
        setting, host.width - 172, 55, 160, host.width, host.height, changed);
  }
  private static void generic(Host host, List<String> labels, int selected, int top, int span, IntConsumer accepted) throws Exception {
    call(host.popup, "open", new Class<?>[]{List.class, List.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, IntConsumer.class},
        labels, List.of(), selected, -1, 10, top, span, host.width, host.height, accepted);
  }
  private static void key(Host host, int key) throws Exception { call(host.popup, "key", new Class<?>[]{UiScreen.class, int.class}, host, key); }
  private static void press(Host host, int x, int y) throws Exception { call(host.popup, "press", new Class<?>[]{UiScreen.class, int.class, int.class, int.class}, host, x, y, 0); }
  private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
    Method method = TYPE.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(target, args);
  }
  private static Object field(Object target, String name) throws Exception { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
  private static void field(Object target, String name, Object value) throws Exception { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target, value); }
  private static int integer(Object target, String name) throws Exception { return (int)field(target, name); }
}
