package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Focused native context-row evidence; all changes belong to the disposable client. */
final class Release467UiChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-467/ui");
  private static final List<Map<String, Object>> captures = new ArrayList<>();
  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("capture", "check").contains(action), "use release-467-ui capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && !mc.world.isRemote && Display.isActive(), "use focused disposable singleplayer");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute disposable client");
    check(Display.getWidth() >= 1708 && Display.getHeight() >= 960, "resize disposable display");
    Files.createDirectories(REPORT.resolve("probes")); captures.clear(); failures = 0;
    try (var state = new Release462Checks.State(mc)) {
      for (int[] size : new int[][]{{854,480},{320,240}})
        for (String view : List.of("library", "playlist", "favorites", "queue")) one(mc, state, size, view, false);
      Files.writeString(REPORT.resolve("captures.json"), local.luke.power.config.Catalog.JSON.toJson(captures));
      if (action.equals("check"))
        for (int[] size : new int[][]{{854,480},{320,240}})
          for (String view : List.of("library", "playlist", "favorites", "queue"))
            test("RELEASE467 context ownership " + view + " " + size[0], () -> one(mc,state,size,view,true));
    }
    Files.writeString(REPORT.resolve(action+".json"), "{\"failures\":"+failures+",\"captures\":"+captures.size()+"}");
    log("RELEASE467 UI "+action+" failures="+failures+" actualCaptures="+captures.size());
    check(failures==0,"release-467-ui failures="+failures);
  }
  private static void one(Minecraft mc, Release462Checks.State state, int[] size, String view, boolean verify) throws Exception {
    var options=state.open(size,MusicPlaylist.BETA,false); call(options,"openMusicEditor");
    var library=(MusicLibraryScreen)get(options,"library");
    switch(view) {
      case "library" -> library.showTrack("music:calm1.ogg");
      case "playlist" -> library.showPlaylist("ui462-long");
      case "favorites" -> call(library,"selectFilter",new Class<?>[]{String.class},"favorites");
      case "queue" -> { MusicRequests.edit(q->{q.tracks.add("music:calm1.ogg");q.tracks.add("music:calm2.ogg");q.tracks.add("music:calm1.ogg");});library.showQueue(); }
    }
    int top=n(library,"listTop"),bottom=n(library,"bottom"),left=n(library,"left");
    List<?> rows=(List<?>)get(library,"rows"); Object owner=null;int ownerY=0;
    for(Object row:rows) {int y=top+n(row,"y")-n(library,"scroll");if(call(row,"id")!=null&&y>=top&&y+n(row,"height")<=bottom){owner=row;ownerY=y;break;}}
    check(owner!=null,"view has no complete song row");
    BufferedImage clean=shot(mc,options,size,view+"-clean",-1,-1,false);
    BufferedImage hovered=shot(mc,options,size,view+"-hover",left+3,ownerY+7,false);
    ((ScreenInput)(Object)options).power$click(left+8,ownerY+7,1);
    Object popup=get(options,"choices");check((boolean)call(popup,"active"),"track context did not open");
    int px=((Number)get(popup,"x")).intValue()+((Number)get(popup,"width")).intValue()-12;
    int py=((Number)get(popup,"y")).intValue()+25;
    BufferedImage context=shot(mc,options,size,view+"-context",px,py,!verify);
    if(!verify)return;
    int sampleX=left-1,sampleY=ownerY+2;
    check(pixel(context,sampleX,sampleY)==pixel(hovered,sampleX,sampleY)
        &&pixel(context,sampleX,sampleY)!=pixel(clean,sampleX,sampleY),"context owner lost hover background");
    for(Object row:rows){int y=top+n(row,"y")-n(library,"scroll");if(row!=owner&&call(row,"id")!=null&&y>=top&&y+n(row,"height")<=bottom)
      check(pixel(context,sampleX,y+2)==pixel(clean,sampleX,y+2),"context hover reached another song row");}
    ((ScreenInput)(Object)options).power$key('\0',Keyboard.KEY_ESCAPE);
    BufferedImage closed=shot(mc,options,size,view+"-closed",-1,-1,false);
    check(!(boolean)call(popup,"active")&&pixel(closed,sampleX,sampleY)==pixel(clean,sampleX,sampleY),"Escape retained owner highlight");
    ((ScreenInput)(Object)options).power$click(left+8,ownerY+7,1);
    ((ScreenInput)(Object)options).power$click(0,0,0);
    closed=shot(mc,options,size,view+"-outside",-1,-1,false);
    check(!(boolean)call(popup,"active")&&pixel(closed,sampleX,sampleY)==pixel(clean,sampleX,sampleY),"outside click retained owner highlight");
  }
  private static BufferedImage shot(Minecraft mc,PowerOptionsScreen screen,int[] size,String view,int x,int y,boolean actual)throws Exception{
    String name=view+"-"+size[0]+".png";Path path=(actual?REPORT:REPORT.resolve("probes")).resolve(name);
    SettingsSnapshot.render(mc,screen,size[0],size[1],path,x,y);
    if(actual)captures.add(Map.of("file",name,"view",view,"width",size[0],"height",size[1]));return ImageIO.read(path.toFile());
  }
  private static int pixel(BufferedImage image,int x,int y){return image.getRGB(x*2,y*2)&0xffffff;}
  private static int n(Object o,String s)throws Exception{return ((Number)call(o,s)).intValue();}
  private static Object get(Object o,String name)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
  private static Object call(Object o,String name)throws Exception{return call(o,name,new Class<?>[0]);}
  private static Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Method m=c.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
}
