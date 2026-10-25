package local.luke.power.map;

/** Bounded voxel traversal for a read-only photo pointer ray. Coordinates are physical blocks. */
public final class PhotoRay {
  public record Hit(int x, int y, int z) {}
  @FunctionalInterface public interface Terrain { int block(int x, int y, int z); }
  private PhotoRay() {}
  /** A negative terrain result stops at unloaded terrain; no ray may pass through it. */
  public static Hit trace(double x, double y, double z, double X, double Y, double Z,
      int limit, Terrain terrain) {
    if (!Double.isFinite(x+y+z+X+Y+Z)) return null;
    double dx=X-x,dy=Y-y,dz=Z-z;
    int bx=(int)Math.floor(x),by=(int)Math.floor(y),bz=(int)Math.floor(z);
    int sx=Double.compare(dx,0),sy=Double.compare(dy,0),sz=Double.compare(dz,0);
    double tx=boundary(x,bx,dx),ty=boundary(y,by,dy),tz=boundary(z,bz,dz);
    double stepx=dx==0?Double.POSITIVE_INFINITY:Math.abs(1/dx),
        stepy=dy==0?Double.POSITIVE_INFINITY:Math.abs(1/dy),stepz=dz==0?Double.POSITIVE_INFINITY:Math.abs(1/dz);
    for(int step=0;step<Math.max(0,Math.min(8192,limit));step++) {
      int block=terrain.block(bx,by,bz);
      if(block<0)return null;
      if(block>0)return new Hit(bx,by,bz);
      double next=Math.min(tx,Math.min(ty,tz));
      if(next>1 || !Double.isFinite(next))return null;
      if(tx==next){bx+=sx;tx+=stepx;}
      if(ty==next){by+=sy;ty+=stepy;}
      if(tz==next){bz+=sz;tz+=stepz;}
    }
    return null;
  }
  private static double boundary(double at,int cell,double direction) {
    return direction==0?Double.POSITIVE_INFINITY:((direction>0?cell+1:cell)-at)/direction;
  }
}
