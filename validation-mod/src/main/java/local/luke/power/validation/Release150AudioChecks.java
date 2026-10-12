package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;

final class Release150AudioChecks {
  private static AudioSettings settings;
  private static List<String> queue;
  private static String background, preview;
  private static double position;
  static void run(Minecraft mc,String action) throws Exception {
    switch(action) {
      case "setup" -> {
        settings=AudioConfig.copy();queue=MusicRequests.tracks();
        var s=AudioConfig.copy();s.master=100;s.waitBetweenTracks=false;AudioConfig.preview(s);
        mc.options.musicVolume=.2f;mc.soundManager.method_2008();
        MusicRequests.edit(q->q.tracks.clear());AudioController.next();background=AudioController.currentTrackId();
        preview=AudioController.music(mc).stream().filter(id->!id.equals(background)).findFirst().orElseThrow();
        MusicRequests.edit(q->q.tracks.add(preview));
      }
      case "preview" -> {
        check(AudioController.musicPlaying(),"background did not start");
        position=AudioController.position();AudioController.previewSound(preview);
      }
      case "next" -> {
        check(AudioController.previewing(preview),"preview not active");
        PowerOptionsScreen.openMusic(mc,null,false,null);
        var screen=(PowerOptionsScreen)mc.currentScreen;
        musicClick(screen,AudioToolbar.Action.NEXT);
        check(!AudioController.previewing(preview),"Next left preview playing");
        check(AudioController.currentTrackId().equals(background),"Next skipped background");
        check(MusicRequests.tracks().equals(List.of(preview)),"Next consumed queue");
      }
      case "check" -> {
        check(AudioController.musicPlaying() && AudioController.currentTrackId().equals(background),"background did not resume");
        check(AudioController.position()>=position,"background playback position reset");
        for(int width:new int[]{320,640}) {
          PowerOptionsScreen.openMusicDestination(mc,mc.currentScreen);
          var screen=(PowerOptionsScreen)mc.currentScreen;screen.init(mc,width,420);
          check((boolean)call(field(screen,"library"),"queueVisible"),"queue destination missing");
          screen.render(-1,-1,0);
          var status=field(screen,"musicStatus");
          int x=(int)field(status,"labelX")+2,y=(int)field(status,"top")+5;
          MusicRequests.edit(q->q.tracks.clear());
          ((ScreenInput)(Object)screen).power$click(x,y,1);
          check(!(boolean)field(screen,"libraryOpen") && (double)field(screen,"scroll")==0,"empty queue did not go to Audio top");
          MusicRequests.edit(q->q.tracks.add(preview));
        }
        MusicRequests.edit(q->q.tracks.clear());PowerOptionsScreen.openMusicDestination(mc,mc.currentScreen);
        var screen=(PowerOptionsScreen)mc.currentScreen;
        check(field(screen,"page").equals("Audio") && !(boolean)field(screen,"libraryOpen") && (double)field(screen,"scroll")==0,"menu empty queue destination");
        log("PASS preview Next restores exact track, position and queue; right-click destinations at narrow and wide widths");
      }
      case "menus" -> {
        var s=AudioConfig.copy();s.menuControls=true;s.menuControlsMainMenu=true;AudioConfig.preview(s);
        for(boolean title:new boolean[]{false,true})for(boolean queued:new boolean[]{false,true})for(int width:new int[]{320,640}) {
          MusicRequests.edit(q->{q.tracks.clear();if(queued)q.tracks.add(preview);});
          net.minecraft.client.gui.screen.Screen menu=title?new net.minecraft.client.gui.screen.TitleScreen():new net.minecraft.class_525();
          mc.setScreen(menu);menu.init(mc,width,420);menu.render(-1,-1,0);
          var widget=field(menu,"power$music");var bounds=(MenuMusicLayout)field(widget,"bounds");
          ((ScreenInput)(Object)menu).power$click(bounds.x()+10,bounds.y()+25,1);
          var dest=(PowerOptionsScreen)mc.currentScreen;
          check(queued?(boolean)call(field(dest,"library"),"queueVisible"):
              field(dest,"page").equals("Audio") && !(boolean)field(dest,"libraryOpen") && (double)field(dest,"scroll")==0,"menu song right click destination");
        }
        MusicRequests.edit(q->{q.tracks.clear();q.tracks.add(preview);});
        AudioController.previewSound(preview);check(AudioController.previewing(preview),"menu preview missing");
        var menu=new net.minecraft.class_525();mc.setScreen(menu);menu.render(-1,-1,0);
        var bounds=(MenuMusicLayout)field(field(menu,"power$music"),"bounds");
        ((ScreenInput)(Object)menu).power$click(bounds.x()+(bounds.width()-92)/2+49,bounds.y()+5,0);
        check(!AudioController.previewing(preview) && MusicRequests.tracks().equals(List.of(preview)),"menu Next skipped queue");
        log("PASS title/pause right-click destinations across widths and pause Next preview restoration");
      }
      case "stress" -> {
        for(int i=0;i<100;i++) {AudioController.previewSound(preview);AudioController.next();}
      }
      case "alive" -> {
        var threads=Thread.getAllStackTraces().keySet().stream().filter(t->t instanceof paulscode.sound.StreamThread).toList();
        check(!threads.isEmpty() && threads.stream().allMatch(Thread::isAlive),"sound stream thread died");
        log("PASS rapid preview/Next cancellation keeps the sound stream thread alive");
      }
      case "finish" -> {
        AudioController.quiet();AudioConfig.preview(settings);mc.options.musicVolume=0;
        MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(queue);});mc.setScreen(null);
      }
      default -> throw new IllegalArgumentException(action);
    }
  }
}
