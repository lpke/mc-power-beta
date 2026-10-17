package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.entity.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** One native cow and unedited forest terrain; captures its real native F3 tag projection. */
final class Forest241Capture {
  private static Entity cow;
  private static Minecraft client;
  private static boolean hold, project;
  static void scene(Minecraft mc)throws Exception {
    client=mc;
    if(mc.world==null)Forest240Capture.run(mc,"forest");
    AccessRules.of(mc.world.method_262()).allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
    WebsiteGallery.setting(mc,"native.renderDistance","12");WebsiteGallery.setting(mc,"native.fov","70");
    WebsiteGallery.setting(mc,"native.fogDensity","100");
    WebsiteGallery.x=474;WebsiteGallery.y=66;WebsiteGallery.z=-626;WebsiteGallery.camera(mc,0,0,0,-225,0);
    ChatChecks.submit(mc,"/tp 474 66 -626");ChatChecks.submit(mc,"/time set noon");ChatChecks.submit(mc,"/weather clear");
    mc.options.hideHud=true;mc.options.debugHud=false;mc.options.thirdPerson=false;
    for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)mc.world.method_214((474>>4)+dx,(-626>>4)+dz);
    cow=net.minecraft.class_206.method_732("Cow",mc.world);check(cow!=null,"Cow registry unavailable");
    for(Object object:new ArrayList<>(mc.world.field_198))if(object instanceof Entity entity
        && entity.getClass()==cow.getClass())entity.markDead();
    StringBuilder report=new StringBuilder();
    for(int x=470;x<=474;x++)for(int z=-631;z<=-626;z++) {
      report.append(x).append('/').append(z).append(':');
      for(int y=64;y<=69;y++)report.append(' ').append(y).append('=').append(mc.world.getBlockId(x,y,z));report.append('\n');
    }
    Files.writeString(Path.of("power-beta-data/reports/release-241/forest-ground.txt"),report);
    position(470.8,66,-628.3);check(mc.world.method_210(cow),"Could not spawn native capture cow");hold=true;
    mc.inGameHud.clearChat();log("NATIVE FOREST COW id="+cow.id+" 12 chunks, noon/clear; capture pose held");
  }
  private static double x=470.8,y=66,z=-628.3;
  private static void position(double px,double py,double pz) {
    x=px;y=py;z=pz;cow.method_1338(x,y+cow.field_1631,z,-45,0);
    cow.prevX=cow.field_1637=cow.x;cow.prevY=cow.field_1638=cow.y;cow.prevZ=cow.field_1639=cow.z;
    cow.prevYaw=cow.yaw;cow.prevPitch=cow.pitch;cow.velocityX=cow.velocityY=cow.velocityZ=0;
  }
  static void pose(String text)throws Exception {String[] p=text.split(" ");position(Double.parseDouble(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2]));log("COW POSE "+text);}
  static void tick(Minecraft mc) {if(hold&&cow!=null&&mc==client&&cow.world==mc.world)position(x,y,z);}
  static void project(boolean enabled) {project=enabled;if(client!=null)client.options.debugHud=enabled;}
  static void tag(String text,int tx,int ty) {
    if(!project||cow==null||!text.equals(Integer.toString(cow.id)))return;
    try {
      var model=BufferUtils.createFloatBuffer(16);var projection=BufferUtils.createFloatBuffer(16);var viewport=BufferUtils.createIntBuffer(16);
      GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,model);GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,projection);GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);
      var centre=BufferUtils.createFloatBuffer(3);var unit=BufferUtils.createFloatBuffer(3);
      int span=client.textRenderer.getWidth(text);
      if(!GLU.gluProject(tx+span/2F,ty,0,model,projection,viewport,centre)||!GLU.gluProject(tx+span/2F+1,ty+1,0,model,projection,viewport,unit))return;
      JsonObject result=new JsonObject();result.addProperty("id",text);result.addProperty("x",centre.get(0)/client.displayWidth);result.addProperty("y",1-centre.get(1)/client.displayHeight);
      result.addProperty("unitX",Math.abs(unit.get(0)-centre.get(0))/client.displayWidth);result.addProperty("unitY",Math.abs(unit.get(1)-centre.get(1))/client.displayHeight);
      Files.writeString(Path.of("power-beta-data/reports/release-241/native-tag.json"),result.toString());project=false;
    }catch(Exception e){throw new RuntimeException(e);}
  }
}
