package local.luke.power.validation;

import static local.luke.power.validation.Validation.check;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Observes the native world draw. Query results never stall the render thread. */
final class HeldLight400Frames implements AutoCloseable {
  static final class Samples {
    final List<Double> cpu = new ArrayList<>(), gpu = new ArrayList<>(), ticks = new ArrayList<>(), intervals = new ArrayList<>();
    double minimumStrength = 1, maximumStrength, imageMean;
    long frames, requests, admissions, neutralPasses, waterPasses, mismatches, firstWarmMillis = -1;
    Map<String,Object> report() {
      var out = new LinkedHashMap<String,Object>();
      out.put("frames", frames); out.put("renderCpuMs", percentiles(cpu)); out.put("renderGpuMs", percentiles(gpu));
      out.put("tickCpuMs", percentiles(ticks)); out.put("frameIntervalMs", percentiles(intervals));
      out.put("minimumStrength", minimumStrength); out.put("maximumStrength", maximumStrength);
      out.put("firstWarmMillis", firstWarmMillis); out.put("requests", requests); out.put("admissions", admissions);
      out.put("neutralPasses", neutralPasses); out.put("waterPasses", waterPasses); out.put("mismatches", mismatches);
      out.put("imageCenterMean", imageMean); return out;
    }
  }
  private record Pending(int query, Samples owner) {}
  private final boolean timers = GLContext.getCapabilities().OpenGL33;
  private final ArrayDeque<Integer> available = new ArrayDeque<>();
  private final ArrayDeque<Pending> pending = new ArrayDeque<>();
  private int activeQuery;
  private long frameStart, previousStart, tickStart;
  private Samples frameOwner, tickOwner;
  HeldLight400Frames() { if (timers) for (int i=0;i<8;i++) available.add(GL15.glGenQueries()); }
  boolean gpuTimers() { return timers; }

  void beforeFrame(Samples owner) {
    poll(); frameOwner = owner;
    long now = System.nanoTime();
    if (owner != null && previousStart != 0 && owner.intervals.size()<240) owner.intervals.add((now-previousStart)/1e6);
    previousStart = now;
    if (owner != null && timers && !available.isEmpty()
        && GL15.glGetQueryi(GL33.GL_TIME_ELAPSED, GL15.GL_CURRENT_QUERY)==0) {
      activeQuery = available.remove(); GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, activeQuery);
    }
    frameStart = System.nanoTime();
  }
  void afterFrame() {
    long end = System.nanoTime();
    if (frameOwner != null && frameOwner.cpu.size()<240) frameOwner.cpu.add((end-frameStart)/1e6);
    if (activeQuery != 0) {
      GL15.glEndQuery(GL33.GL_TIME_ELAPSED); pending.add(new Pending(activeQuery, frameOwner)); activeQuery=0;
    }
    frameOwner=null; poll();
  }
  void beforeTick(Samples owner) { tickOwner=owner; tickStart=System.nanoTime(); }
  void afterTick() {
    if (tickOwner != null && tickOwner.ticks.size()<240) tickOwner.ticks.add((System.nanoTime()-tickStart)/1e6);
    tickOwner=null;
  }
  private void poll() {
    while (!pending.isEmpty() && GL15.glGetQueryObjecti(pending.peek().query, GL15.GL_QUERY_RESULT_AVAILABLE)!=0) {
      Pending result=pending.remove();
      double time=GL33.glGetQueryObjectui64(result.query, GL15.GL_QUERY_RESULT)/1e6;
      if (result.owner!=null && result.owner.gpu.size()<240) result.owner.gpu.add(time);
      available.add(result.query);
    }
  }
  @Override public void close() {
    if (activeQuery!=0) { GL15.glEndQuery(GL33.GL_TIME_ELAPSED); GL15.glDeleteQueries(activeQuery); activeQuery=0; }
    for (Pending result:pending) GL15.glDeleteQueries(result.query);
    for (int query:available) GL15.glDeleteQueries(query);
    pending.clear(); available.clear();
  }

  static Map<String,Object> percentiles(List<Double> values) {
    if (values.isEmpty()) return Map.of("samples",0);
    var sorted=new ArrayList<>(values); Collections.sort(sorted);
    return Map.of("samples",sorted.size(),"p50",at(sorted,.50),"p95",at(sorted,.95),"p99",at(sorted,.99),"max",sorted.get(sorted.size()-1));
  }
  private static double at(List<Double> values,double fraction) { return values.get(Math.min(values.size()-1,(int)Math.ceil(values.size()*fraction)-1)); }

  static BufferedImage screenshot(Minecraft mc,Path file) throws Exception {
    int w=mc.displayWidth,h=mc.displayHeight;
    check(w>0&&h>0&&(long)w*h<=16_777_216,"held-light capture exceeds 16 megapixels");
    if (GLContext.getCapabilities().GL_EXT_framebuffer_object)
      check(GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT)==0,"native window framebuffer is not bound");
    int read=GL11.glGetInteger(GL11.GL_READ_BUFFER), pack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
    ByteBuffer pixels=BufferUtils.createByteBuffer(w*h*3);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    try {
      GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0); GL11.glReadBuffer(GL11.GL_BACK);
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1); GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,0); GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,0);
      GL11.glReadPixels(0,0,w,h,GL11.GL_RGB,GL11.GL_UNSIGNED_BYTE,pixels);
    } finally { GL11.glReadBuffer(read); GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pack); GL11.glPopClientAttrib(); }
    var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
    for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
      int p=(y*w+x)*3; image.setRGB(x,h-y-1,(pixels.get(p)&255)<<16|(pixels.get(p+1)&255)<<8|pixels.get(p+2)&255);
    }
    Files.createDirectories(file.getParent()); check(ImageIO.write(image,"png",file.toFile()),"PNG writer unavailable"); return image;
  }
  static double centerMean(BufferedImage image) {
    long total=0,count=0;
    for(int y=image.getHeight()*3/10;y<image.getHeight()*6/10;y++) for(int x=image.getWidth()*3/10;x<image.getWidth()*7/10;x++) {
      int rgb=image.getRGB(x,y); total+=(rgb>>16&255)+(rgb>>8&255)+(rgb&255); count+=3;
    }
    return total/(double)Math.max(1,count);
  }
}
