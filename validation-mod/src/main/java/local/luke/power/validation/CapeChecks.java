package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.visual.*;
import local.luke.power.validation.mixin.CapeRenderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.class_549;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

public final class CapeChecks {
  public static boolean recording;
  public static final List<Float> angles=new ArrayList<>();
  static void run(Minecraft mc) throws Exception {
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null,"load a disposable singleplayer world");
    check(mc.options.soundVolume==0&&mc.options.musicVolume==0,"mute the disposable client");
    check(mc.player.field_1595==null,"dismount before checking capes");
    failures=0;var before=VisualConfig.copy();var player=mc.player;
    var building=local.luke.power.building.config.Config.current().copy();
    String cape=player.playerCapeUrl;
    double prevX=player.prevX,prevY=player.prevY,prevZ=player.prevZ;
    var fields=List.of("field_530","field_531","field_532","field_533","field_534","field_535","field_1012","field_1013","field_524","field_525","field_1634","field_1635");
    Map<String,Object> saved=new HashMap<>();for(String name:fields)saved.put(name,PlayerEntity.class.getField(name).get(player));
    try {
      check(player.skinUrl!=null&&mc.textureManager.method_1093(player.skinUrl,null)>=0,"load a skin first");
      player.playerCapeUrl=player.skinUrl;
      player.prevX=player.x;player.prevY=player.y;player.prevZ=player.z;
      player.field_524=player.field_525=0;
      var renderer=new PlayerEntityRenderer();renderer.setDispatcher(EntityRenderDispatcher.field_2489);
      var model=((CapeRenderAccess)renderer).power$capeModel();
      model.setAngles(0,0,0,0,0,.0625f);
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
      test("backward walking and sprinting keep animated legs and armor ahead of the native cape plane",()->{
        int nativeIntersections=0;
        player.field_1012=player.field_1013=0;
        player.field_530=player.field_533=player.x;
        player.field_532=player.field_535=player.z+.2;
        for(float amplitude:new float[]{.35f,1})for(int phase=0;phase<64;phase++){
          float stride=phase*(float)Math.PI/32;
          model.setAngles(stride,amplitude,0,0,0,.0625f);
          player.field_531=player.field_534=player.y-.6;
          player.field_524=player.field_525=.2f;
          player.field_1634=player.field_1635=stride/6;
          var nativeAngles=render(renderer,player,false,.5f);
          if(!clears(nativeAngles.get(0),model.rightLeg)||!clears(nativeAngles.get(0),model.leftLeg))nativeIntersections++;
          var limited=render(renderer,player,true,.5f);
          check(limited.get(0)>=0&&limited.get(0)<=90,"cape turned through the torso: "+limited);
          check(clears(limited.get(0),model.rightLeg)&&clears(limited.get(0),model.leftLeg),
              "backward stride intersects the cape amplitude="+amplitude+" phase="+phase+" angles="+limited);
          check(limited.get(1)==0&&limited.get(2)==0,"straight backward fixture acquired lateral motion");
        }
        check(nativeIntersections>0,"the native renderer never reproduced backward leg clipping");
        log("CAPE backward native intersections="+nativeIntersections+" corrected samples=128");
      });
      test("moving minecart capes stay square to the rider with faster minecarts on and off",()->{
        int x=((int)Math.floor(player.x)>>4)*16+8,z=((int)Math.floor(player.z)>>4)*16+8,y=112;
        check(mc.world.method_155(x-7,y-1,z-1,x+7,y+3,z+1),"cape cart fixture needs loaded chunk-center terrain");
        var blocks=new ArrayList<int[]>();
        for(int bx=x-6;bx<=x+6;bx++)for(int by=y-1;by<=y+3;by++)for(int bz=z-1;bz<=z+1; bz++){
          check(mc.world.method_1777(bx,by,bz)==null,"cape cart fixture contains a block entity");
          blocks.add(new int[]{bx,by,bz,mc.world.getBlockId(bx,by,bz),mc.world.method_1778(bx,by,bz)});
        }
        double ordinaryDistance=0;
        try{
          for(var b:blocks)mc.world.method_154(b[0],b[1],b[2],b[1]==y-1?1:0,0);
          for(int bx=x-6;bx<=x+6;bx++)mc.world.method_154(bx,y,z,66,1);
          for(boolean fast:new boolean[]{false,true}){
            var settings=building.copy();settings.fastMinecarts=fast;local.luke.power.building.config.Config.preview(settings);
            var cart=new class_549(mc.world,x-4.5,y+.5,z+.5,0);
            player.field_1595=cart;cart.field_1594=player;cart.velocityX=.35;
            double start=cart.x;
            try{
              model.riding=true;model.setAngles(0,0,0,0,0,.0625f);
              player.field_524=player.field_525=0;
              for(int tick=0;tick<6;tick++){
                double previous=cart.x;cart.tick();double travel=cart.x-previous;
                check(travel>0,"native fixture cart stopped fast="+fast);
                for(float yaw:new float[]{0,90,180,270})for(float delta:new float[]{0,.25f,.5f,.75f,1}){
                  player.field_1012=player.field_1013=yaw;
                  player.field_530=player.x-travel;player.field_533=player.x-travel*.8;
                  player.field_531=player.field_534=player.y+.05;
                  player.field_532=player.field_535=player.z;
                  for(boolean limited:new boolean[]{false,true}){
                    double px=player.x,py=player.y,pz=player.z,cx=cart.x,cv=cart.velocityX;
                    var result=render(renderer,player,limited,delta);
                    check(result.get(1)==0&&result.get(2)==0,"minecart tilted the cape fast="+fast+" yaw="+yaw+" partial="+delta+" "+result);
                    check(player.x==px&&player.y==py&&player.z==pz&&cart.x==cx&&cart.velocityX==cv,"cape render changed native travel");
                  }
                }
              }
              double distance=cart.x-start;
              if(!fast)ordinaryDistance=distance;
              else check(distance>ordinaryDistance*1.5,"fixture did not exercise faster minecart physics");
              log("CAPE minecart fast="+fast+" native distance="+distance);
            }finally{player.field_1595=null;cart.field_1594=null;cart.markDead();model.riding=false;}
          }
          player.field_1012=player.field_1013=0;model.setAngles(0,0,0,0,0,.0625f);
          player.field_530=player.field_533=player.x-.2;
          var dismounted=render(renderer,player,true,.5f);
          check(dismounted.get(1)!=0&&dismounted.get(2)!=0,"dismount retained the rider's lateral cape override");
        }finally{
          player.field_1595=null;model.riding=false;
          Collections.reverse(blocks);for(var b:blocks)mc.world.method_154(b[0],b[1],b[2],b[3],b[4]);
          local.luke.power.building.config.Config.preview(building);
        }
      });
    } finally {
      recording=false;player.playerCapeUrl=cape;
      player.prevX=prevX;player.prevY=prevY;player.prevZ=prevZ;
      for(var entry:saved.entrySet())PlayerEntity.class.getField(entry.getKey()).set(player,entry.getValue());
      VisualConfig.preview(before);
      local.luke.power.building.config.Config.preview(building);
    }
    log("CAPE FAILURES "+failures);
  }

  private static List<Float> render(PlayerEntityRenderer renderer,PlayerEntity player,boolean limited,float delta){
    var settings=VisualConfig.copy();settings.limitCapeRotation=limited;VisualConfig.preview(settings);
    angles.clear();recording=true;GL11.glPushMatrix();
    try{((CapeRenderAccess)renderer).power$renderCape(player,delta);}
    finally{GL11.glPopMatrix();recording=false;}
    check(angles.size()==4,"native cape did not render: "+angles);
    check(GL11.glGetError()==0,"native cape GL error");
    return List.copyOf(angles);
  }

  private static boolean clears(float angle,ModelPart leg){
    double cape=Math.toRadians(angle),sin=Math.sin(leg.pitch),cos=Math.cos(leg.pitch);
    // Check the native leg cuboid's armor-expanded corners against the observed GL cape plane.
    for(double y:new double[]{-1,13})for(double z:new double[]{-3,3}){
      double py=leg.pivotY+y*cos-z*sin,pz=leg.pivotZ+y*sin+z*cos;
      if(py*Math.sin(cape)-(pz-2)*Math.cos(cape)<-1e-4)return false;
    }
    return true;
  }
}
