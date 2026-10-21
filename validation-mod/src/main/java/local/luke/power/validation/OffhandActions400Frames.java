package local.luke.power.validation;

import static local.luke.power.validation.Validation.check;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Reads ordinary frames promptly; PNG conversion waits until the transition has finished. */
final class OffhandActions400Frames {
  record Frame(int width,int height,byte[] rgb) {}
  private ByteBuffer pixels;

  void prepare(Minecraft mc) {
    int w=mc.displayWidth,h=mc.displayHeight;
    check(w>0&&h>0&&(long)w*h<=2_097_152,"resize offhand captures to at most two megapixels before starting");
    if(pixels==null||pixels.capacity()<w*h*3)pixels=BufferUtils.createByteBuffer(w*h*3);
  }

  Frame read(Minecraft mc) {
    prepare(mc);int w=mc.displayWidth,h=mc.displayHeight;
    if(GLContext.getCapabilities().GL_EXT_framebuffer_object)
      check(GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT)==0,"native window framebuffer is not bound");
    int size=w*h*3;
    pixels.clear();pixels.limit(size);
    int read=GL11.glGetInteger(GL11.GL_READ_BUFFER),pack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    try{
      GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);GL11.glReadBuffer(GL11.GL_BACK);
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,0);
      GL11.glReadPixels(0,0,w,h,GL11.GL_RGB,GL11.GL_UNSIGNED_BYTE,pixels);
    }finally{GL11.glReadBuffer(read);GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pack);GL11.glPopClientAttrib();}
    byte[] rgb=new byte[size];pixels.position(0);pixels.get(rgb);return new Frame(w,h,rgb);
  }

  static void write(Frame frame,Path file)throws Exception {
    BufferedImage image=new BufferedImage(frame.width,frame.height,BufferedImage.TYPE_INT_RGB);
    int[] dest=((DataBufferInt)image.getRaster().getDataBuffer()).getData();byte[] src=frame.rgb;
    for(int y=0;y<frame.height;y++)for(int x=0;x<frame.width;x++){
      int p=(y*frame.width+x)*3;
      dest[(frame.height-y-1)*frame.width+x]=(src[p]&255)<<16|(src[p+1]&255)<<8|src[p+2]&255;
    }
    check(ImageIO.write(image,"png",file.toFile()),"PNG writer unavailable");
  }
}
