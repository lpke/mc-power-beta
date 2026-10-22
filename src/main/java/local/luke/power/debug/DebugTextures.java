package local.luke.power.debug;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.modificationstation.stationapi.mixin.render.client.TextureManagerAccessor;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class DebugTextures {
  private DebugTextures() {}
  static void dump(Minecraft mc) throws Exception {
    Path output=Path.of("debug/textures",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")));
    Files.createDirectories(output);
    int old=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),count=0;
    try {
      var seen=new HashSet<Integer>();
      for(var entry:((TextureManagerAccessor)mc.textureManager).getTextures().entrySet()) {
        int texture=entry.getValue();if(!seen.add(texture))continue;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
        int w=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),h=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
        if(w<1 || h<1 || w>4096 || h>4096)continue;
        ByteBuffer pixels=BufferUtils.createByteBuffer(w*h*4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
          int i=(y*w+x)*4,r=pixels.get(i)&255,g=pixels.get(i+1)&255,b=pixels.get(i+2)&255,a=pixels.get(i+3)&255;
          image.setRGB(x,y,a<<24|r<<16|g<<8|b);
        }
        String name=entry.getKey().replaceAll("[^a-zA-Z0-9._-]","_");
        if(name.length()>120)name=name.substring(name.length()-120);
        ImageIO.write(image,"png",output.resolve(String.format("%04d-%s.png",++count,name)).toFile());
      }
    } finally { GL11.glBindTexture(GL11.GL_TEXTURE_2D,old); }
    DebugActions.message(mc,"Saved "+count+" textures to "+output);
  }
}
