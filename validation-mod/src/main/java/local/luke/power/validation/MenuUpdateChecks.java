package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_525;
import net.minecraft.client.Minecraft;

public final class MenuUpdateChecks {
  static int number(Object o, String name, int value) throws Exception {
    Method m = o.getClass().getDeclaredMethod(name, int.class);
    m.setAccessible(true);
    return (int) m.invoke(o, value);
  }

  static void clickScreen(net.minecraft.client.gui.screen.Screen s, int x, int y, int b) {
    ((ScreenInput) (Object) s).power$click(x, y, b);
  }

  static PowerOptionsScreen open(Minecraft mc, String page) throws Exception {
    PowerOptionsScreen s = new PowerOptionsScreen(null);
    mc.setScreen(s);
    field(s, "page", page);
    field(s, "libraryOpen", false);
    field(s, "changedOnly", false);
    field(s, "conflictIds", List.of());
    field(s, "relatedIds", List.of());
    ((Set<?>) field(s, "collapsed")).clear();
    search(s, "");
    return s;
  }

  public static void persisted(Minecraft mc) throws Exception {
    test("auto-apply and direct pause persist across process restart", () -> {
      check(MenuPreferences.current().autoApply && MenuPreferences.pauseToOptions(), "menu preferences lost on restart");
      mc.setScreen(null); mc.method_2135();
      check(mc.currentScreen instanceof PowerOptionsScreen && (boolean) field(mc.currentScreen, "directPause"), "direct pause not restored");
    });
    test("independent quality, fog cycle and frame cap persist across restart", () -> {
      var video = local.luke.power.video.VideoConfig.current();
      check(!video.leaves && !video.clouds && video.fogCycle.equals(List.of(20, 6)), "video preferences lost");
      check(find(SettingsRegistry.open(mc), "native.fpsLimit").value.getAsInt() == 850, "frame cap lost");
    });
  }

  public static void run(Minecraft mc) throws Exception {
    failures = 0;
    MenuPreferences.update(v -> v.autoApply = false);
    ConfigSession before = SettingsRegistry.open(mc);
    PowerOptionsScreen s = open(mc, "Audio");
    test(
        "fuzzy search finds misspelled brightness",
        () -> {
          search(s, "brighness");
          check(
              ((List<?>) field(s, "rows"))
                  .stream()
                      .anyMatch(
                          r -> {
                            try {
                              return call(r, "setting") instanceof Setting v
                                  && v.id.equals("native.brightness");
                            } catch (Exception e) {
                              throw new RuntimeException(e);
                            }
                          }),
              "misspelled query missed brightness");
        });
    test(
        "search and result scroll survive closing and reopening",
        () -> {
          search(s, "volume");
          field(s, "scroll", 48d);
          call(s, "layout");
          double scroll = (double) field(s, "scroll");
          mc.setScreen(null);
          PowerOptionsScreen reopened = new PowerOptionsScreen(null);
          mc.setScreen(reopened);
          check(((TextInput) field(reopened, "search")).text().equals("volume"), "query lost");
          check(field(reopened, "scroll").equals(scroll), "search scroll lost");
        });
    PowerOptionsScreen video = open(mc, "Video");
    test(
        "search clear is absent without filters",
        () -> check(!(boolean) call(video, "canClear"), "clear button visible"));
    test(
        "separate quality settings and 1000 FPS limit",
        () -> {
          check(
              video.session().settings().stream().noneMatch(v -> v.id.equals("native.fancy")),
              "duplicate fancy option");
          Class<?> mod = Class.forName("local.luke.power.controls.util.ModOptions");
          Setting fps = find(video.session(), "native.fpsLimit");
          fps.value = new JsonPrimitive(1000);
          video.changed(fps);
          check(
              (int) mod.getMethod("getFpsLimitValue").invoke(null) == 1000, "1000 cap not applied");
          check(
              (boolean) mod.getMethod("isFramerateLimited").invoke(null),
              "1000 incorrectly unlimited");
          fps.value = new JsonPrimitive(1005);
          video.changed(fps);
          check(
              !(boolean) mod.getMethod("isFramerateLimited").invoke(null)
                  && fps.display().equals("Unlimited"),
              "unlimited cap missing");
          for (Setting v : video.session().settings())
            if (v.id.startsWith("video.") && v.kind == Setting.Kind.BOOLEAN) {
              v.cycle(1);
              video.changed(v);
            }
          check(((String) field(video, "error")).isEmpty(), "quality preview failed");
        });
    test(
        "legacy frame caps migrate and new caps survive native save/load",
        () -> {
          JsonObject original = PowerConfig.section("native"), legacy = original.deepCopy();
          try {
            legacy.remove("framerate_limit");
            legacy.addProperty("fps_limit", .4);
            PowerConfig.put("native", legacy);
            NativeStorage.load(mc.options);
            check(
                find(SettingsRegistry.open(mc), "native.fpsLimit").value.getAsInt() == 125,
                "legacy default cap not migrated");
            legacy.addProperty("fps_limit", .52816904);
            PowerConfig.put("native", legacy);
            NativeStorage.load(mc.options);
            check(
                find(SettingsRegistry.open(mc), "native.fpsLimit").value.getAsInt() == 160,
                "configured cap not migrated");
            ConfigSession cap = SettingsRegistry.open(mc);
            find(cap, "native.fpsLimit").value = new JsonPrimitive(850);
            cap.save(Path.of(".").toAbsolutePath());
            NativeStorage.load(mc.options);
            check(
                find(SettingsRegistry.open(mc), "native.fpsLimit").value.getAsInt() == 850
                    && !PowerConfig.section("native").has("fps_limit"),
                "new cap failed round trip");
          } finally {
            PowerConfig.put("native", original);
            NativeStorage.load(mc.options);
          }
        });
    test(
        "custom fog cycle is live and reversible",
        () -> {
          Setting fog = find(video.session(), "video.fogCycle");
          fog.parse("[24, 10, 3]");
          video.changed(fog);
          Class<?> mod = Class.forName("local.luke.power.controls.util.ModOptions");
          mod.getMethod("setRenderDistanceChunks", int.class).invoke(null, 24);
          mod.getMethod("cycleRenderDistance").invoke(null);
          check(
              (int) mod.getMethod("getRenderDistanceChunks").invoke(null) == 10,
              "custom fog cycle ignored");
          fog.reset();
          video.changed(fog);
          check(
              Arrays.equals(
                  (int[]) mod.getField("renderDistanceCycle").get(null), new int[] {12, 8, 4, 2}),
              "Beta defaults incorrect");
        });
    test(
        "video preview hides and restores without leaving options",
        () -> {
          clickScreen(video, (int) call(video, "disabledX") + 4, video.height - 20, 0);
          check((boolean) field(video, "hidden"), "preview did not hide UI");
          video.render(0, 0, 0);
          clickScreen(video, (int) call(video, "disabledX") + 4, video.height - 20, 0);
          check(
              !(boolean) field(video, "hidden") && mc.currentScreen == video,
              "preview changed screens");
        });
    video.session().discard();
    test(
        "Apply right-click persists auto-apply and continuous edits preview without disk churn",
        () -> {
          clickScreen(video, number(video, "actionX", 1) + 4, video.height - 20, 1);
          check(
              MenuPreferences.current().autoApply
                  && PowerConfig.section("interface").get("autoApply").getAsBoolean(),
              "auto preference not saved");
          Setting volume = find(video.session(), "native.music");
          volume.value = new JsonPrimitive(41);
          video.changed(volume);
          check(
              video.session().changes() == 0 && Math.abs(mc.options.musicVolume - .41) < .001,
              "auto edit not applied");
          byte[] bytes = Files.readAllBytes(PowerConfig.path());
          volume.value = new JsonPrimitive(42);
          video.changed(volume, true);
          check(Math.abs(mc.options.musicVolume - .42) < .001, "drag preview delayed");
          check(
              Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())),
              "drag wrote config every movement");
          video.finishContinuousChange();
          check(video.session().changes() == 0, "slider release not saved");
          clickScreen(video, number(video, "actionX", 1) + 4, video.height - 20, 1);
        });
    test(
        "pause preference and colour swatches save independently",
        () -> {
          Setting pause = find(video.session(), "interface.pauseToOptions");
          pause.value = new JsonPrimitive(true);
          video.changed(pause);
          boolean saved =
              PowerConfig.section("interface").has("pauseToOptions")
                  && PowerConfig.section("interface").get("pauseToOptions").getAsBoolean();
          MenuPreferences.update(
              v -> {
                if (!v.colours.contains("#123456")) v.colours.add("#123456");
              });
          check(
              PowerConfig.section("interface").get("pauseToOptions").getAsBoolean() == saved,
              "swatch accidentally saved pause draft");
          check(MenuPreferences.pauseToOptions(), "saving swatch reverted live pause preview");
          mc.setScreen(null);
          mc.method_2135();
          check(
              mc.currentScreen instanceof PowerOptionsScreen
                  && (boolean) field(mc.currentScreen, "directPause"),
              "pause did not open options");
          PowerOptionsScreen direct = (PowerOptionsScreen) mc.currentScreen;
          clickScreen(
              direct, (int) call(direct, "origin") + 10, (int) call(direct, "footerY") + 5, 0);
          check(mc.currentScreen instanceof class_525, "Menu did not open normal pause menu");
          video.session().discard();
        });
    test(
        "colour picker previews locally and cancels safely",
        () -> {
          Setting colour = find(video.session(), "lightOverlay.lowColor");
          String old = colour.value.getAsString();
          ColourScreen picker = new ColourScreen(video, colour);
          mc.setScreen(picker);
          ((TextInput) field(picker, "hex")).setText("#123456");
          call(picker, "parse");
          check(colour.value.getAsString().equals(old), "colour changed before Done");
          picker.render(0, 0, 0);
          clickScreen(picker, picker.width / 2 + 5, picker.height - 20, 0);
          check(
              mc.currentScreen == video && colour.value.getAsString().equals(old),
              "Cancel changed colour");
        });
    ConfigSession restore = SettingsRegistry.open(mc);
    for (Setting v : restore.settings()) v.value = find(before, v.id).value.deepCopy();
    restore.save(Path.of(".").toAbsolutePath());
    MenuPreferences.update(
        v -> {
          v.autoApply = false;
          v.colours.remove("#123456");
        });
    mc.setScreen(null);
    log("MENU UPDATE FAILURES " + failures);
  }
}
