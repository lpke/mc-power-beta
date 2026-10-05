package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonParser;
import java.util.List;
import local.luke.power.config.Setting;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class FogArrayChecks {
  private static void click(Object screen, int x, int y) {
    ((ScreenInput) screen).power$click(x, y, 0);
  }

  private static void open(PowerOptionsScreen options, Setting setting) throws Exception {
    var activate = PowerOptionsScreen.class.getDeclaredMethod("activate", Setting.class, int.class);
    activate.setAccessible(true);
    activate.invoke(options, setting, 1);
  }

  public static void run(Minecraft mc) throws Exception {
    failures = 0;
    MenuPreferences.update(v -> v.autoApply = false);
    var options = MenuUpdateChecks.open(mc, "Video");
    search(options, "fog key");
    Setting setting = find(options.session(), "video.fogCycle");
    var original = setting.value.deepCopy();
    try {
      setting.value = JsonParser.parseString("[12,8,4,2]");
      open(options, setting);
      check(mc.currentScreen instanceof IntegerArrayScreen, "JSON editor still opens");
      var editor = (IntegerArrayScreen) mc.currentScreen;
      var draft = (IntegerArrayDraft) field(editor, "draft");
      test("fog array renders at all supported menu sizes", () -> {
        for (int[] size : new int[][]{{320,240},{427,240},{550,380},{854,480}}) {
          editor.init(mc, size[0], size[1]);
          editor.render(-1, -1, 0);
          check(GL11.glGetError() == 0, "OpenGL error at " + size[0]);
        }
        mc.setScreen(editor);
      });
      test("fog array sliders edit only their local draft", () -> {
        click(editor, (int) call(editor, "sliderX") + (int) call(editor, "sliderWidth") - 2, 78);
        field(editor, "dragging", -1);
        check(draft.get(0) == 32, "slider did not reach maximum");
        check(setting.value.toString().equals("[12,8,4,2]"), "slider published before Done");
      });
      test("fog exact entry validates range inline", () -> {
        ((ScreenInput)(Object)editor).power$click((int)call(editor,"sliderX")+8,78,1);
        var exact=field(editor,"valueEditor");
        check((boolean)call(exact,"active"),"exact editor missing");
        ((TextInput)field(exact,"input")).setText("33");call(exact,"commit");
        check(mc.currentScreen==editor&&draft.get(0)==32&&(boolean)call(exact,"active"),"invalid exact value accepted");
        ((TextInput)field(exact,"input")).setText("16");call(exact,"commit");
        check(mc.currentScreen==editor&&draft.get(0)==16&&!(boolean)call(exact,"active"),"exact edit not retained");
      });
      test("fog duplicate values block Done until fixed", () -> {
        draft.set(0, 8);
        call(editor, "save");
        check(mc.currentScreen == editor, "duplicate accepted");
        draft.set(0, 16);
      });
      test("fog add reorder remove defaults and cancel preserve parent", () -> {
        int right = (int) call(editor, "right");
        click(editor, right - 40, 78);
        check(draft.values().equals(List.of(8,16,4,2)), "reorder failed");
        click(editor, right - 20, 78);
        check(draft.values().equals(List.of(16,4,2)), "remove failed");
        click(editor, (int) call(editor, "left") + 10, editor.height - 44);
        check(draft.size() == 4, "add failed");
        click(editor, (int) call(editor, "left") + 110, editor.height - 44);
        check(draft.values().equals(List.of(12,8,4,2)), "defaults failed");
        draft.set(0, 24);
        click(editor, editor.width / 2 + 12, editor.height - 18);
        check(mc.currentScreen == options, "Cancel lost parent");
        check(setting.value.toString().equals("[12,8,4,2]"), "Cancel published draft");
      });
      test("fog Done previews valid cycle and retains settings search", () -> {
        open(options, setting);
        var next = (IntegerArrayScreen) mc.currentScreen;
        ((IntegerArrayDraft) field(next, "draft")).set(0, 24);
        call(next, "save");
        check(mc.currentScreen == options, "Done lost parent");
        check(setting.value.toString().equals("[24,8,4,2]"), "Done lost values");
        check(((TextInput) field(options, "search")).text().equals("fog key"), "Done lost filter");
        check(local.luke.power.video.VideoConfig.copy().fogCycle.equals(List.of(24,8,4,2)), "cycle not live");
      });
    } finally {
      setting.value = original;
      options.changed(setting);
      mc.setScreen(options);
    }
    log("FOG ARRAY FAILURES " + failures);
  }
}
