package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.bow.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.inventory.HandUse;
import local.luke.power.sprint.SprintController;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Native draw ownership, camera-height release origins and official FOV tick timing. */
final class Bow471Checks {
  private record Pose(String name, boolean crouched, boolean compact, boolean swimming,
      boolean lower, boolean roof, float physicalEye, float previousEye, float eye, float smoothing) {}
  private static final Pose[] POSES = {
      new Pose("standing",false,false,false,false,false,1.62f,1.62f,1.62f,0),
      new Pose("native sneak",false,false,false,false,false,1.62f,1.62f,1.62f,.08f),
      new Pose("crouch Beta camera",true,false,false,false,false,1.27f,1.62f,1.62f,.08f),
      new Pose("crouch modern camera",true,false,false,true,false,1.27f,1.27f,1.27f,0),
      new Pose("crouch easing",true,false,false,true,false,1.27f,1.62f,1.445f,0),
      new Pose("forced crouch",true,false,false,false,true,1.27f,1.27f,1.27f,0),
      new Pose("crawl",false,true,false,false,true,.4f,.4f,.4f,0),
      new Pose("swim",false,true,true,false,false,.4f,.4f,.4f,0),
      new Pose("standing exit",false,false,false,true,false,1.62f,1.27f,1.445f,0)
  };
  private Bow471Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world!=null && mc.player!=null && !mc.world.isRemote && mc.currentScreen==null
        && Display.isActive() && mc.options.soundVolume==0 && mc.options.musicVolume==0
        && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "focus the muted disposable singleplayer client and release both buttons");
    var settings=GameplayConfig.copy();
    var use=UseInput.class.getDeclaredField("held");use.setAccessible(true);var savedUse=(BooleanSupplier)use.get(null);
    var sprintProvider=SprintView.class.getDeclaredField("fov");sprintProvider.setAccessible(true);Object savedSprintProvider=sprintProvider.get(null);
    Map<Field,Object> sprintFov=new LinkedHashMap<>();
    for(String name:new String[]{"previousFov","fov","targetFov"}) {
      Field field=SprintController.class.getDeclaredField(name);field.setAccessible(true);sprintFov.put(field,field.get(null));
    }
    var state=((SwimPlayer)mc.player).power$swim();Map<Field,Object> poseState=new LinkedHashMap<>();
    for(Field field:SwimState.class.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){
      field.setAccessible(true);poseState.put(field,field.get(state));
    }
    float height=mc.player.spacingY,width=mc.player.spacingXZ,eye=mc.player.field_1631,smoothing=mc.player.field_1640;
    int swingTicks=mc.player.handSwingTicks;boolean swinging=mc.player.handSwinging;
    var cooldown=Minecraft.class.getDeclaredField("field_2787");cooldown.setAccessible(true);int savedCooldown=cooldown.getInt(mc);
    failures=0;
    try(var s=new OffhandActions400State(mc)) {
      UseInput.register(()->true);
      for(boolean off:new boolean[]{false,true}) {
        test("BOW471 draw owns press, held mining and entity attack off="+off,()->{
          s.reset();options(true,true);pose(mc,POSES[0]);
          ItemStack bow=OffhandActions400State.tagged(261,1,12,"draw-owner-471");
          s.hands(off?new ItemStack(274,1,0):bow,off?bow:null);mc.player.inventory.main[35]=new ItemStack(262,4,0);
          s.use();check(ModernBow.hand(mc.player)==(off?HandUse.Hand.OFF:HandUse.Hand.MAIN),"wrong draw owner");
          String[] before=s.items();s.nativeState.scene(31,1);cooldown.setInt(mc,0);
          mc.player.handSwinging=false;mc.player.handSwingTicks=0;
          ((Client253Access)(Object)mc).power$use(0);
          check(mc.world.getBlockId(s.nativeState.x,s.nativeState.y,s.nativeState.z)==31
              && !mc.player.handSwinging,"draw admitted native block click/swing");
          Field progress=SingleplayerInteractionManager.class.getDeclaredField("field_2184");progress.setAccessible(true);
          progress.setFloat(mc.interactionManager,.8f);
          for(int i=0;i<8;i++)((Client253Access)(Object)mc).power$heldMining(0,true);
          check(progress.getFloat(mc.interactionManager)==0 && mc.world.getBlockId(s.nativeState.x,s.nativeState.y,s.nativeState.z)==31,
              "draw retained mining progress or broke held target");
          Probe target=new Probe(mc.world);s.owned.add(target);mc.field_2823=new class_27(target);
          ((Client253Access)(Object)mc).power$use(0);
          check(target.damage==0 && Arrays.equals(before,s.items()) && ModernBow.using(mc.player,bow),
              "left click damaged target/items or canceled the draw");
          mc.world.isRemote=true;
          try{check(!ModernBow.ownsAttack(mc),"draw claimed remote Attack");}finally{mc.world.isRemote=false;}
          ModernBow.cancel();cooldown.setInt(mc,0);((Client253Access)(Object)mc).power$use(0);
          check(target.damage>0,"canceling draw did not restore native attack");
        });
      }
      for(Pose pose:POSES)for(boolean off:new boolean[]{false,true})for(boolean third:new boolean[]{false,true}) {
        test("BOW471 release eye "+pose.name+" off="+off+" third="+third,()->{
          s.reset();options(true,true);pose(mc,pose);mc.options.thirdPerson=third;
          ItemStack bow=new ItemStack(261,1,0),ammo=new ItemStack(262,4,0);
          s.hands(off?ammo:bow,off?bow:ammo);mc.field_2823=null;s.use();
          // Source eye Y is native player.y plus the selected camera's pose offset.
          double expectedEye=mc.player.y+(double)(pose.eye-pose.physicalEye);
          double x=mc.player.x,z=mc.player.z;
          for(int tick=ModernBow.ticks(mc.player,bow);tick<20;tick++)ModernBow.tick(mc);
          Set<Entity> before=entities(mc);ModernBow.release(mc);s.nativeState.finish();capture(s,before);
          class_410 arrow=(class_410)s.owned.stream().filter(entity->entity instanceof class_410&&!entity.dead).findFirst().orElseThrow();
          near(arrow.x,x,"launch X");near(arrow.z,z,"launch Z");
          near(arrow.y,expectedEye-(double).1f,"launch below selected first-person eye");
        });
      }
      test("BOW471 disabled native attack remains available",()->{
        s.reset();options(false,true);pose(mc,POSES[0]);s.hands(new ItemStack(261,1,0),new ItemStack(262,4,0));s.use();
        check(!ModernBow.ownsAttack(mc),"disabled modern bow owned Attack");
        s.nativeState.scene(31,1);cooldown.setInt(mc,0);((Client253Access)(Object)mc).power$use(0);
        check(mc.world.getBlockId(s.nativeState.x,s.nativeState.y,s.nativeState.z)==0,"disabled bow blocked native mining");
      });
      test("BOW471 exact FOV drawing, release, pause, toggle and reset",()->fov(s));
    } finally {
      ModernBow.cancel();GameplayConfig.preview(settings);UseInput.register(savedUse);
      sprintProvider.set(null,savedSprintProvider);for(var entry:sprintFov.entrySet())entry.getKey().set(null,entry.getValue());
      for(var entry:poseState.entrySet())entry.getKey().set(state,entry.getValue());
      mc.player.spacingY=height;mc.player.spacingXZ=width;mc.player.field_1631=eye;mc.player.field_1640=smoothing;
      mc.player.method_1340(mc.player.x,mc.player.y,mc.player.z);
      mc.player.handSwingTicks=swingTicks;mc.player.handSwinging=swinging;cooldown.setInt(mc,savedCooldown);
    }
    log("BOW471 checks complete failures="+failures);check(failures==0,"Bow471 failures="+failures);
  }

  private static void fov(OffhandActions400State s)throws Exception {
    var mc=s.mc;s.reset();options(true,true);pose(mc,POSES[0]);
    var field=ModernBow.class.getDeclaredField("fov");field.setAccessible(true);var filter=(BowFov)field.get(null);filter.reset();
    SprintController.reset();SprintView.configure(delta->1);
    ItemStack bow=new ItemStack(261,1,0);s.hands(bow,new ItemStack(262,4,0));s.use();
    float previous=1,current=1;
    for(int i=1;i<=25;i++) {
      ModernBow.tick(mc);int elapsed=ModernBow.ticks(mc.player,bow)-1;
      float draw=Math.min(elapsed/20f,1),target=1-draw*draw*.15f;
      previous=current;current+=(target-current)*.5f;
      ModernBow.tickFov(mc);
      for(float delta:new float[]{0,.25f,.5f,1})near(BowView.apply(70,delta),70*(previous+(current-previous)*delta),"source FOV draw tick "+i);
    }
    ModernBow.cancel();check(filter.active(),"cancel removed FOV before its release transition");
    mc.paused=true;float frozen=BowView.apply(70,.5f);ModernBow.tickFov(mc);near(BowView.apply(70,.5f),frozen,"paused FOV");mc.paused=false;
    for(int i=0;i<30;i++) {
      previous=current;current+=(1-current)*.5f;ModernBow.tickFov(mc);
      near(BowView.apply(70,.5f),70*(previous+(current-previous)*.5f),"source FOV release tick "+i);
    }
    check(!filter.active(),"settled release retained FOV work");
    for(boolean disableHandling:new boolean[]{false,true})for(boolean paused:new boolean[]{false,true}) {
      ModernBow.cancel();filter.reset();options(true,true);s.use();
      for(int i=0;i<10;i++){ModernBow.tick(mc);ModernBow.tickFov(mc);}
      check(filter.active() && BowView.apply(70,1)<70,"disable fixture did not establish bow FOV");
      options(!disableHandling,disableHandling);mc.paused=paused;
      // The render gate must bypass an already-active filter even before another tick.
      var provider=BowView.class.getDeclaredField("fov");provider.setAccessible(true);Object saved=provider.get(null);
      provider.set(null,(java.util.function.DoubleUnaryOperator)delta->{throw new AssertionError("disabled FOV evaluated its render provider");});
      try {
        for(float delta:new float[]{0,.5f,1})near(BowView.apply(70,delta),70,"immediate disabled render bypass");
        ModernBow.tickFov(mc);
        check(!filter.active(),"disabled preference retained camera samples paused="+paused);
        for(int i=0;i<3;i++){ModernBow.tickFov(mc);near(BowView.apply(70,.5f),70,"disabled FOV work");}
      } finally {provider.set(null,saved);mc.paused=false;}
    }
    options(true,true);ModernBow.cancel();s.use();ModernBow.tick(mc);ModernBow.tickFov(mc);
    TweakLifecycle.reset();check(!filter.active(),"world exit retained FOV samples");
  }

  private static void options(boolean modern,boolean fov) {
    var settings=GameplayConfig.copy();settings.modernBowHandling=modern;settings.modernBowFov=fov;settings.modernCrawling=true;GameplayConfig.preview(settings);
  }
  private static void pose(Minecraft mc,Pose pose) {
    var p=mc.player;var state=((SwimPlayer)p).power$swim();var settings=GameplayConfig.copy();settings.crouchCameraHeight=pose.lower;GameplayConfig.preview(settings);
    state.crouched=pose.crouched;state.compact=pose.compact;state.swimming=pose.swimming;state.lowCeiling=pose.roof;
    state.previousEyeHeight=pose.previousEye;state.eyeHeight=pose.eye;
    double feet=p.boundingBox.minY;p.spacingXZ=.6f;p.spacingY=pose.compact?.6f:pose.crouched?1.5f:1.8f;
    p.field_1631=pose.physicalEye;p.field_1640=pose.smoothing;
    p.method_1340(p.x,feet+pose.physicalEye-pose.smoothing,p.z);
    p.velocityX=p.velocityY=p.velocityZ=0;
  }
  private static void near(double actual,double expected,String label){check(Math.abs(actual-expected)<1e-5,label+" actual="+actual+" expected="+expected);}
  private static Set<Entity> entities(Minecraft mc){Set<Entity> out=Collections.newSetFromMap(new IdentityHashMap<>());for(Object entity:mc.world.field_198)out.add((Entity)entity);return out;}
  private static void capture(OffhandActions400State s,Set<Entity> before){for(Object entity:s.mc.world.field_198)if(!before.contains(entity))s.owned.add((Entity)entity);}
  private static final class Probe extends Entity {
    int damage;Probe(World world){super(world);}protected void method_1310(){}public void writeNbt(NbtCompound tag){}public void readtNbt(NbtCompound tag){}
    public boolean damage(Entity attacker,int amount){damage+=amount;return true;}
  }
}
