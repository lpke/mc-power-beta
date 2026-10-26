package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.class_267;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Native music controls in a muted disposable client. Restores files and runtime state. */
final class MusicPlaylists460Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/music-playlists-460");
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg";
  private static final String MISSING = "music:playlist-fixture-missing.ogg";
  private static final List<String> VIEWS = List.of("library", "library-expanded", "folder-picker", "folder-remove", "favorites", "playlists", "new", "editor", "grouped", "add-library", "from-playlist", "context", "audio-playback", "audio-controls", "menu");
  private static State staged;
  private static PowerOptionsScreen activeOptions;
  private static List<String> fixtureFolders=List.of();
  private static String importedTrack="", importedLabel="", importedGroup="";
  private static class_267 importedEntry;
  private MusicPlaylists460Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) { stop(); return; }
    SilentAudioChecks.ready(mc);
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world");
    if(action.equals("prepare")) {
      stop();staged=new State(mc);prepareImports(staged);AudioConfig.preview(seed());
      log("MUSIC460 prepared two audio folders plus empty/missing sources; wait for scanner, then capture|run|source-start|view VIEW");return;
    }
    if(action.startsWith("source-")){source(mc,action);return;}
    if (action.startsWith("view ")) {
      if(staged==null)staged=new State(mc);prepareImports(staged);
      try { stage(mc, action.substring(5), 640, 420); }
      catch (Exception | Error failure) { stop(); throw failure; }
      log("MUSIC460 staged=" + action.substring(5) + "; use music-playlists-460 stop to restore"); return;
    }
    check(List.of("capture", "run", "menu-pools").contains(action), "use music-playlists-460 run|capture|menu-pools|view VIEW|stop");
    failures = 0;
    State owned=staged==null?new State(mc):staged;staged=null;
    try (var state = owned) {
      prepareImports(state);
      if (action.equals("capture")) capture(mc);
      else if (action.equals("menu-pools")) menuPools();
      else { selections(mc); sessions(mc); editor(mc); MusicPlaylistRevisionChecks.run(mc); libraryControls(mc); routes(mc); orderedPlayback(mc); menuAssignment(mc); menuPools(); MusicFolderChecks.run(mc); }
    }
    log("MUSIC460 " + action + " FAILURES " + failures + " directory=" + REPORT);
    check(failures == 0, "playlist failures=" + failures);
  }

  private static void stop() throws Exception {
    if (staged != null) { State state = staged; staged = null; state.close(); }
  }

  private static void prepareImports(State state) throws Exception {
    if(state.imports!=null)return;
    state.imports=Files.createTempDirectory(Path.of(".").toAbsolutePath(),"music-playlists-460-").toRealPath();
    List<String> folders=new ArrayList<>();
    String[] names={"Imported quiet","Imported building"},tracks={"calm1.ogg","creative1.ogg"};
    for(int i=0;i<names.length;i++) {
      Path folder=Files.createDirectory(state.imports.resolve(names[i]));Path song=folder.resolve(i==0?"Quiet sample.ogg":"Building sample.ogg");
      try(var in=MusicPlaylists460Checks.class.getResourceAsStream("/assets/powerbeta/music/"+tracks[i])){Files.copy(Objects.requireNonNull(in),song);}
      folders.add(folder.toString());if(i==0) {
        importedTrack=new MusicLibrary.Track(song.toRealPath(),song.getFileName().toString()).id();
        for(int copy=1;copy<8;copy++)Files.copy(song,folder.resolve("Quiet sample " + copy + ".ogg"));
      }
    }
    folders.add(Files.createDirectory(state.imports.resolve("Empty source")).toString());
    folders.add(state.imports.resolve("Missing source").toString());
    fixtureFolders=List.copyOf(folders);
  }

  private static void source(Minecraft mc,String action) throws Exception {
    check(staged!=null&&!importedTrack.isEmpty(),"run music-playlists-460 prepare and wait for scan first");
    switch(action) {
      case "source-start" -> {
        importedEntry=AudioController.resolveTrack(importedTrack);check(importedEntry!=null,"import scan pending");
        var sources = AudioController.sourceLibrary();
        check(MusicLibrarySources.id(fixtureFolders.get(0)).equals(sources.trackSources().get(importedTrack)), "catalog source ownership uses physical parent group");
        check(sources.folders().stream().anyMatch(source -> source.path().equals(fixtureFolders.get(2)) && source.status().equals("Empty")), "empty source status missing after scan");
        check(sources.folders().stream().anyMatch(source -> source.path().equals(fixtureFolders.get(3)) && source.status().equals("Unavailable")), "missing source status missing after scan");
        var settings=AudioConfig.copy();settings.sounds.put(importedTrack,37);AudioConfig.preview(settings);
        importedLabel=AudioController.musicLabel(importedTrack);importedGroup=AudioController.musicGroup(importedTrack);
        AudioController.playNow(importedTrack);AudioController.pause();AudioController.duration();
        check(AudioController.currentTrackId().equals(importedTrack),"imported current identity missing");
        log("MUSIC460 source started paused; source-remove then wait for scan, source-check");
      }
      case "source-remove" -> {
        MenuPreferences.update(value -> value.autoApply = false);
        var options = open(mc); var library = library(options);
        MusicFolderChecks.sourceClick(options, library, fixtureFolders.get(0), "sourceRemoveX");
        check(library.modal(), "native Remove skipped confirmation");
        click(options, options.width / 2 - 60, options.height / 2 + 20);
        check(!AudioConfig.current().musicDirectories.contains(fixtureFolders.get(0)), "native Remove retained source");
        log("MUSIC460 source removed inline; wait for scan then source-check");
      }
      case "source-check" -> {
        check(AudioController.customTracks().stream().noneMatch(t->t.id().equals(importedTrack)),"Removed source still scanned");
        check(AudioController.resolveTrack(importedTrack)==null,"Removed source still available to future selection");
        check(AudioController.trackId(importedEntry).equals(importedTrack),"owned stream/history entry lost stable identity after rescan");
        check(AudioController.currentTrackId().equals(importedTrack),"Remove rescan replaced current stream");
        check(AudioController.musicLabel(importedTrack).equals(importedLabel),"Remove rescan lost current label");
        check(AudioController.musicGroup(importedTrack).equals(importedGroup),"Remove rescan lost current group");
        check(AudioController.trackVolume(importedTrack)==37,"Remove rescan lost owned per-track gain");
        check(AudioController.duration()>0,"Removed current stream lost duration");
        log("MUSIC460 source Remove retains owned identity/label/group/gain/duration and blocks future selection PASS");
      }
      case "source-sticky" -> {
        failures = 0;
        MusicFolderChecks.sticky(mc, fixtureFolders.get(0));
        check(failures == 0, "sticky source control failures=" + failures);
        log("MUSIC460 clipped sticky source native controls PASS");
      }
      case "source-restore" -> {
        check(!AudioConfig.current().musicDirectories.contains(fixtureFolders.get(0)), "source must be removed before restore");
        check(activeOptions != null, "source Options session missing");
        activeOptions.session().discard();
        check(AudioConfig.current().musicDirectories.contains(fixtureFolders.get(0)), "Discard did not restore source");
        log("MUSIC460 source restored by Options Discard; wait for scan then source-restored-check");
      }
      case "source-restored-check" -> {
        check(AudioController.resolveTrack(importedTrack) != null, "restored source unavailable after scan");
        var source = AudioController.sourceLibrary().folders().stream().filter(value -> value.path().equals(fixtureFolders.get(0))).findFirst().orElseThrow();
        check(source.status().equals("Ready") && source.trackIds().contains(importedTrack), "restored source did not regain rows");
        check(AudioController.currentTrackId().equals(importedTrack), "restore rescan replaced owned stream");
        log("MUSIC460 source restore rescans and restores rows PASS");
      }
      default -> throw new IllegalArgumentException(action);
    }
  }

  static AudioSettings seed() {
    var settings = new AudioSettings();
    settings.musicDirectories=new ArrayList<>(fixtureFolders);
    settings.playlists.add(new MusicPlaylist("quiet", "Quiet afternoons", List.of(SECOND, FIRST, MISSING), Map.of("Alpha", 40)));
    settings.playlists.add(new MusicPlaylist("building", "Building", List.of("music:creative4.ogg", "music:creative1.ogg"), Map.of("Beta / Creative", 65)));
    settings.playlists.add(new MusicPlaylist("empty", "Empty", List.of(), Map.of()));
    settings.favourites.add(FIRST); settings.sounds.put(FIRST, 35);
    MusicPlaylist.load(settings, "quiet"); MusicPlaylist.loadMenu(settings, "building"); return settings;
  }

  static MusicPlaylist selected(AudioSettings settings) { return MusicPlaylist.find(settings, settings.playlist); }
  static PowerOptionsScreen open(Minecraft mc) throws Exception {
    activeOptions = MenuUpdateChecks.open(mc, "Audio");
    // Each case starts with a fresh editor rather than another case's remembered collapse state.
    field(activeOptions,"library",new MusicLibraryScreen(activeOptions));field(activeOptions,"libraryState",null);
    activeOptions.init(mc,640,420);
    ((Set<?>)field(activeOptions, "collapsed")).clear(); call(activeOptions, "layout"); return activeOptions;
  }

  static MusicLibraryScreen library(PowerOptionsScreen options) throws Exception {
    // The production Edit control supplies the native hit target after ordinary scrolling.
    revealMusic(options);
    click(options, (int)call(options, "musicEditX") + 8, (int)call(options, "musicEditY") + 8);
    check((boolean)call(options, "libraryVisible"), "Edit did not open music editor");
    return (MusicLibraryScreen)field(options, "library");
  }

  private static void revealMusic(PowerOptionsScreen options) throws Exception {
    int y=(int)call(options,"musicEditY"),top=(int)call(options,"top"),bottom=(int)call(options,"bottom");
    if(y<top+24||y+18>bottom) {
      Object row=call(options,"musicVolumeRow");field(options,"scroll",Math.max(0d,(int)call(row,"y")-24d));call(options,"layout");
      int overflow=(int)call(options,"musicEditY")+18-(int)call(options,"bottom");
      if(overflow>0){field(options,"scroll",(double)field(options,"scroll")+overflow);call(options,"layout");}
    }
    check((int)call(options,"musicEditY")>=(int)call(options,"top")&&(int)call(options,"musicEditY")+18<=(int)call(options,"bottom"),"Music Edit could not be scrolled into view");
  }

  static void tab(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    var tabs = (MusicFilters.Layout)call(library, "filters");
    var target = tabs.tabs().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
    click(options, target.x() + 5, target.y() + 5);
    check(library.state().folder().equals(id), "tab did not select " + id);
  }

  private static void openSelector(PowerOptionsScreen options) throws Exception {
    click(options, (int)call(options, "musicPlaylistX") + 5, (int)call(options, "musicPlaylistY") + 5);
    check((boolean)call(field(options, "choices"), "active"), "top playlist selector did not open");
  }

  private static void showGroup(PowerOptionsScreen options, String group) throws Exception {
    check(!(boolean)call(options, "libraryVisible"), "music settings unexpectedly inside editor");
    check(field(options, "page").equals("Audio"), "music settings left Audio");
    Object heading = ((List<?>)field(options, "rows")).stream().filter(row -> {
      try { return (boolean)call(row, "heading") && group.equals(call(row, "group")); }
      catch (Exception e) { throw new RuntimeException(e); }
    }).findFirst().orElseThrow(() -> new AssertionError("Audio group missing " + group));
    field(options, "scroll", (double)(int)call(heading, "y")); call(options, "layout");
  }

  private static void openSetting(PowerOptionsScreen options, String id) throws Exception {
    Object target=null;
    for(Object row:(List<?>)field(options,"rows")) {
      Setting setting=(Setting)call(row,"setting");if(setting!=null&&setting.id.equals(id)){target=row;break;}
    }
    check(target!=null,"music settings row missing "+id);
    int rowY=(int)call(target,"y");field(options,"scroll",Math.max(0d,rowY-24d));call(options,"layout");
    int x=(int)Dropdown450Screens.invoke(options,"mainControlLeft",new Class<?>[]{target.getClass()},target)+5;
    var offset=Class.forName("local.luke.power.ui.SettingRows").getDeclaredMethod("buttonOffset",int.class);offset.setAccessible(true);
    int y=(int)call(options,"top")+rowY-(int)(double)field(options,"scroll")+(int)offset.invoke(null,(int)call(options,"span"))+5;
    click(options,x,y);check((boolean)call(field(options,"choices"),"active"),"music setting dropdown missed "+id);
  }

  static void choose(PowerOptionsScreen options, int index) {
    var input = (ScreenInput)(Object)options; input.power$key('\0', Keyboard.KEY_HOME);
    for (int i = 0; i < index; i++) input.power$key('\0', Keyboard.KEY_DOWN);
    input.power$key('\0', Keyboard.KEY_RETURN);
  }

  private static void choosePlaylist(PowerOptionsScreen options, String id) {
    var values = MusicPlaylist.all(AudioConfig.current());
    int index = java.util.stream.IntStream.range(0, values.size()).filter(i -> values.get(i).id().equals(id)).findFirst().orElseThrow();
    choose(options, index);
  }

  static void context(PowerOptionsScreen options, MusicLibraryScreen library, String id, List<String> labels) throws Exception {
    ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows");
    int y = trackY(library, id);
    options.render(-1,-1,0);
    ((ScreenInput)(Object)options).power$click((int)call(library, "left") + 22, y + 8, 1);
    Object choices = field(options, "choices");
    check((boolean)call(choices, "active"), "song right click did not open parent dropdown");
    check(field(choices, "labels").equals(labels), "song context action order changed: " + field(choices, "labels"));
  }

  static void adding(PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    var panel = field(library, "presets");
    click(options, (int)call(panel, "addX") + 5, (int)call(panel, "actionY") + 5);
    check((boolean)call(field(panel, "choices"), "active"), "Add songs selector missed");
    choose(options, 0);
    check((boolean)call(panel, "adding"), "From Library did not enter nested Add view");
    call(library,"refresh");
  }

  static void edit(PowerOptionsScreen options, MusicLibraryScreen library, String id) throws Exception {
    tab(options, library, "playlists"); var panel = field(library, "presets");
    int index = 0;
    for (var playlist : MusicPlaylist.all(AudioConfig.current())) {
      if (playlist.id().equals(id)) break; index++;
    }
    var bounds = (ScrollBar.Track)call(panel, "track");
    field(panel, "scroll", Math.min(index * (int)call(panel, "rowHeight"), (int)bounds.maximum()));
    options.render(-1,-1,0);
    click(options, (int)Dropdown450Screens.invoke(panel, "editX", new Class<?>[]{int.class}, index) + 5,
        (int)Dropdown450Screens.invoke(panel, "actionY", new Class<?>[]{int.class}, index) + 5);
    check((boolean)call(panel, "editing"), "Edit missed playlist " + id);
  }

  static PowerOptionsScreen stage(Minecraft mc, String view, int width, int height) throws Exception {
    MenuPreferences.update(v -> v.autoApply = false); AudioConfig.preview(seed());
    var options = open(mc); options.init(mc, width, height);
    if (view.equals("settings")) { revealMusic(options); options.render(-1, -1, 0); return options; }
    if (view.equals("selector")) { openSelector(options); options.render(-1, -1, 0); return options; }
    if (Set.of("audio-playback", "audio-controls", "menu").contains(view)) {
      showGroup(options, view.equals("audio-controls") ? "Music menu controls" : "Music playback");
      if (view.equals("menu")) openSetting(options, "audio.menuPlaylist");
      options.render(-1, -1, 0); return options;
    }
    var library = library(options);
    switch (view) {
      case "library", "library-expanded" -> {
        tab(options, library, "library");
        if (view.equals("library-expanded")) { ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows"); }
      }
      case "folder-picker" -> {
        tab(options, library, "library");
        click(options, (int)call(library, "addFolderX") + 5, (int)call(library, "actionTop") + 5);
        check(mc.currentScreen instanceof DirectoryScreen, "Add folder did not open picker");
      }
      case "folder-remove" -> {
        tab(options, library, "library");
        int y = MusicFolderChecks.sourceY(library, MusicLibrarySources.id(fixtureFolders.get(0)));
        click(options, (int)call(library, "sourceRemoveX") + 5, y + 8);
        check(library.modal(), "Remove did not open confirmation");
      }
      case "favorites" -> {
        tab(options, library, "favorites");
        ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows");
      }
      case "playlists", "beta" -> {
        tab(options, library, "playlists");
        if (view.equals("beta")) { var s = find(options.session(), "audio.playlist"); s.value = new JsonPrimitive(MusicPlaylist.BETA); options.changed(s); }
      }
      case "new", "empty" -> {
        tab(options, library, "playlists"); var panel = field(library, "presets");
        click(options, (int)call(panel, "left") + 8, (int)call(panel, "top") + 8);
        check(((MusicPresetDraft)call(panel, "draft")).tracks().isEmpty(), "new playlist did not start empty");
      }
      case "editor", "group-gain", "grouped", "grouped-scrolled", "add-library", "add-library-scrolled", "from-playlist", "context" -> {
        edit(options, library, "quiet"); var panel = field(library, "presets");
        ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows");
        if (view.equals("group-gain") || view.startsWith("grouped")) {
          click(options, (int)call(panel, "groupedX") + 5, (int)call(panel, "actionY") + 5);
          if (view.equals("group-gain")) {
            int y=MusicGroupChecks.groupY(library,"Alpha");
            ((ScreenInput)(Object)options).power$click((int)call(library,"groupVolumeX")+8,y+8,1);
            check((boolean)call(field(options,"valueEditor"),"active"),"group exact entry did not open");
          }
        }
        if (view.startsWith("add-library")) { adding(options, library); ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows"); }
        if (view.equals("from-playlist")) {
          click(options, (int)call(panel, "addX") + 5, (int)call(panel, "actionY") + 5); choose(options, 1);
          check((boolean)call(field(panel, "choices"), "active"), "From playlist dropdown missing");
        }
        if (view.endsWith("-scrolled")) {
          ((Set<?>)field(library, "collapsed")).clear(); call(library, "rebuildRows");
          Object row=((List<?>)field(library,"rows")).stream().filter(r->{try{return FIRST.equals(call(r,"id"));}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
          Dropdown450Screens.invoke(library,"scroll",new Class<?>[]{int.class},(int)call(row,"y"));
        }
        if (view.equals("context")) context(options, library, SECOND, List.of("Add to queue", "Favourite", "Preview", "Remove from playlist"));
      }
      case "queue" -> {
        MusicRequests.edit(q -> { q.tracks.clear(); q.add(FIRST); q.add(SECOND); q.add(FIRST); }); library.showQueue();
      }
      default -> throw new IllegalArgumentException("Unknown playlist view " + view);
    }
    mc.currentScreen.render(-1, -1, 0); return options;
  }

  private static void capture(Minecraft mc) throws Exception {
    Files.createDirectories(REPORT); var captures = new ArrayList<String>();
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : new int[][]{{320, 240}, {854, 480}})
        for (String view : VIEWS) {
          var screen = stage(mc, view, size[0], size[1]); Screen captured = mc.currentScreen;
          if (captured != screen) captured.init(mc, size[0], size[1]);
          if (view.equals("menu") || view.equals("selector")) check((boolean)call(field(screen, "choices"), "active"), view + " lost its dropdown");
          String file = view + "-" + size[0] + ".png"; SettingsSnapshot.render(mc, captured, size[0], size[1], REPORT.resolve(file));
          captures.add(file); screen.session().discard(); screen.removed();
        }
      for (String view : List.of("grouped-scrolled", "add-library-scrolled")) {
        var screen=stage(mc,view,320,240);
        String file=view+"-320.png";
        SettingsSnapshot.render(mc,screen,320,240,REPORT.resolve(file));
        captures.add(file);screen.session().discard();screen.removed();
      }
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(captures)); log("MUSIC460 native captures=" + captures.size());
  }

  static void sessions(Minecraft mc) throws Exception {
    for (boolean automatic : new boolean[]{false, true})
      test("MUSIC460 native top selector Apply/Discard auto=" + automatic, () -> {
        MenuPreferences.update(v -> v.autoApply = automatic); AudioConfig.save(seed()); var options = open(mc);
        MusicRequests.edit(q -> { q.tracks.clear(); q.add(FIRST); q.add(FIRST); q.add(MISSING); }); var queued = MusicRequests.tracks();
        openSelector(options); choosePlaylist(options, "building");
        check(AudioConfig.current().playlist.equals("building"), "top selector did not choose the saved world playlist");
        check(PowerConfig.readAudio(AudioSettings.class).playlist.equals(automatic ? "building" : "quiet"), "auto/manual persistence boundary failed");
        check(MusicPlaylist.find(AudioConfig.current(), "quiet").tracks().equals(seed().playlists.get(0).tracks()), "selection edited saved membership");
        if (!automatic) click(options, actionX(options, 1) + 8, options.height - 20);
        check(PowerConfig.readAudio(AudioSettings.class).playlist.equals("building"), "Apply did not save selected identity");
        check(MusicRequests.tracks().equals(queued), "Apply replaced newer queue");
        check(PowerConfig.musicQueue().getAsJsonArray("tracks").size() == queued.size(), "Apply changed durable queue");
        if (!automatic) {
          openSelector(options); choose(options, 0);
          click(options, actionX(options, 0) + 8, options.height - 20);
          check((boolean)field(options, "confirmClose"), "dirty Options exit skipped confirmation");
          click(options, options.width / 2 + 12, options.height / 2 + 20);
          check(AudioConfig.current().playlist.equals("building"), "Discard lost committed world selection");
          check(MusicRequests.tracks().equals(queued), "Discard rolled back queue");
        } else check(options.session().changes() == 0, "auto-apply retained dirty draft");
      });
  }

  private static int actionX(PowerOptionsScreen screen, int action) throws Exception {
    return (int)Dropdown450Screens.invoke(screen, "actionX", new Class<?>[]{int.class}, action);
  }

  static void editor(Minecraft mc) throws Exception {
    test("MUSIC460 selected saved playlist changes only after isolated editor Save", () -> {
      MenuPreferences.update(v -> v.autoApply = false); AudioConfig.preview(seed());
      var options = open(mc); var library = library(options); edit(options, library, "quiet");
      var panel = field(library, "presets"); var draft = (MusicPresetDraft)call(panel, "draft"); var original = draft.tracks();
      check(!(boolean)call(panel, "grouped") && ((List<?>)field(library, "tracks")).equals(original), "editor did not start in saved flat order");
      check(org.lwjgl.opengl.Display.isActive(), "focus disposable client for drag checks");
      int x=(int)call(library, "left")+22, firstY=trackY(library, SECOND), lastY=trackY(library, MISSING);
      var input=(ScreenInput)(Object)options;
      input.power$click(x, firstY+8, 0); input.power$release(x,lastY+22,-1);
      check((boolean)call(field(library, "reorder"), "active"), "native motion release ended row drag");
      input.power$release(x,lastY+22,0);
      check(draft.tracks().equals(List.of(FIRST,MISSING,SECOND)), "flat drag ignored final release coordinates: " + draft.tracks());
      check(selected(AudioConfig.current()).tracks().equals(original), "row drag edited playback before Save");
      firstY=trackY(library,FIRST);input.power$click(x,firstY+8,0);input.power$key('\0',Keyboard.KEY_ESCAPE);
      input.power$release(x,lastY+22,0);
      check(draft.tracks().equals(List.of(FIRST,MISSING,SECOND)), "Escape failed to cancel row drag");
      firstY=trackY(library,FIRST);input.power$click(x,firstY+8,0);input.power$release(0,0,0);
      check(draft.tracks().equals(List.of(FIRST,MISSING,SECOND)), "outside release changed row order");
      draft.order(original);call(library, "rebuildRows");
      click(options,(int)call(panel,"groupedX")+5,(int)call(panel,"actionY")+5);
      check((boolean)call(panel,"grouped") && draft.tracks().equals(original), "Grouped changed stored order");
      ((Set<?>)field(library,"collapsed")).clear();call(library,"rebuildRows");
      int groupY=MusicGroupChecks.groupY(library,"Alpha");
      int gainX=(int)call(library,"groupVolumeX"),gainWidth=(int)call(library,"groupVolumeWidth");
      click(options,gainX+4+(int)Math.round((gainWidth-8)*.25),groupY+8);
      input.power$release(gainX+4+(int)Math.round((gainWidth-8)*.25),groupY+8,0);
      check(Math.abs(draft.volume("Alpha")-25)<=1,"native group gain missed");
      check(selected(AudioConfig.current()).groupVolumes().equals(Map.of("Alpha",40)),"group gain leaked before Save");
      input.power$click(gainX+8,MusicGroupChecks.groupY(library,"Alpha")+8,1);
      Object exact=field(options,"valueEditor");check((boolean)call(exact,"active"),"group exact entry missed");
      ((TextInput)field(exact,"input")).setText("31");input.power$key('\0',Keyboard.KEY_RETURN);
      check(draft.volume("Alpha")==31,"group exact Enter did not commit");
      input.power$click(gainX+8,MusicGroupChecks.groupY(library,"Alpha")+8,1);
      ((TextInput)field(exact,"input")).setText("99");input.power$key('\0',Keyboard.KEY_ESCAPE);
      check(draft.volume("Alpha")==31,"group exact Escape changed draft");
      click(options,(int)call(library,"groupResetX")+5,MusicGroupChecks.groupY(library,"Alpha")+8);
      check(draft.volume("Alpha")==100,"group reset missed");
      int groupedY=trackY(library,FIRST);input.power$click(x,groupedY+8,0);
      check(!(boolean)call(field(library,"reorder"),"active"),"Grouped allowed drag");input.power$release(x,groupedY+24,0);
      click(options,(int)call(panel,"groupedX")+5,(int)call(panel,"actionY")+5);
      check(draft.tracks().equals(original),"List/Grouped toggling changed order");
      context(options,library,SECOND,List.of("Add to queue","Favourite","Preview","Remove from playlist"));choose(options,3);
      check(!draft.included(SECOND) && draft.included(MISSING),"context removal changed wrong membership");
      adding(options, library);
      context(options,library,SECOND,List.of("Add to queue","Favourite","Preview","Add to playlist"));choose(options,3);
      check(draft.tracks().equals(List.of(FIRST,MISSING,SECOND)),"From Library did not append one ID");
      click(options,(int)call(panel,"addX")+5,(int)call(panel,"actionY")+5);
      check(!(boolean)call(panel,"adding"),"Back to playlist did not leave Add view");
      draft.include(SECOND,false);draft.order(List.of(MISSING,FIRST));draft.volume("Alpha",25);
      check(selected(AudioConfig.current()).tracks().equals(original),"unsaved membership leaked into playback");
      click(options, (int)call(panel, "saveX") + 5, (int)call(panel, "editButtonY") + 5);
      check(AudioConfig.current().playlist.equals("quiet"), "Save changed selected identity");
      check(selected(AudioConfig.current()).tracks().equals(List.of(MISSING, FIRST)), "Save lost membership/order/missing entry");
      check(new ArrayList<>(AudioController.activeMusic(mc)).equals(List.of(FIRST)), "selected saved playlist edit did not affect playback after Save");
      check(selected(AudioConfig.current()).groupVolumes().equals(Map.of("Alpha", 25)), "Save did not apply selected group gain");
      check(AudioConfig.current().sounds.get(FIRST) == 35 && AudioConfig.current().favourites.contains(FIRST), "playlist Save changed global song values");
      check(AudioConfig.current().menuPlaylist.equals("building"), "world Save changed menu assignment");
      options.session().discard(); check(selected(AudioConfig.current()).tracks().equals(original), "Options Discard retained previewed playlist save");
    });
    test("MUSIC460 native Beta entry blocks Edit/Delete and Copy opens an isolated draft",()->{
      AudioConfig.preview(seed());var options=open(mc);var library=library(options);tab(options,library,"playlists");
      var panel=field(library,"presets");int y=(int)Dropdown450Screens.invoke(panel,"actionY",new Class<?>[]{int.class},0)+5;
      click(options,(int)Dropdown450Screens.invoke(panel,"editX",new Class<?>[]{int.class},0)+5,y);
      check(!(boolean)call(panel,"editing"),"Beta exposed Edit");
      click(options,(int)call(panel,"deleteX")+5,y);check(!library.modal(),"Beta exposed Delete");
      click(options,(int)call(panel,"copyX")+5,y);var copy=(MusicPresetDraft)call(panel,"draft");
      check(copy!=null&&copy.tracks().equals(BuiltinMusic.VANILLA),"Beta Copy missed original membership/order");
      String id=copy.snapshot(copy.name()).id();check(!id.equals(MusicPlaylist.BETA),"Beta Copy reused reserved identity");
      copy.include(FIRST,false);click(options,(int)call(panel,"saveX")+5,(int)call(panel,"editButtonY")+5);
      check(MusicPlaylist.find(AudioConfig.current(),id).tracks().size()==11,"Beta Copy Save failed");
      check(MusicPlaylist.beta().tracks().equals(BuiltinMusic.VANILLA),"copy edited Beta originals");
      check(AudioConfig.current().playlist.equals("quiet"),"copy Save changed world selection");options.session().discard();
    });
    test("MUSIC460 Beta fixed originals and copy isolation", () -> {
      var settings = seed(); check(MusicPlaylist.beta().tracks().equals(BuiltinMusic.VANILLA), "Beta original IDs changed");
      check(MusicPlaylist.beta().tracks().size() == 12, "Beta is not twelve tracks");
      var copy = new MusicPresetDraft(MusicPlaylist.beta(), BuiltinMusic.VANILLA).snapshot("Beta copy");
      check(!copy.id().equals(MusicPlaylist.BETA), "Beta draft reused reserved identity");
      check(copy.tracks().equals(BuiltinMusic.VANILLA), "Beta copy lost native order");
      settings.playlists.add(new MusicPlaylist(MusicPlaylist.BETA, "Invalid", List.of(), Map.of()));
      boolean rejected = false; try { settings.validate(); } catch (IllegalArgumentException expected) { rejected = true; }
      check(rejected, "saved list accepted a replacement Beta entry");
    });
    test("MUSIC460 empty/new drafts preserve missing references", () -> {
      check(new MusicPresetDraft(null, List.of(FIRST, SECOND)).tracks().isEmpty(), "new draft copied library");
      var restored = new MusicPresetDraft(seed().playlists.get(0), List.of(FIRST, SECOND));
      restored.includeAll(); check(restored.tracks().contains(MISSING), "Include all removed unavailable ID");
    });
  }

  static int trackY(MusicLibraryScreen library,String id) throws Exception {
    call(library,"refresh");
    Object row=((List<?>)field(library,"rows")).stream().filter(r->{try{return id.equals(call(r,"id"));}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
    int y=(int)call(row,"y");Dropdown450Screens.invoke(library,"scroll",new Class<?>[]{int.class},Math.max(0,y-24));
    return (int)call(library,"listTop")+y-(int)call(library,"scroll");
  }

  static void libraryControls(Minecraft mc) throws Exception {
    test("MUSIC460 editor tabs and library controls preserve playlist membership", () -> {
      AudioConfig.preview(seed()); var options = open(mc); var library = library(options);
      String saved = Catalog.JSON.toJson(AudioConfig.current().playlists); var queue = MusicRequests.tracks();
      for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
        options.init(mc, size[0], size[1]); tab(options, library, "library");
        if (!library.sectionsCollapsed()) library.toggleSections();
        check(library.sectionsCollapsed(), "Library collapse all did not collapse current view");
        library.toggleSections(); check(!library.sectionsCollapsed(), "Library expand all failed");
        library.toggleSections(); check(library.sectionsCollapsed(), "Library collapse all failed");
        tab(options, library, "playlists"); tab(options, library, "favorites");
        check(((List<?>)field(library, "tracks")).equals(List.of(FIRST)), "Favorites tab did not filter starred tracks");
        check((boolean)call(field(library,"presets"),"favorites"), "Favorites did not open isolated fixed-name draft");
        var header = (MusicFilters.Layout)call(library, "filters");
        check(header.tabs().stream().map(MusicFilters.Tab::id).toList().equals(List.of("library", "playlists", "favorites")), "unexpected editor tabs");
        int finalTabRight = header.tabs().get(2).x() + header.tabs().get(2).width();
        check(header.actionsY() == header.tabs().get(0).y() ? header.reloadX() - finalTabRight >= 8 : header.actionsY() > header.tabs().get(0).y() + 18, "catalog actions overlap tabs");
        tab(options, library, "library");
        options.render(-1, -1, 0); check(GL11.glGetError() == 0, "native editor GL error");
      }
      library.showTrack(FIRST);int y=trackY(library,FIRST);var geometry=(MusicRowLayout)call(library,"rowLayout");
      click(options,geometry.volumeX()+4,y+geometry.controlsY()+5);library.removed();
      check(AudioConfig.current().sounds.get(FIRST)==0,"Library song gain slider missed");
      check(geometry.favouriteX()==-1 && geometry.previewX()==-1 && geometry.queueX()==-1 && geometry.upX()==-1 && geometry.downX()==-1,"song row retained obsolete controls");
      context(options,library,FIRST,List.of("Add to queue","Remove favourite","Preview"));choose(options,1);
      check(!AudioConfig.current().favourites.contains(FIRST),"Library context favourite action missed");
      click(options,(int)call(library,"addFolderX")+5,(int)call(library,"actionTop")+5);
      check(mc.currentScreen instanceof DirectoryScreen,"Library Add folder button missed");
      ((ScreenInput)(Object)mc.currentScreen).power$key('\0',Keyboard.KEY_ESCAPE);
      check(mc.currentScreen==options,"picker Cancel did not return to editor");
      click(options,(int)call(library,"reloadX")+5,(int)call(library,"actionTop")+5);
      check(saved.equals(Catalog.JSON.toJson(AudioConfig.current().playlists)), "library browsing edited saved playlists");
      check(queue.equals(MusicRequests.tracks()), "library browsing edited queue"); options.session().discard();
    });
  }

  private static void orderedPlayback(Minecraft mc) throws Exception {
    test("MUSIC460 native Shuffle toggle and saved order replace stale random reservations",()->{
      String third="music:calm3.ogg",id="native-order-fixture";
      var settings=seed();settings.shuffle=true;settings.avoidRepeats=false;
      settings.playlists.add(new MusicPlaylist(id,"Ordered native fixture",List.of(FIRST,SECOND,third),Map.of()));MusicPlaylist.load(settings,id);AudioConfig.preview(settings);
      var options=open(mc);var library=library(options);tab(options,library,"playlists");var panel=field(library,"presets");
      TrackSelector selector=(TrackSelector)staticField(AudioController.class,"WORLD");
      @SuppressWarnings("unchecked") var cursors=(Map<Object,Object>)field(selector,"cursors");var cursorSnapshot=new LinkedHashMap<>(cursors);
      Object previous=field(selector,"previous"),oldUpcoming=staticField(AudioController.class,"upcoming"),oldKey=staticField(AudioController.class,"nextKey");
      @SuppressWarnings("unchecked") var history=(List<class_267>)staticField(AudioController.class,"history");var savedHistory=List.copyOf(history);
      Object historyIndex=staticField(AudioController.class,"historyIndex");var queued=MusicRequests.tracks();
      try {
        history.clear();staticField(AudioController.class,"historyIndex",-1);MusicRequests.edit(q->q.tracks.clear());
        var tracks=List.of(AudioController.resolveTrack(FIRST),AudioController.resolveTrack(SECOND),AudioController.resolveTrack(third));
        String context="playlist:"+id;selector.reserve(context,tracks,false,false,new Random(0),AudioController::trackId);selector.played(context,FIRST);
        Class<?> contextType=Class.forName("local.luke.power.audio.AudioController$Context"),keyType=Class.forName("local.luke.power.audio.AudioController$PoolKey");
        var contextMethod=AudioController.class.getDeclaredMethod("context",Minecraft.class);contextMethod.setAccessible(true);Object nativeContext=contextMethod.invoke(null,mc);
        var key=keyType.getDeclaredConstructor(AudioSettings.class,MusicRules.class,contextType,int.class);key.setAccessible(true);
        staticField(AudioController.class,"upcoming",tracks.get(2));staticField(AudioController.class,"nextKey",key.newInstance(AudioConfig.current(),AudioController.rules(),nativeContext,AudioController.libraryRevision()));
        click(options,(int)call(panel,"shuffleX")+5,(int)call(panel,"top")+5);
        check(!AudioConfig.current().shuffle&&!find(options.session(),"audio.shuffle").value.getAsBoolean(),"Playlists Shuffle button missed shared setting");
        var upcoming=AudioController.class.getDeclaredMethod("upcoming");upcoming.setAccessible(true);
        Object next=upcoming.invoke(null);check(AudioController.trackId((class_267)next).equals(SECOND),"Shuffle Off kept stale random reservation");
        var gain=find(options.session(),"audio.sound."+FIRST);gain.value=new JsonPrimitive(12);options.changed(gain);
        check(upcoming.invoke(null)==next,"gain preview consumed/replaced ordered reservation");
        var reordered=AudioConfig.copy();int index=0;while(!reordered.playlists.get(index).id().equals(id))index++;
        reordered.playlists.set(index,new MusicPlaylist(id,"Ordered native fixture",List.of(FIRST,third,SECOND),Map.of()));
        var saved=find(options.session(),"audio.playlists");saved.value=Catalog.JSON.toJsonTree(reordered.playlists);options.changed(saved);
        check(AudioController.trackId((class_267)upcoming.invoke(null)).equals(third),"saved same-membership reorder kept old successor");
      } finally {
        options.session().discard();cursors.clear();cursors.putAll(cursorSnapshot);field(selector,"previous",previous);
        history.clear();history.addAll(savedHistory);staticField(AudioController.class,"historyIndex",historyIndex);
        staticField(AudioController.class,"upcoming",oldUpcoming);staticField(AudioController.class,"nextKey",oldKey);
        MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(queued);});
      }
    });
  }

  private static void modal(PowerOptionsScreen options,int choice) {
    int width=Math.min(312,options.width-20);
    click(options,(options.width-width)/2+choice*(width/3)+5,options.height/2+8);
  }

  private static void playlistAction(PowerOptionsScreen options,MusicLibraryScreen library,String id,String action) throws Exception {
    tab(options,library,"playlists");var panel=field(library,"presets");var list=MusicPlaylist.all(AudioConfig.current());
    int index=0;while(index<list.size()&&!list.get(index).id().equals(id))index++;
    check(index<list.size(),"playlist action target missing "+id);
    var bounds=(ScrollBar.Track)call(panel,"track");field(panel,"scroll",Math.min(index*(int)call(panel,"rowHeight"),(int)bounds.maximum()));
    options.render(-1,-1,0);
    int y=(int)Dropdown450Screens.invoke(panel,"actionY",new Class<?>[]{int.class},index)+5;
    click(options,(int)call(panel,action+"X")+5,y);
  }

  static void routes(Minecraft mc) throws Exception {
    test("MUSIC460 deleting selected world/menu playlist updates both references atomically",()->{
      MenuPreferences.update(v->v.autoApply=false);var settings=seed();MusicPlaylist.loadMenu(settings,"quiet");AudioConfig.preview(settings);
      var options=open(mc);var library=library(options);var queued=MusicRequests.tracks();
      playlistAction(options,library,"quiet","delete");check(library.modal(),"Delete skipped confirmation");
      modal(options,1);check(AudioConfig.current().playlist.equals("quiet"),"Delete Cancel changed selection");
      playlistAction(options,library,"quiet","delete");modal(options,0);
      check(AudioConfig.current().playlists.stream().noneMatch(p->p.id().equals("quiet")),"Delete retained saved playlist");
      check(AudioConfig.current().playlist.equals(MusicPlaylist.BETA),"deleted world selection did not return to Beta");
      check(AudioConfig.current().menuPlaylist.equals(MusicPlaylist.SAME_AS_WORLD),"deleted menu selection did not return to same world");
      AudioConfig.current().validate();check(MusicRequests.tracks().equals(queued),"Delete changed queue");
      options.session().discard();check(AudioConfig.current().playlist.equals("quiet")&&AudioConfig.current().menuPlaylist.equals("quiet"),"Discard did not restore deleted selection/references");
    });
    for(int choice:new int[]{0,1,2})test("MUSIC460 playlist navigation modal choice="+choice,()->{
      AudioConfig.preview(seed());var options=open(mc);var library=library(options);tab(options,library,"playlists");var panel=field(library,"presets");
      click(options,(int)call(panel,"left")+5,(int)call(panel,"top")+5);var draft=(MusicPresetDraft)call(panel,"draft");
      ((TextInput)field(panel,"name")).setText("Modal fixture "+choice);draft.include(FIRST,true);
      var target=((MusicFilters.Layout)call(library,"filters")).tabs().stream().filter(t->t.id().equals("library")).findFirst().orElseThrow();
      click(options,target.x()+5,target.y()+5);check(library.modal(),"dirty tab navigation skipped modal");
      click(options,1,1);check(draft.tracks().equals(List.of(FIRST)),"modal click reached draft behind it");
      modal(options,choice);
      if(choice==2) {
        check(library.dirtyPreset()&&!library.modal()&&draft.tracks().equals(List.of(FIRST)),"Cancel lost isolated draft");
        click(options,target.x()+5,target.y()+5);modal(options,1);
      } else {
        check(library.state().folder().equals("library"),"Save/Discard did not finish tab navigation");
        check(AudioConfig.current().playlists.size()==(choice==0?4:3),"modal saved/discarded wrong playlist state");
      }
      check(AudioConfig.current().playlist.equals("quiet"),"modal changed world selection");options.session().discard();
    });
    test("MUSIC460 Edit from global Music search returns to original page/query/scroll",()->{
      AudioConfig.preview(seed());var options=open(mc);Class<?> scope=Class.forName("local.luke.power.ui.OptionsSearchScope");
      Object previousScope=staticField(scope,"choice");
      try {
        staticField(scope,"choice",false);field(options,"page","General");((TextInput)field(options,"search")).setText("Music");call(options,"layout");
        Object row=((List<?>)field(options,"rows")).stream().filter(r->{try{var setting=(Setting)call(r,"setting");return setting!=null&&setting.id.equals("native.music");}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
        field(options,"scroll",Math.max(0d,(int)call(row,"y")-24d));call(options,"layout");double scroll=(double)field(options,"scroll");
        library(options);check(field(options,"page").equals("Audio"),"global-search Edit did not enter Audio editor");
        click(options,(int)call(options,"left")+5,35);
        check(!(boolean)call(options,"libraryVisible")&&field(options,"page").equals("General"),"Back lost original global-search page");
        check(((TextInput)field(options,"search")).text().equals("Music")&&(double)field(options,"scroll")==scroll,"Back lost Music query/scroll");
      } finally {staticField(scope,"choice",previousScope);options.session().discard();}
    });
    test("MUSIC460 Audio playback settings accept arrow Tab and Enter",()->{
      AudioConfig.preview(seed());var options=open(mc);showGroup(options,"Music playback");
      ((TextInput)field(options,"search")).focused=false;field(options,"selected",-1);
      var input=(ScreenInput)(Object)options;input.power$key('\0',Keyboard.KEY_DOWN);int first=(int)field(options,"selected");
      input.power$key('\0',Keyboard.KEY_TAB);check((int)field(options,"selected")!=first,"Settings Tab did not move selection");
      var rows=(List<?>)field(options,"rows");int index=-1;
      for(int i=0;i<rows.size();i++){var setting=(Setting)call(rows.get(i),"setting");if(setting!=null&&setting.id.equals("audio.menuMusic")){index=i;break;}}
      check(index>=0,"menu behavior row missing");field(options,"selected",index);
      field(options,"scroll",Math.max(0d,(int)call(rows.get(index),"y")-24d));call(options,"layout");int before=find(options.session(),"audio.menuMusic").value.getAsInt();
      input.power$key('\0',Keyboard.KEY_RIGHT);check((boolean)call(field(options,"choices"),"active"),"Settings Right did not open shared dropdown");
      input.power$key('\0',Keyboard.KEY_DOWN);input.power$key('\0',Keyboard.KEY_RETURN);
      check(find(options.session(),"audio.menuMusic").value.getAsInt()!=before,"Settings dropdown Down/Enter did not choose next value");
      input.power$key('\0',Keyboard.KEY_LEFT);check((boolean)call(field(options,"choices"),"active"),"Settings Left did not open shared dropdown");
      input.power$key('\0',Keyboard.KEY_UP);input.power$key('\0',Keyboard.KEY_RETURN);
      check(find(options.session(),"audio.menuMusic").value.getAsInt()==before,"Settings dropdown Up/Enter did not restore prior value");
      for(int i=0;i<rows.size();i++){var setting=(Setting)call(rows.get(i),"setting");if(setting!=null&&setting.id.equals("audio.menuPlaylist")){field(options,"selected",i);break;}}
      input.power$key('\0',Keyboard.KEY_RETURN);check((boolean)call(field(options,"choices"),"active"),"Settings Enter did not open menu playlist dropdown");
      input.power$key('\0',Keyboard.KEY_ESCAPE);options.session().discard();
    });
  }

  static void menuAssignment(Minecraft mc) throws Exception {
    test("MUSIC460 native menu selector keeps world identity and empty membership", () -> {
      AudioConfig.preview(seed()); var options = open(mc); showGroup(options, "Music playback");
      openSetting(options, "audio.menuPlaylist");
      ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_END); ((ScreenInput)(Object)options).power$key('\0', Keyboard.KEY_RETURN);
      check(AudioConfig.current().menuPlaylist.equals("empty"), "menu selector missed final saved playlist");
      check(MusicFixtureSupport.menuTracks(AudioConfig.current()).isEmpty(), "empty menu playlist expanded");
      check(AudioConfig.current().playlist.equals("quiet"), "menu selector changed world playlist");
      options.session().discard(); check(AudioConfig.current().menuPlaylist.equals("building"), "Discard lost menu assignment");
    });
  }

  static void selections(Minecraft mc) throws Exception {
    test("MUSIC460 selected identity reads saved membership and rejects retired storage", () -> {
      var settings = seed(); AudioConfig.preview(settings);
      check(new ArrayList<>(AudioController.activeMusic(mc)).equals(List.of(SECOND, FIRST)), "world saved order/missing resolution changed");
      MusicPlaylist.loadMenu(settings, "empty"); check(MusicFixtureSupport.menuTracks(settings).isEmpty(), "empty menu fell back to Beta");
      MusicPlaylist.loadMenu(settings, MusicPlaylist.SAME_AS_WORLD); check(MusicFixtureSupport.menuTracks(settings).equals(selected(settings).tracks()), "same-as-world lost reference");
      MusicPlaylist.load(settings, "empty"); AudioConfig.preview(settings); check(AudioController.activeMusic(mc).isEmpty(), "empty world playlist fell back");
      MusicPlaylist.load(settings, MusicPlaylist.BETA); AudioConfig.preview(settings);
      check(new ArrayList<>(AudioController.activeMusic(mc)).equals(BuiltinMusic.VANILLA), "Beta selection is not original twelve IDs");
      var json = Catalog.JSON.toJsonTree(settings).getAsJsonObject();
      for (String retired : List.of("playlistTracks", "menuPlaylistTracks", "disabledTracks", "groupVolumes", "menuGroupVolumes", "musicMode", "customMusic"))
        check(!json.has(retired), "retired storage still serialized: " + retired);
    });
  }

  private static void menuPools() throws Exception {
    Class<?> contextType = Class.forName("local.luke.power.audio.AudioController$Context");
    var constructor = contextType.getDeclaredConstructor(boolean.class, int.class, String.class); constructor.setAccessible(true);
    Object menuContext = constructor.newInstance(false, 0, null);
    var pool = AudioController.class.getDeclaredMethod("backgroundPool", contextType);
    var origin = AudioController.class.getDeclaredMethod("usesMenuVolumes", String.class, contextType); pool.setAccessible(true); origin.setAccessible(true);
    List<String> world = List.of(SECOND, FIRST); String creative = "music:creative4.ogg";
    List<String> menu = List.of(FIRST, creative, SECOND, "music:creative1.ogg");
    for (var mode : AudioSettings.MenuMusic.values())
      for (String assignment : List.of(MusicPlaylist.SAME_AS_WORLD, MusicPlaylist.BETA, "building", "empty"))
        test("MUSIC460 native menu pool and source gain " + mode + " " + assignment, () -> {
          var settings = seed(); settings.master = 80; settings.sounds.put(creative, 50);
          settings.playlists.set(1, new MusicPlaylist("building", "Building", menu, Map.of("Alpha", 25, "Beta / Creative", 65)));
          settings.menuMusic = mode; MusicPlaylist.loadMenu(settings, assignment); AudioConfig.preview(settings);
          boolean separate = mode != AudioSettings.MenuMusic.WORLD && !assignment.equals(MusicPlaylist.SAME_AS_WORLD);
          List<String> expected = new ArrayList<>(); if (mode != AudioSettings.MenuMusic.CUSTOM || !separate) expected.addAll(world);
          if (separate) for (String id : MusicPlaylist.find(settings, assignment).tracks()) if (!expected.contains(id)) expected.add(id);
          @SuppressWarnings("unchecked") List<class_267> tracks = (List<class_267>)pool.invoke(null, menuContext);
          check(tracks.stream().map(AudioController::trackId).toList().equals(expected), "menu pool changed order/membership");
          for (String id : expected) {
            boolean menuGain = separate && (mode == AudioSettings.MenuMusic.CUSTOM || !world.contains(id));
            check((boolean)origin.invoke(null, id, menuContext) == menuGain, "wrong source gain owner " + id);
            float group = menuGain ? MusicPlaylist.find(settings, assignment).groupVolumes().getOrDefault(AudioController.musicGroup(id), 100) / 100f
                : selected(settings).groupVolumes().getOrDefault(AudioController.musicGroup(id), 100) / 100f;
            float expectedGain = .8f * group * settings.sounds.getOrDefault(id, 100) / 100f;
            Object old = staticField(AudioController.class, "currentMusic"), oldOrigin = staticField(AudioController.class, "currentUsesMenuVolumes"), paused = staticField(AudioController.class, "paused");
            try {
              staticField(AudioController.class, "currentMusic", id); staticField(AudioController.class, "currentUsesMenuVolumes", menuGain); staticField(AudioController.class, "paused", false);
              check(Math.abs(AudioController.backgroundVolume(1f) - expectedGain) < .00001f, "native source gain differs " + id);
            } finally { staticField(AudioController.class, "currentMusic", old); staticField(AudioController.class, "currentUsesMenuVolumes", oldOrigin); staticField(AudioController.class, "paused", paused); }
          }
        });
  }

  static final class State implements AutoCloseable {
    private final Minecraft mc;
    private final Screen previous;
    private final AudioSettings audio;
    private final Object queue, menu;
    private final boolean repeat;
    private final Map<Path, byte[]> files = new LinkedHashMap<>();
    private final Map<Path, Set<Path>> backups = new LinkedHashMap<>();
    private final Set<Path> newBackupDirectories = new HashSet<>();
    private Path imports;

    State(Minecraft mc) throws Exception {
      activeOptions = null; this.mc = mc; previous = mc.currentScreen; repeat = Keyboard.areRepeatEventsEnabled();
      PowerConfig.flushPending(); audio = AudioConfig.copy();
      queue = staticField(MusicRequests.class, "queue"); menu = staticField(MenuPreferences.class, "current");
      for (Path path : List.of(PowerConfig.path(), PowerConfig.musicPath(), Path.of("options.txt").toAbsolutePath())) {
        files.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
        Path directory = path.getParent().resolve("backups");
        if (!Files.exists(directory)) newBackupDirectories.add(directory);
        try (var children = Files.exists(directory) ? Files.list(directory) : java.util.stream.Stream.<Path>empty()) {
          backups.put(directory, new HashSet<>(children.toList()));
        }
      }
    }

    @Override public void close() throws Exception {
      Exception discardFailure = null;
      if(imports!=null&&importedEntry!=null){AudioController.quiet();importedEntry=null;}
      if (activeOptions != null) try { activeOptions.session().discard(); activeOptions.removed(); activeOptions = null; }
      catch (Exception failure) { discardFailure = failure; }
      AudioConfig.preview(audio); staticField(MusicRequests.class, "queue", queue); staticField(MenuPreferences.class, "current", menu);
      PowerConfig.flushPending();
      for (var entry : files.entrySet()) {
        if (entry.getValue() == null) Files.deleteIfExists(entry.getKey());
        else Files.write(entry.getKey(), entry.getValue());
      }
      for (var entry : backups.entrySet()) if (Files.exists(entry.getKey())) {
        try (var children = Files.list(entry.getKey())) {
          for (Path path : children.toList()) if (!entry.getValue().contains(path)) Files.delete(path);
        }
      }
      for (Path directory : newBackupDirectories) Files.deleteIfExists(directory);
      mc.setScreen(previous); Keyboard.enableRepeatEvents(repeat);
      for (var entry : files.entrySet()) check(entry.getValue() == null ? !Files.exists(entry.getKey()) : Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())), "fixture did not restore " + entry.getKey().getFileName());
      check(Catalog.JSON.toJson(AudioConfig.current()).equals(Catalog.JSON.toJson(audio)), "fixture did not restore runtime audio");
      if(imports!=null) {
        try(var paths=Files.walk(imports)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
        fixtureFolders=List.of();importedTrack="";
      }
      if (discardFailure != null) throw discardFailure;
    }
  }

  private static Object staticField(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(null);
  }
  private static void staticField(Class<?> type, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
  }
}
