package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Native Library source actions share the Options transaction. */
final class MusicFolderChecks {
  private MusicFolderChecks() {}

  static int sourceY(MusicLibraryScreen library, String group) throws Exception {
    call(library, "refresh");
    Object row = ((List<?>)field(library, "rows")).stream().filter(value -> {
      try { return call(value, "id") == null && group.equals(call(value, "group")); }
      catch (Exception failure) { throw new RuntimeException(failure); }
    }).findFirst().orElseThrow(() -> new AssertionError("Source header missing " + group));
    int y = (int)call(row, "y");
    Dropdown450Screens.invoke(library, "scroll", new Class<?>[]{int.class}, y);
    return (int)call(library, "listTop") + y - (int)call(library, "scroll");
  }

  static void sourceClick(PowerOptionsScreen options, MusicLibraryScreen library,
      String path, String control) throws Exception {
    int y = sourceY(library, MusicLibrarySources.id(path));
    check(y >= (int)call(library, "listTop") && y + 20 <= (int)call(library, "bottom"), "source controls outside list");
    options.render(-1, -1, 0);
    click(options, (int)call(library, control) + 5, y + 8);
  }

  private static MusicLibrarySources.Folder source(MusicLibraryScreen library, String path) throws Exception {
    sourceY(library, MusicLibrarySources.id(path));
    return (MusicLibrarySources.Folder)((Map<?,?>)field(library, "sourceFolders")).get(MusicLibrarySources.id(path));
  }

  private static DirectoryScreen add(Minecraft mc, PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    click(options, (int)call(library, "addFolderX") + 5, (int)call(library, "actionTop") + 5);
    if ((boolean)call(library, "libraryActionsMenu")) {
      Object choices = field(options, "choices");
      check((boolean)call(choices, "active") && ((List<?>)field(choices, "labels")).get(0).equals("Add folder"), "compact Library actions did not open");
      MusicPlaylists460Checks.choose(options, 0);
    }
    check(mc.currentScreen instanceof DirectoryScreen, "Add folder did not open picker");
    return (DirectoryScreen)mc.currentScreen;
  }

  private static void typePath(DirectoryScreen picker, Path path) throws Exception {
    ((ScreenInput)(Object)picker).power$click((int)call(picker, "panelLeft") + 20, 45, 0);
    ((TextInput)field(picker, "path")).selectAll();
    for (char character : path.toString().toCharArray()) ((ScreenInput)(Object)picker).power$key(character, 0);
  }

  private static StickyHeader pinSource(PowerOptionsScreen options, MusicLibraryScreen library, String path) throws Exception {
    String group = MusicLibrarySources.id(path);
    // Later native rows leave enough scroll range to push the source header upward.
    if (library.state().collapsed().contains("Alpha")) {
      int alphaY = sourceY(library, "Alpha");
      click(options, (int)call(library, "left") + 5, alphaY + 8);
    }
    int y = sourceY(library, group);
    if (library.state().collapsed().contains(group)) click(options, (int)call(library, "left") + 5, y + 8);
    List<?> rows = (List<?>)field(library, "rows");
    Object last = null;
    int children = 0;
    for (Object row : rows) if (group.equals(call(row, "group")) && call(row, "id") != null) { last = row; children++; }
    check(children >= 8, "sticky source requires eight scanned tracks");
    Dropdown450Screens.invoke(library, "scroll", new Class<?>[]{int.class}, (int)call(last, "y") - 16);
    options.render(-1, -1, 0);
    var sticky = (StickyHeader)call(library, "stickyHeader");
    if ((int)call(library, "bottom") - (int)call(library, "listTop") < 24 + (int)call(library, "rowHeight")) {
      check(sticky == null, "source header pinned without room for a track row");
      return null;
    }
    check(sticky != null && group.equals(call(((List<?>)field(library, "rows")).get(sticky.index()), "group")), "source header did not pin");
    check(sticky.y() == (int)call(library, "listTop") - 8, "source header did not reach partially clipped position");
    return sticky;
  }

  static void sticky(Minecraft mc, String path) throws Exception {
    boolean automatic = MenuPreferences.current().autoApply;
    MenuPreferences.update(value -> value.autoApply = false);
    var options = MusicPlaylists460Checks.open(mc); var library = MusicPlaylists460Checks.library(options);
    String playlists = Catalog.JSON.toJson(AudioConfig.current().playlists);
    try {
      for (int[] size : new int[][]{{320, 240}, {320, 300}, {640, 420}})
        test("MUSIC460 clipped sticky source control bounds size=" + size[0] + "x" + size[1], () -> {
          options.init(mc, size[0], size[1]);
          var pinned = pinSource(options, library, path);
          if (pinned == null) {
            check((int)call(library, "bottom") - (int)call(library, "listTop") < 24 + (int)call(library, "rowHeight"), "source header failed to pin with room for a song row");
            sourceClick(options, library, path, "sourceRemoveX");
            check(library.modal(), "ordinary source controls missed in short viewport");
            ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_ESCAPE);
            check(!library.modal() && AudioConfig.current().musicDirectories.contains(path), "short viewport Remove cancel changed source");
            return;
          }
          check(pinned != null, "source did not pin in supported viewport");
          int top = (int)call(library, "listTop"), removeX = (int)call(library, "sourceRemoveX");
          click(options, removeX + 5, top - 1);
          check(!library.modal(), "clipped Remove accepted a click above list");
          click(options, removeX + 18, top);
          check(!library.modal(), "Remove accepted exclusive right edge");
          pinSource(options, library, path);
          click(options, removeX + 5, top + 13);
          check(!library.modal(), "Remove accepted exclusive lower edge of clipped button");
          pinSource(options, library, path);
          click(options, removeX + 17, top + 12);
          check(library.modal(), "visible pinned Remove control missed last horizontal/vertical pixel");
          ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_ESCAPE);
          check(!library.modal() && AudioConfig.current().musicDirectories.contains(path), "sticky Remove cancel changed source");
          check(playlists.equals(Catalog.JSON.toJson(AudioConfig.current().playlists)), "sticky controls changed playlist references");
        });
    } finally { options.session().discard(); MenuPreferences.update(value -> value.autoApply = automatic); }
  }

  static void run(Minecraft mc) throws Exception {
    Path root = Files.createTempDirectory(Path.of(".").toAbsolutePath(), "music-folder-controls-").toRealPath();
    try (var preserved = new MusicPlaylists460Checks.State(mc)) {
      Path first = Files.createDirectory(root.resolve("first"));
      Path second = Files.createDirectory(root.resolve("second"));
      Path empty = Files.createDirectory(root.resolve("empty"));
      Path missing = root.resolve("missing");
      for (int i=0;i<20;i++) Files.createDirectory(root.resolve("browser-row-" + i));
      for (Path folder : List.of(first, second)) try (var in = MusicFolderChecks.class.getResourceAsStream("/assets/powerbeta/music/calm1.ogg")) {
        Files.copy(Objects.requireNonNull(in), folder.resolve("song.ogg"));
      }
      String track = new MusicLibrary.Track(first.resolve("song.ogg").toRealPath(), "song.ogg").id();
      var baseline = new AudioSettings();
      baseline.musicDirectories = new ArrayList<>(List.of(first.toString(), empty.toString(), missing.toString()));
      baseline.playlists.add(new MusicPlaylist("folder-fixture", "Folder fixture", List.of(track, "music:calm1.ogg")));
      MusicPlaylist.load(baseline, "folder-fixture");
      for (boolean automatic : new boolean[]{false, true}) {
        MenuPreferences.update(value -> { value.autoApply = automatic; value.musicBrowserDirectory = root.toString(); });
        AudioConfig.save(baseline);
        var options = MusicPlaylists460Checks.open(mc);
        var library = MusicPlaylists460Checks.library(options);
        MusicPlaylists460Checks.tab(options, library, "library");
        test("MUSIC460 Library sources and typed Add folder auto=" + automatic, () -> {
          check(source(library, empty.toString()).trackIds().isEmpty(), "empty source header missing or has tracks");
          check(source(library, missing.toString()).trackIds().isEmpty(), "missing source header missing or has tracks");
          var picker = add(mc, options, library);
          typePath(picker, second);
          ((ScreenInput)(Object)picker).power$click(picker.width / 2 + 90, picker.height - 20, 0);
          check(mc.currentScreen == options, "picker Cancel did not return directly to Library");
          check(AudioConfig.current().musicDirectories.equals(baseline.musicDirectories), "Cancel added typed source");
          picker = add(mc, options, library); typePath(picker, second);
          ((ScreenInput)(Object)picker).power$click(picker.width / 2 - 100, picker.height - 20, 0);
          check(mc.currentScreen == options, "typed Add did not return to Library");
          check(AudioConfig.current().musicDirectories.contains(second.toString()), "typed Add did not preview source");
          check(source(library, second.toString()) != null, "added source header missing");
          check(PowerConfig.readAudio(AudioSettings.class).musicDirectories.contains(second.toString()) == automatic, "Add crossed Options persistence boundary");
          check(Catalog.JSON.toJson(AudioConfig.current().playlists).equals(Catalog.JSON.toJson(baseline.playlists)), "Add changed saved playlist entries");
          if (!automatic) {
            options.session().discard();
            check(!AudioConfig.current().musicDirectories.contains(second.toString()), "Options Discard retained added source");
            picker = add(mc, options, library); typePath(picker, root);
            ((ScreenInput)(Object)picker).power$key('\0', Keyboard.KEY_RETURN);
            ((ScreenInput)(Object)picker).power$click((int)call(picker, "selectX") + 5, (int)call(picker, "selectY") + 5, 0);
            check(((Set<?>)field(picker, "selected")).size() == ((List<?>)field(picker, "children")).size(), "Select all missed visible folders");
            ((ScreenInput)(Object)picker).power$click((int)call(picker, "selectX") + 75, (int)call(picker, "selectY") + 5, 0);
            check(((Set<?>)field(picker, "selected")).isEmpty(), "Deselect all retained visible folders");
            var bounds=(ScrollBar.Track)call(picker,"track");
            check(bounds.visible(),"picker fixture has no scrollbar");
            var input=(ScreenInput)(Object)picker;
            input.power$click(bounds.x(),bounds.top()+2,0);
            input.power$release(bounds.x(),bounds.top()+bounds.height()-1,-1);
            check((boolean)field(field(picker,"scrollbar"),"dragging"),"native motion ended picker scrollbar ownership");
            input.power$release(bounds.x(),bounds.top()+bounds.height()-1,0);
            check((int)field(picker,"scroll")>0 && !(boolean)field(field(picker,"scrollbar"),"dragging"),"picker release ignored final position or retained ownership");
            ((ScreenInput)(Object)picker).power$key('\0', Keyboard.KEY_ESCAPE); options.session().discard();
          }
        });
        test("MUSIC460 inline Remove modal preserves files and playlists auto=" + automatic, () -> {
          sourceClick(options, library, first.toString(), "sourceRemoveX");
          check(library.modal(), "Remove skipped confirmation");
          click(options, (int)call(library, "addFolderX") + 5, (int)call(library, "actionTop") + 5);
          check(mc.currentScreen == options && library.modal(), "modal leaked click to Add folder");
          click(options, options.width / 2 + 10, options.height / 2 + 20);
          check(!library.modal() && AudioConfig.current().musicDirectories.contains(first.toString()), "Cancel removed source");
          sourceClick(options, library, first.toString(), "sourceRemoveX");
          click(options, options.width / 2 - 60, options.height / 2 + 20);
          check(!library.modal() && !AudioConfig.current().musicDirectories.contains(first.toString()), "confirmed Remove retained source");
          call(library, "refresh");
          check(!((Map<?,?>)field(library, "sourceFolders")).containsKey(MusicLibrarySources.id(first.toString())), "Remove retained source header");
          check(Files.isRegularFile(first.resolve("song.ogg")), "Remove deleted source file");
          check(Catalog.JSON.toJson(AudioConfig.current().playlists).equals(Catalog.JSON.toJson(baseline.playlists)), "Remove changed playlist references");
          if (!automatic) {
            check(PowerConfig.readAudio(AudioSettings.class).musicDirectories.contains(first.toString()), "manual Remove persisted before Apply");
            options.session().discard();
            check(source(library, first.toString()) != null, "Options Discard did not restore source header");
            sourceClick(options, library, first.toString(), "sourceRemoveX");
            click(options, options.width / 2 - 60, options.height / 2 + 20);
            int applyX=(int)Dropdown450Screens.invoke(options,"actionX",new Class<?>[]{int.class},1);
            click(options,applyX+8,options.height-20);
            check(!PowerConfig.readAudio(AudioSettings.class).musicDirectories.contains(first.toString()), "Apply did not persist source removal");
            options.session().discard();
            check(!AudioConfig.current().musicDirectories.contains(first.toString()), "Discard restored removal committed by Apply");
          }
        });
        options.session().discard(); options.removed();
      }
      test("MUSIC460 stale Add callback preserves newer source settings", () -> {
        MenuPreferences.update(value -> value.autoApply = false); AudioConfig.save(baseline);
        var options = MusicPlaylists460Checks.open(mc); var library = MusicPlaylists460Checks.library(options);
        var picker = add(mc, options, library); typePath(picker, second);
        Setting setting = find(options.session(), "audio.musicDirectories");
        var concurrent = MusicFolders.encode(List.of(first.toString(), missing.toString()));
        setting.value = concurrent.deepCopy(); options.changed(setting);
        try {
          ((ScreenInput)(Object)picker).power$click(picker.width / 2 - 100, picker.height - 20, 0);
          check(mc.currentScreen == picker && field(picker, "error").toString().contains("Folders changed"), "stale Add callback lacked rejection feedback");
          check(setting.value.equals(concurrent) && AudioConfig.current().musicDirectories.equals(List.of(first.toString(), missing.toString())), "stale Add overwrote newer source edit");
          check(!AudioConfig.current().musicDirectories.contains(second.toString()), "stale Add imported its old draft");
          check(PowerConfig.readAudio(AudioSettings.class).musicDirectories.equals(baseline.musicDirectories), "rejected Add persisted source changes");
        } finally { ((ScreenInput)(Object)picker).power$key('\0', Keyboard.KEY_ESCAPE); options.session().discard(); }
      });
      test("MUSIC460 stale Remove confirmation preserves newer source settings", () -> {
        MenuPreferences.update(value -> value.autoApply = false); AudioConfig.save(baseline);
        var options = MusicPlaylists460Checks.open(mc); var library = MusicPlaylists460Checks.library(options);
        sourceClick(options, library, first.toString(), "sourceRemoveX");
        Setting setting = find(options.session(), "audio.musicDirectories");
        var concurrent = MusicFolders.encode(List.of(first.toString(), missing.toString()));
        setting.value = concurrent.deepCopy(); options.changed(setting);
        try {
          click(options, options.width / 2 - 60, options.height / 2 + 20);
          check(library.modal() && call(field(library, "folders"), "error").toString().contains("Folders changed"), "stale Remove confirmation lacked rejection feedback");
          check(setting.value.equals(concurrent) && AudioConfig.current().musicDirectories.equals(List.of(first.toString(), missing.toString())), "stale Remove overwrote newer source edit");
          check(AudioConfig.current().musicDirectories.contains(first.toString()) && Files.isRegularFile(first.resolve("song.ogg")), "stale Remove lost source or file");
          check(PowerConfig.readAudio(AudioSettings.class).musicDirectories.equals(baseline.musicDirectories), "rejected Remove persisted source changes");
        } finally { ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_ESCAPE); options.session().discard(); }
      });
      test("MUSIC460 failed auto-save leaves source state and durable files intact", () -> {
        MenuPreferences.update(value -> value.autoApply = true); AudioConfig.save(baseline);
        var options = MusicPlaylists460Checks.open(mc); var library = MusicPlaylists460Checks.library(options);
        var failure = new Setting("folder-save-failure", "folder-save-failure", "Audio", "Music", "Fixture", "", Setting.Kind.BOOLEAN,
            new JsonPrimitive(false), new JsonPrimitive(false), 0, 1, 1, List.of(), false);
        options.session().add(new Backend() {
          public String id() { return "folder-save-failure"; }
          public List<Path> files() { return List.of(); }
          public void validate(Map<String, JsonElement> values) {}
          public void apply(Map<String, JsonElement> values) {
            if (values.get("folder-save-failure").getAsBoolean()) throw new IllegalStateException("Deliberate folder save failure");
          }
        }, List.of(failure));
        failure.value = new JsonPrimitive(true);
        byte[] saved = Files.readAllBytes(PowerConfig.musicPath());
        try {
          sourceClick(options, library, first.toString(), "sourceRemoveX");
          click(options, options.width / 2 - 60, options.height / 2 + 20);
          check(AudioConfig.current().musicDirectories.equals(baseline.musicDirectories), "failed save retained Remove preview");
          check(MusicFolders.decode(find(options.session(), "audio.musicDirectories").value).equals(baseline.musicDirectories), "failed save retained Remove draft");
          check(Arrays.equals(saved, Files.readAllBytes(PowerConfig.musicPath())), "failed save changed durable music data");
          check(!call(field(library, "folders"), "error").toString().isEmpty(), "failed source save lacked feedback");
        } finally { options.session().removeBackend("folder-save-failure"); options.session().discard(); }
      });
      test("MUSIC460 source controls and picker render across native GUI sizes", () -> {
        AudioConfig.preview(baseline); var options = MusicPlaylists460Checks.open(mc); var library = MusicPlaylists460Checks.library(options);
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {640, 420}, {854, 480}}) {
          options.init(mc, size[0], size[1]); options.render(-1, -1, 0);
          sourceClick(options, library, empty.toString(), "sourceRemoveX");
          check(library.modal(), "compact Remove missed at " + size[0]); options.render(-1, -1, 0);
          ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_ESCAPE);
          check(!library.modal(), "Escape did not cancel Remove at " + size[0]);
          var picker = add(mc, options, library); picker.init(mc, size[0], size[1]); picker.render(-1, -1, 0);
          ((ScreenInput)(Object)picker).power$key('\0', Keyboard.KEY_ESCAPE);
          check(mc.currentScreen == options && GL11.glGetError() == 0, "picker return or GL error at " + size[0]);
        }
        options.session().discard();
      });
    } finally {
      try (var paths = Files.walk(root)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
    }
  }
}
