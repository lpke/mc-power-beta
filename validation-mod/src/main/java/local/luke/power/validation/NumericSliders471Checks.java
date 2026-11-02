package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.MusicPlaylist;
import local.luke.power.config.Setting;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Real pointer slider and right-click exact-entry checks on isolated Options drafts. */
final class NumericSliders471Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-471/numeric-sliders");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private static final List<String> IDS = List.of("audio.musicFadeMillis", "audio.mp3ConversionLimit");
  private NumericSliders471Checks() {}
  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "run").contains(action), "use numeric-sliders-471 capture|run");
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0 && !Mouse.isButtonDown(0),
        "focus and mute the disposable client, then release mouse buttons");
    check(Display.getWidth() >= 1708 && Display.getHeight() >= 960, "resize disposable display to at least 1708x960");
    Files.createDirectories(REPORT); failures = 0;
    try (var state = new Release462Checks.State(mc)) {
      for (int[] size : SIZES) for (String id : IDS) {
        var options = open(state, size, id); Setting setting = find(options, id);
        SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve(id.substring(6) + "-" + size[0] + ".png"));
        if (action.equals("run")) test("NUMERIC SLIDERS471 native drag/exact entry " + id + " " + size[0], () -> {
          check(setting.sliderStep == 10 && setting.step == 1, "audio drag/exact step not independent");
          Object row = row(options, id); int left = (int)call(options, "mainControlLeft", new Class<?>[]{row.getClass()}, row);
          int right = (int)call(options, "right") - 38;
          int y = (int)call(options, "top") + (int)call(row, "y") - ((Double)get(options, "scroll")).intValue()
              + ((int)call(row, "height") == 40 ? 16 : 3) + 8;
          for (double point : new double[]{.12, .37, .63, .86}) {
            int x = left + 4 + (int)Math.round(point * Math.max(1, right - left - 8));
            click(options, x, y, 0); call(options, "panelRelease");
            check(setting.value.getAsInt() % 10 == 0, "native pointer drag produced a non-ten value");
          }
          click(options, left + 4, y, 0); call(options, "panelRelease"); check(setting.value.getAsDouble() == setting.min, "minimum endpoint lost");
          click(options, right - 4, y, 0); call(options, "panelRelease"); check(setting.value.getAsDouble() == setting.max,
              "maximum endpoint lost value="+setting.value+" x="+(right-4)+" physical="+mc.displayWidth+"/"+options.width
                  +" anchors="+get(options,"dragLeft")+"+"+get(options,"dragSpan"));
          click(options, left + 8, y, 1); type(options, "137"); key(options, '\0', Keyboard.KEY_RETURN);
          check(setting.value.getAsInt() == 137 && !(boolean)call(get(options, "valueEditor"), "active"), "right-click precise entry was rounded");
          click(options, left + 8, y, 1); type(options, "999"); key(options, '\0', Keyboard.KEY_ESCAPE);
          check(setting.value.getAsInt() == 137, "exact-entry Cancel changed the value");
          click(options, left + 8, y, 1); type(options, "137.5"); key(options, '\0', Keyboard.KEY_RETURN);
          check(setting.value.getAsInt() == 137 && (boolean)call(get(options, "valueEditor"), "active"), "integer entry accepted fractions");
          key(options, '\0', Keyboard.KEY_ESCAPE);
          if (id.equals("audio.musicFadeMillis")) {
            click(options, left + 8, y, 1); type(options, "9999"); key(options, '\0', Keyboard.KEY_RETURN);
            check(setting.value.getAsInt() == 9999, "extended fade range rejected precise value above5000");
          }
          click(options, left + 8, y, 1);
          SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve(id.substring(6) + "-exact-" + size[0] + ".png"));
          key(options, '\0', Keyboard.KEY_ESCAPE);
        });
      }
    }
    Files.writeString(REPORT.resolve(action + ".json"), "{\"failures\":" + failures + "}");
    log("NUMERIC SLIDERS471 " + action + " failures=" + failures); check(failures == 0, "numeric-sliders-471 failures=" + failures);
  }
  private static PowerOptionsScreen open(Release462Checks.State state, int[] size, String id) throws Exception {
    PowerOptionsScreen options = state.open(size, MusicPlaylist.BETA, false);
    ((TextInput)get(options, "search")).setText(find(options, id).label); ((Set<?>)get(options, "collapsed")).clear();
    call(options, "layout"); return options;
  }
  private static Setting find(PowerOptionsScreen options, String id) { return options.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow(); }
  private static Object row(PowerOptionsScreen options, String id) throws Exception {
    for (Object row : (List<?>)get(options, "rows")) { Setting setting = (Setting)call(row, "setting"); if (setting != null && setting.id.equals(id)) return row; }
    throw new AssertionError("Missing numeric setting row " + id);
  }
  private static void type(PowerOptionsScreen options, String text) { for (char character : text.toCharArray()) key(options, character, Keyboard.KEY_NONE); }
  private static void click(PowerOptionsScreen options, int x, int y, int button) { ((ScreenInput)(Object)options).power$click(x, y, button); }
  private static void key(PowerOptionsScreen options, char c, int key) { ((ScreenInput)(Object)options).power$key(c, key); }
  private static Object get(Object value, String name) throws Exception { Field f = value.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(value); }
  private static Object call(Object value, String name) throws Exception { return call(value, name, new Class<?>[0]); }
  private static Object call(Object value, String name, Class<?>[] types, Object... args) throws Exception { Method m = value.getClass().getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(value, args); }
}
