package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

/** Native item placement and mounted movement in a muted disposable world. */
final class Release251BoatChecks {
  private static final int X=16,Z=16,Y=100;
  private static final float[] HEADINGS={0,45,90,135,180,225,270,315,-147.25f,1097.5f};
  private static final Path REPORT=Path.of("power-beta-data/reports/release-251");
  private static double dot(class_113 boat,float look){
    double a=Math.toRadians(look),b=Math.toRadians(boat.yaw);
    return -Math.cos(b)*-Math.sin(a)-Math.sin(b)*Math.cos(a);
  }
  private static List<class_113> boats(Minecraft mc){
    List<class_113> result=new ArrayList<>();
    for(Object e:mc.world.method_291())if(e instanceof class_113 b&&!b.dead)result.add(b);
    return result;
  }
  private static void pose(Minecraft mc,float yaw){
    if(mc.player.field_1595!=null)mc.player.method_1376(null);
    mc.player.method_1341(X+.37,Y+1.25,Z+.61,yaw,60);
    mc.player.prevX=mc.player.x;mc.player.prevY=mc.player.y;mc.player.prevZ=mc.player.z;
    mc.player.prevYaw=yaw;mc.player.prevPitch=60;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
  }
  private static class_113 place(Minecraft mc,float yaw){
    pose(mc,yaw);Set<class_113> before=new HashSet<>(boats(mc));
    ItemStack stack=new ItemStack(Item.field_405,8);
    ItemStack result=Item.field_405.method_451(stack,mc.world,mc.player);
    check(result==stack&&stack.count==7,"placement changed native item consumption at yaw "+yaw+": count "+stack.count);
    var created=boats(mc);created.removeAll(before);
    check(created.size()==1,"native placement spawned "+created.size()+" boats at "+yaw);
    return created.get(0);
  }
  private static void retire(Entity boat){
    boat.markDead();
    // The merged mapping's immediate world-removal method is server-only.
    // Move the dead hitbox away until the next normal world tick removes it.
    boat.method_1340(X+64,Y+8,Z+64);
  }
  private static void settings(boolean fix,boolean steering,boolean speed){
    var g=GameplayConfig.copy();g.boatPlacementFacing=fix;GameplayConfig.preview(g);
    var b=local.luke.power.building.config.Config.current().copy();
    b.boatSteering=steering;b.boatSpeed=speed;b.boatProtection=false;
    local.luke.power.building.config.Config.preview(b);
  }
  static void run(Minecraft mc,String action)throws Exception{
    failures=0;Files.createDirectories(REPORT);
    if(action.equals("saved")){
      test("boat direction preference survives restart",()->check(!GameplayConfig.current().boatPlacementFacing,"saved Off preference lost"));
      log("RELEASE 2.5.1 saved FAILURES "+failures);return;
    }
    check(mc.world!=null,"create the disposable world first");
    var oldGameplay=GameplayConfig.copy();var oldBuilding=local.luke.power.building.config.Config.current().copy();
    var input=mc.player.field_161;double px=mc.player.x,py=mc.player.y,pz=mc.player.z;float yaw=mc.player.yaw,pitch=mc.player.pitch;
    String mode=Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    List<Entity> created=new ArrayList<>();
    try{
      test("boat direction default is On except Beta, with an independent Fixes row",()->{
        for(var base:DefaultBases.Base.values()){
          DefaultBases.load(base,mc,Path.of(".").toAbsolutePath());
          var s=find(SettingsRegistry.open(mc),"gameplay.boatPlacementFacing");
          check(s.page.equals("Fixes")&&s.group.equals("Entities")&&SettingAccess.owner(s)==null,"wrong setting placement/owner");
          check(s.value.getAsBoolean()==(base!=DefaultBases.Base.BETA),"wrong loaded base value "+base);
          check(s.defaultValue.getAsBoolean()==(base!=DefaultBases.Base.BETA),"wrong reset value "+base);
        }
        DefaultBases.load(DefaultBases.Base.POWER_BETA,mc,Path.of(".").toAbsolutePath());
        var options=new PowerOptionsScreen(mc.currentScreen);mc.setScreen(options);UiChecks.search(options,"Boat placement direction fix");
        for(int[] size:new int[][]{{320,240},{960,540}}){options.init(mc,size[0],size[1]);SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("fixes-"+size[0]+".png"));}
      });
      mc.setScreen(null);CreativeVehicleChecks.mode(mc,"SURVIVAL");
      for(int x=X-8;x<=X+8;x++)for(int z=Z-8;z<=Z+8;z++){
        mc.world.method_214(x>>4,z>>4);
        for(int y=Y-1;y<=Y+6;y++)mc.world.method_229(x,y,z,y==Y-1?1:y==Y?9:0);
      }
      test("unfixed native placement uses yaw zero with vanilla or improved steering, at either speed",()->{
        for(boolean steering:new boolean[]{false,true})for(boolean speed:new boolean[]{false,true}){
          settings(false,steering,speed);
          for(float look:HEADINGS){var b=place(mc,look);created.add(b);check(b.yaw==0&&b.prevYaw==0,"native baseline differs");retire(b);}
          log("BASELINE steering="+steering+" speed="+speed+" all 10 headings spawned at yaw 0");
        }
      });
      test("fixed placements face the player bearing in source and flowing-water blocks without a first-tick spin",()->{
        for(int water:new int[]{8,9}){
          for(int x=X-8;x<=X+8;x++)for(int z=Z-8;z<=Z+8;z++)mc.world.method_229(x,Y,z,water);
          for(boolean steering:new boolean[]{false,true})for(boolean speed:new boolean[]{false,true}){
            settings(true,steering,speed);
            for(float look:HEADINGS){
              var b=place(mc,look);created.add(b);
              check(dot(b,look)>.99999&&b.yaw==b.prevYaw,"wrong front/interpolation "+look+" "+b.yaw);
              for(int i=0;i<8;i++)b.tick();
              check(dot(b,look)>.99999,"unoccupied first ticks changed heading");retire(b);
            }
          }
        }
      });
      test("boarding and pressing forward moves along the placement bearing in all four steering/speed combinations",()->{
        var forward=new class_41();forward.field_2533=1;mc.player.field_161=forward;
        for(boolean steering:new boolean[]{false,true})for(boolean speed:new boolean[]{false,true}){
          settings(true,steering,speed);
          for(float look:new float[]{0,90,180,270,37.5f}){
            var b=place(mc,look);created.add(b);for(int i=0;i<8;i++)b.tick();
            b.method_1323(mc.player);check(mc.player.field_1595==b&&b.field_1594==mc.player,"boarding failed");
            double x=b.x,z=b.z;float bearing=mc.player.yaw;
            check(Math.abs(bearing-look)<.001,"boarding turned the player");
            for(int tick=0;tick<16;tick++){forward.field_2533=1;forward.field_2532=0;mc.player.method_1384();b.tick();b.method_1382();}
            double dx=b.x-x,dz=b.z-z,a=Math.toRadians(look),along=dx*-Math.sin(a)+dz*Math.cos(a),across=dx*Math.cos(a)+dz*Math.sin(a);
            check(along>.05&&Math.abs(across)<Math.max(.01,along*.02),"forward mismatch steering="+steering+" speed="+speed+" look="+look+" displacement="+dx+","+dz);
            log("FORWARD steering="+steering+" speed="+speed+" look="+look+" along="+along+" across="+across);
            mc.player.method_1376(null);retire(b);
          }
        }
      });
      test("the fix leaves constructors, existing boats, misses and multiplayer placement native",()->{
        settings(true,true,true);var b=new class_113(mc.world,X+.5,Y+1,Z+.5);created.add(b);check(b.yaw==0,"constructor changed outside item placement");
        b.yaw=b.prevYaw=123;settings(false,false,false);settings(true,true,true);check(b.yaw==123&&b.prevYaw==123,"live setting rotated an existing boat");
        pose(mc,0);mc.player.pitch=mc.player.prevPitch=-90;var stack=new ItemStack(Item.field_405,4);int count=boats(mc).size();Item.field_405.method_451(stack,mc.world,mc.player);check(stack.count==4&&boats(mc).size()==count,"miss changed placement behavior");
        pose(mc,0);mc.world.isRemote=true;
        try{stack=new ItemStack(Item.field_405,4);Item.field_405.method_451(stack,mc.world,mc.player);check(boats(mc).size()==count,"client spawned a server-owned boat");check(stack.count==3,"multiplayer item behavior changed");}
        finally{mc.world.isRemote=false;}
      });
    }finally{
      mc.world.isRemote=false;if(mc.player.field_1595!=null)mc.player.method_1376(null);
      for(Entity e:created)retire(e);mc.player.field_161=input;
      GameplayConfig.preview(oldGameplay);local.luke.power.building.config.Config.preview(oldBuilding);
      CreativeVehicleChecks.mode(mc,mode);mc.player.method_1341(px,py-mc.player.field_1631,pz,yaw,pitch);
    }
    test("manual Apply and durable Off preference use the shared config session",()->{
      var enabled=GameplayConfig.copy();enabled.boatPlacementFacing=true;GameplayConfig.preview(enabled);
      var s=SettingsRegistry.open(mc);find(s,"gameplay.boatPlacementFacing").value=new JsonPrimitive(false);
      check(GameplayConfig.current().boatPlacementFacing,"draft changed gameplay before Apply");
      s.save(Path.of(".").toAbsolutePath());check(!GameplayConfig.current().boatPlacementFacing,"Apply did not update gameplay");
      check(!PowerConfig.section("gameplay").get("boatPlacementFacing").getAsBoolean(),"durable preference differs");
    });
    log("RELEASE 2.5.1 boats FAILURES "+failures);
  }
}
