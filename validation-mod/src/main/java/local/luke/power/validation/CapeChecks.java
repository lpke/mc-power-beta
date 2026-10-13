package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.visual.*;
import local.luke.power.validation.mixin.CapeRenderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

public final class CapeChecks {
  public static boolean recording;
  public static final List<Float> angles=new ArrayList<>();
  static void run(Minecraft mc) throws Exception {
    failures=0;var before=VisualConfig.copy();var player=mc.player;
    String cape=player.playerCapeUrl;
    double prevX=player.prevX,prevY=player.prevY,prevZ=player.prevZ;
    var fields=List.of("field_530","field_531","field_532","field_533","field_534","field_535","field_1012","field_1013","field_524","field_525");
    Map<String,Object> saved=new HashMap<>();for(String name:fields)saved.put(name,PlayerEntity.class.getField(name).get(player));
    try {
      check(player.skinUrl!=null&&mc.textureManager.method_1093(player.skinUrl,null)>=0,"load a skin first");
      player.playerCapeUrl=player.skinUrl;
      player.prevX=player.x;player.prevY=player.y;player.prevZ=player.z;
      player.field_524=player.field_525=0;
      var renderer=new PlayerEntityRenderer();renderer.setDispatcher(EntityRenderDispatcher.field_2489);
      for(float yaw:new float[]{0,90,180,270}) for(double lag:new double[]{0,.1,1,10,100}) {
        final float direction=yaw;final double speed=lag;
        test("cape renderer yaw="+yaw+" displacement="+lag,()->{
          player.field_1012=player.field_1013=direction;
          player.field_530=player.field_533=player.x+speed;
          player.field_531=player.field_534=player.y+speed;
          player.field_532=player.field_535=player.z-speed;
          double x=player.x,y=player.y,z=player.z;List<Float> nativeAngles=null;
          for(boolean enabled:new boolean[]{false,true}) {
            var settings=VisualConfig.copy();settings.limitCapeRotation=enabled;VisualConfig.preview(settings);
            angles.clear();recording=true;GL11.glPushMatrix();
            try {((CapeRenderAccess)renderer).power$renderCape(player,.5f);}
            finally {GL11.glPopMatrix();recording=false;}
            check(angles.size()==4,"cape did not render: "+angles);
            if(!enabled) nativeAngles=List.copyOf(angles);
            else {
              check(angles.get(0)<=90&&Math.abs(angles.get(1))<=30&&Math.abs(angles.get(2))<=30,"cape exceeds limits: "+angles);
              check(angles.get(3)==180,"base cape orientation changed");
              if(speed<=.1)check(angles.equals(nativeAngles),"ordinary cape motion changed");
            }
            check(player.x==x&&player.y==y&&player.z==z&&player.field_533==x+speed,"render changed simulation");
            check(GL11.glGetError()==0,"cape GL error");
          }
        });
      }
    } finally {
      recording=false;player.playerCapeUrl=cape;
      player.prevX=prevX;player.prevY=prevY;player.prevZ=prevZ;
      for(var entry:saved.entrySet())PlayerEntity.class.getField(entry.getKey()).set(player,entry.getValue());
      VisualConfig.preview(before);
    }
    log("CAPE FAILURES "+failures);
  }
}
