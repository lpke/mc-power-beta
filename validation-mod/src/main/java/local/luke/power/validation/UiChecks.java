package local.luke.power.validation;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.visual.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;

import static local.luke.power.validation.Validation.*;

public final class UiChecks {
  static Object field(Object o, String key) throws Exception {
    Field f = o.getClass().getDeclaredField(key); f.setAccessible(true); return f.get(o);
  }
  static void field(Object o, String key, Object value) throws Exception {
    Field f = o.getClass().getDeclaredField(key); f.setAccessible(true); f.set(o, value);
  }
  static Object call(Object o, String key) throws Exception {
    Method m = o.getClass().getDeclaredMethod(key); m.setAccessible(true); return m.invoke(o);
  }
  static void search(PowerOptionsScreen s, String value) throws Exception {
    ((TextInput)field(s, "search")).setText(value); call(s, "layout");
  }
  static void click(PowerOptionsScreen s, int x, int y) { ((ScreenInput)(Object)s).power$click(x,y,0); }

  static void run(Minecraft mc) throws Exception {
    ConfigSession before = SettingsRegistry.open(mc);
    int oldScale = find(before, "native.guiScale").value.getAsInt();
    var parent = mc.currentScreen;
    PowerOptionsScreen s = new PowerOptionsScreen(parent); mc.setScreen(s);
    byte[] options = Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
    test("all settings have useful title-free explanations", () -> {
      for (Setting v : s.session().settings()) {
        check(!v.description.isBlank(), "Missing help: " + v.id);
        check(!v.description.equalsIgnoreCase(v.label), "Repeated title: " + v.id);
        check(!v.description.contains("Left click increases"), "Repeated usage: " + v.id);
      }
    });
    test("live GUI scale resizes existing screen without saving", () -> {
      Setting scale = find(s.session(), "native.guiScale"); scale.value = new JsonPrimitive(1); s.changed(scale);
      check(mc.currentScreen == s && s.width == mc.displayWidth, "screen did not resize");
      check(Arrays.equals(options, Files.readAllBytes(local.luke.power.storage.PowerConfig.path())), "preview saved options");
      scale.value = new JsonPrimitive(2); s.changed(scale);
      check(s.width == (mc.displayWidth + 1) / 2, "2x width incorrect");
    });
    test("audio and invert preview never write options", () -> {
      Setting music = find(s.session(), "native.music"); music.value = new JsonPrimitive(27); s.changed(music);
      check(Math.abs(mc.options.musicVolume - .27) < .0001, "music is not live");
      Setting invert = find(s.session(), "native.invert"); invert.cycle(1); s.changed(invert);
      check(Arrays.equals(options, Files.readAllBytes(local.luke.power.storage.PowerConfig.path())), "preview saved options");
    });
    test("base texture preview is live and does not save", () -> {
      Setting pack = find(s.session(), "native.texturePack"); pack.value = new JsonPrimitive("Alpha.zip"); s.changed(pack);
      check(!mc.field_2768.field_1175.field_1137.equals("Default"), "base pack not previewed");
      check(Arrays.equals(options, Files.readAllBytes(local.luke.power.storage.PowerConfig.path())), "pack preview saved options");
    });
    test("legacy soundtrack toggle filters playback immediately", () -> {
      Setting toggle = find(s.session(), "power_environment:config.MUSIC_CONFIG.disableDefaultMinecraftBGM");
      toggle.value = new JsonPrimitive(true); s.changed(toggle);
      check(local.luke.power.audio.AudioController.choose(java.util.List.of(
          new net.minecraft.class_267("calm1.ogg", new java.net.URL("file:/tmp/music.ogg")))) == null,
          "disabled soundtrack remains eligible");
      toggle.value = new JsonPrimitive(false); s.changed(toggle);
      check(local.luke.power.audio.AudioController.choose(java.util.List.of(
          new net.minecraft.class_267("calm1.ogg", new java.net.URL("file:/tmp/music.ogg")))) != null,
          "soundtrack needs restart to re-enable");
    });
    test("changes filter includes only changed values", () -> {
      field(s, "changedOnly", true); call(s,"layout");
      for (Object row : (List<?>)field(s,"rows")) {
        Setting setting = (Setting)call(row,"setting");
        check(setting == null || setting.changed(), "unchanged row in filter");
      }
    });
    test("discard restores live audio and GUI scale", () -> {
      s.session().discard();
      check(Math.abs(mc.options.musicVolume - find(before,"native.music").value.getAsFloat()/100) < .0001,"volume not restored");
      check(find(SettingsRegistry.open(mc),"native.guiScale").value.getAsInt() == oldScale,"scale not restored");
    });
    test("Apply keeps current page search and scroll", () -> {
      field(s,"changedOnly",false); field(s,"page","Audio"); search(s,"volume");
      Setting volume=find(s.session(),"native.music"); volume.value = new JsonPrimitive(37); s.changed(volume);
      double scroll = (double)field(s,"scroll");
      Method ax=s.getClass().getDeclaredMethod("actionX",int.class);ax.setAccessible(true);
      click(s,(int)ax.invoke(s,1)+5,s.height-20);
      check(mc.currentScreen==s,"Apply closed screen");
      check(s.session().changes()==0,"Apply left unsaved values");
      check(field(s,"page").equals("Audio") && ((TextInput)field(s,"search")).text().equals("volume") && field(s,"scroll").equals(scroll),"Apply moved location");
      check(Math.abs(local.luke.power.storage.PowerConfig.section("native").get("music").getAsDouble()-.37)<.0001,"Apply did not save");
    });
    // Return every saved value to the initial state.
    ConfigSession restore=SettingsRegistry.open(mc);
    for (Setting v:restore.settings()) v.value=find(before,v.id).value.deepCopy();
    restore.save(Path.of(".").toAbsolutePath());
    mc.setScreen(parent);
    log("UI CHECKS COMPLETE");
  }
}
