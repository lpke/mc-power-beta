package local.luke.power.map;

import java.nio.*;
import local.luke.power.input.PhotoProjection;
import local.luke.power.input.RenderedTerrainBounds;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** Retains the actual orthographic matrices, then raycasts only for pointer actions/hover ticks. */
public final class PhotoPicking {
  private static final FloatBuffer MODEL=BufferUtils.createFloatBuffer(16),PROJECTION=BufferUtils.createFloatBuffer(16),
      NEAR=BufferUtils.createFloatBuffer(3),FAR=BufferUtils.createFloatBuffer(3);
  private static final IntBuffer VIEWPORT=BufferUtils.createIntBuffer(16);
  private static Object world;
  private static double ox,oy,oz;
  private PhotoPicking() {}
  public static void clear(){world=null;}
  public static void capture(Minecraft mc,float delta) {
    if(!(mc.currentScreen instanceof PhotoProjection.CameraOwner)||mc.world==null||mc.player==null){clear();return;}
    var camera=mc.field_2807==null?mc.player:mc.field_2807;
    ox=camera.field_1637+(camera.x-camera.field_1637)*delta;
    oy=camera.field_1638+(camera.y-camera.field_1638)*delta;
    oz=camera.field_1639+(camera.z-camera.field_1639)*delta;
    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,MODEL);GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,PROJECTION);
    GL11.glGetInteger(GL11.GL_VIEWPORT,VIEWPORT);world=mc.world;
  }
  public static PhotoRay.Hit pick(Minecraft mc,int x,int y,int steps) {
    if(world!=mc.world || world==null || !(mc.worldRenderer instanceof RenderedTerrainBounds bounds))return null;
    float px=(float)x*mc.displayWidth/mc.currentScreen.width,
        py=mc.displayHeight-(float)y*mc.displayHeight/mc.currentScreen.height;
    if(!GLU.gluUnProject(px,py,0,MODEL,PROJECTION,VIEWPORT,NEAR)
        || !GLU.gluUnProject(px,py,1,MODEL,PROJECTION,VIEWPORT,FAR))return null;
    double ax=NEAR.get(0)+ox,ay=NEAR.get(1)+oy,az=NEAR.get(2)+oz;
    double dx=FAR.get(0)-NEAR.get(0),dy=FAR.get(1)-NEAR.get(1),dz=FAR.get(2)-NEAR.get(2);
    double start=0,end=1;
    double[] at={ax,ay,az},direction={dx,dy,dz},low={bounds.powerTerrain$minX(),0,bounds.powerTerrain$minZ()},
        high={bounds.powerTerrain$maxX(),128,bounds.powerTerrain$maxZ()};
    for(int i=0;i<3;i++) {
      if(Math.abs(direction[i])<1e-12){if(at[i]<low[i]||at[i]>=high[i])return null;continue;}
      double a=(low[i]-at[i])/direction[i],b=(high[i]-at[i])/direction[i];
      start=Math.max(start,Math.min(a,b));end=Math.min(end,Math.max(a,b));
    }
    if(start>=end)return null;
    start+=1e-7;end-=1e-7;
    return PhotoRay.trace(ax+dx*start,ay+dy*start,az+dz*start,ax+dx*end,ay+dy*end,az+dz*end,steps,
        (bx,by,bz)-> {
          if(by<0||by>=128)return -1;
          var chunk=MapSampler.loaded(mc.world,bx>>4,bz>>4);
          if(chunk==null)return -1;
          int id=chunk.method_859(bx&15,by,bz&15);
          if(id==0)return 0;
          var block=net.minecraft.block.Block.BLOCKS[id];
          if(block==null)return 0;
          var material=block.field_1900;
          // A waypoint targets terrain rather than a non-colliding flower or torch.
          return material==net.minecraft.block.Material.WATER||material==net.minecraft.block.Material.LAVA
              ||material==net.minecraft.block.Material.LEAVES||block.method_1624(mc.world,bx,by,bz)!=null?1:0;
        });
  }
}
