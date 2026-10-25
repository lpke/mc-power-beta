package local.luke.power.debug;

import local.luke.power.input.DetachedCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

/** Loaded entities and fixed Beta-height chunk geometry; no world queries or workers. */
public final class DebugWorld {
  private DebugWorld() {}
  public static void render(Minecraft mc,float delta) {
    if(!DebugConfig.enabled() || (!DebugActions.hitboxes && !DebugActions.borders)
        || mc.world==null || mc.player==null || mc.options.hideHud) return;
    var camera=mc.field_2807==null?mc.player:mc.field_2807;
    double cx=camera.field_1637+(camera.x-camera.field_1637)*delta;
    double cy=camera.field_1638+(camera.y-camera.field_1638)*delta;
    double cz=camera.field_1639+(camera.z-camera.field_1639)*delta;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
      GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(false); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glLineWidth(1); GL11.glTranslated(-cx,-cy,-cz);
      GL11.glBegin(GL11.GL_LINES);
      if(DebugActions.borders && !(mc.currentScreen instanceof local.luke.power.input.PhotoProjection.CameraOwner)) borders(cx,cz);
      if(DebugActions.hitboxes) {
        int count=0;
        for(Object item:mc.world.field_198) {
          if(++count>16384) break;
          if(!(item instanceof Entity e) || e.dead || local.luke.power.render.EntityVisibility.hidden(e)) continue;
          if(e==camera && !mc.options.thirdPerson && !DetachedCamera.isActive()) continue;
          double dx=e.x-cx,dy=e.y-cy,dz=e.z-cz;
          if(dx*dx+dy*dy+dz*dz>128*128) continue;
          var b=e.boundingBox;
          double ox=(e.field_1637-e.x)*(1-delta),oy=(e.field_1638-e.y)*(1-delta),oz=(e.field_1639-e.z)*(1-delta);
          double x=b.minX+ox,y=b.minY+oy,z=b.minZ+oz,X=b.maxX+ox,Y=b.maxY+oy,Z=b.maxZ+oz;
          GL11.glColor3f(1,1,1); box(x,y,z,X,Y,Z);
          double eye=y+(e instanceof net.minecraft.entity.LivingEntity living ? living.method_1378() : (Y-y)*.5), ex=(x+X)*.5,ez=(z+Z)*.5;
          GL11.glColor3f(1,0,0); box(x,eye-.01,z,X,eye+.01,Z);
          float yaw=e.prevYaw+(e.yaw-e.prevYaw)*delta,pitch=e.prevPitch+(e.pitch-e.prevPitch)*delta;
          double yr=Math.toRadians(yaw),pr=Math.toRadians(pitch);
          GL11.glColor3f(0,0,1); line(ex,eye,ez,ex-Math.sin(yr)*Math.cos(pr)*2,eye-Math.sin(pr)*2,ez+Math.cos(yr)*Math.cos(pr)*2);
        }
      }
      GL11.glEnd();
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
  /** Photo hover reuses the native debug-style chunk geometry without changing debug state. */
  public static void renderPhotoBorders(Minecraft mc,float delta,double x,double z) {
    if(mc.world==null||mc.player==null)return;
    var camera=mc.field_2807==null?mc.player:mc.field_2807;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
    try {
      GL11.glTranslated(-camera.field_1637-(camera.x-camera.field_1637)*delta,
          -camera.field_1638-(camera.y-camera.field_1638)*delta,-camera.field_1639-(camera.z-camera.field_1639)*delta);
      GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);
      GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glLineWidth(1);GL11.glBegin(GL11.GL_LINES);borders(x,z);GL11.glEnd();
    } finally {GL11.glPopMatrix();GL11.glPopAttrib();}
  }
  private static void borders(double cx,double cz) {
    double x=Math.floor(cx/16)*16,z=Math.floor(cz/16)*16;
    GL11.glColor3f(1,0,0);
    for(int ix=-16;ix<=32;ix+=16) for(int iz=-16;iz<=32;iz+=16) line(x+ix,0,z+iz,x+ix,128,z+iz);
    for(int y=0;y<=128;y+=2) {
      if(y%16==0) GL11.glColor3f(0,1,1); else GL11.glColor3f(1,1,0);
      line(x,y,z,x+16,y,z); line(x+16,y,z,x+16,y,z+16);
      line(x+16,y,z+16,x,y,z+16); line(x,y,z+16,x,y,z);
    }
    GL11.glColor3f(0,0,1); box(x,0,z,x+16,128,z+16);
  }
  private static void box(double x,double y,double z,double X,double Y,double Z) {
    line(x,y,z,X,y,z);line(X,y,z,X,y,Z);line(X,y,Z,x,y,Z);line(x,y,Z,x,y,z);
    line(x,Y,z,X,Y,z);line(X,Y,z,X,Y,Z);line(X,Y,Z,x,Y,Z);line(x,Y,Z,x,Y,z);
    line(x,y,z,x,Y,z);line(X,y,z,X,Y,z);line(X,y,Z,X,Y,Z);line(x,y,Z,x,Y,Z);
  }
  private static void line(double x,double y,double z,double X,double Y,double Z) { GL11.glVertex3d(x,y,z);GL11.glVertex3d(X,Y,Z); }
}
