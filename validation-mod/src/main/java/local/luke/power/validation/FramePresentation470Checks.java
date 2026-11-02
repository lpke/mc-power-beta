package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import local.luke.power.controls.util.ModOptions;
import local.luke.power.input.TweakLifecycle;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/** Production swap accounting and a disposable legacy comparison; never a shipped setting. */
public final class FramePresentation470Checks {
  private static boolean legacy,frameLate,pending,registered,inFrame,flushing;
  private static Thread thread;
  private static Minecraft fullscreenClient;
  private static Boolean originalFullscreen;
  private static Integer originalSwapInterval;
  private static int updateDepth,updates,swaps,deferred,requests,explicit;
  private static long frames,activeSingleUpdateFrames,invalidActiveFrames,uncappedFrames,uncappedDeferred,scopeLeaks,multipleUpdateFrames,nativeSkippedFrames,explicitSwaps;
  private FramePresentation470Checks() {}
  public static boolean legacyMode(){return legacy;}
  public static String mode(){return legacy?"legacy-native-swap":ModOptions.isFramerateLimited()?"production-late-swap-only":"production-native-uncapped-bypass";}
  public static void run(Minecraft mc,String action)throws Exception{
    check(Display.isCreated(),"open the disposable native client");
    if(action.equals("status")){log("PRESENTATION470 "+stats());return;}
    if(action.equals("reset-stats")){resetStats();log("PRESENTATION470 counters reset "+mode());return;}
    if(action.equals("check")){
      try{
        check(activeSingleUpdateFrames>0,"no active visible frames sampled");
        check(invalidActiveFrames==0&&scopeLeaks==0,"presentation invariant failed "+stats());
        check(uncappedDeferred==0,"uncapped frame deferred a swap "+stats());
        if(!legacy&&!ModOptions.isFramerateLimited())check(uncappedFrames>0,"uncapped bypass was not exercised");
        log("PRESENTATION470 PASS "+stats());return;
      }catch(AssertionError failure){restore();throw failure;}
    }
    if(Smoothness470Checks.observing())Smoothness470Checks.stop();
    if(!registered){registered=true;TweakLifecycle.register("validation_presentation_470",FramePresentation470Checks::restore);}
    if(action.equals("fullscreen")){
      if(originalFullscreen==null){originalFullscreen=Display.isFullscreen();fullscreenClient=mc;}
      mc.toggleFullscreen();log("PRESENTATION470 fullscreen="+Display.isFullscreen()+" "+stats());return;
    }
    if(action.equals("outside-update")){
      check(updateDepth==0,"outside-update was called from inside a native update");
      Display.update();log("PRESENTATION470 explicit unscoped Display.update completed "+stats());return;
    }
    if(action.equals("vsync-on")||action.equals("vsync-off")){
      if(originalSwapInterval==null)originalSwapInterval=swapInterval();
      Display.setVSyncEnabled(action.equals("vsync-on"));
      log("PRESENTATION470 requestedSwapInterval="+swapInterval()+"; actual swap durations are measured separately, physical scanout is unverified");return;
    }
    if(action.equals("off")||action.equals("stop")){restore();resetStats();log("PRESENTATION470 restored "+mode());return;}
    if(!action.equals("native")&&!action.equals("late")&&!action.equals("production")&&!action.equals("legacy")){
      restore();check(false,"presentation-470 native|legacy|status|check|reset-stats|fullscreen|outside-update|vsync-on|vsync-off|stop");
    }
    legacy=action.equals("legacy");resetStats();log("PRESENTATION470 "+mode());
  }
  public static void restore(){
    legacy=false;
    if(Display.isCreated()){
      if(originalSwapInterval!=null)Display.setSwapInterval(originalSwapInterval);
      if(originalFullscreen!=null&&originalFullscreen!=Display.isFullscreen()&&fullscreenClient!=null)fullscreenClient.toggleFullscreen();
    }
    originalSwapInterval=null;originalFullscreen=null;fullscreenClient=null;
  }
  private static int swapInterval()throws ReflectiveOperationException{
    Field field=Display.class.getDeclaredField("swap_interval");field.setAccessible(true);return field.getInt(null);
  }
  public static void beginFrame(){
    if(updateDepth!=0)scopeLeaks++;
    thread=Thread.currentThread();frameLate=!legacy&&ModOptions.isFramerateLimited();pending=false;flushing=false;inFrame=true;
    updateDepth=updates=swaps=deferred=requests=explicit=0;
  }
  public static void beginUpdate(){
    if(Thread.currentThread()!=thread||!inFrame)return;
    updates++;updateDepth++;
  }
  public static void endUpdate(){if(Thread.currentThread()==thread&&updateDepth>0)updateDepth--;}
  public static void swapRequested(boolean wasDeferred){
    if(Thread.currentThread()!=thread||!inFrame||updateDepth==0)return;
    requests++;if(wasDeferred){pending=true;deferred++;}
  }
  public static void actualSwap(){if(Thread.currentThread()==thread&&inFrame){swaps++;if(updateDepth==0&&!flushing)explicit++;}}
  public static void beginFlush(){flushing=true;}
  public static void updateFailed(){restore();frameLate=pending=false;updateDepth=0;}
  public static void endFrame(){
    try{
      frames++;
      if(updates>1)multipleUpdateFrames++;
      if(!legacy&&!frameLate){uncappedFrames++;uncappedDeferred+=deferred;}
      explicitSwaps+=explicit;
      if(updates==1&&Display.isActive()&&Display.isVisible()){
        if(requests==1){activeSingleUpdateFrames++;if(swaps!=1+explicit)invalidActiveFrames++;}
        else if(requests==0)nativeSkippedFrames++;
      }
      if(updateDepth!=0)scopeLeaks++;
    }finally{pending=flushing=inFrame=false;}
  }
  private static void resetStats(){frames=activeSingleUpdateFrames=invalidActiveFrames=uncappedFrames=uncappedDeferred=scopeLeaks=multipleUpdateFrames=nativeSkippedFrames=explicitSwaps=0;}
  private static Map<String,Object> stats(){
    var values=new LinkedHashMap<String,Object>();values.put("mode",mode());values.put("frames",frames);values.put("activeSingleUpdateFrames",activeSingleUpdateFrames);
    values.put("invalidActiveFrames",invalidActiveFrames);values.put("uncappedBypassFrames",uncappedFrames);values.put("uncappedDeferredSwaps",uncappedDeferred);
    values.put("nativeVisibilitySkippedFrames",nativeSkippedFrames);values.put("explicitOutsideScopeSwaps",explicitSwaps);values.put("scopeLeaks",scopeLeaks);values.put("multipleNativeUpdateFrames",multipleUpdateFrames);values.put("frameLate",frameLate);values.put("pending",pending);values.put("fullscreen",Display.isFullscreen());
    try{values.put("requestedSwapInterval",swapInterval());}catch(ReflectiveOperationException failure){values.put("requestedSwapInterval","unavailable: "+failure);}
    return values;
  }
}
