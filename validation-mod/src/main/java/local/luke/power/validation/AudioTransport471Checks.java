package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.Release240Checks.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import local.luke.power.audio.*;
import local.luke.power.config.Setting;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import paulscode.sound.*;

/** Real streams on a verified silent sink, with native UI request and pixel checks. */
final class AudioTransport471Checks {
  private static final String FIRST="music:calm1.ogg", SECOND="music:calm2.ogg", PREVIEW="PowerBetaMusicPreview";
  private static final Path REPORT=Path.of("power-beta-data/reports/release-471/audio-transport");
  static void run(Minecraft mc)throws Exception { run(mc,"run"); }
  static void run(Minecraft mc,String phase)throws Exception {
    check(Set.of("capture","run").contains(phase),"use audio-transport-471 capture|run");
    check(mc.world!=null&&Display.isActive()&&mc.options.musicVolume==0&&mc.options.soundVolume==0,"use focused muted disposable world");
    check(Display.getWidth()>=1708&&Display.getHeight()>=960,"resize disposable display to at least1708x960");
    SilentAudioChecks.ready(mc);Files.createDirectories(REPORT);failures=0;float music=mc.options.musicVolume;
    try(var state=new Release462Checks.State(mc)){
      state.playback=true;
      for(int[] size:new int[][]{{854,480},{320,240}}){
        var options=state.open(size,MusicPlaylist.BETA,false);
        var audio=AudioConfig.copy();audio.musicFadeMillis=0;AudioConfig.preview(audio);
        setting(options.session(),"audio.showMusicPreviewButton").parse("true");options.session().preview();mc.options.musicVolume=1;
        if(phase.equals("capture")){
          captureViews(mc,options,size);
          continue;
        }
        test("AUDIO TRANSPORT471 native Audio/title/pause actions "+size[0],()->transports(mc,options,size));
        mc.setScreen(options);options.init(mc,size[0],size[1]);call(options,"openMusicEditor");
        var library=(MusicLibraryScreen)get(options,"library");library.showTracks();((Set<?>)get(library,"collapsed")).clear();call(library,"rebuildRows");
        test("AUDIO TRANSPORT471 native previews/restoration/colors "+size[0],()->library(mc,options,library,size));
        test("AUDIO TRANSPORT471 Add songs red remove/context/draft ownership "+size[0],()->additions(mc,options,library,size));
        test("AUDIO TRANSPORT471 native sound sample "+size[0],()->effect(options));
      }
    }finally{AudioController.quiet();mc.options.musicVolume=music;}
    Files.writeString(REPORT.resolve(phase+".json"),"{\"failures\":"+failures+"}");
    log("AUDIO TRANSPORT471 "+phase+" complete failures="+failures);check(failures==0,"audio-transport-471 failures="+failures);
  }
  private static void captureViews(Minecraft mc,PowerOptionsScreen options,int[] size)throws Exception{
    SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("audio-settings-"+size[0]+".png"));
    AudioController.playNow(FIRST);await(()->system().playing("BgMusic"),"capture background preparation");
    call(options,"openMusicEditor");var library=(MusicLibraryScreen)get(options,"library");library.showTracks();
    AudioController.previewSound(SECOND);await(()->system().playing(PREVIEW),"capture audition preparation");
    colors(mc,options,library,size,FIRST,SECOND,false);
    AudioController.stopPreview(SECOND);
    library.showPlaylist("ui462-long");Object panel=get(library,"presets");call(panel,"adding",true);call(library,"syncPanel");
    Object row=reveal(library,FIRST);var layout=(MusicRowLayout)call(library,"rowLayout");int y=rowY(library,row);
    for(boolean hover:new boolean[]{false,true})SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("included-remove-"+(hover?"hover-":"plain-")+size[0]+".png"),hover?layout.nameX()+5:-1,hover?y+layout.controlsY()+5:-1);
    click(options,layout.nameX()+26,y+8,1);SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("included-remove-context-"+size[0]+".png"));
    key(options,Keyboard.KEY_ESCAPE);library.cancelPlaylist();AudioController.quiet();
  }
  private static void transports(Minecraft mc,PowerOptionsScreen options,int[] size)throws Exception{
    AudioController.quiet();toolbar(options,AudioToolbar.Action.PLAY,0);await(()->system().playing("BgMusic"),"Audio Play");
    toolbar(options,AudioToolbar.Action.PLAY,1);await(()->!system().playing("BgMusic"),"Audio Pause");
    toolbar(options,AudioToolbar.Action.PLAY,0);await(()->system().playing("BgMusic"),"Audio Resume");
    toolbar(options,AudioToolbar.Action.PREVIOUS,0);toolbar(options,AudioToolbar.Action.NEXT,0);toolbar(options,AudioToolbar.Action.QUIET,1);
    for(Screen menu:List.of(new net.minecraft.class_525(),new TitleScreen())){
      mc.setScreen(menu);menu.init(mc,size[0],size[1]);call(menu,"power$layout");var widget=(PauseMenuMusic)get(menu,"power$music");
      var bounds=widget.bounds();check(bounds!=null,"menu transport unavailable");int x=n(widget,"controlsX"),y=bounds.y()+2;
      AudioController.playNow(FIRST);await(()->system().playing("BgMusic"),"menu start");
      counted(menu,x+2,y,0,1,"menu Pause");await(()->!system().playing("BgMusic"),"menu paused source");
      counted(menu,x+2,y,0,0,"menu Resume");await(()->system().playing("BgMusic"),"menu resumed source");
      counted(menu,x+26,y,0,0,"menu Previous");counted(menu,x+50,y,0,0,"menu Next");counted(menu,x+74,y,0,1,"menu Quiet");
    }
  }
  private static void library(Minecraft mc,PowerOptionsScreen options,MusicLibraryScreen library,int[] size)throws Exception{
    AudioController.quiet();action(options,library,FIRST,"play",0,"row Play");await(()->system().playing("BgMusic"),"row Play stream");
    action(options,library,FIRST,"play",1,"row Pause");await(()->!system().playing("BgMusic"),"row paused stream");
    action(options,library,FIRST,"play",0,"row Resume");await(()->system().playing("BgMusic"),"row resumed stream");
    Object row=reveal(library,SECOND);var layout=(MusicRowLayout)call(library,"rowLayout");int y=rowY(library,row);
    setting(options.session(),"audio.musicDoubleClickPreview").parse("true");options.session().preview();
    counted(options,layout.nameX()+4,y+8,0,0,"first name click");counted(options,layout.nameX()+4,y+8,0,0,"preview double-click");
    await(()->system().playing(PREVIEW),"double-click audition");check(AudioController.previewing(SECOND),"double-click selected wrong audition");
    AudioController.stopPreview(SECOND);await(()->source(PREVIEW)==null&&system().playing("BgMusic"),"double-click cleanup");
    Object background=source("BgMusic"),queue=MusicRequests.tracks();String id=AudioController.backgroundTrackId();int history=(int)field(AudioController.class,"historyIndex");
    action(options,library,SECOND,"preview",0,"preview button start");await(()->system().playing(PREVIEW),"button audition");
    check(!system().playing("BgMusic")&&source("BgMusic")==background&&AudioController.backgroundTrackId().equals(id),"preview replaced background source");
    colors(mc,options,library,size,FIRST,SECOND,true);
    action(options,library,SECOND,"preview",0,"active preview button stop");stopped(background,id,history,queue);
    action(options,library,SECOND,"preview",0,"preview before pause");await(()->system().playing(PREVIEW),"preview before pause");
    action(options,library,SECOND,"play",1,"preview row Pause");await(()->!system().playing(PREVIEW),"paused audition");
    check(AudioController.previewPaused(),"preview transport did not pause");context(options,library,SECOND,true);stopped(background,id,history,queue);
    context(options,library,SECOND,false);await(()->system().playing(PREVIEW),"context audition");context(options,library,SECOND,true);stopped(background,id,history,queue);
    AudioController.pause();await(()->!system().playing("BgMusic"),"paused background");
    action(options,library,SECOND,"preview",0,"preview over paused background");await(()->system().playing(PREVIEW),"audition over paused background");
    action(options,library,SECOND,"preview",0,"stop over paused background");await(()->source(PREVIEW)==null,"paused-background cleanup");
    check(!system().playing("BgMusic")&&source("BgMusic")==background,"preview revived paused background");
    setting(options.session(),"audio.musicDoubleClickPreview").parse("false");options.session().preview();
    row=reveal(library,SECOND);y=rowY(library,row);call(get(library,"doubleClick"),"cancel");
    counted(options,layout.nameX()+4,y+8,0,0,"first playback-name click");counted(options,layout.nameX()+4,y+8,0,0,"playback double-click");
    await(()->system().playing("BgMusic"),"double-click playback");check(AudioController.backgroundTrackId().equals(SECOND)&&!AudioController.previewing(SECOND),"playback double-click did not replace background");
  }
  private static void stopped(Object background,String id,int history,Object queue)throws Exception{
    await(()->source(PREVIEW)==null&&system().playing("BgMusic"),"preview removal/background restoration");
    check(!AudioController.previewing(SECOND)&&!AudioController.previewPaused()&&AudioController.currentTrackId().equals(id)&&source("BgMusic")==background
        &&(int)field(AudioController.class,"historyIndex")==history&&MusicRequests.tracks()==queue,"preview stop changed background/history/queue ownership");
    Class<?> preview=Class.forName("local.luke.power.audio.MusicPreview");
    check(field(preview,"entry")==null&&field(preview,"world")==null&&((List<?>)field(preview,"resume")).isEmpty(),"preview retained restoration data");
  }
  private static void context(PowerOptionsScreen options,MusicLibraryScreen library,String id,boolean stopping)throws Exception{
    Object row=reveal(library,id);var layout=(MusicRowLayout)call(library,"rowLayout");counted(options,layout.nameX()+4,rowY(library,row)+8,1,0,"preview context open");
    Object choices=get(options,"choices");check(((List<?>)get(choices,"labels")).get(2).equals(stopping?"Stop preview":"Preview"),"context label disagreed with source state");
    counted(options,(int)get(choices,"x")+8,(int)get(choices,"y")+2*(int)get(choices,"rowHeight")+8,0,0,"preview context selection");
  }
  private static void colors(Minecraft mc,PowerOptionsScreen options,MusicLibraryScreen library,int[] size,String background,String preview,boolean validate)throws Exception{
    for(String id:List.of(background,preview)){
      Object row=reveal(library,id);var layout=(MusicRowLayout)call(library,"rowLayout");int y=rowY(library,row);
      Path file=REPORT.resolve((id.equals(preview)?"blue-preview-":"gold-background-")+size[0]+".png");SettingsSnapshot.render(mc,options,size[0],size[1],file);BufferedImage image=ImageIO.read(file.toFile());
      if(validate)check(pixels(image,layout.nameX(),y,layout.volumeX()-layout.nameX()-4,18,id.equals(preview)?0x77baff:0xffdd66)>0,"title lost preview/background color");
      if(validate&&id.equals(preview))check(pixels(image,layout.previewX(),y+layout.controlsY(),layout.buttonWidth(),18,0x77baff)>4,"active preview icon is not blue");
    }
  }
  private static void effect(PowerOptionsScreen options)throws Exception{
    call(options,"closeLibrary");Setting sample=setting(options.session(),"audio.bucketSounds");((TextInput)get(options,"search")).setText(sample.label);((Set<?>)get(options,"collapsed")).clear();call(options,"layout");
    Object row=null;for(Object candidate:(List<?>)get(options,"rows"))if(call(candidate,"setting")==sample){row=candidate;break;}check(row!=null,"missing sample row");
    int x=((Number)call(options,"controlLeft",row)).intValue()+5,y=n(options,"top")+(int)call(row,"y")-((Double)get(options,"scroll")).intValue()+((int)call(row,"height")==40?16:3)+5;
    String id=(String)call(options,"previewId",sample);counted(options,x,y,0,0,"sound sample start");check(AudioController.previewing(id),"sample preview did not start");
    counted(options,x,y,0,0,"sound sample stop");await(()->source("PowerBetaPreview")==null,"sample cleanup");check(!AudioController.previewing(id),"sample retained preview state");
  }
  private static void additions(Minecraft mc,PowerOptionsScreen options,MusicLibraryScreen library,int[] size)throws Exception{
    String saved=local.luke.power.config.Catalog.JSON.toJson(AudioConfig.current());
    Object sources=AudioController.sourceLibrary();var beta=MusicPlaylist.beta().tracks();
    library.showPlaylist("ui462-long");Object panel=get(library,"presets");
    click(options,n(panel,"addX")+6,n(panel,"actionY")+7,0);key(options,org.lwjgl.input.Keyboard.KEY_HOME);key(options,org.lwjgl.input.Keyboard.KEY_RETURN);call(library,"syncPanel");
    check((boolean)call(panel,"adding"),"Add songs > From Library did not open");
    var draft=(MusicPresetDraft)call(panel,"draft");check(draft.included(FIRST),"fixture song was not included");
    Object row=reveal(library,FIRST);var layout=(MusicRowLayout)call(library,"rowLayout");int y=rowY(library,row);
    for(boolean hover:new boolean[]{false,true}){
      Path file=REPORT.resolve("included-remove-"+(hover?"hover-":"plain-")+size[0]+".png");
      SettingsSnapshot.render(mc,options,size[0],size[1],file,hover?layout.nameX()+5:-1,hover?y+layout.controlsY()+5:-1);
      check(pixels(ImageIO.read(file.toFile()),layout.nameX(),y+layout.controlsY(),18,18,0xff8888)>0,"included song remove icon lost red glyph");
    }
    membershipContext(options,library,FIRST,"Remove from playlist");check(!draft.included(FIRST),"context Remove retained song in draft");
    membershipContext(options,library,FIRST,"Add to playlist");check(draft.included(FIRST),"context Add did not restore song in draft");
    row=reveal(library,FIRST);click(options,layout.nameX()+5,rowY(library,row)+layout.controlsY()+5,0);
    check(!draft.included(FIRST),"row remove icon did not edit the same playlist draft");
    check(saved.equals(local.luke.power.config.Catalog.JSON.toJson(AudioConfig.current()))&&AudioController.sourceLibrary()==sources,"draft membership changed saved music/library/source data");
    library.cancelPlaylist();check(MusicPlaylist.find(AudioConfig.current(),"ui462-long").tracks().contains(FIRST),"Cancel published draft removal");
    library.showPlaylist(MusicPlaylist.BETA);panel=get(library,"presets");call(panel,"adding",true);
    check(!(boolean)call(panel,"editable")&&!(boolean)call(panel,"adding"),"read-only Beta admitted Add songs editing");
    row=reveal(library,FIRST);click(options,layout.nameX()+5,rowY(library,row)+8,1);
    Object choices=get(options,"choices");check(((List<?>)get(choices,"labels")).stream().noneMatch(label->label.equals("Remove from playlist")||label.equals("Add to playlist")),"read-only Beta exposed membership mutation");
    key(options,org.lwjgl.input.Keyboard.KEY_ESCAPE);library.cancelPlaylist();
    check(MusicPlaylist.beta().tracks().equals(beta)&&saved.equals(local.luke.power.config.Catalog.JSON.toJson(AudioConfig.current()))&&AudioController.sourceLibrary()==sources,"read-only or Cancel altered published music/source data");
  }
  private static void membershipContext(PowerOptionsScreen options,MusicLibraryScreen library,String id,String label)throws Exception{
    Object row=reveal(library,id);var layout=(MusicRowLayout)call(library,"rowLayout");click(options,layout.nameX()+26,rowY(library,row)+8,1);
    Object choices=get(options,"choices");var labels=(List<?>)get(choices,"labels");int index=labels.indexOf(label);check(index>=0&&((List<?>)get(choices,"enabled")).get(index).equals(true),"missing active membership action "+label);
    click(options,(int)get(choices,"x")+8,(int)get(choices,"y")+index*(int)get(choices,"rowHeight")+8,0);
  }
  private static void action(PowerOptionsScreen options,MusicLibraryScreen library,String id,String action,int expected,String label)throws Exception{
    Object row=reveal(library,id);var layout=(MusicRowLayout)call(library,"rowLayout");counted(options,(action.equals("preview")?layout.previewX():layout.playX())+3,rowY(library,row)+layout.controlsY()+5,0,expected,label);
  }
  private static Object reveal(MusicLibraryScreen library,String id)throws Exception{
    ((TextInput)get(library,"search")).setText("");((Set<?>)get(library,"collapsed")).clear();call(library,"rebuild");
    for(Object row:(List<?>)get(library,"rows"))if(id.equals(call(row,"id"))){call(library,"scroll",Math.max(0,(int)call(row,"y")-24));return row;}throw new AssertionError("Missing song row "+id);
  }
  private static int rowY(MusicLibraryScreen library,Object row)throws Exception{return n(library,"listTop")+(int)call(row,"y")-n(library,"scroll");}
  private static void toolbar(PowerOptionsScreen options,AudioToolbar.Action action,int expected)throws Exception{
    @SuppressWarnings("unchecked")var buttons=(List<AudioToolbar.Button>)call(options,"musicButtons");var button=buttons.stream().filter(b->b.action()==action).findFirst().orElseThrow();counted(options,button.x()+2,button.y()+2,0,expected,"Audio "+action);
  }
  private static void counted(Screen screen,int x,int y,int button,int expected,String label){int before=clicks;click(screen,x,y,button);check(clicks-before==expected,label+" UI click requests="+(clicks-before));}
  private static int pixels(BufferedImage image,int x,int y,int width,int height,int color){int count=0;for(int dy=Math.max(0,y*2);dy<Math.min(image.getHeight(),(y+height)*2);dy++)for(int dx=Math.max(0,x*2);dx<Math.min(image.getWidth(),(x+width)*2);dx++)if((image.getRGB(dx,dy)&0xffffff)==color)count++;return count;}
  private static SoundSystem system(){return SoundManagerAccessor.power$system();}
  private static Object source(String name){try{synchronized(SoundSystemConfig.THREAD_SYNC){Object library=field(system(),"soundLibrary");return((Map<?,?>)library.getClass().getMethod("getSources").invoke(library)).get(name);}}catch(Exception failure){throw new IllegalStateException(failure);}}
  private static Object field(Object owner,String name)throws Exception{Class<?> type=owner instanceof Class<?> c?c:owner.getClass();for(;type!=null;type=type.getSuperclass())try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner instanceof Class<?>?null:owner);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
  private static void await(BooleanSupplier ready,String label)throws Exception{long deadline=System.nanoTime()+5_000_000_000L;while(!ready.getAsBoolean()&&System.nanoTime()<deadline)Thread.sleep(10);check(ready.getAsBoolean(),"native audio timeout: "+label);}
}
