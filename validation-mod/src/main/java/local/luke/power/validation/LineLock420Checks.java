package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.autowalk.AutoWalk;
import local.luke.power.building.config.Config;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.rules.GameRule;
import local.luke.power.sprint.*;
import net.minecraft.class_413;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.opengl.Display;

/** Exercise the installed local-player Entity.move clamp and native travel, without a bridge stub. */
final class LineLock420Checks {
  private LineLock420Checks() {}
  static void run(Minecraft mc)throws Exception {
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&mc.player.health>0&&!mc.player.dead
        &&Display.isActive()&&!DetachedCamera.isActive()&&mc.player.field_1595==null,"focus a living disposable singleplayer world outside vehicles/freecam");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable client first");
    check(!DebugInput.held()&&!Bindings.physical(local.luke.power.debug.DebugKeys.MODIFIER.code),"release the debug hold first");
    failures=0;
    try(var saved=new Release420Checks.State(mc);var walk=new WalkState(mc)) {
      mc.setScreen(null);mc.paused=false;mc.field_2778=true;mc.field_2807=mc.player;
      AccessRules.set(mc,GameRule.ALLOW_SPRINTING,true);CreativeVehicleChecks.mode(mc,"SURVIVAL");
      mc.player.field_161=new class_413(mc.options);
      var settings=Config.current().copy();settings.inventoryWhileMoving=true;settings.autoWalkHoldToWalk=false;Config.preview(settings);
      var sprint=SprintConfig.copy();sprint.activation=1;sprint.doubleTap=false;sprint.stamina=true;sprint.noSprintInCombat=false;
      sprint.stopAtWalls=true;sprint.sprintLadders=false;sprint.speed=150;sprint.particles=false;sprint.jumpBoost=false;SprintConfig.preview(sprint);
      AutoWalk.KEY.code=117;AutoWalk.LINE_LOCK_KEY.code=118;Bindings.register(mc.options.allKeys);AutoWalk.tick(mc);
      MovementOwnership.registerAutoWalkLineLock(AutoWalk::lineLocked);
      int x=((int)Math.floor(mc.player.x)>>4)*16+7,z=((int)Math.floor(mc.player.z)>>4)*16+6,y=110;
      try(var scene=new Release420Checks.Scene(mc,x,y,z)) {
        scene.clear();scene.floor();
        test("LINE420 line-lock key alone cannot start Auto-walk",()->{
          AutoWalk.stop();toggle(saved);check(!AutoWalk.isWalking()&&!AutoWalk.lineLocked(),"line-lock key started walking");
        });
        for(float yaw:new float[]{30,120,210,300})for(boolean backwards:new boolean[]{false,true}) {
          test("LINE420 native diagonal travel stays in lane yaw="+yaw+" backwards="+backwards,()->{
            scene.clear();scene.floor();place(mc,x+.5,y,z+1.5,yaw);start(mc,saved,backwards);toggle(saved);
            check(AutoWalk.lineLocked()&&MovementOwnership.autoWalkLineLocked(),"real Auto-walk did not publish line lock");
            double startX=mc.player.x,startZ=mc.player.z;boolean constrainX=Math.abs(Math.cos(Math.toRadians(yaw)))>=Math.abs(Math.sin(Math.toRadians(yaw)));
            double lane=Math.floor(constrainX?startX:startZ),half=(mc.player.boundingBox.maxX-mc.player.boundingBox.minX)/2;
            for(int tick=0;tick<8;tick++) {
              travel(mc);double lateral=constrainX?mc.player.x:mc.player.z;
              check(lateral>=lane+half-1e-6&&lateral<=lane+1-half+1e-6,"installed Entity.move let player leave lane: "+lateral);
              check(mc.player.yaw==yaw&&mc.player.pitch==-7,"line lock rotated the player");
              check(Math.abs(mc.player.field_161.field_2533-(backwards?-1:1))<1e-6,"native input lost activation direction");
            }
            double longitudinal=constrainX?mc.player.z-startZ:mc.player.x-startX;
            double intended=(constrainX?Math.cos(Math.toRadians(yaw)):-Math.sin(Math.toRadians(yaw)))*(backwards?-1:1);
            check(longitudinal*intended>.3,"native Auto-walk failed actual longitudinal displacement: "+longitudinal);
            log("LINE420 native displacement yaw="+yaw+" backwards="+backwards+" dx="+(mc.player.x-startX)+" dz="+(mc.player.z-startZ));
          });
        }
        test("LINE420 ghost lane edge retains real native sprint and stamina",()->{
          scene.clear();scene.floor();place(mc,x+.5,y,z+.5,0);start(mc,saved,false);toggle(saved);
          saved.keys.put(29,(byte)1);saved.keys.put(32,(byte)1);resetSprint(mc);
          double startZ=mc.player.z;
          for(int tick=0;tick<8;tick++){travel(mc);check(((SprintPlayer)mc.player).power$sprint().sprinting,"ghost side wall canceled sprint");}
          double half=(mc.player.boundingBox.maxX-mc.player.boundingBox.minX)/2;
          double edge=Math.min(Math.abs(mc.player.x-(x+half)),Math.abs(mc.player.x-(x+1-half)));
          check(edge<1e-5&&mc.player.z>startZ+.5,"native sprint did not reach and travel along ghost wall: x="+mc.player.x+", dz="+(mc.player.z-startZ));
          check(((SprintPlayer)mc.player).power$sprint().stamina<100,"ghost-wall sprint bypassed stamina");saved.keys.put(32,(byte)0);
        });
        test("LINE420 actual solid wall keeps sprint while locked and cancels after toggle off",()->{
          scene.clear();scene.floor();for(int bx=x-1;bx<=x+1;bx++)for(int by=y;by<=y+2;by++)scene.set(bx,by,z+2,1,0);
          place(mc,x+.5,y,z+1.5,0);start(mc,saved,false);toggle(saved);saved.keys.put(29,(byte)1);resetSprint(mc);
          for(int tick=0;tick<6;tick++){travel(mc);check(((SprintPlayer)mc.player).power$sprint().sprinting,"real wall canceled locked sprint");}
          check(mc.player.field_1624&&mc.player.z<z+1.701,"fixture did not collide with native solid wall");
          toggle(saved);check(AutoWalk.isWalking()&&!MovementOwnership.autoWalkLineLocked(),"line-lock off changed Auto-walk ownership");
          SprintController.movement(mc.player);check(!((SprintPlayer)mc.player).power$sprint().sprinting,"unlocked real wall retained sprint despite enabled preference");
        });
        test("LINE420 toggle off restores lateral movement and other entities never inherit lane walls",()->{
          scene.clear();scene.floor();place(mc,x+.5,y,z+.5,0);start(mc,saved,false);toggle(saved);
          var other=new net.minecraft.class_443(mc.world);other.method_1340(x+.5,y,z+.5);other.move(1,0,0);
          check(other.x>x+1.49,"local line lock constrained another entity");
          mc.player.move(1,0,0);check(mc.player.x<x+.701,"installed local Entity.move clamp absent");
          toggle(saved);double before=mc.player.x;mc.player.move(1,0,0);
          check(mc.player.x>before+.99&&AutoWalk.isWalking(),"toggle off retained lateral walls or stopped walking");
        });
      }
    }
    log("LINE420 failures="+failures);check(failures==0,"line-lock native checks failed="+failures);
  }
  private static void start(Minecraft mc,Release420Checks.State saved,boolean backwards) {
    AutoWalk.stop();saved.keys.put(29,(byte)0);saved.keys.put(32,(byte)0);saved.keys.put(31,(byte)(backwards?1:0));saved.keys.put(117,(byte)0);AutoWalk.tick(mc);
    saved.keys.put(117,(byte)1);Bindings.keyPress(117);AutoWalk.tick(mc);saved.keys.put(117,(byte)0);Bindings.released(117);saved.keys.put(31,(byte)0);AutoWalk.tick(mc);
    check(AutoWalk.isWalking()&&AutoWalk.input()==(backwards?-1:1),"native Auto-walk activation failed");
  }
  private static void toggle(Release420Checks.State saved) {saved.keys.put(118,(byte)1);Bindings.keyPress(118);saved.keys.put(118,(byte)0);}
  private static void resetSprint(Minecraft mc) {
    SprintController.reset();var state=((SprintPlayer)mc.player).power$sprint();state.cancel(false);state.stamina=100;state.exhausted=false;state.combatTicks=state.rechargeTicks=0;
  }
  private static void travel(Minecraft mc) {
    mc.player.field_161.method_1942(mc.player);SprintController.movement(mc.player);
    mc.player.method_945(mc.player.field_161.field_2532,mc.player.field_161.field_2533);SprintController.movement(mc.player);
  }
  private static void place(Minecraft mc,double x,double feet,double z,float yaw)throws Exception {
    AutoWalk.stop();mc.player.method_1340(x,feet+mc.player.field_1631-mc.player.field_1640,z);mc.player.yaw=mc.player.prevYaw=yaw;mc.player.pitch=mc.player.prevPitch=-7;
    mc.player.prevX=mc.player.field_1637=mc.player.x;mc.player.prevY=mc.player.field_1638=mc.player.y;mc.player.prevZ=mc.player.field_1639=mc.player.z;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;mc.player.field_1623=true;mc.player.field_1624=false;mc.player.field_1642=false;
    field(mc.player.getClass(),"field_1636").setFloat(mc.player,0);ModernSwimming.preparePose(mc.player);
  }
  private static Field field(Class<?> type,String name)throws Exception {
    for(;type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(name);
  }
  private static final class WalkState implements AutoCloseable {
    final local.luke.power.building.config.Settings building=Config.current().copy();
    final List<Release420Checks.Fields> fields=new ArrayList<>();final Map<KeyBinding,Integer> keys=new LinkedHashMap<>();
    final double[] ratios;final boolean[] sprinting;
    WalkState(Minecraft mc)throws Exception {
      for(Object value:new Object[]{AutoWalk.class,field(AutoWalk.class,"TOGGLE").get(null),field(AutoWalk.class,"LINE_LOCK").get(null),((SwimPlayer)mc.player).power$swim(),SprintView.class})fields.add(new Release420Checks.Fields(value));
      ratios=((double[])field(SprintView.class,"ratios").get(null)).clone();sprinting=((boolean[])field(SprintView.class,"active").get(null)).clone();
      KeyBinding[] movement={mc.options.forwardKey,mc.options.backKey,mc.options.leftKey,mc.options.rightKey,mc.options.jumpKey,mc.options.sneakKey,AutoWalk.KEY,AutoWalk.LINE_LOCK_KEY};
      int[] defaults={17,31,30,32,57,42,117,118};for(int index=0;index<movement.length;index++){keys.put(movement[index],movement[index].code);movement[index].code=defaults[index];}
    }
    public void close()throws Exception {
      AutoWalk.stop();Config.preview(building);for(var entry:keys.entrySet())entry.getKey().code=entry.getValue();
      System.arraycopy(ratios,0,(double[])field(SprintView.class,"ratios").get(null),0,ratios.length);System.arraycopy(sprinting,0,(boolean[])field(SprintView.class,"active").get(null),0,sprinting.length);
      for(int index=fields.size()-1;index>=0;index--)fields.get(index).close();
    }
  }
}
