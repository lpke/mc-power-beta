package local.luke.power.debug;

import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.LongAdder;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Bounded timing buffers. JFR and packet accounting run only when requested. */
public final class DebugPerformance {
  private static final float[] FRAME=new float[240],TICK=new float[240];
  private static int frameIndex,tickIndex;
  private static long frameTime,tickTime,recordUntil;
  private static Recording recording;
  private static final LongAdder sent=new LongAdder(),received=new LongAdder();
  private static long sentBefore,receivedBefore,networkTime;
  private static float sendRate,receiveRate;
  private DebugPerformance() {}
  public static boolean monitoring() { return (recording!=null || DebugActions.frames || DebugActions.profiler || DebugActions.network) && DebugConfig.enabled(); }
  public static boolean networkEnabled() { return (recording!=null || DebugActions.network) && DebugConfig.enabled(); }
  public static void packet(boolean outgoing,int bytes) { if(networkEnabled()) (outgoing?sent:received).add(Math.max(0,bytes)); }
  public static void beginTick() { tickTime=monitoring()?System.nanoTime():0; }
  public static void tick(Minecraft mc) {
    if(tickTime!=0) { TICK[tickIndex++%TICK.length]=(System.nanoTime()-tickTime)/1_000_000f;tickTime=0; }
    if(recording!=null && (System.nanoTime()>=recordUntil || mc.world==null || !DebugConfig.enabled())) finish(mc);
    if(!monitoring()) { frameTime=networkTime=0; return; }
    if(DebugActions.network) {
      long now=System.nanoTime();
      if(networkTime==0) {networkTime=now;sentBefore=sent.sum();receivedBefore=received.sum();}
      else if(now-networkTime>=1_000_000_000L) {
        long s=sent.sum(),r=received.sum();float seconds=(now-networkTime)/1_000_000_000f;
        sendRate=(s-sentBefore)/seconds; receiveRate=(r-receivedBefore)/seconds;
        sentBefore=s;receivedBefore=r;networkTime=now;
      }
    }
  }
  public static void record(Minecraft mc) throws Exception {
    if(recording!=null) {finish(mc);return;}
    recording=new Recording(Configuration.getConfiguration("profile"));
    recording.setName("Power Beta performance"); recording.setMaxSize(32*1024*1024);recording.setMaxAge(Duration.ofSeconds(20));
    recording.start();recordUntil=System.nanoTime()+10_000_000_000L;
    DebugActions.message(mc,"Recording performance for 10 seconds. Press again to finish early.");
  }
  private static void finish(Minecraft mc) {
    Recording finished=recording;recording=null;
    try(finished) {
      finished.stop();Path directory=Path.of("debug/profiling");Files.createDirectories(directory);
      Path target=directory.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"))+".jfr");
      finished.dump(target);DebugActions.message(mc,"Saved performance recording to "+target);
    } catch(Exception e) {local.luke.power.PowerBeta.LOG.error("Could not save performance recording",e);DebugActions.message(mc,"Could not save performance recording. See the game log.");}
  }
  public static void render(Minecraft mc) {
    if(!DebugConfig.enabled() || mc.world==null || mc.player==null) return;
    if(monitoring()) {
      long now=System.nanoTime();if(frameTime!=0)FRAME[frameIndex++%FRAME.length]=Math.min(1000,(now-frameTime)/1_000_000f);frameTime=now;
    }
    if(mc.options.hideHud) return;
    var size=new net.minecraft.class_564(mc.options,mc.displayWidth,mc.displayHeight);
    int w=size.method_1857(),h=size.method_1858(),x=w-125,y=h-88;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      if(DebugActions.frames || DebugActions.profiler) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(0,0,0,.7f);GL11.glBegin(GL11.GL_QUADS);GL11.glVertex2i(x-3,y-12);GL11.glVertex2i(w-2,y-12);GL11.glVertex2i(w-2,h-4);GL11.glVertex2i(x-3,h-4);GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        for(int i=0;i<120;i++) {
          float f=FRAME[Math.floorMod(frameIndex-120+i,240)],t=TICK[Math.floorMod(tickIndex-120+i,240)];
          GL11.glColor3f(f>50?1:0,f>50?0:1,0);GL11.glVertex2i(x+i,h-8);GL11.glVertex2f(x+i,h-8-Math.min(64,f));
          if(DebugActions.profiler){GL11.glColor3f(1,.5f,0);GL11.glVertex2i(x+i,h-8);GL11.glVertex2f(x+i,h-8-Math.min(64,t));}
        }
        GL11.glEnd();GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glColor4f(1,1,1,1);
        mc.textRenderer.drawWithShadow(DebugActions.profiler?"Frame / tick time (ms)":"Frame time (ms)",x,y-10,0xffffff);
      }
      if(DebugActions.network) {
        String label=mc.world.isRemote?String.format(java.util.Locale.ROOT,"Network: %.1f / %.1f KiB/s",receiveRate/1024,sendRate/1024):"Network: singleplayer";
        mc.textRenderer.drawWithShadow(label,2,h-12,0xffffff);
      }
      if(DebugActions.light) {
        // Beta has no GPU lightmap. Display its actual 16-level dimension brightness ramp.
        float[] levels=mc.world.dimension.field_2178;
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glBegin(GL11.GL_QUADS);
        for(int i=0;i<16;i++){float value=levels[i];GL11.glColor3f(value,value,value);int left=w-132+i*8;GL11.glVertex2i(left,30);GL11.glVertex2i(left+8,30);GL11.glVertex2i(left+8,54);GL11.glVertex2i(left,54);}
        GL11.glEnd();GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glColor4f(1,1,1,1);
        mc.textRenderer.drawWithShadow("Light brightness 0 - 15",w-132,18,0xffffff);
      }
    } finally {GL11.glPopAttrib();}
  }
}
