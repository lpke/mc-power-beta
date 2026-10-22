package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.building.mining.HoldUse;
import local.luke.power.input.*;
import local.luke.power.offhand.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.storage.JournalQueue;
import local.luke.power.rules.*;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.Display;

/** Arm the real binding, then observe normal native ticks and use-repeat dispatch. */
final class HoldUse420Checks {
  private static Run active;
  private HoldUse420Checks() {}
  static void run(Minecraft mc,String command)throws Exception {
    if(command.equals("hold-use-status")){log(active==null?"HOLD420 idle":"HOLD420 stage="+active.stage+" ticks="+active.elapsed);return;}
    if(command.equals("hold-use-stop")){finish("stopped");return;}
    check(active==null,"hold-use fixture already active");
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&mc.player.health>0&&!mc.player.dead
        &&Display.isActive()&&!DetachedCamera.isActive()&&mc.currentScreen==null&&mc.player.field_1595==null,
        "focus a disposable living singleplayer world outside menus, vehicles and freecam");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable instance before native use tests");
    check(!org.lwjgl.input.Mouse.isButtonDown(0)&&!org.lwjgl.input.Mouse.isButtonDown(1),"release physical attack/use buttons before the fixture");
    check(!AttackInput.held(),"stop automatic attack before the fixture");
    check(!NativeInventoryPlan.busy()&&!local.luke.power.inventory.InventoryActions.busy(),"finish native inventory plans before the fixture");
    failures=0;
    try{active=new Run(mc);active.prepare();log("HOLD420 started");}
    catch(Exception error){finish("setup failed");throw error;}
  }
  static void beforeTick(Minecraft mc) {
    if(active==null)return;
    try{active.target();}catch(Exception error){failed(error);}
  }
  static void afterTick(Minecraft mc) {
    if(active==null)return;
    try {
      var run=active;
      if(((Client253Access)(Object)mc).power$ticks()==run.armedAt)return;
      if(run.elapsed==0)test("HOLD420 initial native use stage "+run.stage,run::initial);
      if(++run.elapsed>=(run.stage==4?24:12)) {
        test("HOLD420 native held repeats stage "+run.stage,run::repeated);
        if(++run.stage==5)finish("done");else run.prepare();
      }
    }catch(Exception error){failed(error);}
  }
  private static void failed(Exception error) {
    try{test("HOLD420 runtime failure",()->{throw error;});finish("failed");}catch(Exception ignored){}
  }
  private static void finish(String reason)throws Exception {
    var run=active;active=null;if(run!=null)run.close();log("HOLD420 "+reason+" failures="+failures);
  }
  private static final class Run implements AutoCloseable {
    final Minecraft mc;final Release420Checks.State state;final Release420Checks.Scene scene;
    final OffhandSettings offhand=OffhandConfig.copy();final int keyCode=HoldUse.KEY.code;
    final local.luke.power.building.config.Settings building=local.luke.power.building.config.Config.current().copy();
    final InventoryToolsSettings tools=InventoryToolsConfig.copy();
    final net.minecraft.world.World ownedWorld;final local.luke.power.world.WorldDifficulty difficulty;final int savedDifficulty,nativeDifficulty;
    final net.minecraft.entity.LivingEntity view;final class_27 originalTarget;final boolean paused,focused;
    final Set<Entity> vehicles=Collections.newSetFromMap(new IdentityHashMap<>());
    final int x,y=110,z;int stage,elapsed,armedAt,initialCount;long pressRevision;
    Run(Minecraft mc)throws Exception {
      this.mc=mc;view=mc.field_2807;originalTarget=mc.field_2823;paused=mc.paused;focused=mc.field_2778;
      state=new Release420Checks.State(mc);
      ownedWorld=mc.world;difficulty=(local.luke.power.world.WorldDifficulty)ownedWorld.method_262();savedDifficulty=difficulty.power$difficulty();nativeDifficulty=ownedWorld.field_213;
      // An asynchronous run advances real world time. Retain the live client tick counters.
      state.fields.remove(0);
      x=((int)Math.floor(mc.player.x)>>4)*16+7;z=((int)Math.floor(mc.player.z)>>4)*16+6;
      try{scene=new Release420Checks.Scene(mc,x,y,z);}catch(Exception|Error error){state.close();throw error;}
      var rules=state.rules.copy();for(GameRule rule:GameRule.values())if(rule.access())rules.set(rule,true);AccessRules.restore(mc,rules);
      difficulty.power$difficulty(2);ownedWorld.field_213=2;
      var enabled=OffhandConfig.copy();enabled.enabled=true;OffhandConfig.preview(enabled);
      HoldUse.KEY.code=118;mc.player.inventory.selectedSlot=0;mc.paused=false;mc.field_2778=true;mc.field_2807=mc.player;
    }
    void prepare()throws Exception {
      HoldUse.stop();scene.clear();scene.floor();Offhand.set(mc.player.inventory,null);
      rememberVehicles();for(Entity entity:vehicles)entity.markDead();vehicles.clear();
      CreativeVehicleChecks.mode(mc,stage==2||stage==3?"CREATIVE":"SURVIVAL");
      var placement=building.copy();placement.placement.enabled=stage==4;placement.placement.attemptsPerTick=2;
      placement.placement.restrictionEnabled=false;placement.placement.rememberOrientation=false;
      local.luke.power.building.config.Config.preview(placement);
      var inventoryTools=InventoryToolsConfig.copy();inventoryTools.restock=stage==4;inventoryTools.toolSwitch=false;
      inventoryTools.restockBeforeEmpty=true;inventoryTools.restockThreshold=2;InventoryToolsConfig.preview(inventoryTools);
      mc.player.method_1340(x+.5,y+.01+mc.player.field_1631-mc.player.field_1640,z+.5);
      mc.player.prevX=mc.player.field_1637=mc.player.x;mc.player.prevY=mc.player.field_1638=mc.player.y;mc.player.prevZ=mc.player.field_1639=mc.player.z;
      mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;mc.player.health=stage==1?2:20;
      mc.player.field_161.field_2533=mc.player.field_161.field_2532=0;
      switch(stage) {
        case 0 -> mc.player.inventory.main[0]=new ItemStack(4,8,0);
        case 1 -> {mc.player.inventory.main[0]=new ItemStack(280,1,0);Offhand.set(mc.player.inventory,new ItemStack(260,1,0));}
        case 2 -> {
          mc.player.inventory.main[0]=new ItemStack(333,1,0);
          for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)scene.set(x+1+dx,y,z+3+dz,1,0);
          scene.set(x+1,y,z+3,9,0);
        }
        case 3 -> {mc.player.inventory.main[0]=new ItemStack(328,1,0);scene.set(x+1,y,z+3,66,0);}
        case 4 -> {mc.player.inventory.main[0]=new ItemStack(4,3,0);mc.player.inventory.main[9]=new ItemStack(4,16,0);}
        default -> throw new IllegalStateException();
      }
      target();
      if(stage==3) {
        mc.field_2818.method_1838(1);
        check(mc.field_2823!=null&&mc.field_2823.field_1984==x+1&&mc.field_2823.field_1985==y&&mc.field_2823.field_1986==z+3
            &&mc.world.getBlockId(x+1,y,z+3)==66,"native minecart aim did not select the supported rail");
      }
      HoldUse.tick(mc);pressRevision=OffhandUse.pressRevision();Bindings.keyPress(HoldUse.KEY.code);check(HoldUse.active(),"real Hold-use binding did not activate");
      initialCount=mc.player.inventory.main[0].count;elapsed=0;armedAt=((Client253Access)(Object)mc).power$ticks();
    }
    void target() {
      if(stage==1){mc.field_2823=null;return;}
      int targetY=stage==0||stage==4?y-1:y,targetZ=stage==0||stage==4?z+2:z+3;
      if(stage==0)scene.set(x+1,y,targetZ,0,0);
      double tx=x+1.5,ty=targetY+(stage==3?.0625:.5),tz=targetZ+.5,eye=mc.player.y+1.62-mc.player.field_1631;
      double dx=tx-mc.player.x,dy=ty-eye,dz=tz-mc.player.z;
      mc.player.yaw=mc.player.prevYaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
      mc.player.pitch=mc.player.prevPitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
      mc.field_2823=new class_27(x+1,targetY,targetZ,1,Vec3d.createCached(tx,ty,tz));
    }
    void initial() {
      check(HoldUse.active(),"Hold use canceled before its native initial press");
      check(OffhandUse.pressRevision()==pressRevision+1,"activation dispatched duplicate native initial presses");
      switch(stage) {
        case 0 -> check(mc.player.inventory.main[0].count==initialCount-1,"initial use did not place exactly one native block");
        case 1 -> check(mc.player.health==6&&Offhand.get(mc.player.inventory)==null,"initial use failed native offhand food fallback");
        case 2,3 -> check(vehicles()==1,"initial Creative vehicle use spawned "+vehicles()+" vehicles");
        case 4 -> check(mc.player.inventory.main[0].count==initialCount-1&&mc.player.inventory.main[9].count==16,
            "initial fast-placement press bypassed the pending partial refill guard");
      }
      check(!UseInput.consumePress(),"initial use edge remained pending after native dispatch");
    }
    void repeated() {
      switch(stage) {
        case 0 -> check(mc.player.inventory.main[0].count<initialCount-1,"held use did not reach normal native repeats");
        case 1 -> check(mc.player.health==6&&Offhand.get(mc.player.inventory)==null,"held food state: health="+mc.player.health+", offhand="+Offhand.get(mc.player.inventory)+", difficulty="+ownedWorld.field_213);
        case 2,3 -> check(vehicles()==1,"held Creative vehicle use count="+vehicles()+" expected 1, rail="+mc.world.getBlockId(x+1,y,z+3)+", attack="+AttackInput.held());
        case 4 -> {
          check(HoldUse.active(),"partial refill canceled Hold use");
          check(mc.player.inventory.main[9]==null&&mc.player.inventory.main[0]!=null&&mc.player.inventory.main[0].count>3,
              "normal durable partial-stack refill did not complete while holding Use");
          check(mc.player.inventory.main[0].count<18,"fast-placement session did not resume after its partial refill");
          check(!NativeInventoryPlan.busy(),"fast placement retained a durable refill plan");
        }
      }
    }
    void rememberVehicles(){for(Object object:mc.world.field_198)if(object instanceof Entity entity&&!state.entities.contains(object)
        &&(entity instanceof class_113||entity instanceof class_549))vehicles.add(entity);}
    int vehicles(){rememberVehicles();int count=0;for(Entity entity:vehicles)if(!entity.dead&&(stage==2?entity instanceof class_113:entity instanceof class_549))count++;return count;}
    public void close()throws Exception {
      HoldUse.stop();HoldUse.KEY.code=keyCode;
      rememberVehicles();for(Object object:mc.world.field_198)if(object instanceof Entity entity&&!vehicles.contains(entity))state.entities.add(object);
      Restock.clear();NativeInventoryPlan.cancel();JournalQueue.awaitQueued();NativeInventoryPlan.tick(mc);
      try{scene.close();}finally{OffhandConfig.preview(offhand);InventoryToolsConfig.preview(tools);local.luke.power.building.config.Config.preview(building);
        difficulty.power$difficulty(savedDifficulty);ownedWorld.field_213=nativeDifficulty;
        state.close();mc.field_2807=view;mc.field_2823=originalTarget;mc.paused=paused;mc.field_2778=focused;}
    }
  }
}
