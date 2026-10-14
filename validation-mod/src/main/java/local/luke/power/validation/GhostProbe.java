package local.luke.power.validation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
public final class GhostProbe {
  public static boolean capture;
  public static String details="";
  public static int samples;
  private static int query;
  private static java.nio.ByteBuffer before,after;
  private static int width,height;
  public static int changed;
  private static java.nio.ByteBuffer pixels(){var b=BufferUtils.createByteBuffer(width*height*4);GL11.glReadPixels(0,0,width,height,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);return b;}
  public static void save() throws Exception {
    var image=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_ARGB);
    for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=(y*width+x)*4;image.setRGB(x,height-1-y,0xff000000|((after.get(i)&255)<<16)|((after.get(i+1)&255)<<8)|(after.get(i+2)&255));}
    javax.imageio.ImageIO.write(image,"png",new java.io.File("ghost-pass.png"));
  }
  public static void before(){
    if(!capture)return;capture=false;
    var buffer=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,buffer);
    float[] matrix=new float[16];buffer.get(matrix);
    details="HEAD FRAME matrix="+java.util.Arrays.toString(matrix)+" cull="+GL11.glIsEnabled(GL11.GL_CULL_FACE)+" fog="+GL11.glIsEnabled(GL11.GL_FOG)+" mode="+GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    var viewport=BufferUtils.createIntBuffer(16);GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);width=viewport.get(2);height=viewport.get(3);before=pixels();
    var mask=BufferUtils.createByteBuffer(16);GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK,mask);details+=" mask="+mask.get(0)+mask.get(1)+mask.get(2)+mask.get(3);
    query=GL15.glGenQueries();GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED,query);
  }
  public static void after(){
    if(query==0)return;GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
    samples=GL15.glGetQueryObjectui(query,GL15.GL_QUERY_RESULT);GL15.glDeleteQueries(query);query=0;
    after=pixels();changed=0;for(int i=0;i<after.capacity();i+=4)if(after.getInt(i)!=before.getInt(i))changed++;
  }
}
