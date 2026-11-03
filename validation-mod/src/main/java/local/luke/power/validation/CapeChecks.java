package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.visual.*;
import local.luke.power.validation.mixin.CapeRenderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.class_549;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public final class CapeChecks {
  private static final String CAPE="power-beta-validation:cape-checks";
  public static boolean recording;
  public static final List<Float> angles=new ArrayList<>();

  private static final class FixtureRenderer extends PlayerEntityRenderer {
    @Override protected boolean method_2027(String url,String fallback){
      method_2026("/mob/char.png");
      return true;
    }
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null,"load a disposable singleplayer world");
    check(mc.options.soundVolume==0&&mc.options.musicVolume==0,"mute the disposable client");
    check(mc.player.field_1595==null,"dismount before checking capes");
    failures=0;var before=VisualConfig.copy();var player=mc.player;
    var building=local.luke.power.building.config.Config.current().copy();
    String cape=player.playerCapeUrl;
    double prevX=player.prevX,prevY=player.prevY,prevZ=player.prevZ;
    var fields=List.of("field_530","field_531","field_532","field_533","field_534","field_535","field_1012","field_1013","field_524","field_525","field_1634","field_1635","field_1048","field_1049");
    Map<String,Object> saved=new HashMap<>();for(String name:fields)saved.put(name,PlayerEntity.class.getField(name).get(player));
    try {
      player.playerCapeUrl=CAPE;
      player.prevX=player.x;player.prevY=player.y;player.prevZ=player.z;
      player.field_524=player.field_525=0;
      player.field_1048=player.field_1049=0;
      var renderer=new FixtureRenderer();renderer.setDispatcher(EntityRenderDispatcher.field_2489);
      var model=((CapeRenderAccess)(Object)renderer).power$capeModel();
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
            try {((CapeRenderAccess)(Object)renderer).power$renderCape(player,.5f);}
            finally {GL11.glPopMatrix();recording=false;}
            check(angles.size()==4,"cape did not render: "+angles);
            if(!enabled) nativeAngles=List.copyOf(angles);
            else {
              check(angles.get(0)<=90&&Math.abs(angles.get(1))<=30&&Math.abs(angles.get(2))<=30,"cape exceeds limits: "+angles);
              check(angles.get(3)==180,"base cape orientation changed");
              check(angles.get(1)==0&&angles.get(2)==0,"enabled cape pulled sideways from the back");
              if(speed<=.1)check(angles.get(0).equals(nativeAngles.get(0)),"ordinary trailing pitch changed");
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
        for(float amplitude:new float[]{.35f,1}){
          player.field_1048=player.field_1049=amplitude;
          float minimum=90,maximum=0;
          for(int phase=0;phase<64;phase++){
          float stride=phase*(float)Math.PI/32;
          model.setAngles(stride,amplitude,0,0,0,.0625f);
          player.field_531=player.field_534=player.y-.6;
          player.field_524=player.field_525=.2f;
          player.field_1634=player.field_1635=stride/6;
          var nativeAngles=render(renderer,player,false,.5f);
          if(!clears(nativeAngles.get(0),model.rightLeg)||!clears(nativeAngles.get(0),model.leftLeg))nativeIntersections++;
          var limited=render(renderer,player,true,.5f);
          minimum=Math.min(minimum,limited.get(0));maximum=Math.max(maximum,limited.get(0));
          check(limited.get(0)>=0&&limited.get(0)<=90,"cape turned through the torso: "+limited);
          check(clears(limited.get(0),model.rightLeg)&&clears(limited.get(0),model.leftLeg),
              "backward stride intersects the cape amplitude="+amplitude+" phase="+phase+" angles="+limited);
          check(limited.get(1)==0&&limited.get(2)==0,"straight backward fixture acquired lateral motion");
          }
          check(maximum-minimum<.001,"backward stride pumped the cape amplitude="+amplitude+" range="+minimum+".."+maximum);
          check(maximum<(amplitude<1?13:46),"cape reserved clearance for legs below its hem");
        }
        check(nativeIntersections>0,"the native renderer never reproduced backward leg clipping");
        log("CAPE backward native intersections="+nativeIntersections+" corrected samples=128");
      });
      test("native backward and sideways walking stay aligned through starts, stops and partial ticks",()->nativeStrides(mc));
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
          player.field_1012=player.field_1013=0;player.field_1048=player.field_1049=0;model.setAngles(0,0,0,0,0,.0625f);
          player.field_530=player.field_533=player.x-.2;
          var dismounted=render(renderer,player,true,.5f);
          var nativeDismounted=render(renderer,player,false,.5f);
          check(nativeDismounted.get(1)!=0&&nativeDismounted.get(2)!=0,"dismount retained the rider's lateral cape override");
          check(dismounted.get(1)==0&&dismounted.get(2)==0,"enabled dismounted cape lost its back alignment");
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
    try{((CapeRenderAccess)(Object)renderer).power$renderCape(player,delta);}
    finally{GL11.glPopMatrix();recording=false;}
    check(angles.size()==4,"native cape did not render: "+angles);
    check(GL11.glGetError()==0,"native cape GL error");
    return List.copyOf(angles);
  }

  private static boolean clears(float angle,ModelPart leg){
    // Intersect the actual cape segment with an independently inverse-transformed
    // armor box. Legs below the hem cannot push the cape away from the back.
    double cape=Math.toRadians(angle),sin=Math.sin(leg.pitch),cos=Math.cos(leg.pitch);
    double y=-leg.pivotY*cos+(2-leg.pivotZ)*sin,z=leg.pivotY*sin+(2-leg.pivotZ)*cos;
    double dy=Math.cos(cape)*cos+Math.sin(cape)*sin;
    double dz=-Math.cos(cape)*sin+Math.sin(cape)*cos;
    double enter=0,exit=16;
    for(double[] slab:new double[][]{{y,dy,-.9999,12.9999},{z,dz,-2.9999,2.9999}}){
      if(Math.abs(slab[1])<1e-10){if(slab[0]<=slab[2]||slab[0]>=slab[3])return true;}
      else{
        double a=(slab[2]-slab[0])/slab[1],b=(slab[3]-slab[0])/slab[1];
        enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));
      }
    }
    return enter>=exit;
  }

  private static void nativeStrides(Minecraft mc) throws Exception{
    int x=((int)Math.floor(mc.player.x)>>4)*16+8,z=((int)Math.floor(mc.player.z)>>4)*16+8,y=112;
    check(mc.world.method_155(x-7,y-1,z-7,x+7,y+3,z+7),"native cape gait fixture needs a loaded chunk");
    var blocks=new ArrayList<int[]>();
    for(int bx=x-7;bx<=x+7;bx++)for(int by=y-1;by<=y+3;by++)for(int bz=z-7;bz<=z+7;bz++){
      check(mc.world.method_1777(bx,by,bz)==null,"native cape gait fixture contains a block entity");
      blocks.add(new int[]{bx,by,bz,mc.world.getBlockId(bx,by,bz),mc.world.method_1778(bx,by,bz)});
    }
    var renderer=new FixtureRenderer();renderer.setDispatcher(EntityRenderDispatcher.field_2489);
    var model=((CapeRenderAccess)(Object)renderer).power$capeModel();
    try{
      for(var b:blocks)mc.world.method_154(b[0],b[1],b[2],b[1]==y-1?1:0,0);
      for(String direction:List.of("backward","left","right")){
        var actor=new ClientPlayerEntity(mc,mc.world,mc.session,mc.player.dimensionId);
        actor.field_161=new net.minecraft.class_41();actor.skinUrl=null;actor.playerCapeUrl=CAPE;
        actor.method_1341(x+.5,y,z+.5,0,0);
        actor.prevX=actor.x;actor.prevY=actor.y;actor.prevZ=actor.z;
        actor.field_530=actor.field_533=actor.x;actor.field_531=actor.field_534=actor.y;actor.field_532=actor.field_535=actor.z;
        actor.field_1623=true;actor.velocityX=actor.velocityY=actor.velocityZ=0;
        float previous=Float.NaN,largestFrameStep=0,maximum=0;double distance=0;
        int nativeTwists=0,samples=0;float startPhase=actor.field_1050;
        for(int tick=0;tick<64;tick++){
          boolean moving=tick<24;
          actor.field_161.field_2533=moving&&direction.equals("backward")?-1:0;
          actor.field_161.field_2532=moving&&!direction.equals("backward")?(direction.equals("left")?1:-1):0;
          actor.tick();
          if(tick==23)distance=Math.hypot(actor.x-x-.5,actor.z-z-.5);
          for(float delta:new float[]{0,.125f,.25f,.375f,.5f,.625f,.75f,.875f,1}){
            double px=actor.x,py=actor.y,pz=actor.z,vx=actor.velocityX,vy=actor.velocityY,vz=actor.velocityZ;
            float phase=actor.field_1050,amplitude=actor.field_1049;
            var nativeAngles=renderBody(renderer,actor,false,delta);
            var expected=nativeAngles(actor,delta);
            for(int i=0;i<4;i++)check(Math.abs(nativeAngles.get(i)-expected[i])<.001,
                "disabled cape changed native transform direction="+direction+" index="+i+" "+nativeAngles);
            if(Math.abs(nativeAngles.get(1))>.01)nativeTwists++;
            var enabled=renderBody(renderer,actor,true,delta);
            check(enabled.get(1)==0&&enabled.get(2)==0,"native strafe pulled cape sideways direction="+direction);
            check(clears(enabled.get(0),model.rightLeg)&&clears(enabled.get(0),model.leftLeg),
                "native gait intersected cape direction="+direction+" tick="+tick+" partial="+delta+" angles="+enabled);
            check(actor.x==px&&actor.y==py&&actor.z==pz&&actor.velocityX==vx&&actor.velocityY==vy&&actor.velocityZ==vz
                &&actor.field_1050==phase&&actor.field_1049==amplitude,"cape render changed native movement or animation");
            float angle=enabled.get(0);maximum=Math.max(maximum,angle);
            if(Float.isFinite(previous))largestFrameStep=Math.max(largestFrameStep,Math.abs(angle-previous));
            previous=angle;samples++;
          }
        }
        check(distance>2,"native "+direction+" fixture did not move: "+distance);
        check(actor.field_1050-startPhase>3,"native gait did not complete a stride");
        check(previous<8,"native cape did not settle near the back after stopping: "+previous);
        check(largestFrameStep<4,"native cape snapped during gait transition direction="+direction+" step="+largestFrameStep);
        if(!direction.equals("backward"))check(nativeTwists>0,"native strafe never reproduced lateral cape pulling");
        log("CAPE native direction="+direction+" distance="+distance+" samples="+samples+" maxPitch="+maximum
            +" largestFrameStep="+largestFrameStep+" idlePitch="+previous+" nativeTwists="+nativeTwists);
      }
    }finally{
      Collections.reverse(blocks);for(var b:blocks)mc.world.method_154(b[0],b[1],b[2],b[3],b[4]);
    }
  }

  private static List<Float> renderBody(PlayerEntityRenderer renderer,PlayerEntity player,boolean limited,float delta){
    var settings=VisualConfig.copy();settings.limitCapeRotation=limited;VisualConfig.preview(settings);
    angles.clear();recording=true;GL11.glPushMatrix();
    try{renderer.render(player,0,0,0,player.yaw,delta);}
    finally{GL11.glPopMatrix();recording=false;}
    check(angles.size()==4,"native walking body did not render its cape: "+angles);
    check(GL11.glGetError()==0,"native walking cape GL error");
    return List.copyOf(angles);
  }

  private static float[] nativeAngles(PlayerEntity player,float delta){
    double x=player.field_530+(player.field_533-player.field_530)*delta-(player.prevX+(player.x-player.prevX)*delta);
    double y=player.field_531+(player.field_534-player.field_531)*delta-(player.prevY+(player.y-player.prevY)*delta);
    double z=player.field_532+(player.field_535-player.field_532)*delta-(player.prevZ+(player.z-player.prevZ)*delta);
    float yaw=player.field_1013+(player.field_1012-player.field_1013)*delta;
    double sine=MathHelper.sin(yaw*(float)Math.PI/180),cosine=-MathHelper.cos(yaw*(float)Math.PI/180);
    float vertical=Math.max(-6,Math.min(32,(float)y*10));
    float trailing=Math.max(0,(float)(x*sine+z*cosine)*100),lateral=(float)(x*cosine-z*sine)*100;
    float walk=player.field_524+(player.field_525-player.field_524)*delta;
    vertical+=MathHelper.sin((player.field_1634+(player.field_1635-player.field_1634)*delta)*6)*32*walk;
    if(player.method_1373())vertical+=25;
    return new float[]{6+trailing/2+vertical,lateral/2,-lateral/2,180};
  }
}
