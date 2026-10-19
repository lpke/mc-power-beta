package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.death.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_41;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import org.lwjgl.input.Keyboard;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Actual transformed player ticks/collision/model/NBT, against independent 1.21.1 equations. */
final class Swimming300Checks {
  private static final int X=64,Z=64,Y=94;
  private static ByteBuffer keys;
  private static void near(double a,double b,double tolerance,String label) {
    check(Math.abs(a-b)<=tolerance,label+": "+a+" expected "+b);
  }
  private static Field field(Class<?> type,String name)throws Exception {
    for(;type!=null;type=type.getSuperclass())try { var f=type.getDeclaredField(name);f.setAccessible(true);return f; }
    catch(NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static void permissions(Minecraft mc,boolean allowed) {
    var rules=AccessRules.copy(mc);rules.set(GameRule.ALLOW_SPRINTING,allowed);AccessRules.restore(mc,rules);
  }
  private static void modern(boolean enabled) {
    var config=GameplayConfig.copy();config.modernWaterMovement=enabled;GameplayConfig.preview(config);
  }
  private static void reset(Minecraft mc,float pitch)throws Exception {
    var p=mc.player;var swim=((SwimPlayer)p).power$swim();
    swim.swimming=swim.compact=swim.crouched=false;swim.previousAmount=swim.amount=0;
    swim.resetCamera(1.62f);
    swim.waterDepth=0;swim.waterJumpTicks=0;
    p.spacingXZ=.6f;p.spacingY=1.8f;p.field_1631=1.62f;p.field_1640=0;
    if(p.field_1595!=null)p.method_1376(null);
    p.health=20;p.dead=false;p.air=300;p.method_1341(X+.5,Y,Z+.5,0,pitch);
    p.prevX=p.x;p.prevY=p.y;p.prevZ=p.z;p.prevPitch=pitch;p.prevYaw=0;
    p.velocityX=p.velocityY=p.velocityZ=0;p.field_1623=p.field_1624=false;
    p.field_161=new class_41();mc.field_2807=p;mc.field_2778=true;mc.paused=false;
    field(Entity.class,"field_1612").setBoolean(p,false);
    ((SprintPlayer)p).power$sprint().cancel(false);SprintController.reset();
  }
  private static void tick(Minecraft mc,float forward,boolean jump,boolean descend) {
    mc.player.air=300;mc.player.field_161.field_2533=forward;
    mc.player.field_161.field_2535=jump;mc.player.field_161.field_2536=descend;mc.player.tick();
  }
  private static void pool(Minecraft mc) {
    for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+30;z++)for(int y=80;y<=102;y++)
      mc.world.method_154(x,y,z,y==80?1:y<=100?9:0,0);
  }
  static void run(Minecraft mc)throws Exception {
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&mc.player.health>0,"open a disposable singleplayer world");
    check(Display.isActive()&&!DetachedCamera.isActive(),"focus the muted disposable client outside freecam");
    failures=0;var p=mc.player;
    var saved=new NbtCompound();p.write(saved);var originalInput=p.field_161;
    var gameplay=GameplayConfig.copy();var sprint=SprintConfig.copy();var rules=AccessRules.copy(mc);
    var previousScreen=mc.currentScreen;boolean paused=mc.paused;
    byte[] config=Files.readAllBytes(PowerConfig.path());
    keys=(ByteBuffer)field(Keyboard.class,"keyDownBuffer").get(null);byte[] oldKeys=new byte[keys.capacity()];
    for(int i=0;i<oldKeys.length;i++){oldKeys[i]=keys.get(i);keys.put(i,(byte)0);}
    var modifiers=Bindings.modifiers();Map<KeyBinding,Integer> codes=new LinkedHashMap<>();
    for(Object object:(Object[])field(Bindings.class,"registered").get(null))
      if(object instanceof Binding binding&&binding.power$id().equals("powerbeta.sprint")) {
        var key=(KeyBinding)object;codes.put(key,key.code);key.code=29;
      }
    Bindings.configure(Map.of());
    String originalMode=(String)Editor230Checks.invoke(Editor230Checks.invoke(p,"power_mode",new Class<?>[0]),"name",new Class<?>[0]);
    var blocks=new ArrayList<int[]>();
    try {
      mc.setScreen(null);AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,"SURVIVAL");
      check(mc.world.method_155(X-6,0,Z-6,X+6,127,Z+33),"swim fixture requires the already loaded scene chunks");
      for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+30;z++)for(int y=80;y<=102;y++)
        blocks.add(new int[]{x,y,z,mc.world.getBlockId(x,y,z),mc.world.method_1778(x,y,z)});
      pool(mc);
      var settings=SprintConfig.copy();settings.activation=1;settings.doubleTap=false;settings.stamina=false;
      settings.noSprintInCombat=false;settings.speed=130;settings.particles=true;settings.jumpBoost=true;SprintConfig.preview(settings);
      test("all four modern-water / sprint-rule combinations use the independent native reference",()->{
        for(boolean water:new boolean[]{false,true})for(boolean allow:new boolean[]{false,true}) {
          modern(water);permissions(mc,allow);reset(mc,0);keys.put(29,(byte)1);
          double startZ=p.z,startFeet=p.boundingBox.minY,vz=0,vy=0,dz=0,dy=0;
          boolean swim=water&&allow;
          for(int tick=0;tick<60;tick++) {
            if(swim)vy+=(0-vy)*.06;
            vz+=.02f*.98f;dz+=vz;dy+=vy;vz*=swim?.9f:.8f;vy*=.8f;
            if(!swim)vy-=water?.005:.02;
            tick(mc,1,false,false);
          }
          near(p.z-startZ,dz,2e-5,"horizontal displacement water="+water+" rule="+allow);
          near(p.boundingBox.minY-startFeet,dy,2e-5,"vertical displacement water="+water+" rule="+allow);
          near(p.velocityZ,vz,1e-6,"horizontal velocity");near(p.velocityY,vy,1e-6,"vertical velocity");
          check(ModernSwimming.swimming(p)==swim&&ModernSwimming.compact(p)==swim,"wrong native swimming/pose");
          near(p.spacingY,swim?.6:1.8,1e-6,"native box height");
          check(p.field_1049>.1&&Float.isFinite(p.field_1048),"native limb interpolation stopped");
          log("SWIM reference modern="+water+" rule="+allow+" distance="+(p.z-startZ)+" velocity="+p.velocityZ);
        }
      });
      test("pitch-directed dives, ascent, Jump and descend match official Player.travel",()->{
        modern(true);permissions(mc,true);keys.put(29,(byte)1);
        for(float pitch:new float[]{-45,0,45})for(int control=0;control<3;control++) {
          reset(mc,pitch);double feet=p.boundingBox.minY,velocity=0,distance=0;
          boolean jump=control==1,descend=control==2;
          for(int step=0;step<16;step++) {
            if(jump)velocity+=(double).04f;if(descend)velocity-=.04;
            double look=-Math.sin(pitch*Math.PI/180);velocity+=(look-velocity)*(look<-.2?.085:.06);
            distance+=velocity;velocity*=.8f;tick(mc,1,jump,descend);
          }
          near(p.boundingBox.minY-feet,distance,2e-5,"look/control displacement "+pitch+"/"+control);
          near(p.velocityY,velocity,2e-6,"look/control velocity");
        }
      });
      test("one-block tunnel uses native movement and keeps collision on both sides",()->{
        reset(mc,0);tick(mc,1,false,false);
        for(int z=Z+2;z<=Z+10;z++)for(int x=X-1;x<=X+1;x++) {
          mc.world.method_154(x,Y-1,z,1,0);mc.world.method_154(x,Y+1,z,1,0);
          if(x!=X)mc.world.method_154(x,Y,z,1,0);
        }
        for(int step=0;step<45;step++)tick(mc,1,false,false);
        check(p.z>Z+6&&p.z<Z+11,"native swimmer did not traverse the one-block tunnel");
        check(p.x>=X+.3&&p.x<=X+.7&&p.boundingBox.maxY<Y+1,"small collision box escaped tunnel");
        p.velocityX=.7;p.move(.7,0,0);check(p.x<=X+.70001,"native lateral collision bypassed wall");
        permissions(mc,false);SprintController.rulesChanged(mc);
        check(!((SprintPlayer)p).power$sprint().sprinting&&!ModernSwimming.swimming(p),"permission revocation retained swimming");
        check(ModernSwimming.compact(p)&&p.boundingBox.maxY<Y+1,"permission revocation stood into roof");
        var snapshot=new NbtCompound();p.write(snapshot);double feet=p.boundingBox.minY,x=p.x,z=p.z;
        p.read(snapshot);near(p.boundingBox.minY,feet,1e-6,"native save/reload feet");
        check(ModernSwimming.compact(p)&&!ModernSwimming.swimming(p),"reload lost safe compact pose or revived sprint");
        near(p.x,x,1e-8,"reload X");near(p.z,z,1e-8,"reload Z");
        modern(false);ModernSwimming.tick(p);check(ModernSwimming.compact(p),"live global disable stood into roof");
        p.method_1341(X+.5,Y,Z+13.5,0,0);ModernSwimming.tick(p);
        check(!ModernSwimming.lowPose(p),"clear exit did not restore standing");near(p.spacingY,1.8,1e-6,"standing height");
        pool(mc);modern(true);permissions(mc,true);
      });
      test("surface continuation, start gate and shallow crouched exits use native fluid/roof geometry",()->{
        reset(mc,-45);tick(mc,1,false,false);p.method_1341(X+.5,100.65,Z+.5,0,-45);p.velocityY=0;
        tick(mc,1,false,false);check(ModernSwimming.swimming(p),"surface stopped an established swim");
        near(p.velocityY,0,1e-7,"look-up pulled swimmer above an empty surface without Jump");
        keys.put(29,(byte)0);tick(mc,0,false,false);check(!ModernSwimming.swimming(p),"release retained swimming");
        reset(mc,0);p.method_1341(X+.5,100.5,Z+.5,0,0);keys.put(29,(byte)1);tick(mc,1,false,false);
        check(!ModernSwimming.swimming(p),"head above water started swimming");
        reset(mc,0);tick(mc,1,false,false);
        for(int x=X-1;x<=X+1;x++)for(int z=Z-1;z<=Z+1;z++)mc.world.method_154(x,Y+2,z,1,0);
        p.method_1341(X+.5,Y+.5,Z+.5,0,0);keys.put(29,(byte)0);tick(mc,0,false,false);
        check(ModernSwimming.crouched(p)&&!ModernSwimming.compact(p),"1.5-block exit did not select crouch");
        near(p.spacingY,1.5,1e-6,"crouched collision");near(p.field_1631,1.27,1e-6,"crouched eye");pool(mc);
      });
      test("real model and armor strokes reset across renders without changing other bipeds",()->models(mc));
      test("native camera matrices follow modern eye easing, bobbing, perspectives and freecam entry",()->camera(mc));
      test("flowing water, collision, input stop, menus and separate player/mode owners",()->boundaries(mc));
      test("native entry splash/bubbles remain and swimming never emits terrain sprint dust",()->particles(mc));
      test("modern settling and grounded shallow-water jumps retain ordinary/native exclusions",()->shallow(mc));
      test("land sprint acceleration, takeoff impulse and glancing-wall handling match modern reference",()->land(mc));
      test("compact deaths retain native body/save coordinates and kept recovery snapshots",()->death(mc));
      test("swimming data and settings stay local and do not write global preferences",()->
          check(Arrays.equals(config,Files.readAllBytes(PowerConfig.path())),"swim fixture wrote global config"));
    } finally {
      FreecamPreviewChecks.active(false);mc.world.isRemote=false;
      for(int[] b:blocks)mc.world.method_154(b[0],b[1],b[2],b[3],b[4]);
      AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,originalMode);
      GameplayConfig.preview(gameplay);SprintConfig.preview(sprint);AccessRules.restore(mc,rules);
      p.read(saved);p.field_161=originalInput;mc.paused=paused;mc.setScreen(previousScreen);
      for(int i=0;i<oldKeys.length;i++)keys.put(i,oldKeys[i]);
      for(var e:codes.entrySet())e.getKey().code=e.getValue();Bindings.configure(modifiers);
      SprintController.reset();Release252Checks.checkingUnloaded=false;
    }
    log("SWIMMING 3.0.0 FAILURES "+failures);
  }

  private static void death(Minecraft mc)throws Exception {
    var p=mc.player;
    var saved=new NbtCompound();p.write(saved);var inventory=KeptInventory.capture(p);
    var kept=((KeptPlayer)p).power$keptInventory();var deathSettings=DeathConfig.copy();
    var rules=AccessRules.copy(mc);boolean dead=p.dead;
    boolean deathAnimation=field(LivingEntity.class,"field_1045").getBoolean(p);
    try {
      DeathConfig.preview(DeathChecks.disabled());
      double nativeY=0,nativeFeet=0,nativeSavedY=0;
      for(int pose=0;pose<3;pose++) {
        var probe=new ClientPlayerEntity(mc,mc.world,mc.session,p.dimensionId);
        var swim=((SwimPlayer)probe).power$swim();
        swim.compact=pose==1;swim.crouched=pose==2;swim.swimming=pose==1;
        swim.previousAmount=swim.amount=1;swim.waterDepth=2;swim.waterJumpTicks=7;
        probe.spacingXZ=.6f;probe.spacingY=pose==1?.6f:pose==2?1.5f:1.8f;
        probe.field_1631=pose==1?.4f:pose==2?1.27f:1.62f;
        probe.method_1341(X+.5,Y,Z+.5,0,0);probe.health=0;probe.method_938(null);
        var tag=new NbtCompound();probe.write(tag);
        double savedY=((NbtDouble)tag.getList("Pos").get(1)).value;
        if(pose==0) { nativeY=probe.y;nativeFeet=probe.boundingBox.minY;nativeSavedY=savedY; }
        near(probe.y,nativeY,1e-7,"death origin pose "+pose);
        near(probe.boundingBox.minY,nativeFeet,1e-7,"death feet pose "+pose);
        near(probe.boundingBox.minY,Y,1e-7,"physical death feet pose "+pose);
        near(savedY,nativeSavedY,1e-7,"immediate dead save pose "+pose);
        near(probe.spacingY,.2,1e-7,"native death height");near(probe.field_1631,.1,1e-7,"native death eye");
        check(!ModernSwimming.lowPose(probe)&&!swim.swimming&&swim.amount==0
            &&swim.waterDepth==0&&swim.waterJumpTicks==0,"death retained transient swim state");
        check(!tag.getBoolean("PowerBetaSwimPose")&&!tag.getBoolean("PowerBetaSwimCrouch"),"dead save retained pose tags");
        ModernSwimming.tick(probe);var later=new NbtCompound();probe.write(later);
        near(((NbtDouble)later.getList("Pos").get(1)).value,nativeSavedY,1e-7,"later dead save pose "+pose);
        // Reusing an entity for read must clear stale flags before the dead-health return.
        swim.compact=swim.crouched=swim.swimming=true;swim.amount=1;swim.waterJumpTicks=7;
        probe.read(tag);
        check(!ModernSwimming.lowPose(probe)&&!swim.swimming&&swim.amount==0&&swim.waterJumpTicks==0,
            "dead read retained stale pose");
        near(probe.spacingY,.2,1e-7,"dead read changed native body height");
        var replacement=new ClientPlayerEntity(mc,mc.world,mc.session,p.dimensionId);replacement.read(tag);
        near(replacement.y,nativeSavedY,1e-7,"fresh dead-player load pose "+pose);
        near(replacement.boundingBox.minY,nativeSavedY-1.62f,1e-7,"fresh dead-player feet pose "+pose);
      }
      reset(mc,0);modern(true);permissions(mc,true);keys.put(29,(byte)1);tick(mc,1,false,false);
      check(ModernSwimming.compact(p),"kept-death setup did not swim");
      DeathChecks.clear(mc);
      p.inventory.main[8]=DeathChecks.tagged(35,201,5,"swim-death-main");
      p.inventory.armor[3]=DeathChecks.tagged(310,1,27,"swim-death-armor");
      p.inventory.setCursorStack(DeathChecks.tagged(264,3,0,"swim-death-cursor"));
      p.playerContainer.method_2084(1).setStack(DeathChecks.tagged(35,2,5,"swim-death-craft"));
      var before=KeptInventory.capture(p);var expected=new DeathRecord();DeathItems.capture(p,expected);
      var selected=AccessRules.copy(mc);selected.set(GameRule.KEEP_INVENTORY,true);
      selected.set(GameRule.ALLOW_DEATH_RECOVERY,false);AccessRules.restore(mc,selected);
      var enabled=DeathChecks.disabled();enabled.enabled=true;enabled.announceItems=enabled.announceLocation=false;
      DeathConfig.preview(enabled);int records=DeathTracker.journal(mc).records().size();
      long drops=mc.world.field_198.stream().filter(e->e instanceof net.minecraft.class_142 item&&!item.dead).count();
      double feet=p.boundingBox.minY;p.health=0;p.method_938(null);
      var record=DeathChecks.latest(mc);
      check(DeathTracker.journal(mc).records().size()==records+1&&record.y==(int)Math.floor(feet),"death HEAD snapshot moved feet");
      check(record.kept&&record.items.equals(expected.items)&&record.slots.equals(expected.slots),"death HEAD lost full item data/slots");
      check(DeathTracker.journal(mc).state(record.id).equals("kept")&&before.matches(p),"disabled recovery changed kept death");
      check(mc.world.field_198.stream().filter(e->e instanceof net.minecraft.class_142 item&&!item.dead).count()==drops,"kept swim death created drops");
      var tag=new NbtCompound();p.write(tag);near(((NbtDouble)tag.getList("Pos").get(1)).value,feet+1.62f,1e-7,"kept dead save origin");
      var restored=new ClientPlayerEntity(mc,mc.world,mc.session,p.dimensionId);restored.read(tag);
      check(KeepInventory.kept(restored)&&before.matches(restored)&&!ModernSwimming.lowPose(restored),"dead reload lost kept full counts/NBT or retained pose");
    } finally {
      DeathConfig.preview(deathSettings);AccessRules.restore(mc,rules);
      p.read(saved);inventory.restore(p);((KeptPlayer)p).power$keptInventory(kept);p.dead=dead;
      field(LivingEntity.class,"field_1045").setBoolean(p,deathAnimation);
      // Native death owns horizontal shrink; an alive read restores only swim height.
      p.spacingXZ=.6f;p.method_1340(p.x,p.y,p.z);
    }
  }

  private static void models(Minecraft mc)throws Exception {
    modern(true);permissions(mc,true);reset(mc,0);keys.put(29,(byte)1);
    for(int i=0;i<12;i++)tick(mc,1,false,false);
    var model=new BipedEntityModel();((SwimmingModel)model).power$swimPose(1,true);
    for(float phase:new float[]{0,7,14,18,22,24}) {
      model.setAngles(phase,1,0,0,30,.0625f);
      near(model.head.pitch,-Math.PI/4,1e-6,"native head pitch");
      double pitch=phase<14?0:phase<22?Math.PI/2*(phase-14)/8:Math.PI/2*(1-(phase-22)/4);
      near(model.leftArm.pitch,pitch,1e-6,"native left stroke");near(model.rightArm.pitch,pitch,1e-6,"native right stroke");
      near(model.rightLeg.pitch,.3*Math.cos(phase/3),1e-6,"native leg flutter");
      near(model.leftLeg.pitch,-.3*Math.cos(phase/3),1e-6,"opposing leg flutter");
    }
    ((SwimmingModel)model).power$swimPose(0,false);model.setAngles(0,0,0,0,30,.0625f);
    near(model.head.pitch,Math.PI/6,1e-6,"model state release");
    var renderer=new PlayerEntityRenderer(){@Override protected boolean method_2027(String url,String fallback){method_2026("/mob/char.png");return true;}};
    renderer.setDispatcher(EntityRenderDispatcher.field_2489);
    EntityRenderDispatcher.field_2489.method_1917(mc.world,mc.textureManager,mc.textRenderer,mc.player,mc.options,1);
    var armor=pArmor(mc);try {
      for(int i=0;i<4;i++)mc.player.inventory.armor[i]=new ItemStack(309-i,1,0);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
      try { renderer.render(mc.player,0,0,-5,0,1); }
      finally { GL11.glPopMatrix();GL11.glPopAttrib(); }
      int modelCount=0;
      for(var member:PlayerEntityRenderer.class.getDeclaredFields())if(member.getType()==BipedEntityModel.class) {
        member.setAccessible(true);var rendered=(BipedEntityModel)member.get(renderer);modelCount++;
        near(rendered.head.pitch,-Math.PI/4,1e-6,"native player/armor render "+member.getName());
        rendered.setAngles(0,0,0,0,30,.0625f);near(rendered.head.pitch,Math.PI/6,1e-6,"renderer leaked pose "+member.getName());
      }
      check(modelCount==3,"native renderer player/armor models were not all checked");
    } finally { System.arraycopy(armor,0,mc.player.inventory.armor,0,armor.length); }
    check(GL11.glGetError()==GL11.GL_NO_ERROR,"native swim render left a GL error");
  }
  private static ItemStack[] pArmor(Minecraft mc) { return mc.player.inventory.armor.clone(); }

  private static float[] cameraMatrix(Minecraft mc,float delta,int eye)throws Exception {
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      var method=class_555.class.getDeclaredMethod("method_1840",float.class,int.class);
      method.setAccessible(true);method.invoke(mc.field_2818,delta,eye);
      var p=mc.player;
      GL11.glTranslated(-(p.prevX+(p.x-p.prevX)*delta),-(p.prevY+(p.y-p.prevY)*delta),-(p.prevZ+(p.z-p.prevZ)*delta));
      FloatBuffer buffer=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,buffer);
      float[] result=new float[16];buffer.get(result);return result;
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
  }

  private static void camera(Minecraft mc)throws Exception {
    boolean bob=mc.options.bobView,third=mc.options.thirdPerson,stereo=mc.options.anaglyph3d;
    Object freecam=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    var active=freecam.getClass().getMethod("setActive",boolean.class);
    var start=freecam.getClass().getMethod("startFromView",Minecraft.class);
    try {
      pool(mc);modern(true);permissions(mc,true);keys.put(29,(byte)1);reset(mc,0);
      var p=mc.player;var state=((SwimPlayer)p).power$swim();
      p.hurtTime=0;p.field_1041=0;p.field_504=p.field_505=0;
      mc.options.bobView=mc.options.thirdPerson=mc.options.anaglyph3d=false;
      float eye=1.62f;
      for(int step=0;step<16;step++) {
        if(step==8)keys.put(29,(byte)0);
        tick(mc,step<8?1:0,false,false);
        float previous=eye;eye+=((step<8?.4f:1.62f)-eye)*.5f;
        for(float delta:new float[]{0,.25f,.5f,1}) {
          double y=p.y,feet=p.boundingBox.minY;
          cameraMatrix(mc,delta,0);check(RenderedView.valid(mc.world,p),"camera matrix was not captured");
          double interpolatedFeet=p.prevY+(p.y-p.prevY)*delta-p.field_1631;
          near(RenderedView.y()-interpolatedFeet,previous+(eye-previous)*delta,3e-6,"modern rendered eye trace "+step+"/"+delta);
          near(p.y,y,0,"camera changed native origin");near(p.boundingBox.minY,feet,0,"camera changed physical feet");
        }
      }
      reset(mc,0);keys.put(29,(byte)1);tick(mc,1,false,false);
      p.field_1634=3.1f;p.field_1635=3.4f;p.field_524=.05f;p.field_525=.08f;
      p.field_1043=.1f;p.field_1044=.2f;p.hurtTime=0;p.field_1041=0;p.field_504=p.field_505=0;
      for(boolean viewBob:new boolean[]{false,true})for(boolean perspective:new boolean[]{false,true})for(boolean anaglyph:new boolean[]{false,true}) {
        mc.options.bobView=viewBob;mc.options.thirdPerson=perspective;mc.options.anaglyph3d=anaglyph;
        for(int eyeIndex=0;eyeIndex<(anaglyph?2:1);eyeIndex++) {
          float previous=state.previousEyeHeight,current=state.eyeHeight;
          float[] eased=cameraMatrix(mc,.5f,eyeIndex);double easedY=RenderedView.y();
          state.resetCamera(p.field_1631);cameraMatrix(mc,.5f,eyeIndex);
          near(easedY-RenderedView.y(),(previous+current)*.5f-p.field_1631,3e-6,"bobbing/perspective eye offset");
          state.previousEyeHeight=previous;state.eyeHeight=current;
          cameraMatrix(mc,.5f,eyeIndex);start.invoke(freecam,mc);active.invoke(freecam,true);
          float[] detached=cameraMatrix(mc,.5f,eyeIndex);
          for(int i=0;i<16;i++)near(detached[i],eased[i],.0002,"swimming freecam handoff matrix "+i);
          active.invoke(freecam,false);
        }
      }
      check(GL11.glGetError()==GL11.GL_NO_ERROR,"camera trace left a GL error");
    } finally {
      active.invoke(freecam,false);mc.options.bobView=bob;mc.options.thirdPerson=third;mc.options.anaglyph3d=stereo;
    }
  }

  private static void boundaries(Minecraft mc)throws Exception {
    pool(mc);modern(true);permissions(mc,true);keys.put(29,(byte)1);reset(mc,0);
    for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+5;z++)for(int y=90;y<=100;y++)
      mc.world.method_154(x,y,z,8,Math.max(0,Math.min(7,z-Z+3)));
    tick(mc,1,false,false);check(ModernSwimming.swimming(mc.player),"flowing water cannot start swimming");
    pool(mc);reset(mc,0);tick(mc,1,false,false);mc.player.field_1624=true;tick(mc,1,false,false);
    check(ModernSwimming.swimming(mc.player),"horizontal contact cancelled swimming");
    tick(mc,0,false,false);check(!ModernSwimming.swimming(mc.player),"forward stop did not cancel swimming");
    reset(mc,0);tick(mc,1,false,false);mc.currentScreen=new net.minecraft.client.gui.screen.Screen(){};
    SprintController.movement(mc.player);check(!ModernSwimming.swimming(mc.player),"menu kept swimming active");mc.currentScreen=null;
    reset(mc,0);tick(mc,1,false,false);FreecamPreviewChecks.active(true);SprintController.movement(mc.player);
    check(!ModernSwimming.swimming(mc.player)&&!ModernWater.eligible(mc.player),"freecam retained player swim physics");FreecamPreviewChecks.active(false);
    for(String mode:new String[]{"CREATIVE","SPECTATOR"}) {
      AccessRules.restore(mc,RuleSet.cheats());permissions(mc,true);CreativeVehicleChecks.mode(mc,mode);
      if(mode.equals("CREATIVE"))Editor230Checks.invoke(mc.player,"creative_setFlying",new Class<?>[]{boolean.class},true);
      check(!ModernWater.eligible(mc.player),"flight mode admitted swimming "+mode);
    }
    CreativeVehicleChecks.mode(mc,"SURVIVAL");Editor230Checks.invoke(mc.player,"creative_setFlying",new Class<?>[]{boolean.class},false);
    reset(mc,0);tick(mc,1,false,false);
    // Native bed entry on the client path avoids changing the disposable world's clock.
    mc.world.isRemote=true;
    try { check(mc.player.method_495(X,Y,Z)==net.minecraft.class_141.OK,"native bed entry failed"); }
    finally { mc.world.isRemote=false; }
    try {
      double sleepingY=mc.player.y,sleepingFeet=mc.player.boundingBox.minY;
      SprintController.movement(mc.player);
      check(!ModernWater.eligible(mc.player)&&!ModernSwimming.lowPose(mc.player),"bed retained swimming ownership");
      near(mc.player.spacingY,.2,1e-6,"swimming replaced native bed height");
      near(mc.player.field_1631,.2,1e-6,"swimming replaced native bed eye");
      near(mc.player.y,sleepingY,0,"swimming moved sleeping origin");
      near(mc.player.boundingBox.minY,sleepingFeet,0,"swimming moved sleeping feet");
    } finally { mc.player.method_497(true,false,false); }
    ModernSwimming.tick(mc.player);
    near(((SwimPlayer)mc.player).power$swim().cameraEye(.5f),1.62,1e-6,"waking inherited swim camera easing");
    reset(mc,0);mc.world.isRemote=true;try { tick(mc,1,false,false);check(!ModernSwimming.swimming(mc.player),"multiplayer granted swimming"); }
    finally { mc.world.isRemote=false; }
    var mob=new LivingEntity(mc.world){};check(!ModernWater.eligible(mob),"mob admitted player physics");
    var other=new net.minecraft.entity.player.ClientPlayerEntity(mc,mc.world,mc.session,mc.player.dimensionId);
    check(!ModernWater.eligible(other)&&!ModernSwimming.lowPose(other),"new player inherited swim pose");
    reset(mc,0);tick(mc,1,false,false);mc.player.method_1340(31_999_900,94,31_999_900);
    permissions(mc,false);Release252Checks.chunkRequests=0;Release252Checks.checkingUnloaded=true;
    try { ModernSwimming.cancel(mc.player);check(!ModernSwimming.eyesInWater(mc.player),"unloaded eyes returned water"); }
    finally { Release252Checks.checkingUnloaded=false; }
    check(Release252Checks.chunkRequests==0,"pose/fluid check loaded a chunk");
  }

  private static void particles(Minecraft mc)throws Exception {
    pool(mc);modern(true);permissions(mc,true);reset(mc,0);keys.put(29,(byte)1);
    field(Entity.class,"field_1649").setBoolean(mc.player,false);mc.player.velocityY=-.1;
    boolean oldBubble=Release175Checks.capture,oldSplash=Release173Checks.capture,oldDust=Release260Checks.captureDust;
    Entity oldEntity=Release173Checks.splashEntity;int oldBubbles=Release175Checks.bubbles,oldSplashes=Release173Checks.splashes;
    try {
      Release175Checks.capture=Release173Checks.capture=Release260Checks.captureDust=true;
      Release173Checks.splashEntity=mc.player;Release175Checks.bubbles=Release173Checks.splashes=0;Release260Checks.dust.clear();
      tick(mc,1,false,false);
      check(Release175Checks.bubbles>0&&Release173Checks.splashes>0,"native water entry particles/sound disappeared");
      for(int i=0;i<8;i++)tick(mc,1,false,false);
      check(Release260Checks.dust.isEmpty(),"underwater sprint emitted terrain particles");
    } finally {
      Release175Checks.capture=oldBubble;Release173Checks.capture=oldSplash;Release260Checks.captureDust=oldDust;
      Release173Checks.splashEntity=oldEntity;Release175Checks.bubbles=oldBubbles;Release173Checks.splashes=oldSplashes;
    }
  }

  private static void land(Minecraft mc)throws Exception {
    pool(mc);modern(true);permissions(mc,true);keys.put(29,(byte)1);
    for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+30;z++)for(int y=Y-1;y<=102;y++)
      mc.world.method_154(x,y,z,y==Y-1?1:0,0);
    reset(mc,0);mc.player.field_1623=true;mc.player.velocityY=-.08*(double).98f;
    double velocity=0,distance=0,start=mc.player.z;
    float friction=.6f*.91f;
    float modernAcceleration=.13f*(.21600002f/(.6f*.6f*.6f));
    for(int i=0;i<40;i++) {
      velocity+=modernAcceleration*.98f;distance+=velocity;velocity*=friction;
      tick(mc,1,false,false);
    }
    near(mc.player.z-start,distance,3e-5,"modern land sprint displacement");
    near(mc.player.velocityZ,velocity,2e-6,"modern land sprint terminal velocity");
    double before=mc.player.velocityZ;tick(mc,1,true,false);
    near(mc.player.velocityZ,(before+.2+modernAcceleration*.98f)*friction,2e-6,"takeoff sprint impulse/order");
    near(mc.player.velocityY,((double).42f-.08)*(double).98f,1e-8,"native takeoff vertical response");
    before=mc.player.velocityZ;tick(mc,1,false,false);
    near(mc.player.velocityZ,(before+(double).025999999f*.98f)*.91f,2e-6,"modern airborne steering");
    reset(mc,0);mc.player.field_1623=true;mc.player.velocityY=-.08*(double).98f;mc.player.method_1341(X+.69,Y,Z+.5,0,0);
    for(int z=Z-2;z<=Z+20;z++)for(int y=Y;y<=Y+1;y++)mc.world.method_154(X+1,y,z,1,0);
    boolean touched=false;
    for(int i=0;i<24;i++) {
      mc.player.field_161.field_2532=.1f;tick(mc,1,false,false);
      touched|=mc.player.field_1624;
      check(((SprintPlayer)mc.player).power$sprint().sprinting,"glancing wall cancelled native sprint");
    }
    check(touched,"glancing fixture never touched its native wall");
    mc.player.field_161.field_2532=1;tick(mc,1,false,false);tick(mc,1,false,false);
    check(!((SprintPlayer)mc.player).power$sprint().sprinting,"large wall contact retained sprint");
    pool(mc);
  }

  private static void shallow(Minecraft mc)throws Exception {
    pool(mc);permissions(mc,false);keys.put(29,(byte)0);
    for(boolean enabled:new boolean[]{false,true})for(double initial:new double[]{-.00301,-.003,-.00299,.00299,.003,.00301}) {
      modern(enabled);reset(mc,0);var p=mc.player;
      double x=p.x,y=p.boundingBox.minY,z=p.z;
      p.velocityX=p.velocityY=initial;p.velocityZ=-initial;
      tick(mc,0,false,false);
      double expected=enabled&&Math.abs(initial)<.003?0:initial;
      near(p.x-x,expected,1e-7,"native water X settling");
      near(p.boundingBox.minY-y,expected,1e-7,"native water Y settling");
      near(p.z-z,-expected,1e-7,"native water Z settling");
    }
    for(int metadata:new int[]{4,5}) {
      for(int x=X-3;x<=X+3;x++)for(int z=Z-3;z<=Z+30;z++)for(int y=Y-1;y<=102;y++)
        mc.world.method_154(x,y,z,y==Y-1?1:y==Y?8:0,y==Y?metadata:0);
      for(boolean enabled:new boolean[]{false,true}) {
        modern(enabled);reset(mc,0);var p=mc.player;p.field_1623=true;
        double feet=p.boundingBox.minY;tick(mc,0,true,false);
        double expected=enabled&&metadata==5?(double).42f:(double).04f;
        near(p.boundingBox.minY-feet,expected,1e-7,"grounded water jump height metadata="+metadata);
        near(p.velocityY,expected*(double).8f-(enabled?.005:.02),1e-7,"water jump drag/gravity");
        // Native fluid surface arithmetic rounds block Y plus the fluid height to float.
        if(enabled)near(((SwimPlayer)p).power$swim().waterDepth,(double)(Y+1-(metadata+1)/9f)-feet-.001,1e-6,"sampled native water depth");
        if(enabled&&metadata==5) {
          check(((SwimPlayer)p).power$swim().waterJumpTicks==10,"modern shallow jump delay missing");
          for(int i=1;i<=10;i++) {
            p.method_1341(X+.5,Y,Z+.5,0,0);p.field_1623=true;p.velocityX=p.velocityY=p.velocityZ=0;
            tick(mc,0,true,false);
            near(p.boundingBox.minY-Y,i<10?0:(double).42f,1e-7,"shallow jump repeat delay "+i);
          }
          p.method_1341(X+.5,Y,Z+.5,0,0);p.field_1623=true;p.velocityX=p.velocityY=p.velocityZ=0;
          tick(mc,0,false,false);
          check(((SwimPlayer)p).power$swim().waterJumpTicks==0,"Jump release kept shallow repeat delay");
          p.method_1341(X+.5,Y,Z+.5,0,0);p.field_1623=true;p.velocityX=p.velocityY=p.velocityZ=0;
          tick(mc,0,true,false);
          near(p.boundingBox.minY-Y,(double).42f,1e-7,"fresh shallow-water Jump was delayed");
        }
      }
      modern(true);reset(mc,0);double feet=mc.player.boundingBox.minY;tick(mc,0,true,false);
      near(mc.player.boundingBox.minY-feet,(double).04f,1e-7,"airborne water jump keeps native rise");
    }
    modern(true);reset(mc,0);var p=mc.player;
    p.velocityX=p.velocityY=p.velocityZ=.002;
    mc.world.isRemote=true;try { ModernWater.livingStep(p);near(p.velocityX,.002,0,"multiplayer settling override"); }
    finally { mc.world.isRemote=false; }
    var mob=new LivingEntity(mc.world){};mob.velocityX=mob.velocityY=mob.velocityZ=.002;
    ModernWater.livingStep(mob);near(mob.velocityY,.002,0,"mob settling override");
    pool(mc);permissions(mc,true);
  }
}
