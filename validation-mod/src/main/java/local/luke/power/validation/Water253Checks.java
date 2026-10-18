package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.rules.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.*;
import org.lwjgl.input.Keyboard;

/** Compare actual Beta ticks with the unenchanted modern reference in docs/water-movement.md. */
final class Water253Checks {
  private static final int X=64,Z=64;
  private static ByteBuffer keys;
  private static void enabled(boolean value){var s=GameplayConfig.copy();s.modernWaterMovement=value;GameplayConfig.preview(s);}
  private static void reset(Minecraft mc,double x,double feet,double z,float yaw,float pitch){
    var p=mc.player;if(p.field_1595!=null)p.method_1376(null);p.health=20;p.dead=false;p.air=300;
    p.method_1341(x,feet,z,yaw,pitch);p.prevX=p.x;p.prevY=p.y;p.prevZ=p.z;p.prevYaw=yaw;p.prevPitch=pitch;
    p.velocityX=p.velocityY=p.velocityZ=0;p.field_161=new class_41();mc.field_2807=p;mc.field_2778=true;mc.paused=false;
  }
  private static double[] swim(Minecraft mc,boolean modern,boolean ctrl,float pitch,int ticks){
    enabled(modern);reset(mc,X+.5,94,Z+.5,0,pitch);keys.put(29,(byte)(ctrl?1:0));var p=mc.player;
    double x=p.x,y=p.y,z=p.z;
    for(int i=0;i<ticks;i++){p.air=300;p.field_161.field_2533=1;p.tick();}
    return new double[]{p.x-x,p.y-y,p.z-z,p.velocityX,p.velocityY,p.velocityZ};
  }
  private static void near(double actual,double expected,double error,String label){check(Math.abs(actual-expected)<error,label+": "+actual+" expected "+expected);}
  private static void pool(Minecraft mc,int fluid){
    for(int x=X-18;x<=X+18;x++)for(int z=Z-18;z<=Z+18;z++){
      mc.world.method_154(x,80,z,1,0);for(int y=81;y<=100;y++)mc.world.method_154(x,y,z,fluid,0);
      for(int y=101;y<=104;y++)mc.world.method_154(x,y,z,0,0);
    }
  }
  static void run(Minecraft mc)throws Exception{
    failures=0;DeathChecks.alive(mc);mc.setScreen(null);FreecamPreviewChecks.active(false);
    var saved=new net.minecraft.nbt.NbtCompound();mc.player.write(saved);var rules=AccessRules.copy(mc);var old=GameplayConfig.copy();
    var input=mc.player.field_161;boolean paused=mc.paused;
    var keyField=Keyboard.class.getDeclaredField("keyDownBuffer");keyField.setAccessible(true);keys=(ByteBuffer)keyField.get(null);
    byte ctrl=keys.get(29);
    try{
      AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,"SURVIVAL");AccessRules.restore(mc,new RuleSet());
      pool(mc,9);
      test("all bases, Gameplay row and a manual settings draft keep physics and durable data isolated",()->{
        for(var base:DefaultBases.Base.values())check(DefaultBases.value(base,"gameplay.modernWaterMovement",new com.google.gson.JsonPrimitive(true)).getAsBoolean()==(base==DefaultBases.Base.POWER_BETA),"wrong base");
        enabled(false);var session=SettingsRegistry.open(mc);var row=find(session,"gameplay.modernWaterMovement");
        check(row.page.equals("Gameplay")&&!row.restart&&SettingAccess.visible(session,row),"wrong metadata or access");
        var bytes=java.nio.file.Files.readAllBytes(local.luke.power.storage.PowerConfig.path());row.parse("true");session.preview(true);
        check(!GameplayConfig.current().modernWaterMovement,"manual draft changed gameplay");session.discard();
        check(!GameplayConfig.current().modernWaterMovement&&Arrays.equals(bytes,java.nio.file.Files.readAllBytes(local.luke.power.storage.PowerConfig.path())),"Cancel wrote or leaked");
      });
      test("still-water full ticks keep normal speed, ignore Ctrl and reduce sinking to the modern response",()->{
        var beta=swim(mc,false,false,0,80);var walk=swim(mc,true,false,0,80);var ctrlHeld=swim(mc,true,true,0,80);
        near(walk[2],beta[2],.002,"ordinary horizontal displacement");near(ctrlHeld[2],walk[2],.002,"Ctrl changed swimming speed");
        double v=0,distance=0;for(int i=0;i<80;i++){v+=.02f*.98f;distance+=v;v*=.8f;}
        near(walk[2],distance,.002,"modern ordinary displacement");near(walk[5],v,.0001,"terminal velocity");
        near(walk[1],beta[1]/4,.002,"modern sinking");near(ctrlHeld[1],walk[1],.002,"Ctrl changed sinking");
        log("WATER displacement Beta="+Arrays.toString(beta)+" modern="+Arrays.toString(walk));
      });
      test("Jump rises and Sneak dives without adding look-directed or sprint swimming",()->{
        var down=swim(mc,true,true,45,20);var up=swim(mc,true,true,-45,20);
        near(down[1],up[1],.002,"pitch changed vertical motion");near(down[2],up[2],.002,"pitch changed forward motion");
        reset(mc,X+.5,94,Z+.5,0,0);keys.put(29,(byte)0);double start=mc.player.y;
        for(int i=0;i<20;i++){mc.player.field_161.field_2535=true;mc.player.tick();}check(mc.player.y>start+.2,"Jump did not rise");
        reset(mc,X+.5,94,Z+.5,0,0);start=mc.player.y;
        for(int i=0;i<20;i++){mc.player.field_161.field_2536=true;mc.player.tick();}check(mc.player.y<start-.5,"Sneak did not descend");
      });
      test("full-strength currents retain their push and ordinary upstream travel remains possible",()->{
        for(int x=X-5;x<=X+5;x++)for(int z=Z-4;z<=Z+7;z++)for(int y=81;y<=100;y++)mc.world.method_154(x,y,z,8,Math.max(0,Math.min(7,z-(Z-2))));
        double[] direct=new double[2];
        for(int mode=0;mode<2;mode++){
          enabled(mode==1);reset(mc,X+.5,94,Z+3.5,180,0);mc.player.method_1393();direct[mode]=mc.player.velocityZ;
        }
        near(direct[0],.014,1e-10,"native full current");near(direct[1],direct[0],1e-10,"modern full current");
        log("WATER full-strength push "+Arrays.toString(direct));
        double[] distance=new double[2];
        for(int mode=0;mode<2;mode++){
          enabled(mode==1);reset(mc,X+.5,94,Z+3.5,180,0);keys.put(29,(byte)0);double start=mc.player.z;
          for(int i=0;i<30;i++){mc.player.field_161.field_2533=1;mc.player.tick();}distance[mode]=start-mc.player.z;
        }
        check(distance[0]>0,"native upstream fixture did not move");check(distance[1]>=distance[0]-.002,"modern upstream regressed");log("WATER upstream "+Arrays.toString(distance));
      });
      test("shallow currents weaken without sprinting; unloaded water checks never request chunks",()->{
        for(int x=X-2;x<=X+2;x++)for(int z=Z-3;z<=Z+3;z++){
          mc.world.method_154(x,94,z,1,0);mc.world.method_154(x,95,z,8,Math.max(0,Math.min(7,z-Z+5)));
          for(int y=96;y<=101;y++)mc.world.method_154(x,y,z,0,0);
        }
        keys.put(29,(byte)0);double[] pushes=new double[2];
        for(int mode=0;mode<2;mode++){enabled(mode==1);reset(mc,X+.5,95.27,Z+.5,0,0);mc.player.velocityX=.01;
          check(mc.player.method_1393(),"fixture not in shallow water");pushes[mode]=Math.hypot(mc.player.velocityX-.01,mc.player.velocityZ);}
        check(pushes[0]>.01&&pushes[1]>0&&pushes[1]<pushes[0]*.5,"shallow push "+Arrays.toString(pushes));log("WATER shallow push "+Arrays.toString(pushes));
        double[] upstream=new double[2];
        for(int mode=0;mode<2;mode++){
          enabled(mode==1);reset(mc,X+.5,95.27,Z+.5,180,0);mc.player.velocityX=.01;
          double start=mc.player.z;mc.player.baseTick();mc.player.method_945(0,.98f);upstream[mode]=start-mc.player.z;
        }
        check(upstream[0]>0&&upstream[1]>upstream[0]*2,"ordinary shallow upstream displacement "+Arrays.toString(upstream));
        log("WATER shallow upstream "+Arrays.toString(upstream));
        enabled(true);reset(mc,X+.5,95.27,Z+.5,0,0);mc.player.method_1393();
        near(Math.hypot(mc.player.velocityX,mc.player.velocityZ),.0045,.000001,"minimum current from rest");
        reset(mc,31_999_900,94,31_999_900,0,0);Release252Checks.chunkRequests=0;Release252Checks.checkingUnloaded=true;
        try{check(!mc.player.method_1393(),"unloaded terrain counted as water");}finally{Release252Checks.checkingUnloaded=false;}
        check(Release252Checks.chunkRequests==0,"water queried unloaded chunks");
      });
      pool(mc,9);
      test("native water collisions and limb interpolation remain active",()->{
        for(int x=X-2;x<=X+2;x++)for(int y=81;y<=102;y++)mc.world.method_154(x,y,Z+3,1,0);
        double[] stopped=new double[2];
        for(int mode=0;mode<2;mode++){
          enabled(mode==1);reset(mc,X+.5,94,Z+.5,0,0);double start=mc.player.z;
          for(int i=0;i<60;i++){mc.player.air=300;mc.player.field_161.field_2533=1;mc.player.tick();if(i==10)check(mc.player.field_1049>.05,"limb animation did not advance");}
          stopped[mode]=mc.player.z;
          check(mc.player.z>start+1&&mc.player.z<Z+3-.29,"swimmer clipped through the wall");
          check(Float.isFinite(mc.player.field_1048)&&Float.isFinite(mc.player.field_1049),"limb interpolation became invalid");
        }
        near(stopped[0],stopped[1],.001,"native collision limit");
        for(int x=X-2;x<=X+2;x++)for(int y=81;y<=102;y++)mc.world.method_154(x,y,Z+3,y<=100?9:0,0);
      });
      test("mob and boat currents, multiplayer, riding, Spectator and flight keep their own physics",()->{
        keys.put(29,(byte)0);
        for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+3;z++)for(int y=91;y<=99;y++)mc.world.method_154(x,y,z,8,Math.max(0,Math.min(7,z-Z+3)));
        var mob=new LivingEntity(mc.world){};var boat=new class_113(mc.world,X+.5,94,Z+.5);
        for(Entity entity:new Entity[]{mob,boat}){
          double[] values=new double[2];for(int mode=0;mode<2;mode++){enabled(mode==1);entity.method_1340(X+.5,94,Z+.5);entity.velocityX=entity.velocityY=entity.velocityZ=0;entity.method_1393();values[mode]=entity.velocityZ;}
          if(entity==mob)check(Math.abs(values[0])>.0001,"mob fixture had no current");near(values[0],values[1],1e-10,"other entity flow");check(!ModernWater.eligible(entity),"non-player eligible");
        }
        enabled(true);reset(mc,X+.5,94,Z+.5,0,0);mc.player.method_1376(boat);check(!ModernWater.eligible(mc.player),"riding changed");mc.player.method_1376(null);
        AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,"SPECTATOR");check(!ModernWater.eligible(mc.player),"Spectator changed");
        CreativeVehicleChecks.mode(mc,"CREATIVE");Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer").getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
        check(((PlayerMode)mc.player).power$flying()&&!ModernWater.eligible(mc.player),"flight changed");Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer").getMethod("creative_setFlying",boolean.class).invoke(mc.player,false);CreativeVehicleChecks.mode(mc,"SURVIVAL");
        mc.world.isRemote=true;try{check(!ModernWater.eligible(mc.player),"multiplayer changed");}finally{mc.world.isRemote=false;}
        mob.markDead();boat.markDead();
      });
      test("lava and land travel remain identical and disabling restores Beta on the next tick",()->{
        for(int fluid:new int[]{0,11}){
          for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+3;z++)for(int y=91;y<=99;y++)mc.world.method_154(x,y,z,fluid,0);
          double[][] result=new double[2][];for(int m=0;m<2;m++)result[m]=swim(mc,m==1,true,0,5);
          for(int i=0;i<6;i++)near(result[0][i],result[1][i],.000001,"fluid "+fluid+" component "+i);
        }
        for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+3;z++)for(int y=91;y<=99;y++)mc.world.method_154(x,y,z,9,0);
        enabled(true);reset(mc,X+.5,94,Z+.5,0,0);keys.put(29,(byte)1);mc.player.field_161.field_2533=1;mc.player.tick();
        enabled(false);double before=mc.player.velocityZ;mc.player.field_161.field_2533=1;mc.player.tick();
        near(mc.player.velocityZ,(before+.02f*.98f)*.8f,.00001,"live disable retained swim drag");
      });
    }finally{
      Release252Checks.checkingUnloaded=false;FreecamPreviewChecks.active(false);mc.world.isRemote=false;
      keys.put(29,ctrl);GameplayConfig.preview(old);
      mc.player.field_161=input;mc.player.read(saved);mc.paused=paused;AccessRules.restore(mc,rules);mc.setScreen(null);
    }
    log("WATER 2.5.3 FAILURES "+failures);
  }
}
