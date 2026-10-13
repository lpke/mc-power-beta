package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;

final class MenuScrubChecks {
  static void run(Minecraft mc) throws Exception {
    failures=0;var before=AudioConfig.copy();var parent=mc.currentScreen;
    try {
      var settings=AudioConfig.copy();settings.menuControls=settings.menuControlsMainMenu=settings.menuControlsScrub=true;
      settings.menuControlsOffsetX=settings.menuControlsOffsetY=0;AudioConfig.preview(settings);
      AudioController.playNow("music:calm1.ogg");AudioController.pause();
      long deadline=System.nanoTime()+3_000_000_000L;
      while(AudioController.duration()<=0&&System.nanoTime()<deadline)Thread.sleep(10);
      check(AudioController.duration()>0,"fixture duration not ready");
      for (Screen screen:new Screen[]{new TitleScreen(),new net.minecraft.class_525()})
        for (var anchor:new AudioSettings.MenuControlsPosition[]{AudioSettings.MenuControlsPosition.MENU_TOP,AudioSettings.MenuControlsPosition.MENU_BOTTOM})
          for (int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}})
            test("menu scrub alignment and hit bounds "+screen.getClass().getSimpleName()+" "+anchor+" "+size[0],()->{
              settings.menuControlsPosition=anchor;AudioConfig.preview(settings);
              mc.setScreen(screen);screen.init(mc,size[0],size[1]);screen.render(-1,-1,0);
              var panel=(PauseMenuMusic)field(screen,"power$music");
              var bounds=(MenuMusicLayout)field(panel,"bounds");var scrub=field(panel,"scrub");
              check((boolean)field(scrub,"available"),"scrubber absent");
              int x=(int)field(scrub,"barX"),w=(int)field(scrub,"barWidth"),y=(int)field(scrub,"top");
              check(Math.abs(2*x+w-screen.width)<=1,"track off centre");
              check(x>=bounds.x()&&x+w+40<=bounds.x()+bounds.width(),"timestamp clipped");
              check(!panel.press(x-1,y+8,0)&&!panel.press(x+w,y+8,0),"outside track captures seek");
              check(panel.press(x,y+8,0)&&panel.press(x+w-1,y+8,0),"track endpoints miss seek");
              call(scrub,"cancel");
              var buttons=((ScreenInput)screen).power$buttons();
              int top=buttons.stream().filter(b->b.visible).mapToInt(b->b.y).min().orElseThrow();
              int bottom=buttons.stream().filter(b->b.visible).mapToInt(b->b.y+20).max().orElseThrow();
              check(anchor==AudioSettings.MenuControlsPosition.MENU_TOP?bounds.y()+bounds.height()<=top:bounds.y()>=bottom,"controls overlap menu");
              check(org.lwjgl.opengl.GL11.glGetError()==0,"GL error");
            });
    } finally {AudioController.quiet();AudioConfig.preview(before);mc.setScreen(parent);}
    log("MENU SCRUB FAILURES "+failures);
  }
}
