package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.ui.*;
import local.luke.power.permissions.CheatWorld;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class MusicErasChecks {
  private static AudioSettings before;
  private static List<String> queueBefore;
  private static float volumeBefore;
  private static PowerOptionsScreen options;

  public static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "setup" -> {
        failures = 0; before = AudioConfig.copy(); queueBefore = MusicRequests.tracks(); volumeBefore = mc.options.musicVolume;
        var config = AudioConfig.copy(); config.master = 100; config.musicMode = AudioSettings.MusicMode.ALL_MINECRAFT;
        config.customMusic = AudioSettings.CustomMusic.OFF; config.menuMusic = AudioSettings.MenuMusic.WORLD;
        config.disabledTracks.clear(); config.waitBetweenTracks = true; config.delayQueuedTracks = true;
        MusicRequests.edit(q -> q.tracks.clear()); AudioConfig.preview(config); AudioController.pause(); mc.options.musicVolume = .1f;
      }
      case "ui" -> {
        test("soundtrack pools are exactly 12, 35 and 83 tracks", () -> {
          int[] counts={12,35,83};
          for(var mode : AudioSettings.MusicMode.values()) {
            var config=AudioConfig.copy();config.musicMode=mode;AudioConfig.preview(config);
            check(AudioController.activeMusic(mc).size()==counts[mode.ordinal()], mode.toString());
          }
        });
        options=MenuUpdateChecks.open(mc,"Audio");
        test("music settings have seconds and one gap section without duplicate playback switches", () -> {
          field(options,"collapsed",new HashSet<>()); call(options,"layout");
          var gaps=options.session().settings().stream().filter(s->s.group.equals("Music gaps")).toList();
          check(gaps.size()==4,"unexpected gaps count");
          check(find(options.session(),"audio.gapMinSeconds").label.contains("seconds"),"ticks still displayed");
          for(Setting s:options.session().settings()) check(!s.id.contains("disableBackgroundMusic") && !s.id.contains("musicCoundown"),s.id);
          var groups=new ArrayList<String>();
          for(Object row:(List<?>)field(options,"rows")) if(call(row,"setting")==null) groups.add((String)call(row,"group"));
          check(groups.indexOf("Music gaps")==groups.indexOf("Music library")+1,groups.toString());
          check(!groups.contains("Individual music tracks"),"duplicate track controls");
        });
        musicClick(options,AudioToolbar.Action.LIBRARY);
        var library=(MusicLibraryScreen)field(options,"library");
        library.restore(new MusicLibraryScreen.State(false,"","",0,0));call(library,"rebuild");options.render(-1,-1,0);
        test("era accordions collapse, locate playing tracks and remember collapse state", () -> {
          var headings=new ArrayList<String>();
          for(Object row:(List<?>)field(library,"rows")) if(call(row,"id")==null) headings.add((String)call(row,"group"));
          check(headings.subList(0,BuiltinMusic.GROUPS.size()).equals(BuiltinMusic.GROUPS),headings.toString());
          int count=((List<?>)field(library,"rows")).size();
          click(options,(int)call(library,"left")+3,(int)call(library,"listTop")+9);
          check(((List<?>)field(library,"rows")).size()==count-12,"Alpha did not collapse");
          check(library.state().collapsed().contains("Alpha"),"collapse not remembered");
          library.showTrack("music:calm1.ogg");
          check(!library.state().collapsed().contains("Alpha"),"located track still collapsed");
        });
        test("inclusion icon changes rotation without removing the track", () -> {
          options.render(-1,-1,0);
          Object first=((List<?>)field(library,"rows")).stream().filter(r->{try{return call(r,"id")!=null;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
          String id=(String)call(first,"id");
          int y=(int)call(library,"listTop")+(int)call(first,"y")-library.state().trackScroll();
          click(options,(int)call(library,"left")+8,y+24);
          check(AudioConfig.current().disabledTracks.contains(id),"note icon did not exclude");
          check(AudioController.activeMusic(mc).contains(id),"excluded track disappeared from Active pool");
          click(options,(int)call(library,"left")+8,y+24);
          check(!AudioConfig.current().disabledTracks.contains(id),"note icon did not reinclude");
        });
        MusicGroupChecks.run(mc, options, library);
        test("queue controls and volume stay inline at compact and wide sizes", () -> {
          MusicRequests.edit(q->{q.tracks.clear();q.add("music:calm1.ogg");q.add("music:creative4.ogg");});
          library.showQueue();
          for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
            options.init(mc,size[0],size[1]);options.render(-1,-1,0);
            check((int)call(library,"rowHeight")==24,"multiline queue");
            check((int)call(library,"volumeEnd")-(int)call(library,"volumeX")>=44,"volume too narrow");
            check((int)call(library,"volumeX")-(int)call(library,"left")>=40,"name has no space");
            check(GL11.glGetError()==0,"GL error");
          }
          mc.setScreen(options);options.render(-1,-1,0);
          click(options,(int)call(library,"volumeX")+4,(int)call(library,"listTop")+8);
          library.removed();
          check(AudioController.trackVolume("music:calm1.ogg")==0,"inline volume missed");
          check(MusicRequests.tracks().equals(List.of("music:calm1.ogg","music:creative4.ogg")),"volume changed queue");
          options.session().discard();MusicRequests.edit(q->q.tracks.clear());
        });
        log("MUSIC ERAS FAILURES "+failures);
      }
      case "view-settings", "view-library", "view-queue", "view-controls" -> {
        options=MenuUpdateChecks.open(mc,action.equals("view-controls")?"Controls":"Audio");
        AudioController.pause();
        if(action.equals("view-library")) {
          musicClick(options,AudioToolbar.Action.LIBRARY);
          var library=(MusicLibraryScreen)field(options,"library");
          library.restore(new MusicLibraryScreen.State(false,"","",0,0,"",Set.of("Alpha")));
          call(library,"rebuild");
        } else if(action.equals("view-queue")) {
          MusicRequests.edit(q->{q.tracks.clear();q.add("music:calm1.ogg");q.add("music:creative4.ogg");q.add("music:axolotl.ogg");});
          musicClick(options,AudioToolbar.Action.QUEUE);
        }
      }
      case "natural" -> {
        var config=AudioConfig.copy();config.waitBetweenTracks=false;AudioConfig.preview(config);
        AudioController.quiet();
      }
      case "natural-check" -> {
        test("natural playback publishes a nonblank filename and title while Options stays open",()->{
          check(SoundManagerAccessor.power$system().playing("BgMusic"),"natural source did not start");
          check(!AudioController.currentTrackId().isBlank(),"natural identity missing");
          check(AudioController.status().startsWith("Playing: ") && AudioController.status().length()>9,AudioController.status());
          check(mc.currentScreen==options,"playback required leaving Options");
        });
        AudioController.quiet();
        test("async Quiet never exposes an empty Playing label",()->check(!AudioController.status().equals("Playing: "),"empty status"));
        AudioController.pause();
      }
      case "portal-survival", "portal-creative", "portal-spectator" -> {
        String mode=action.substring(7);
        ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
        ChatChecks.submit(mc,"/gamemode "+mode);
        Object actual=Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player);
        check(actual.toString().equalsIgnoreCase(mode),"mode not changed");
        mc.setScreen(null);
        int dimension=mc.player.dimensionId;
        mc.player.field_511=0;
        if(mode.equals("spectator")) {
          int x=(int)Math.floor(mc.player.x),y=(int)Math.floor(mc.player.boundingBox.minY),z=(int)Math.floor(mc.player.z);
          // A valid frame in a disposable world keeps portal callbacks alive while no-clipping.
          for(int dx=-1;dx<=2;dx++)for(int dy=-1;dy<=3;dy++)
            mc.world.method_200(x+dx,y+dy,z,dx==-1||dx==2||dy==-1||dy==3?49:90);
        } else net.minecraft.block.Block.BLOCKS[90].method_1615(mc.world, (int)mc.player.x, (int)mc.player.y, (int)mc.player.z, mc.player);
        mc.player.method_937();
        test(mode+" portal uses the proper delay",()->{
          check((mc.player.dimensionId!=dimension)==!mode.equals("survival"),"wrong portal timing");
          if(mode.equals("survival")) check(mc.player.field_504<.1F,"survival warm-up skipped");
          else check(mc.player.field_511>0,"portal cooldown removed");
        });
        mc.setScreen(new PowerOptionsScreen(null));
        log("MUSIC ERAS FAILURES "+failures);
      }
      case "finish" -> {
        AudioController.pause();AudioConfig.preview(before);mc.options.musicVolume=volumeBefore;
        MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(queueBefore);});mc.setScreen(null);
        log("MUSIC ERAS FAILURES "+failures);
      }
      default -> {
        if(action.startsWith("play ")) AudioController.playNow("music:"+action.substring(5));
        else if(action.startsWith("check ")) test("bundled stream decodes: "+action.substring(6),()->{
          String id="music:"+action.substring(6);
          check(AudioController.playingTrack(id),"wrong track identity");
          check(SoundManagerAccessor.power$system().playing("BgMusic"),"stream failed to decode");
          check(!AudioController.nowPlaying().isBlank(),"blank label");
        });
      }
    }
  }
}
