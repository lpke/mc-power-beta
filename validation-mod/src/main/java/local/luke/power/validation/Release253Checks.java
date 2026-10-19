package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.render.RiddenVehicle;
import local.luke.power.rules.*;
import local.luke.power.validation.mixin.CapeRenderAccess;
import local.luke.power.validation.mixin.Client253Access;
import local.luke.power.visual.VisualConfig;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Native event/held use paths, boat rendering and cape transforms in a disposable world. */
public final class Release253Checks {
  public static boolean captureBoats;
  public static final Set<Entity> renderedBoats = new HashSet<>();
  private static ByteBuffer events, buttons;
  private static final int X=16,Y=100,Z=16;
  private static List<Entity> vehicles(Minecraft mc) {
    var result=new ArrayList<Entity>();
    for(Object e:mc.world.method_291())if(e instanceof Entity v&&!v.dead&&(v instanceof class_113||v instanceof class_549))result.add(v);
    return result;
  }
  private static void retire(Minecraft mc){
    if(mc.player.field_1595!=null)mc.player.method_1376(null);
    for(Entity e:vehicles(mc)){e.markDead();e.method_1340(140,110,140);}
  }
  private static void movePlaced(Minecraft mc){for(Entity vehicle:vehicles(mc))vehicle.method_1340(X+8,Y+1,Z+8);}
  private static void tick(Minecraft mc)throws Exception{
    var access=(Client253Access)mc;access.power$ticks(access.power$ticks()+1);
    access.power$lastTick(System.currentTimeMillis());mc.field_2778=true;mc.paused=true;mc.tick();
  }
  private static void event(Minecraft mc,boolean pressed)throws Exception{
    events.clear();events.put((byte)1).put((byte)(pressed?1:0)).putInt(0).putInt(0).putInt(0).putLong(System.nanoTime()).flip();
    buttons.put(1,(byte)(pressed?1:0));tick(mc);
  }
  private static void prepare(Minecraft mc,int item)throws Exception{
    retire(mc);mc.player.inventory.selectedSlot=0;
    mc.player.inventory.main[0]=DeathChecks.tagged(item,8,0,"vehicle-use");
    mc.player.method_1341(X+.37,Y+1.25,Z+.61,0,60);
    mc.player.prevX=mc.player.x;mc.player.prevY=mc.player.y;mc.player.prevZ=mc.player.z;mc.player.prevYaw=0;mc.player.prevPitch=60;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
    if(item!=333)mc.player.pitch=mc.player.prevPitch=90;
    mc.field_2823=item==333?null:new class_27(X,Y,Z,1,Vec3d.createCached(X+.5,Y+1,Z+.5));
    if(item!=333)for(int x=X-1;x<=X+1;x++)for(int z=Z-1;z<=Z+1;z++)mc.world.method_154(x,Y,z,0,0);
    mc.world.method_154(X,Y,Z,item==333?9:66,1);
    check(mc.world.getBlockId(X,Y,Z)==(item==333?9:66),"fixture target did not remain placed");
  }
  private static void aim(Minecraft mc,double x,double y,double z){
    var eye=mc.player.method_931(1);double dx=x-eye.x,dy=y-eye.y,dz=z-eye.z;
    mc.player.yaw=mc.player.prevYaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
    mc.player.pitch=mc.player.prevPitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
  }
  static void run(Minecraft mc)throws Exception{
    failures=0;DeathChecks.alive(mc);mc.setScreen(null);FreecamPreviewChecks.active(false);mc.field_2807=mc.player;
    var saved=new net.minecraft.nbt.NbtCompound();mc.player.write(saved);var rules=AccessRules.copy(mc);
    var building=local.luke.power.building.config.Config.current().copy();var visual=VisualConfig.copy();
    var input=mc.player.field_161;boolean paused=mc.paused;int ticks=((Client253Access)mc).power$ticks();
    var eventField=Mouse.class.getDeclaredField("readBuffer");eventField.setAccessible(true);events=(ByteBuffer)eventField.get(null);
    var buttonField=Mouse.class.getDeclaredField("buttons");buttonField.setAccessible(true);buttons=(ByteBuffer)buttonField.get(null);byte oldButton=buttons.get(1);
    try{
      AccessRules.restore(mc,RuleSet.cheats());
      for(int x=X-12;x<=X+12;x++)for(int z=Z-12;z<=Z+12;z++){
        mc.world.method_154(x,Y-1,z,1,0);mc.world.method_154(x,Y,z,9,0);
        for(int y=Y+1;y<=Y+6;y++)mc.world.method_154(x,y,z,0,0);
      }
      for(boolean steering:new boolean[]{false,true})for(boolean fast:new boolean[]{false,true}){
        var b=building.copy();b.boatSteering=steering;b.boatSpeed=fast;local.luke.power.building.config.Config.preview(b);
        test("Creative boat: one per press, held use and quick consecutive clicks steering="+steering+" fast="+fast,()->{
          CreativeVehicleChecks.mode(mc,"CREATIVE");prepare(mc,333);String before=DeathItems.encode(mc.player.inventory.main[0]);
          event(mc,true);check(vehicles(mc).size()==1,"initial click did not place exactly one boat");
          for(int i=0;i<40;i++)tick(mc);check(vehicles(mc).size()==1,"held click spawned extra boats");
          for(int click=0;click<3;click++){movePlaced(mc);event(mc,false);event(mc,true);check(vehicles(mc).size()==click+2,"quick click lost or repeated");}
          event(mc,false);check(before.equals(DeathItems.encode(mc.player.inventory.main[0])),"Creative changed count or native data");
        });
      }
      for(int id:new int[]{328,342,343})test("Creative minecart item "+id+" uses each press once",()->{
        CreativeVehicleChecks.mode(mc,"CREATIVE");prepare(mc,id);String before=DeathItems.encode(mc.player.inventory.main[0]);
        event(mc,true);for(int i=0;i<40;i++)tick(mc);check(vehicles(mc).size()==1,"minecart count after held use: "+vehicles(mc).size()+" target="+mc.world.getBlockId(X,Y,Z));
        for(Entity vehicle:vehicles(mc))vehicle.method_1340(X+8,Y+1,Z+8);
        event(mc,false);event(mc,true);event(mc,false);check(vehicles(mc).size()==2,"second minecart press did not place once");
        check(before.equals(DeathItems.encode(mc.player.inventory.main[0])),"Creative changed minecart item");
      });
      test("Survival keeps native held use and consumption; ordinary Creative block use repeats",()->{
        CreativeVehicleChecks.mode(mc,"SURVIVAL");prepare(mc,333);event(mc,true);for(int i=0;i<8;i++){movePlaced(mc);tick(mc);}event(mc,false);
        check(vehicles(mc).size()>1&&mc.player.inventory.main[0].count==8-vehicles(mc).size(),"Survival repeat/consumption changed");
        CreativeVehicleChecks.mode(mc,"CREATIVE");prepare(mc,4);mc.world.method_154(X+3,Y,Z,1,0);mc.field_2823=new class_27(X+3,Y,Z,1,Vec3d.createCached(X+3.5,Y+1,Z+.5));
        aim(mc,X+3.5,Y+1,Z+.5);event(mc,true);aim(mc,X+3.5,Y+2,Z+.5);
        for(int i=0;i<8;i++)tick(mc);event(mc,false);
        check(mc.world.getBlockId(X+3,Y+1,Z)==4&&mc.world.getBlockId(X+3,Y+2,Z)==4,"ordinary held block placement stopped");
      });
      test("menus never resume held vehicle placement",()->{
        CreativeVehicleChecks.mode(mc,"CREATIVE");prepare(mc,333);event(mc,true);
        mc.setScreen(new net.minecraft.class_525());for(int i=0;i<6;i++)tick(mc);mc.setScreen(null);
        for(int i=0;i<6;i++)tick(mc);check(vehicles(mc).size()==1,"menu resumed held vehicle placement");
        movePlaced(mc);mc.field_2778=false;event(mc,false);event(mc,true);event(mc,false);
        check(vehicles(mc).size()==2,"new click after menu lost");
      });
      retire(mc);mc.paused=paused;
      for(int x=X-2;x<=X+4;x++)for(int z=Z-2;z<=Z+2;z++){mc.world.method_154(x,Y,z,9,0);for(int y=Y+1;y<=Y+3;y++)mc.world.method_154(x,y,z,0,0);}
      CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.field_161=new class_41();
      for(boolean steering:new boolean[]{false,true})for(boolean fast:new boolean[]{false,true}){
        var b=building.copy();b.boatSteering=steering;b.boatSpeed=fast;b.boatProtection=true;local.luke.power.building.config.Config.preview(b);
        test("ridden boat survives native frustum/occlusion rendering steering="+steering+" fast="+fast,()->{
          var boat=new class_113(mc.world,X+.5,Y+1,Z+.5);mc.world.method_210(boat);mc.player.method_1376(boat);boat.velocityZ=.15;
          boolean flag=boat.field_1622;double startX=boat.x,startZ=boat.z;
          for(int t=0;t<12;t++){
            boat.tick();mc.player.method_1384();boat.method_1382();mc.player.yaw=mc.player.prevYaw=180;
            boat.getClass().getMethod("setCulled",boolean.class).invoke(boat,true);
            boat.getClass().getMethod("setOutOfCamera",boolean.class).invoke(boat,true);
            var timeout=Entity.class.getDeclaredField("lastTime");timeout.setAccessible(true);timeout.setLong(boat,0);
            check(RiddenVehicle.visible(boat)&&!(boolean)boat.getClass().getMethod("isCulled").invoke(boat)
                &&!(boolean)boat.getClass().getMethod("isOutOfCamera").invoke(boat),"ridden boat was culled");
            for(float delta:new float[]{0,.25f,.5f,.75f,1}){render(mc,delta);check(renderedBoats.contains(boat),"boat flickered on rendered frame");}
          }
          check(Math.hypot(boat.x-startX,boat.z-startZ)>.05,"boat never travelled");check(boat.field_1622==flag,"saved frustum flag changed");
          var hidden=VisualConfig.copy();hidden.hideEntities=true;hidden.hideVehicles=true;VisualConfig.preview(hidden);render(mc,.5f);
          check(!renderedBoats.contains(boat),"hidden vehicles preference ignored");VisualConfig.preview(visual);
          var view=mc.field_2807;mc.field_2807=null;check(!RiddenVehicle.visible(boat),"other view inherited override");mc.field_2807=view;
          mc.player.method_1376(null);render(mc,.5f);check(!RiddenVehicle.visible(boat)&&!renderedBoats.contains(boat),"dismount kept culling override");
          boat.markDead();boat.method_1340(140,110,140);
        });
      }
      test("boat cape keeps lateral alignment, pitch response, dismount response and player state",()->cape(mc));
    }finally{
      CapeChecks.recording=false;captureBoats=false;events.clear().limit(0);buttons.put(1,oldButton);retire(mc);
      mc.paused=paused;((Client253Access)mc).power$ticks(ticks);local.luke.power.building.config.Config.preview(building);VisualConfig.preview(visual);
      mc.player.field_161=input;mc.player.read(saved);AccessRules.restore(mc,rules);mc.setScreen(null);
    }
    log("RELEASE 2.5.3 FAILURES "+failures);
  }
  private static void render(Minecraft mc,float delta)throws Exception{
    UiChecks.field(mc.worldRenderer,"field_1783",0);renderedBoats.clear();captureBoats=true;
    try{mc.worldRenderer.method_1544(Vec3d.createCached(mc.player.x,mc.player.y,mc.player.z),new class_68(){
      public boolean method_2007(Box box){return false;}public void method_2006(double x,double y,double z){}
    },delta);}finally{captureBoats=false;}
  }
  private static List<Float> angles(PlayerEntityRenderer renderer,net.minecraft.entity.player.PlayerEntity player,float delta){
    CapeChecks.angles.clear();CapeChecks.recording=true;GL11.glPushMatrix();
    try{((CapeRenderAccess)renderer).power$renderCape(player,delta);return List.copyOf(CapeChecks.angles);}
    finally{GL11.glPopMatrix();CapeChecks.recording=false;}
  }
  private static void cape(Minecraft mc)throws Exception{
    var p=mc.player;String cape=p.playerCapeUrl;var before=VisualConfig.copy();
    var fields=List.of("field_530","field_531","field_532","field_533","field_534","field_535","field_1012","field_1013","field_524","field_525");
    var saved=new HashMap<String,Object>();for(String f:fields)saved.put(f,p.getClass().getField(f).get(p));
    var renderer=new PlayerEntityRenderer(){@Override protected boolean method_2027(String url,String fallback){method_2026("/mob/char.png");return true;}};
    renderer.setDispatcher(EntityRenderDispatcher.field_2489);var boat=new class_113(mc.world,X+.5,Y+1,Z+.5);mc.world.method_210(boat);
    try{
      p.playerCapeUrl="fixture";p.method_1376(boat);p.field_524=p.field_525=0;
      for(boolean limited:new boolean[]{false,true})for(float yaw:new float[]{0,90,180,270})for(double lag:new double[]{.1,1,10}){
        var v=VisualConfig.copy();v.limitCapeRotation=limited;VisualConfig.preview(v);p.field_1012=p.field_1013=yaw;
        p.field_530=p.field_533=p.x+lag;p.field_531=p.field_534=p.y+.2;p.field_532=p.field_535=p.z-lag;
        var snapshot=new ArrayList<Object>();for(String f:fields)snapshot.add(p.getClass().getField(f).get(p));
        for(float delta:new float[]{0,.5f,1}){
          var mounted=angles(renderer,p,delta);check(mounted.size()==4&&mounted.get(1)==0&&mounted.get(2)==0&&mounted.get(3)==180,"boat cape turned sideways "+mounted);
          p.field_1595=null;var unmounted=angles(renderer,p,delta);p.field_1595=boat;
          check(mounted.get(0).equals(unmounted.get(0)),"boat correction changed pitch response");
          if(yaw==0)check(unmounted.get(1)!=0,"dismount did not restore lateral response");
        }
        for(int i=0;i<fields.size();i++)check(snapshot.get(i).equals(p.getClass().getField(fields.get(i)).get(p)),"cape render changed player interpolation");
      }
      check(GL11.glGetError()==0,"cape renderer GL error");
    }finally{p.method_1376(null);boat.markDead();p.playerCapeUrl=cape;for(var e:saved.entrySet())p.getClass().getField(e.getKey()).set(p,e.getValue());VisualConfig.preview(before);}
  }
}
