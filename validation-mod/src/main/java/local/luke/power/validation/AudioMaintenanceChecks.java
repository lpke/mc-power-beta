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
    try {
      MenuPreferences.update(v -> { v.autoApply = false; v.musicBrowserDirectory = root.toString(); });
      for (String child : List.of("first", "second")) {
        Files.createDirectory(root.resolve(child));
        try (var in = AudioMaintenanceChecks.class.getResourceAsStream("/assets/powerbeta/music/calm1.ogg")) {
          Files.copy(Objects.requireNonNull(in), root.resolve(child + "/song.ogg"));
        }
      }
      AudioSettings config = new AudioSettings();
      config.presets.add(new MusicPreset("one", "One", Set.of(), Map.of(), Set.of("music:calm1.ogg")));
      MusicPreset.load(config, "one");
      AudioConfig.preview(config);
      PowerOptionsScreen options = MenuUpdateChecks.open(mc, "Audio");
      test("loaded preset admissions survive previews and rollback without mutating saved presets", () -> {
        Setting enabled = find(options.session(), "audio.trackEnabled.music:calm2.ogg");
        check(!enabled.value.getAsBoolean(), "new track already admitted");
        enabled.value = new JsonPrimitive(true); options.changed(enabled);
        check(AudioConfig.current().presetTrackPool.contains("music:calm2.ogg"), "preview missed admission");
        check(!AudioConfig.current().presets.get(0).includes("music:calm2.ogg"), "live edit mutated preset");
        Setting favourite = find(options.session(), "audio.favourites");
        favourite.value = Catalog.JSON.toJsonTree(Set.of("music:calm1.ogg")); options.changed(favourite);
        check(AudioConfig.current().presetTrackPool.contains("music:calm2.ogg"), "next edit lost admission");
        options.session().discard();
        check(!AudioConfig.current().presetTrackPool.contains("music:calm2.ogg"), "discard retained admission");
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
        check(AudioConfig.current().presets.get(0).trackPool().equals(Set.of("music:calm1.ogg")), "folders mutated preset");
        folder = new FolderScreen(options, folders); mc.setScreen(folder);
        clickScreen(folder, (int)call(folder,"panelRight") - 30, 80);
        check(((List<?>)field(folder,"folders")).size() == 1, "row remove missed");
        escape(folder); clickScreen(folder, folder.width/2, folder.height/2 + 16);
        check(AudioConfig.current().musicDirectories.size() == 2, "Discard removed a folder");
      });
      test("child import checks every selected parent and skips already listed directories", () -> {
        var fresh = find(options.session(), "audio.menuDirectories");
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
      test("single-section library filter expands and pause scrub option defaults off", () -> {
        check(!new AudioSettings().menuControlsScrub,"scrubber enabled by default");
        mc.setScreen(options); field(options,"libraryOpen",true);
        var library = (MusicLibraryScreen)field(options,"library");
        Setting favourites = find(options.session(),"audio.favourites");
        favourites.value=Catalog.JSON.toJsonTree(Set.of("music:calm1.ogg")); options.changed(favourites);
        var select=MusicLibraryScreen.class.getDeclaredMethod("selectFilter",String.class);
        select.setAccessible(true); select.invoke(library,"favourites");
        check(((List<?>)field(library,"rows")).size()==2,"single section starts collapsed");
      });
      test("ffmpeg was discovered without a conversion process", () -> check(Mp3Converter.available(),"ffmpeg probe failed"));
    } finally {
      AudioConfig.preview(before);
      MenuPreferences.update(v -> { v.autoApply=automatic; v.musicBrowserDirectory=oldBrowser; });
      mc.setScreen(parent);
      try(var files=Files.walk(root)) { for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p); }
    }
    log("AUDIO MAINTENANCE FAILURES " + failures);
  }
}
