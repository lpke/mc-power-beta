package local.luke.power.map;

import java.util.*;
import local.luke.power.input.RenderedTerrainBounds;
import local.luke.power.light.LightOverlay;
import local.luke.power.light.LightSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import org.lwjgl.opengl.GL11;

/** Loaded-only surface survey, bounded on ticks. Drawing never reads terrain. */
public final class PhotoTerrainSurvey {
  private record Cell(int x,double y,int z,boolean border,boolean spawn) {}
  private static final LightSettings SAMPLE=new LightSettings();
  static { SAMPLE.spawnableOnly=true; }
  private final List<Cell> pending=new ArrayList<>();
  private List<Cell> cells=List.of();
  private Object world;
  private int left,top,right,bottom,cursor,ticks;
  private boolean borders,spawn,completed;
  public int checksLastTick;
  public void clear(){world=null;completed=false;cursor=ticks=checksLastTick=0;pending.clear();cells=List.of();}
  public void tick(Minecraft mc,boolean chunks,boolean light) {
    checksLastTick=0;
    if(!chunks&&!light || mc.world==null || !(mc.worldRenderer instanceof RenderedTerrainBounds bounds)) {if(world!=null)clear();return;}
    // One shared bounded surface footprint. This follows the existing renderer, never a requested chunk.
    int radius=MapConfig.current().lightRadius;
    int cx=(int)Math.floor((bounds.powerTerrain$minX()+bounds.powerTerrain$maxX())*.5),
        cz=(int)Math.floor((bounds.powerTerrain$minZ()+bounds.powerTerrain$maxZ())*.5);
    int l=chunks?(int)Math.floor(bounds.powerTerrain$minX()):Math.max((int)Math.floor(bounds.powerTerrain$minX()),cx-radius),
        t=chunks?(int)Math.floor(bounds.powerTerrain$minZ()):Math.max((int)Math.floor(bounds.powerTerrain$minZ()),cz-radius),
        r=chunks?(int)Math.ceil(bounds.powerTerrain$maxX()):Math.min((int)Math.ceil(bounds.powerTerrain$maxX()),cx+radius+1),
        b=chunks?(int)Math.ceil(bounds.powerTerrain$maxZ()):Math.min((int)Math.ceil(bounds.powerTerrain$maxZ()),cz+radius+1);
    if(world!=mc.world||borders!=chunks||spawn!=light||left!=l||top!=t||right!=r||bottom!=b) {
      clear();world=mc.world;borders=chunks;spawn=light;left=l;top=t;right=r;bottom=b;
    }
    int width=right-left,total=width*(bottom-top);
    if(width<=0||total<=0)return;
    long deadline=System.nanoTime()+650_000;
    int visited=0;
    while(cursor<total && checksLastTick<256 && visited++<8192 && System.nanoTime()<deadline) {
      int x=left+cursor%width,z=top+cursor/width;cursor++;
      boolean edge=chunks&&((x&15)==0||(z&15)==0);
      int dx=x-cx,dz=z-cz;
      boolean checkLight=light&&dx*dx+dz*dz<=radius*radius;
      if(!checkLight&&!edge)continue;
      checksLastTick++;
      var chunk=MapSampler.loaded(mc.world,x>>4,z>>4);if(chunk==null)continue;
      int y=127;
      Block surface=null;net.minecraft.util.math.Box box=null;
      while(y>=0) {
        int id=chunk.method_859(x&15,y,z&15);
        surface=id==0?null:Block.BLOCKS[id];
        if(surface!=null) {
          box=surface.method_1624(mc.world,x,y,z);
          // Flowers/grass do not replace their support. Leaves, water and lava remain surfaces.
          if(box!=null||surface.field_1900==Material.LEAVES||surface.field_1900==Material.WATER
              ||surface.field_1900==Material.LAVA||id==78)break;
        }
        y--;
      }
      if(y<0)continue;
      boolean snow=surface.id==78;
      int supportY=snow&&box==null?y-1:y;
      int level=checkLight&&supportY>=0?LightOverlay.sample(mc.world,SAMPLE,x,supportY,z):-1;
      boolean dark=level>=0&&level<8;
      double surfaceY=snow?y+(1+(chunk.method_875(x&15,y,z&15)&7))/8d:box!=null?box.maxY:y+1;
      if(edge||dark)pending.add(new Cell(x,surfaceY,z,edge,dark));
    }
    if(++ticks%8==0&&!completed||cursor==total)cells=List.copyOf(pending);
    if(cursor==total){completed=true;cursor=0;pending.clear();}
  }
  public void render(Minecraft mc,float delta) {
    if(world!=mc.world||cells.isEmpty()||!borders&&!spawn)return;
    var camera=mc.field_2807==null?mc.player:mc.field_2807;
    double cx=camera.field_1637+(camera.x-camera.field_1637)*delta,
        cy=camera.field_1638+(camera.y-camera.field_1638)*delta,
        cz=camera.field_1639+(camera.z-camera.field_1639)*delta;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
    try {
      GL11.glTranslated(-cx,-cy,-cz);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false);
      if(spawn){GL11.glColor4f(1,.12f,.08f,.38f);GL11.glBegin(GL11.GL_QUADS);
        for(Cell c:cells)if(c.spawn){double y=c.y+.015;GL11.glVertex3d(c.x,y,c.z);GL11.glVertex3d(c.x+1,y,c.z);GL11.glVertex3d(c.x+1,y,c.z+1);GL11.glVertex3d(c.x,y,c.z+1);}GL11.glEnd();}
      if(borders){GL11.glColor4f(1,1,.1f,.9f);GL11.glLineWidth(1);GL11.glBegin(GL11.GL_LINES);
        for(Cell c:cells)if(c.border){double y=c.y+.025;
          if((c.x&15)==0){GL11.glVertex3d(c.x,y,c.z);GL11.glVertex3d(c.x,y,c.z+1);}
          if((c.z&15)==0){GL11.glVertex3d(c.x,y,c.z);GL11.glVertex3d(c.x+1,y,c.z);}}
        GL11.glEnd();}
    }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
  }
}
