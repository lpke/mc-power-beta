package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

final class AudioMaintenanceChecks {
  private static void clickScreen(Object screen, int x, int y) {
    ((ScreenInput) screen).power$click(x, y, 0);
  }
  private static void escape(Object screen) {
    ((ScreenInput) screen).power$key('\0', Keyboard.KEY_ESCAPE);
  }

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    AudioSettings before = AudioConfig.copy();
    boolean automatic = MenuPreferences.current().autoApply;
    String oldBrowser = MenuPreferences.current().musicBrowserDirectory;
    var parent = mc.currentScreen;
    Path root = Files.createTempDirectory(Path.of(".").toAbsolutePath(), "audio-import-test-");
    var preserved = new MusicPlaylists460Checks.State(mc);
    try {
      MenuPreferences.update(v -> { v.autoApply = false; v.musicBrowserDirectory = root.toString(); });
      for (String child : List.of("first", "second")) {
        Files.createDirectory(root.resolve(child));
        try (var in = AudioMaintenanceChecks.class.getResourceAsStream("/assets/powerbeta/music/calm1.ogg")) {
          Files.copy(Objects.requireNonNull(in), root.resolve(child + "/song.ogg"));
        }
      }
      AudioSettings config = new AudioSettings();
      config.playlists.add(new MusicPlaylist("one", "One", List.of("music:calm1.ogg"), Map.of()));
      MusicPlaylist.load(config, "one");
      AudioConfig.preview(config);
      PowerOptionsScreen options = MenuUpdateChecks.open(mc, "Audio");
      test("saved playlist membership survives global favourites edits and rollback", () -> {
        var draft = new MusicPresetDraft(config.playlists.get(0), List.of("music:calm1.ogg", "music:calm2.ogg"));
        draft.include("music:calm2.ogg", true);
        check(MusicFixtureSupport.tracks(AudioConfig.current()).equals(List.of("music:calm1.ogg")), "unsaved membership leaked");
        Setting favourite = find(options.session(), "audio.favourites");
        favourite.value = Catalog.JSON.toJsonTree(Set.of("music:calm1.ogg")); options.changed(favourite);
        check(draft.tracks().contains("music:calm2.ogg"), "global edit replaced isolated draft");
        check(MusicFixtureSupport.tracks(AudioConfig.current()).equals(List.of("music:calm1.ogg")), "global edit expanded saved playlist");
        options.session().discard();
      });
      Setting folders = find(options.session(), "audio.musicDirectories");
      test("multi-select browse adds unique children and folder drafts require Save or Discard", () -> {
        FolderScreen folder = new FolderScreen(options, folders); mc.setScreen(folder);
        clickScreen(folder, (int)call(folder,"panelRight") - 40, 50);
        check(mc.currentScreen instanceof DirectoryScreen, "Browse missed");
        DirectoryScreen browser = (DirectoryScreen)mc.currentScreen;
        check(field(browser,"directory").equals(root), "last directory ignored");
        clickScreen(browser, (int)call(browser,"panelLeft") + 22, 98);
        clickScreen(browser, (int)call(browser,"panelLeft") + 22, 118);
        check(((Set<?>)field(browser,"selected")).size() == 2, "checkbox selection lost");
        clickScreen(browser, browser.width/2 - 100, browser.height - 20);
        check(mc.currentScreen == folder, "browser did not return");
        check(((List<?>)field(folder,"folders")).size() == 2, "multi-import incomplete");
        check(folders.value.getAsJsonArray().size() == 0, "unsaved folder edit leaked");
        escape(folder); check((boolean)field(folder,"confirm"), "Escape discarded without warning");
        clickScreen(folder, folder.width/2 + 80, folder.height/2 + 16);
        check(!(boolean)field(folder,"confirm"), "Cancel did not return to draft");
        escape(folder); clickScreen(folder, folder.width/2 - 80, folder.height/2 + 16);
        check(mc.currentScreen == options, "Save did not return");
        check(AudioConfig.current().musicDirectories.size() == 2, "Save lost imported folders");
        check(AudioConfig.current().playlists.get(0).tracks().equals(List.of("music:calm1.ogg")), "folders mutated preset");
        check(MusicFixtureSupport.tracks(AudioConfig.current()).equals(List.of("music:calm1.ogg")), "folder import expanded selected saved playlist");
        folder = new FolderScreen(options, folders); mc.setScreen(folder);
        clickScreen(folder, (int)call(folder,"panelRight") - 30, 80);
        check(((List<?>)field(folder,"folders")).size() == 1, "row remove missed");
        escape(folder); clickScreen(folder, folder.width/2, folder.height/2 + 16);
        check(AudioConfig.current().musicDirectories.size() == 2, "Discard removed a folder");
      });
      test("child import checks every selected parent and skips already listed directories", () -> {
        var fresh = new Setting(folders.id, folders.backend, folders.page, folders.group, folders.label, folders.description, folders.kind, Catalog.JSON.toJsonTree(List.of()), folders.defaultValue, folders.min, folders.max, folders.step, folders.choices, folders.restart);
        FolderScreen folder = new FolderScreen(options, fresh); mc.setScreen(folder);
        clickScreen(folder, (int)call(folder,"panelRight") - 40, 50);
        DirectoryScreen browser = (DirectoryScreen)mc.currentScreen;
        clickScreen(browser, browser.width/2 + 10, browser.height - 20);
        check(((List<?>)field(folder,"folders")).size() == 2, "child import missed audio folders");
        clickScreen(folder, (int)call(folder,"panelRight") - 40, 50);
        browser = (DirectoryScreen)mc.currentScreen;
        clickScreen(browser, browser.width/2 + 10, browser.height - 20);
        check(mc.currentScreen == browser, "duplicate import closed browser");
        check(field(browser,"error").toString().contains("No new"), "duplicate import lacked feedback");
        escape(browser); escape(folder); clickScreen(folder, folder.width/2, folder.height/2 + 16);
      });
      test("folder browser and confirmation render cleanly across GUI sizes", () -> {
        for (int[] size : new int[][]{{320,240},{427,240},{854,480},{1920,480}}) {
          FolderScreen folder = new FolderScreen(options, folders);
          folder.init(mc,size[0],size[1]); folder.render(-1,-1,0);
          field(folder,"confirm",true); folder.render(-1,-1,0); folder.removed();
          DirectoryScreen browser = new DirectoryScreen(folder, values -> {}, List.of());
          browser.init(mc,size[0],size[1]); browser.render(-1,-1,0); browser.removed();
          check(GL11.glGetError()==0,"GL error at " + size[0]);
        }
      });
      test("library sections start collapsed and pause scrub option defaults off", () -> {
        check(!new AudioSettings().menuControlsScrub,"scrubber enabled by default");
        mc.setScreen(options);field(options,"libraryOpen",false);call(options,"layout");
        var library = MusicPlaylists460Checks.library(options);
        MusicPlaylists460Checks.tab(options,library,"library");
        check(library.sectionsCollapsed(),"library starts expanded");
      });
      test("ffmpeg was discovered without a conversion process", () -> check(Mp3Converter.available(),"ffmpeg probe failed"));
    } finally {
      AudioConfig.preview(before);
      MenuPreferences.update(v -> { v.autoApply=automatic; v.musicBrowserDirectory=oldBrowser; });
      mc.setScreen(parent);
      try(var files=Files.walk(root)) { for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p); }
      preserved.close();
    }
    log("AUDIO MAINTENANCE FAILURES " + failures);
  }
}
