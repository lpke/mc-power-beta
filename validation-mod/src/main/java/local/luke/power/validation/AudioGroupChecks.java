package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.class_525;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.opengl.GL11;

final class AudioGroupChecks {
  private static AudioSettings before;
  private static boolean auto;
  private static PowerOptionsScreen options;
  private static float music;
  private static List<String> queueBefore;

  private static Object header(MusicLibraryScreen library, String group) throws Exception {
    for (Object row : (List<?>) field(library, "rows"))
      if (call(row, "id") == null && group.equals(call(row, "group"))) return row;
    return null;
  }

  private static int groupY(MusicLibraryScreen library, String group) throws Exception {
    Object row = header(library, group);
    check(row != null, "group missing: " + group);
    field(library, "trackScroll", (int) call(row, "y"));
    return (int) call(library, "listTop") + 8;
  }

  private static void setGroupVolume(MusicLibraryScreen library, String group, int volume)
      throws Exception {
    int y = groupY(library, group);
    int x = (int) call(library, "groupVolumeX"), w = (int) call(library, "groupVolumeWidth");
    click(options, x + 4 + (int) Math.round((w - 8) * volume / 100d), y);
    options.render(-1, -1, 0);
  }

  static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "setup" -> {
        failures = 0;
        before = AudioConfig.copy();
        music = mc.options.musicVolume;
        queueBefore = MusicRequests.tracks();
        auto = MenuPreferences.current().autoApply;
        MenuPreferences.update(v -> v.autoApply = false);
        AudioSettings s = new AudioSettings();
        MusicFixtureSupport.allBuiltin(s);
        Set<String> excluded = new TreeSet<>();
        BuiltinMusic.TRACKS.stream()
            .filter(t -> t.group().equals("Alpha"))
            .forEach(t -> excluded.add(t.id()));
        excluded.add("music:creative1.ogg");
        s.playlists.add(new MusicPlaylist("draft", "Quiet afternoons", AudioController.music(mc).stream().filter(id -> !excluded.contains(id)).toList(), Map.of("Alpha", 40)));
        AudioConfig.preview(s);
        mc.options.musicVolume = .1f;
        AudioController.pause();
      }
      case "ui" -> {
        MusicPlaylists460Checks.editor(mc);
        log("AUDIO GROUP UI FAILURES " + failures);
      }
      case "menu" -> {
        test(
            "menu music controls render all anchors and follow title visibility",
            () -> {
              AudioSettings s = AudioConfig.copy();
              s.menuControls = true;
              AudioConfig.preview(s);
              class_525 pause = new class_525();
              mc.setScreen(pause);
              for (boolean showScrub : new boolean[]{false, true})
              for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 420}, {854, 480}})
                for (var anchor : AudioSettings.MenuControlsPosition.values()) {
                  s.menuControlsScrub = showScrub;
                  s.menuControlsPosition = anchor;
                  AudioConfig.preview(s);
                  pause.init(mc, size[0], size[1]);
                  pause.render(-1, -1, 0);
                  var panel = field(pause, "power$music");
                  var bounds = (MenuMusicLayout) field(panel, "bounds");
                  check(bounds.height() == (showScrub ? 48 : 32), "scrub layout height incorrect");
                  check(bounds.width() <= 200, "panel wider than menu buttons");
                  var buttons = ((ScreenInput) (Object) pause).power$buttons();
                  var settings = buttons.stream().filter(b -> b.id == 0).findFirst().orElseThrow();
                  var stats = buttons.stream().filter(b -> b.id == 6).findFirst().orElseThrow();
                  check(settings.y == stats.y + 24, "music panel restored the old Options gap");
                  buttons.stream().filter(b -> b.id == 20).forEach(b ->
                      check(b.y == settings.y, "photo button detached from Options"));
                  check(GL11.glGetError() == 0, "pause GL error " + anchor);
                }
              mc.setScreen(pause);
              s.menuControlsPosition = AudioSettings.MenuControlsPosition.MENU_BOTTOM;
              AudioConfig.preview(s);
              pause.render(-1, -1, 0);
              var panel = (PauseMenuMusic) field(pause, "power$music");
              var bounds = (MenuMusicLayout) field(panel, "bounds");
              int x = (int) call(panel, "controlsX");
              AudioController.pause();
              ((ScreenInput) (Object) pause).power$click(x + 8, bounds.y() + 8, 0);
              check(!AudioController.status().matches("Music paused|Paused: .+"), "pause Play not routed");
              check(mc.currentScreen == pause, "transport closed pause menu");
              s.menuControls = false;
              AudioConfig.preview(s);
              check(!panel.press(x + 8, bounds.y() + 8, 0), "hidden control captured click");
              s.menuControls = true;
              AudioConfig.preview(s);
              TitleScreen title = new TitleScreen();
              mc.setScreen(title);
              title.render(-1, -1, 0);
              check(((PauseMenuMusic) field(title, "power$music")).visible() == s.menuControlsMainMenu,
                  "title control visibility differs from settings");
            });
        log("AUDIO GROUP MENU FAILURES " + failures);
      }
      case "gain-play" -> {
        var s = AudioConfig.copy();
        s.master = 80;
        MusicFixtureSupport.volume(s,"Alpha",50);
        s.sounds.put("music:calm1.ogg", 50);
        AudioConfig.preview(s);
        mc.options.musicVolume = .1f;
        AudioController.playNow("music:calm1.ogg");
      }
      case "gain-check" ->
          test(
              "actual playback multiplies master, music, track and group volumes",
              () -> {
                var sound = local.luke.power.mixin.SoundManagerAccessor.power$system();
                check(sound.playing("BgMusic"), "track not playing");
                check(
                    Math.abs(sound.getVolume("BgMusic") - .02) < .0001,
                    "gain " + sound.getVolume("BgMusic"));
              });
      case "gain-preview" -> AudioController.previewSound("music:calm1.ogg");
      case "gain-check-preview" ->
          test(
              "preview uses the same group multiplier",
              () -> {
                var sound = local.luke.power.mixin.SoundManagerAccessor.power$system();
                check(sound.playing("PowerBetaMusicPreview"), "preview not playing");
                check(
                    Math.abs(sound.getVolume("PowerBetaMusicPreview") - .02) < .0001,
                    "preview gain");
              });
      case "gain-mute" -> {
        var s = AudioConfig.copy();
        MusicFixtureSupport.volume(s,"Alpha",0);
        AudioConfig.preview(s);
      }
      case "gain-check-mute" ->
          test(
              "live zero group volume immediately mutes an active preview",
              () -> {
                check(
                    local.luke.power.mixin.SoundManagerAccessor.power$system()
                            .getVolume("PowerBetaMusicPreview")
                        == 0,
                    "preview not muted");
              });
      case "gain-queue" -> {
        var s = AudioConfig.copy();
        MusicFixtureSupport.volume(s,"Alpha",50);
        AudioConfig.preview(s);
        MusicRequests.edit(
            q -> {
              q.tracks.clear();
              q.add("music:calm1.ogg");
            });
        AudioController.next();
      }
      case "view-library", "view-editor", "view-tabs" -> {
        options = MenuUpdateChecks.open(mc, "Audio");
        field(options, "libraryOpen", true);
        var library = (MusicLibraryScreen) field(options, "library");
        if (action.equals("view-editor")) {
          library.openPresets();
          var panel = field(library, "presets");
          click(options, (int) call(panel, "right") - 145, presetRowY(panel, 1));
          groupY(library, "Beta / Creative");
        } else {
          library.showTrack("music:calm1.ogg");
          if (action.equals("view-tabs")) {
            var method = MusicLibraryScreen.class.getDeclaredMethod("selectFilter", String.class);
            method.setAccessible(true);
            method.invoke(library, "presets");
          } else {
            groupY(library, "Alpha");
          }
        }
        options.render(-1, -1, 0);
      }
      case "pause" -> {
        var s = AudioConfig.copy();
        s.menuControls = true;
        s.menuControlsPosition = AudioSettings.MenuControlsPosition.MENU_BOTTOM;
        AudioConfig.preview(s);
        mc.setScreen(new class_525());
      }
      case "finish" -> {
        AudioController.pause();
        AudioConfig.preview(before);
        mc.options.musicVolume = music;
        MusicRequests.edit(
            q -> {
              q.tracks.clear();
              q.tracks.addAll(queueBefore);
            });
        MenuPreferences.update(v -> v.autoApply = auto);
        mc.setScreen(null);
        log("AUDIO GROUP FAILURES " + failures);
      }
      default -> throw new IllegalArgumentException(action);
    }
  }
}
