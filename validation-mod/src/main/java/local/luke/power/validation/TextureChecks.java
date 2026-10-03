package local.luke.power.validation;

import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.visual.*;
import net.minecraft.class_285;
import net.minecraft.client.Minecraft;
import static local.luke.power.validation.Validation.*;

final class TextureChecks {
  static BufferedImage read(class_285 pack, String path) throws Exception {
    try (var in=pack.method_976(path)) { return ImageIO.read(in); }
  }
  static void run(Minecraft mc) throws Exception {
    var originalPack=mc.field_2768.field_1175;
    ConfigSession session=SettingsRegistry.open(mc);
    List<String> ids=List.of("visual.softRain","visual.softSnow","visual.oldCobble","visual.oldBricks");
    Map<String,com.google.gson.JsonElement> original=new HashMap<>();
    for(String id:ids) original.put(id,find(session,id).value.deepCopy());
    BufferedImage old;
    try(var in=TextureChecks.class.getResourceAsStream("/assets/powerbeta/textures/old-terrain.png")){old=ImageIO.read(in);}
    try {
      for(Object o:mc.field_2768.method_1000()) {
        class_285 pack=(class_285)o;
        if(!Set.of("Default","Alpha.zip","1.14 Textures.zip","faithful32pack.zip").contains(pack.field_1137))continue;
        for(String id:ids)find(session,id).value=new JsonPrimitive(false);
        session.preview(); mc.field_2768.method_999(pack); mc.textureManager.method_1096();
        BufferedImage base=read(pack,"/terrain.png");
        int tile=base.getWidth()/16;
        for(int bits=0;bits<16;bits++) {
          final int flags=bits;
          test("texture combination "+pack.field_1137+" "+bits,()->{
            for(int i=0;i<4;i++)find(session,ids.get(i)).value=new JsonPrimitive((flags&(1<<i))!=0);
            session.preview();
            BufferedImage result=read(pack,"/terrain.png");
            check(result.getWidth()==base.getWidth()&&result.getHeight()==base.getHeight(),"resolution changed");
            for(int y=0;y<base.getHeight();y++)for(int x=0;x<base.getWidth();x++) {
              boolean cobble=(flags&4)!=0 && x/tile==0 && y/tile==1;
              boolean brick=(flags&8)!=0 && x/tile==7 && y/tile==0;
              int expected=cobble||brick ? old.getRGB(x*old.getWidth()/base.getWidth(),y*old.getHeight()/base.getHeight()):base.getRGB(x,y);
              check(result.getRGB(x,y)==expected,"wrong pixel "+x+","+y);
            }
            for(int i=0;i<2;i++)if((flags&(1<<i))!=0) {
              String name=i==0?"rain":"snow";
              BufferedImage actual=read(pack,"/environment/"+name+".png");
              try(var in=TextureChecks.class.getResourceAsStream("/assets/powerbeta/textures/"+name+".png")) {
                BufferedImage expected=ImageIO.read(in);
                check(actual.getWidth()==expected.getWidth(),"weather size");
                check(Arrays.equals(actual.getRGB(0,0,actual.getWidth(),actual.getHeight(),null,0,actual.getWidth()),expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth())),"weather pixels");
              }
            }
          });
        }
      }
    } finally {
      for(String id:ids)find(session,id).value=original.get(id);
      session.preview(); mc.field_2768.method_999(originalPack); mc.textureManager.method_1096();
    }
    log("TEXTURE CHECKS COMPLETE");
  }
}
