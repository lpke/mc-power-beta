package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import local.luke.power.map.MinimapGeometry;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Native pixel comparison against the reference atlas operations, independently of the cached mesh. */
final class Release300FrameChecks {
  private static final int SIZE = 1024;
  private Release300FrameChecks() {}
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    int oldFbo=GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    int oldMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    int texture=GL11.glGenTextures(), fbo=EXTFramebufferObject.glGenFramebuffersEXT();
    var pixels=BufferUtils.createByteBuffer(SIZE*SIZE*4);
    var expected=BufferUtils.createByteBuffer(SIZE*SIZE*4);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
      GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,SIZE,SIZE,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(ByteBuffer)null);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,fbo);
      EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,GL11.GL_TEXTURE_2D,texture,0);
      check(EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)==EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT,"frame comparison framebuffer incomplete");
      var border=Class.forName("local.luke.power.map.MinimapBorder").getDeclaredMethod("draw",Minecraft.class,MinimapGeometry.Layout.class,boolean.class);border.setAccessible(true);
      for(boolean circle:new boolean[]{false,true}) for(int diameter:new int[]{64,108,241,480}) for(int scale:new int[]{1,2}) {
        final int d=diameter,s=scale; final boolean c=circle;
        test("exact reference frame pixels shape="+(c?"circle":"square")+" diameter="+d+" scale="+s,()->{
          setup(s);border.invoke(null,mc,new MinimapGeometry.Layout(8,8,d),c);read(pixels);
          setup(s);GL11.glTranslated(8,8,0);reference(mc,d,c);read(expected);
          int different=0;
          for(int i=0;i<SIZE*SIZE*4;i++)if(pixels.get(i)!=expected.get(i))different++;
          check(different==0,"reference frame mismatch in "+different+" pixel channels");
        });
      }
      setup(2);border.invoke(null,mc,new MinimapGeometry.Layout(16,16,108),true);
      border.invoke(null,mc,new MinimapGeometry.Layout(152,16,108),false);read(pixels);
      var image=new BufferedImage(560,272,BufferedImage.TYPE_INT_ARGB);
      for(int y=0;y<272;y++)for(int x=0;x<560;x++){
        int i=((SIZE-1-y)*SIZE+x)*4;
        image.setRGB(x,y,0xff000000|(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|pixels.get(i+2)&255);
      }
      Path report=Path.of("power-beta-data/reports/release-300/exact-minimap-frames.png");Files.createDirectories(report.getParent());ImageIO.write(image,"PNG",report.toFile());
      check(GL11.glGetError()==GL11.GL_NO_ERROR,"frame comparison produced OpenGL errors");
    } finally {
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,oldFbo);
      GL11.glDeleteTextures(texture);EXTFramebufferObject.glDeleteFramebuffersEXT(fbo);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
      GL11.glPopClientAttrib();GL11.glPopAttrib();GL11.glMatrixMode(oldMode);
    }
    log("300 EXACT FRAMES complete failures="+failures);
  }
  private static void setup(int scale){
    GL11.glViewport(0,0,SIZE,SIZE);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_ALPHA_TEST);
    GL11.glColorMask(true,true,true,true);GL11.glClearColor(.15f,.28f,.2f,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,SIZE,SIZE,0,-1,1);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glScaled(scale,scale,1);
  }
  private static void read(ByteBuffer out){out.clear();GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,0);GL11.glReadPixels(0,0,SIZE,SIZE,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,out);}
  private static void reference(Minecraft mc,int d,boolean circle){
    GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glBindTexture(GL11.GL_TEXTURE_2D,mc.textureManager.getTextureId("/assets/powerbeta/map/minimap-frame.png"));
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
    GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColor4f(1,1,1,1);
    if(!circle){
      GL11.glBegin(GL11.GL_QUADS);rect(-4,-4,192,97,16,16);rect(d-12,-4,192,113,16,16);rect(-4,d-12,192,129,16,16);rect(d-12,d-12,192,145,16,16);
      int horizontal=d-24;
      for(int i=0;i<(int)Math.ceil(horizontal/226d);i++){int x=12+i*226,w=Math.min(226,d-12-x);rect(x,-4,0,0,w,16);rect(x,d-12,0,16,w,16);}
      for(int i=0;i<(int)Math.ceil(horizontal/113d);i++){int y=12+i*113,h=Math.min(113,d-12-y),u=32*(i&1);rect(-4,y,u,97,16,h);rect(d-12,y,u+16,97,16,h);}
      GL11.glEnd();
    }else{
      int sides=Math.max(32,(int)Math.ceil(Math.PI*(d+8)/32)*4);double start=-Math.PI/4-(double)((float)16/(d/2+4));int shade=(int)((Math.PI/4-start)/2/Math.PI*sides);
      ring(d,sides,start,0,shade,false,false,0,73,32);ring(d,sides,start,shade,shade+sides/4,true,false,138,68,20);ring(d,sides,start,shade+sides/4,shade+sides/2,true,true,138,68,20);ring(d,sides,start,shade+sides/2,sides,false,false,0,73,32);
    }
  }
  private static void rect(int x,int y,int u,int v,int w,int h){vertex(x,y+h,u,v+h);vertex(x+w,y+h,u+w,v+h);vertex(x+w,y,u+w,v);vertex(x,y,u,v);}
  private static void vertex(float x,float y,float u,float v){GL11.glTexCoord2f(u/256f,v/256f);GL11.glVertex2f(x,y);}
  private static void ring(int d,int sides,double start,int first,int last,boolean reset,boolean reverse,int textureX,float repeat,int seam){
    float half=d/2f,outer=half+4,step=(float)(Math.PI*2/sides*outer),px=0,py=0,ox=0,oy=0,pu=0;
    int from=reset?(reverse?last:first):0;float threshold=reverse?seam+step:seam;
    GL11.glBegin(GL11.GL_QUADS);
    for(int i=first;i<=last;i++){
      double angle=start+(double)i/sides*Math.PI*2,sin=Math.sin(angle),cos=Math.cos(angle);
      float x=half+(float)(half*sin),y=(float)(half*(1-cos)),a=half+(float)(outer*sin),b=(float)(half-outer*cos),offset=Math.abs(step*(i-from)),u=textureX;
      if(offset>=threshold){u=textureX+seam;if((offset-=threshold)>=repeat)offset%=repeat;}u+=offset;
      if(i>first){vertex(px,py,pu,214);vertex(x,y,u,214);vertex(a,b,u,210);vertex(ox,oy,pu,210);}
      px=x;py=y;ox=a;oy=b;pu=u;
    }GL11.glEnd();
  }
}
