package local.luke.power.validation;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;

public final class Validation {
  private static final Path COMMAND = Path.of("power-beta-validation.command"),
      REPORT = Path.of("power-beta-validation.log");
  private static int ticks;
  static int failures;

  interface Check {
    void run() throws Exception;
  }

  static void log(String s) throws Exception {
    Files.writeString(REPORT, s + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    System.out.println("POWER VALIDATION " + s);
  }

  static void test(String name, Check c) throws Exception {
    try {
      c.run();
      log("PASS " + name);
    } catch (Throwable e) {
      failures++;
      log("FAIL " + name + " " + e);
      e.printStackTrace();
    }
  }

  static void check(boolean okay, String message) {
    if (!okay) throw new AssertionError(message);
  }

  public static void tick(Minecraft mc) {
    if (++ticks % 5 != 0 || !Files.exists(COMMAND)) return;
    try {
      String command = Files.readString(COMMAND).trim();
      Files.delete(COMMAND);
      if (command.equals("cheats-creation")) CheatsChecks.creation(mc);
      else if (command.equals("cheats")) CheatsChecks.run(mc);
      else if (command.equals("modern-commands")) ModernCommandChecks.run(mc);
      else if (command.startsWith("audio-presets-")) AudioPresetChecks.run(mc,command.substring(14));
      else if (command.startsWith("audio-groups-")) AudioGroupChecks.run(mc,command.substring(13));
      else if (command.equals("audio-maintenance")) AudioMaintenanceChecks.run(mc);
      else if (command.equals("inline-values")) InlineValueChecks.run(mc);
      else if (command.equals("sticky-groups")) StickyGroupChecks.run(mc);
      else if (command.equals("audio-refinements")) AudioRefinementChecks.run(mc);
      else if (command.startsWith("music-eras-")) MusicErasChecks.run(mc,command.substring(11));
      else if (command.startsWith("audio-polish-")) AudioPolishChecks.run(mc,command.substring(13));
      else if (command.startsWith("audio-layout-")) AudioLayoutChecks.run(mc,command.substring(13));
      else if (command.equals("fog-array")) FogArrayChecks.run(mc);
      else if (command.startsWith("library-check-")) CommandsLibraryChecks.run(mc,command.substring(14));
      else if (command.equals("menu-persisted")) MenuUpdateChecks.persisted(mc);
      else if (command.startsWith("music-update-")) MusicUpdateChecks.run(mc,command.substring(13));
      else if (command.equals("menu-update")) MenuUpdateChecks.run(mc);
      else if (command.equals("equipment-swap")) LightInventoryChecks.inventory(mc);
      else if (command.startsWith("overlay-")) LightInventoryChecks.light(mc,command.substring(8));
      else if (command.equals("split-restart-start") || command.equals("split-restart-finish")) ChestJoinChecks.restart(mc,command.endsWith("finish"));
      else if (command.startsWith("preview-audio-")) PreviewPickerChecks.audio(mc,command.substring(14));
      else if (command.equals("pack-picker")) PreviewPickerChecks.picker(mc);
      else if (command.equals("filter-navigation")) FilteredNavigationChecks.run(mc);
      else if (command.equals("chest-join")) ChestJoinChecks.run(mc);
      else if (command.equals("carry-hold") || command.equals("carry-release")) NavigationCarryChecks.hold(mc,command.equals("carry-release"));
      else if (command.equals("carry-resume-check")) NavigationCarryChecks.resumed(mc);
      else if (command.equals("navigation-check")) NavigationCarryChecks.menu(mc);
      else if (command.equals("double-carry") || command.equals("carry-demo")) NavigationCarryChecks.carry(mc,command.equals("carry-demo"));
      else if (command.startsWith("feature-")) FeatureChecks.run(mc,command.substring(8));
      else if (command.startsWith("revision-")) RevisionChecks.run(mc,command.substring(9));
      else if (command.startsWith("keys-")) InputChecks.run(mc, command.substring(5));
      else if (command.equals("chat-check")) ChatChecks.run(mc);
      else if (command.startsWith("chat ")) ChatChecks.submit(mc, command.substring(5));
      else if (command.equals("audit")) audit(mc);
      else if (command.equals("ui-check")) UiChecks.run(mc);
      else if (command.equals("texture-check")) TextureChecks.run(mc);
      else if (command.equals("roundtrip")) roundtrip(mc);
      else if (command.equals("screens")) screens(mc);
      else if (command.startsWith("options")) {
        PowerOptionsScreen s = new PowerOptionsScreen(mc.currentScreen);
        if (command.length() > 8) {
          Field p = PowerOptionsScreen.class.getDeclaredField("page");
          p.setAccessible(true);
          p.set(s, command.substring(8));
        }
        mc.setScreen(s);
      } else if (command.equals("audio")) audio(mc);
      else if (command.equals("input")) input(mc);
      else if (command.startsWith("set ")) {
        String[] parts = command.split(" ", 3);
        ConfigSession s = SettingsRegistry.open(mc);
        find(s, parts[1]).value = JsonParser.parseString(parts[2]);
        s.save(Path.of(".").toAbsolutePath());
      } else if ((command.equals("defaults-check") || command.equals("profile-defaults"))) {
        failures = 0;
        ConfigSession s = SettingsRegistry.open(mc);
        for (Setting setting : s.settings())
          test(
              "pack default " + setting.id,
              () ->
                  check(
                      (command.equals("profile-defaults") && setting.id.equals("native.guiScale") && setting.value.getAsInt()==4)
                          || (command.equals("profile-defaults") && setting.id.equals("native.sensitivity") && Math.abs(setting.value.getAsDouble()-80)<.001)
                          || setting.value.equals(setting.defaultValue)
                          || setting.value.isJsonPrimitive()
                              && setting.value.getAsJsonPrimitive().isNumber()
                              && Math.abs(
                                      setting.value.getAsDouble()
                                          - setting.defaultValue.getAsDouble())
                                  < .000001,
                      "Expected " + setting.defaultValue + " got " + setting.value));
        log("DEFAULT CHECK TOTAL " + s.settings().size() + " FAILURES " + failures);
      } else if (command.equals("new-world")) {
        mc.interactionManager = new net.minecraft.SingleplayerInteractionManager(mc);
        mc.method_2120("Power Beta release check", "Power Beta release check", 17320261003L);
        mc.setScreen(null);
      } else if (command.equals("quit")) {
        mc.setWorld(null);
        mc.scheduleStop();
      } else if (command.startsWith("music-")) music(mc, command.substring(6));
      else if (command.equals("pause")) mc.setScreen(new net.minecraft.class_525());
      else if (command.equals("defaults")) {
        ConfigSession s = SettingsRegistry.open(mc);
        s.settings().forEach(Setting::reset);
        s.save(Path.of(".").toAbsolutePath());
        log("DEFAULTS SAVED " + s.settings().size());
      } else if (command.equals("dump"))
        Files.writeString(
            Path.of("power-beta-full-catalog.json"),
            Catalog.JSON.toJson(SettingsRegistry.open(mc).settings()));
      else if (command.equals("title")) {
        mc.setWorld(null);
        mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
      }
      log("COMMAND DONE " + command);
    } catch (Throwable e) {
      try {
        log("COMMAND FAILED " + e);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
    }
  }

  static void audit(Minecraft mc) throws Exception {
    failures = 0;
    ConfigSession session = SettingsRegistry.open(mc);
    Set<String> ids = new HashSet<>();
    int n = 0;
    for (Setting s : session.settings()) {
      test(
          "schema " + s.id,
          () -> {
            check(ids.add(s.id), "duplicate");
            s.validate(s.defaultValue);
            s.validate(s.value);
            check(PowerOptionsScreen.PAGES.contains(s.page), "unknown page");
            check(
                !s.label.matches("(?i).*(ControlFeatures|MojangFix|Lpke|key\\.).*"),
                "untranslated label");
          });
      n++;
      if (s.kind == Setting.Kind.BOOLEAN
          || s.kind == Setting.Kind.CHOICE
          || s.kind == Setting.Kind.INTEGER
          || s.kind == Setting.Kind.DECIMAL) {
        JsonElement old = s.value.deepCopy();
        s.cycle(1);
        s.cycle(-1);
        check(
            s.value.equals(old) || Math.abs(s.value.getAsDouble() - old.getAsDouble()) < 0.000001,
            "inverse cycle " + s.id);
        s.value = old;
      }
    }
    log("AUDIT TOTAL " + n + " FAILURES " + failures);
  }

  static Setting find(ConfigSession s, String id) {
    return s.settings().stream().filter(e -> e.id.equals(id)).findFirst().orElseThrow();
  }

  static void roundtrip(Minecraft mc) throws Exception {
    failures = 0;
    ConfigSession s = SettingsRegistry.open(mc);
    Map<String, JsonElement> old = new LinkedHashMap<>();
    Map<String, JsonElement> edits = new LinkedHashMap<>();
    edits.put("native.music", new JsonPrimitive(43));
    edits.put("audio.category.blocks", new JsonPrimitive(35));
    edits.put("creative.glide", new JsonPrimitive(2));
    edits.put("tweaks.freeLook", new JsonPrimitive(true));
    edits.put("worldedit.opacity", new JsonPrimitive(70));
    edits.put("power_hud:config.chatHistorySize", new JsonPrimitive(101));
    edits.put("logo.logo.animation.enabled", new JsonPrimitive(false));
    Object[] inventory = mc.player == null ? null : mc.player.inventory.main.clone();
    for (var e : edits.entrySet()) {
      Setting a = find(s, e.getKey());
      old.put(e.getKey(), a.value.deepCopy());
      a.value = e.getValue();
    }
    try {
      s.save(Path.of(".").toAbsolutePath());
      ConfigSession reread = SettingsRegistry.open(mc);
      for (var e : edits.entrySet())
        test(
            "save and reload " + e.getKey(),
            () -> {
              JsonElement actual = find(reread, e.getKey()).value;
              check(
                  actual.equals(e.getValue())
                      || actual.isJsonPrimitive()
                          && actual.getAsJsonPrimitive().isNumber()
                          && actual.getAsDouble() == e.getValue().getAsDouble(),
                  "Saved value differs: " + actual);
            });
      if (inventory != null)
        test(
            "menu edits preserve inventory object identity",
            () -> {
              for (int i = 0; i < inventory.length; i++)
                check(inventory[i] == mc.player.inventory.main[i], "Inventory changed");
            });
    } finally {
      ConfigSession restore = SettingsRegistry.open(mc);
      old.forEach((id, value) -> find(restore, id).value = value);
      restore.save(Path.of(".").toAbsolutePath());
    }
    log("ROUNDTRIP FAILURES " + failures);
  }

  static void screens(Minecraft mc) throws Exception {
    failures = 0;
    Screen old = mc.currentScreen;
    for (int[] size : new int[][] {{320, 240}, {427, 240}, {550, 380}, {854, 480}})
      for (String page : PowerOptionsScreen.PAGES)
        test(
            "render " + page + " " + size[0] + "x" + size[1],
            () -> {
              PowerOptionsScreen screen = new PowerOptionsScreen(old);
              Field p = PowerOptionsScreen.class.getDeclaredField("page");
              p.setAccessible(true);
              p.set(screen, page);
              screen.init(mc, size[0], size[1]);
              while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
              screen.render(-1, -1, 0);
              check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error");
              screen.removed();
            });
    mc.setScreen(old);
    log("SCREEN FAILURES " + failures);
  }

  static void audio(Minecraft mc) throws Exception {
    failures = 0;
    AudioSettings old = AudioConfig.copy();
    test(
        "cached sound resources loaded",
        () ->
            check(
                AudioController.sounds(mc).size() > 50,
                "Only " + AudioController.sounds(mc).size() + " sounds"));
    test(
        "cached music resources loaded",
        () -> check(AudioController.music(mc).size() >= 3, "No vanilla playlist"));
    test(
        "empty playlists are safe",
        () -> check(AudioController.choose(List.of()) == null, "Expected empty playlist"));
    test(
        "legacy portal stop requests are consumed",
        () -> {
          Field stop =
              Class.forName("local.luke.power.music_api.MusicState")
                  .getField("cancelCurrentBGM");
          stop.setBoolean(null, true);
          AudioController.tick(mc);
          check(!stop.getBoolean(null), "Request not consumed");
        });
    try {
      AudioSettings draft = AudioConfig.copy();
      draft.master = 50;
      draft.categories.put("hostile", 40);
      draft.sounds.put("mob.zombie", 25);
      AudioConfig.save(draft);
      test(
          "runtime category and individual mixing",
          () ->
              check(
                  Math.abs(AudioController.mix("validation", "mob.zombie", 1, false) - .05)
                      < .00001,
                  "Incorrect gain"));
      draft.master = 0;
      AudioConfig.save(draft);
      test(
          "master mute is exact",
          () -> check(AudioController.mix("validation", "mob.zombie", 1, false) == 0, "Not muted"));
    } finally {
      AudioConfig.save(old);
    }
    log("AUDIO FAILURES " + failures);
  }

  private static AudioSettings musicBefore;
  private static float volumeBefore;

  static void music(Minecraft mc, String action) throws Exception {
    var system = SoundManagerAccessor.power$system();
    if (action.equals("setup")) {
      musicBefore = AudioConfig.copy();
      volumeBefore = mc.options.musicVolume;
      AudioSettings s = AudioConfig.copy();
      s.musicDirectories = List.of("validation-music");
      s.customMusic = AudioSettings.CustomMusic.ONLY;
      s.master = 100;
      s.shuffle = false;
      s.sounds.put("music:Test song été.wav", 50);
      s.sounds.put("music:Test song two.wav", 50);
      AudioConfig.save(s);
      mc.options.musicVolume = .7f;
    } else if (action.equals("next")) AudioController.next();
    else if (action.equals("check")) {
      failures = 0;
      test(
          "custom library loads Unicode filenames",
          () ->
              check(
                  AudioController.customTracks().size() == 2,
                  "tracks=" + AudioController.customTracks().size()));
      test(
          "custom WAV plays through native sound engine",
          () ->
              check(
                  system.playing("BgMusic") && AudioController.nowPlaying().startsWith("Test song"),
                  "Playing " + AudioController.nowPlaying()));
      test(
          "custom track volume multiplies native music",
          () ->
              check(
                  Math.abs(system.getVolume("BgMusic") - .35) < .001,
                  "Volume=" + system.getVolume("BgMusic")));
      test(
          "music debug HUD follows custom playback",
          () ->
              check(
                  Class.forName("local.luke.power.environment.ModHelper$ModHelperFields")
                      .getField("currentBGM")
                      .get(null)
                      .equals(AudioController.nowPlaying()),
                  "Debug track stale"));
      AudioController.togglePause();
      log("MUSIC FAILURES " + failures);
    } else if (action.equals("paused")) {
      test(
          "music pause keeps stream stopped",
          () ->
              check(
                  !system.playing("BgMusic") && AudioController.status().matches("Music paused|Paused: .+"),
                  "Pause ignored"));
      AudioController.togglePause();
    } else if (action.equals("resumed")) {
      test(
          "music resumes without replacing stream",
          () -> check(system.playing("BgMusic"), "Resume ignored"));
      AudioSettings s = AudioConfig.copy();
      s.customMusic = AudioSettings.CustomMusic.ADD;
      AudioConfig.save(s);
    } else if (action.equals("add")) {
      var pool = ((SoundManagerAccessor) mc.soundManager).power$music();
      List<net.minecraft.class_267> nativeTracks;
      synchronized (pool) {
        nativeTracks = List.copyOf(((SoundPoolAccessor) pool).power$tracks());
      }
      Set<String> found = new HashSet<>();
      for (int i = 0; i < nativeTracks.size() + 2; i++)
        found.add(AudioController.choose(nativeTracks).field_2127.toExternalForm());
      test(
          "add mode retains every native and custom track",
          () -> check(found.size() == nativeTracks.size() + 2, "Only " + found.size()));
      AudioSettings s = AudioConfig.copy();
      s.customMusic = AudioSettings.CustomMusic.ONLY;
      s.musicDirectories = List.of("missing-validation-folder");
      AudioConfig.save(s);
    } else if (action.equals("fallback")) {
      var pool = ((SoundManagerAccessor) mc.soundManager).power$music();
      List<net.minecraft.class_267> nativeTracks;
      synchronized (pool) {
        nativeTracks = List.copyOf(((SoundPoolAccessor) pool).power$tracks());
      }
      test(
          "missing replacement library falls back to native music",
          () ->
              check(
                  nativeTracks.contains(AudioController.choose(nativeTracks)), "Missing fallback"));
      test(
          "scanner never creates missing folders",
          () -> check(!Files.exists(Path.of("missing-validation-folder")), "Folder was created"));
      log("MUSIC FINAL FAILURES " + failures);
    } else if (action.equals("restore")) {
      AudioConfig.save(musicBefore);
      mc.options.musicVolume = volumeBefore;
      AudioController.next();
    } else if (action.equals("menu-check")) {
      failures = 0;
      test(
          "custom menu music plays",
          () -> check(system.playing("PowerBetaMenu"), "No menu stream"));
      test(
          "menu override prevents overlapping background music",
          () -> check(!system.playing("BgMusic"), "Overlapping music"));
      log("MENU MUSIC FAILURES " + failures);
    } else if (action.equals("world-check")) {
      test(
          "world entry stops menu music",
          () -> check(!system.playing("PowerBetaMenu"), "Menu music continued in world"));
      log("TRANSITION MUSIC FAILURES " + failures);
    }
  }

  static void input(Minecraft mc) throws Exception {
    failures = 0;
    Screen previous = mc.currentScreen;
    PowerOptionsScreen screen = new PowerOptionsScreen(previous);
    mc.setScreen(screen);
    var input = (local.luke.power.validation.mixin.ScreenInput) (Object) screen;
    test(
        "search filters the shared settings menu",
        () -> {
          int left = Math.max(90, Math.min(155, screen.width / 4)) + 16;
          input.power$click(left + 8, 34, 0);
          for (char c : "Master volume".toCharArray()) input.power$key(c, 0);
          Field rows = PowerOptionsScreen.class.getDeclaredField("rows");
          rows.setAccessible(true);
          int count = ((List<?>) rows.get(screen)).size();
          check(count >= 2 && count < 20, "Search did not filter volume settings: " + count);
        });
    Setting master = find(screen.session(), "audio.master");
    JsonElement original = master.value.deepCopy();
    int controlX = screen.width - 100,
        controlY =
            screen.width - (Math.max(90, Math.min(155, screen.width / 4)) + 16) - 14 < 340
                ? 95
                : 81;
    test(
        "left then right click restores a setting",
        () -> {
          input.power$click(controlX, controlY, 0);
          check(!master.value.equals(original), "Left click ignored");
          input.power$click(controlX, controlY, 1);
          check(master.value.equals(original), "Right click ignored");
        });
    test(
        "Escape cancels key capture",
        () -> {
          Field capture = PowerOptionsScreen.class.getDeclaredField("capture");
          capture.setAccessible(true);
          Setting key =
              screen.session().settings().stream()
                  .filter(s -> s.kind == Setting.Kind.KEY)
                  .findFirst()
                  .orElseThrow();
          JsonElement old = key.value.deepCopy();
          capture.set(screen, key);
          input.power$key('\0', org.lwjgl.input.Keyboard.KEY_ESCAPE);
          check(key.value.equals(old) && capture.get(screen) == null, "Escape changed binding");
        });
    test(
        "Delete clears and mouse input binds keys",
        () -> {
          Field capture = PowerOptionsScreen.class.getDeclaredField("capture");
          capture.setAccessible(true);
          Setting key =
              screen.session().settings().stream()
                  .filter(s -> s.kind == Setting.Kind.KEY)
                  .findFirst()
                  .orElseThrow();
          JsonElement old = key.value.deepCopy();
          capture.set(screen, key);
          input.power$key('\0', org.lwjgl.input.Keyboard.KEY_DELETE);
          check(key.value.getAsInt() == 0, "Delete ignored");
          capture.set(screen, key);
          input.power$click(0, 0, 3);
          check(key.value.getAsInt() == -97, "Mouse binding ignored");
          key.value = old;
        });
    test(
        "Cancel and discard leave live settings untouched",
        () -> {
          master.value = new JsonPrimitive(35);
          input.power$click(screen.width - 120, screen.height - 20, 0);
          input.power$click(screen.width / 2 + 10, screen.height / 2 + 20, 0);
          check(mc.currentScreen == previous, "Discard did not close");
          check(
              find(SettingsRegistry.open(mc), "audio.master").value.equals(original),
              "Draft leaked");
        });
    mc.setScreen(previous);
    log("INPUT FAILURES " + failures);
  }
}
