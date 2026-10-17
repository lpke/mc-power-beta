package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.light.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;

/** Native-font pixel and submission-cost comparison. Disposable clone only. */
final class LightPerformance230 {
  private static final String[] NUMBERS = new String[16];
  static { for (int i=0;i<16;i++) NUMBERS[i]=Integer.toString(i); }
  static void run(Minecraft mc) throws Exception {
    failures=0;mc.setScreen(null);boolean hidden=mc.options.hideHud;mc.options.hideHud=false;
    LightSettings before=LightConfig.copy();
    Field f=LightOverlay.class.getDeclaredField("CACHE");f.setAccessible(true);LightCache cache=(LightCache)f.get(null);
    Field world=LightOverlay.class.getDeclaredField("world");world.setAccessible(true);
    try {
      LightSettings settings=new LightSettings();settings.enabled=true;settings.radius=24;settings.verticalRange=16;
      settings.checksPerTick=16384;LightConfig.preview(settings);world.set(null,mc.world);
      int px=(int)Math.floor(mc.player.x),py=(int)Math.floor(mc.player.boundingBox.minY)-1,pz=(int)Math.floor(mc.player.z);
      for(int i=0;i<100&&cache.cells().size()<2048;i++)cache.tick(mc.world,px,py,pz,settings,(x,y,z)->Math.floorMod(x+z,16));
      var cells=List.copyOf(cache.cells());check(cells.size()==2048,"stress cache did not reach 2048 labels");
      Path report=Path.of("power-beta-data/reports/light-230");Files.createDirectories(report);
      for(int yaw:new int[]{0,90,180,270}) {
        float oldYaw=mc.player.yaw;mc.player.yaw=yaw;
        try {
          Screen nativeView=view(mc,()->legacy(mc,1,cells)),batchView=view(mc,()->LightOverlay.render(mc,1));
          nativeView.init(mc,640,420);batchView.init(mc,640,420);
          SettingsSnapshot.render(mc,nativeView,640,420,report.resolve("native-"+yaw+".png"));
          SettingsSnapshot.render(mc,batchView,640,420,report.resolve("batch-"+yaw+".png"));
          var a=ImageIO.read(report.resolve("native-"+yaw+".png").toFile());var b=ImageIO.read(report.resolve("batch-"+yaw+".png").toFile());
          int changed=0,painted=0;
          for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++) {
            if((a.getRGB(x,y)&0xffffff)!=0)painted++;
            if(a.getRGB(x,y)!=b.getRGB(x,y))changed++;
          }
          check(painted>100,"native font pixels missing");
          check(changed<Math.max(100,painted/100),"font geometry differs at yaw "+yaw+": "+changed+" / "+painted);
          log("PASS native/batched light font pixels yaw="+yaw+" changed="+changed+" painted="+painted);
        } finally {mc.player.yaw=oldYaw;}
      }
      for(boolean batch:new boolean[]{false,true}) {
        long[] times=new long[40];
        for(int run=-10;run<times.length;run++) {
          GL11.glFinish();long start=System.nanoTime();
          if(batch)LightOverlay.render(mc,1);else legacy(mc,1,cells);
          GL11.glFinish();if(run>=0)times[run]=System.nanoTime()-start;
        }
        Arrays.sort(times);log("LIGHT RENDER "+(batch?"batch":"native")+" labels="+cells.size()+" median_ms="+times[20]/1e6+" p95_ms="+times[38]/1e6);
      }
      check(GL11.glGetError()==GL11.GL_NO_ERROR,"light batch GL error");
    } finally {LightConfig.preview(before);cache.clear();mc.options.hideHud=hidden;}
    log("LIGHT 230 FAILURES "+failures);
  }
  private static Screen view(Minecraft mc,Runnable draw) {
    return new Screen() {public void render(int mx,int my,float delta) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(-25,25,-17,17,-100,100);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glRotatef(90,1,0,0);
      try {GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);draw.run();}
      finally {GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopAttrib();}
    }};
  }
  static void legacy(Minecraft mc, float delta, java.util.List<LightCache.Cell> cells) {
    LightSettings s = LightConfig.current();
    if (!s.enabled || mc.world == null || mc.player == null
        || mc.options.hideHud || cells.isEmpty()) return;
    var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
    double cx = camera.field_1637 + (camera.x - camera.field_1637) * delta;
    double cy = camera.field_1638 + (camera.y - camera.field_1638) * delta;
    double cz = camera.field_1639 + (camera.z - camera.field_1639) * delta;
    int low = LightSettings.rgb(s.lowColor), high = LightSettings.rgb(s.highColor);
    // Rotate in 90-degree steps so the text reads toward the viewing direction.
    float angle = Math.round(camera.yaw / 90f) * 90f;
    double scale = s.textSize / 12;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(-cx, -cy, -cz);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_FOG);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDepthMask(false);
      for (LightCache.Cell cell : cells) {
        double dx = cell.x() + .5 - camera.x, dz = cell.z() + .5 - camera.z;
        if (dx * dx + dz * dz > s.radius * s.radius
            || Math.abs(cell.y() + 1 - camera.boundingBox.minY) > s.verticalRange + 1) continue;
        GL11.glPushMatrix();
        try {
          GL11.glTranslated(cell.x() + .5, cell.y() + 1.006, cell.z() + .5);
          GL11.glRotatef(-angle, 0, 1, 0);
          GL11.glRotatef(90, 1, 0, 0);
          GL11.glScaled(-scale, -scale, scale);
          String number = NUMBERS[cell.level()];
          mc.textRenderer.draw(number, -mc.textRenderer.getWidth(number) / 2, -4,
              cell.level() < s.greenFrom ? low : high);
        } finally { GL11.glPopMatrix(); }
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }
}
