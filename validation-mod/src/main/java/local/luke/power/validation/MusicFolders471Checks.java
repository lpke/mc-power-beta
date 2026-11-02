package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.Release240Checks.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Real scanner snapshots, missing-source warnings and native picker/Library geometry. */
final class MusicFolders471Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-471/music-folders");
  static void run(Minecraft mc) throws Exception { run(mc,"run"); }
  static void run(Minecraft mc,String phase) throws Exception {
    check(Set.of("capture","run").contains(phase),"use music-folders-471 capture|run");
    check(mc.world != null && Display.isActive() && mc.options.musicVolume == 0 && mc.options.soundVolume == 0,
        "focus a muted disposable world");
    check(Display.getWidth() >= 1708 && Display.getHeight() >= 960, "resize disposable display to at least1708x960");
    Files.createDirectories(REPORT); Path scratch = Files.createTempDirectory("power-beta-folders-471-"); failures = 0;
    Files.createDirectories(scratch.resolve("replacement-one")); Files.createDirectories(scratch.resolve("replacement-two"));
    String browser = MenuPreferences.current().musicBrowserDirectory;
    var ownPaths = new ArrayList<String>();
    try (var state = new Release462Checks.State(mc)) {
      for (int[] size : new int[][]{{854,480},{320,240}}) test("MUSIC FOLDERS471 "+phase+" multi-missing/relink/single-picker " + size[0], () -> {
        Path first = scratch.resolve("first-" + size[0]).resolve("music"), second = scratch.resolve("second-" + size[0]).resolve("music");
        List<String> roots = List.of(first.toString(), second.toString()); ownPaths.addAll(roots);
        var options = state.open(size, MusicPlaylist.BETA, false);
        setting(options.session(), "audio.musicDirectories").value = MusicFolders.encode(roots); options.session().preview(); scan(mc);
        var snapshot = AudioController.sourceLibrary();
        call(options, "openMusicEditor"); var library = (MusicLibraryScreen)get(options, "library"); call(library, "refresh");
        if(phase.equals("capture")) { captureViews(mc,options,library,size,scratch,first,second); return; }
        check(snapshot.folders().stream().filter(s -> s.status().equals("Unavailable") && roots.contains(s.path())).count() == 2,
            "native scan collapsed missing roots with the same basename");
        Object folders = get(library, "folders"); Set<String> noticed = new HashSet<>();
        for (int index = 0; index < 2; index++) {
          check((boolean)call(folders, "modal"), "missing-source warning ended before every root was shown");
          noticed.add((String)get(folders, "removing"));
          SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("missing-" + index + "-" + size[0] + ".png"));
          key(options, Keyboard.KEY_ESCAPE);
        }
        check(noticed.equals(new HashSet<>(roots)) && !(boolean)call(folders, "modal"), "missing warnings skipped or repeated a path");
        for (int frame = 0; frame < 10; frame++) call(library, "refresh");
        check(!(boolean)call(folders, "modal"), "unchanged missing sources repeated their warnings");
        Object row = reveal(library, MusicLibrarySources.id(first.toString())); int y = rowY(library, row);
        int cy = ((Number)call(library, "groupControlsY", row, y)).intValue(), relink = n(library, "sourceRelinkX"), remove = n(library, "sourceRemoveX");
        check(n(library, "sourceRelinkWidth") == 18 && relink + 18 + 4 == remove && relink >= n(library, "left"), "relink square overlaps neighboring controls");
        Path shot = REPORT.resolve("library-missing-" + size[0] + ".png"); SettingsSnapshot.render(mc, options, size[0], size[1], shot);
        BufferedImage image = ImageIO.read(shot.toFile());
        check(pixels(image, n(library, "left"), y, remove - n(library, "left"), 18, 0xaaaaaa) > 0, "missing source name is not gray");
        check(pixels(image, n(library, "left"), cy, relink - n(library, "left"), 18, 0xff8888) > 0, "Source missing text is not red");
        SettingsSnapshot.render(mc, options, size[0], size[1], REPORT.resolve("relink-tooltip-" + size[0] + ".png"), relink + 5, cy + 5);
        check(call(library, "hoverHelp").equals("Relink source path"), "relink tooltip changed");
        Object identity = AudioController.sourceLibrary(); int revision = AudioController.libraryRevision(); Files.createDirectories(first);
        for (int frame = 0; frame < 25; frame++) { options.tick(); options.render(-1,-1,0); }
        check(AudioController.sourceLibrary() == identity && AudioController.libraryRevision() == revision
            && AudioController.sourceLibrary().folders().stream().anyMatch(s -> s.path().equals(first.toString()) && s.status().equals("Unavailable")),
            "rendering/ticking performed an implicit filesystem rescan");
        MenuPreferences.current().musicBrowserDirectory = scratch.toString();
        click(options, relink + 5, cy + 5, 0);
        check(mc.currentScreen instanceof DirectoryScreen, "relink magnifier did not open the native folder chooser");
        DirectoryScreen chooser = (DirectoryScreen)mc.currentScreen; chooser.init(mc,size[0],size[1]);
        check(get(chooser, "replacing").equals(first.toString()) && (boolean)call(chooser, "singleFolder"), "chooser lost the exact old source path or single selection");
        List<?> context = (List<?>)call(chooser, "sourceContext");
        check(context.get(0).toString().contains("music") && context.size() >= 2 && n(chooser, "listBottom") - n(chooser, "listTop") >= 20,
            "compact relink context hid old folder identity or every complete row");
        SettingsSnapshot.render(mc, chooser, size[0], size[1], REPORT.resolve("relink-picker-" + size[0] + ".png"));
        singleSelection(mc, chooser, scratch, size);
        key(chooser, Keyboard.KEY_ESCAPE); check(mc.currentScreen == options, "relink Cancel lost the Music screen");
        call(library, "refresh"); row = reveal(library, MusicLibrarySources.id(second.toString()));
        click(options, n(library, "left") + 5, rowY(library,row) + 5, 1);
        Object choices = get(options, "choices"); check(((List<?>)get(choices, "labels")).contains("Relink source path"), "heading context lost relink action");
        key(options, Keyboard.KEY_ESCAPE);
        DirectoryScreen adding = new DirectoryScreen(options, values -> {}, roots); mc.setScreen(adding); adding.init(mc,size[0],size[1]); call(adding, "go", scratch);
        List<?> children = (List<?>)get(adding, "children"); check(children.size() >= 2, "fixture needs two child folders");
        int left = n(adding, "panelLeft"), top = n(adding, "listTop"); click(adding,left+20,top+5,0); click(adding,left+20,top+25,0);
        check(((Set<?>)get(adding, "selected")).size() == 2, "Add folder lost ordinary multi-selection"); key(adding, Keyboard.KEY_ESCAPE);
      });
    } finally {
      MenuPreferences.current().musicBrowserDirectory = browser;
      @SuppressWarnings("unchecked") var warned = (Set<String>)staticField(Class.forName("local.luke.power.ui.MusicFolderActions"), "warnedMissing"); warned.removeAll(ownPaths);
      try (var paths = Files.walk(scratch)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
    }
    Files.writeString(REPORT.resolve(phase+".json"),"{\"failures\":"+failures+"}");
    log("MUSIC FOLDERS471 "+phase+" complete failures="+failures);check(failures==0,"music-folders-471 failures="+failures);
  }
  private static void captureViews(Minecraft mc,PowerOptionsScreen options,MusicLibraryScreen library,int[] size,Path scratch,Path first,Path second)throws Exception{
    Object folders=get(library,"folders");
    for(int index=0;index<2;index++){
      SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("missing-"+index+"-"+size[0]+".png"));
      if((boolean)call(folders,"modal"))key(options,Keyboard.KEY_ESCAPE);
    }
    Object row=reveal(library,MusicLibrarySources.id(first.toString()));int y=rowY(library,row);
    int cy=((Number)call(library,"groupControlsY",row,y)).intValue(),relink=n(library,"sourceRelinkX");
    SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("library-missing-"+size[0]+".png"));
    SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("relink-tooltip-"+size[0]+".png"),relink+5,cy+5);
    MenuPreferences.current().musicBrowserDirectory=scratch.toString();click(options,relink+5,cy+5,0);
    if(mc.currentScreen instanceof DirectoryScreen chooser){
      chooser.init(mc,size[0],size[1]);SettingsSnapshot.render(mc,chooser,size[0],size[1],REPORT.resolve("relink-picker-"+size[0]+".png"));
      call(chooser,"go",scratch);int left=n(chooser,"panelLeft"),top=n(chooser,"listTop");
      click(chooser,left+20,top+5,0);click(chooser,left+20,top+25,0);
      SettingsSnapshot.render(mc,chooser,size[0],size[1],REPORT.resolve("relink-single-"+size[0]+".png"));key(chooser,Keyboard.KEY_ESCAPE);
    }
    call(library,"refresh");row=reveal(library,MusicLibrarySources.id(second.toString()));click(options,n(library,"left")+5,rowY(library,row)+5,1);
    SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("relink-context-"+size[0]+".png"));key(options,Keyboard.KEY_ESCAPE);
  }
  private static void singleSelection(Minecraft mc, DirectoryScreen chooser, Path scratch, int[] size) throws Exception {
    call(chooser, "go", scratch); List<?> children = (List<?>)get(chooser, "children"); check(children.size() >= 2, "fixture needs two replacement folders");
    int left=n(chooser,"panelLeft"),top=n(chooser,"listTop"); click(chooser,left+20,top+5,0);click(chooser,left+20,top+25,0);
    Set<?> selected=(Set<?>)get(chooser,"selected");check(selected.size()==1&&selected.contains(children.get(1)),"relink allowed multiple selected folders");
    click(chooser,n(chooser,"selectX")+5,n(chooser,"selectY")+5,0);check(selected.size()==1,"relink retained a hidden Select all hit target");
    SettingsSnapshot.render(mc,chooser,size[0],size[1],REPORT.resolve("relink-single-"+size[0]+".png"));
  }
  private static void scan(Minecraft mc)throws Exception{long end=System.nanoTime()+5_000_000_000L;do{AudioController.tick(mc);if(staticField(AudioController.class,"pending")==null)return;Thread.sleep(10);}while(System.nanoTime()<end);throw new AssertionError("native source scan timed out");}
  private static Object reveal(MusicLibraryScreen library,String id)throws Exception{((TextInput)get(library,"search")).setText("");call(library,"rebuild");for(Object row:(List<?>)get(library,"rows"))if(id.equals(call(row,"group"))&&call(row,"id")==null){call(library,"scroll",Math.max(0,(int)call(row,"y")));return row;}throw new AssertionError("missing source heading "+id);}
  private static int rowY(MusicLibraryScreen library,Object row)throws Exception{return n(library,"listTop")+(int)call(row,"y")-n(library,"scroll");}
  private static Object staticField(Class<?> type,String name)throws Exception{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(null);}
  private static int pixels(BufferedImage image,int x,int y,int width,int height,int color){int count=0;for(int dy=Math.max(0,y*2);dy<Math.min(image.getHeight(),(y+height)*2);dy++)for(int dx=Math.max(0,x*2);dx<Math.min(image.getWidth(),(x+width)*2);dx++)if((image.getRGB(dx,dy)&0xffffff)==color)count++;return count;}
}
