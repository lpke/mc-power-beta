package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.ArrayList;
import local.luke.power.gameplay.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.SprintController;
import net.minecraft.class_41;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.opengl.Display;

/** Physical origins and the first resolved camera tick after a native player reload. */
final class PhysicalPose448Checks {
  private static void near(double actual,double expected,String label) {
    check(Math.abs(actual-expected)<2e-6,label+": "+actual+" expected "+expected);
  }

  static void run(Minecraft mc) throws Exception {
    check(mc.world!=null && !mc.world.isRemote && mc.player!=null && Display.isActive()
        && mc.player.field_1595==null,"focus a muted disposable singleplayer world while unmounted");
    check(mc.options.soundVolume==0 && mc.options.musicVolume==0,"mute the disposable client");
    failures=0;
    var player=mc.player;var saved=new NbtCompound();player.write(saved);
    var settings=GameplayConfig.copy();var rules=AccessRules.copy(mc);
    var input=player.field_161;var hook=player.fishHook;var screen=mc.currentScreen;boolean paused=mc.paused;
    String mode=Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(player).toString();
    int x=((int)Math.floor(player.x)>>4)*16+8,z=((int)Math.floor(player.z)>>4)*16+8,y=112;
    var blocks=new ArrayList<int[]>();
    try {
      check(mc.world.method_155(x-6,0,z-6,x+6,127,z+6),"physical pose scene needs already loaded neighboring chunks");
      for(int bx=x-2;bx<=x+2;bx++)for(int bz=z-2;bz<=z+2;bz++)for(int by=y-1;by<=y+3;by++) {
        check(mc.world.method_1777(bx,by,bz)==null,"physical pose scene intersects a block entity");
        blocks.add(new int[]{bx,by,bz,mc.world.getBlockId(bx,by,bz),mc.world.method_1778(bx,by,bz)});
      }
      mc.setScreen(null);mc.paused=false;AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,"SURVIVAL");
      for(boolean lower:new boolean[]{false,true})for(boolean roof:new boolean[]{false,true}) {
        final boolean camera=lower,forced=roof;
        test("native crouch reload resolves camera lower="+lower+" forced="+roof,()->{
          scene(mc,x,y,z);options(camera);double feet=y;
          if(forced) {
            mc.world.method_154(x,y,z,44,0);mc.world.method_154(x,y+2,z,1,0);feet+=.5;
          }
          reset(mc,x,feet,z);player.field_161.field_2536=true;player.tick();
          var state=((SwimPlayer)player).power$swim();check(state.crouched,"fixture did not crouch");
          var tag=new NbtCompound();player.write(tag);
          near(((net.minecraft.nbt.NbtDouble)tag.getList("Pos").get(1)).value,feet+1.62f,"normalized saved Y");
          player.read(tag);state.lowCeiling=false;player.field_161.field_2536=true;player.tick();
          double target=camera||forced?1.27f:1.62f;
          near(player.boundingBox.minY,feet,"reload feet");near(player.field_1631,1.27,"physical crouch eye");
          near(state.previousEyeHeight,target,"first resolved previous camera");near(state.eyeHeight,target,"first resolved current camera");
          for(float delta:new float[]{0,.5f,1})near(state.cameraEye(delta),target,"reload camera interpolation");
          check(state.crouched && !state.compact,"reload changed collision pose");
        });
      }
      test("compact native reload retains feet, normalized NBT and the low camera",()->{
        scene(mc,x,y,z);options(false);mc.world.method_154(x,y+1,z,1,0);reset(mc,x,y,z);player.tick();
        check(ModernSwimming.compact(player),"fixture did not crawl");
        var tag=new NbtCompound();player.write(tag);
        near(((net.minecraft.nbt.NbtDouble)tag.getList("Pos").get(1)).value,y+(double)1.62f,"compact normalized saved Y");
        player.read(tag);player.tick();var state=((SwimPlayer)player).power$swim();
        near(player.boundingBox.minY,y,"compact reload feet");near(state.cameraEye(0),.4,"compact previous camera");
        near(state.cameraEye(1),.4,"compact current camera");check(state.compact,"reload stood into a roof");
      });
      test("fishing casts use physical low eyes and preserve native standing, remote and other-player origins",()->{
        scene(mc,x,y,z);options(false);reset(mc,x,y,z);cast(player,player.y+1.62-player.field_1631);
        player.field_161.field_2536=true;player.tick();check(ModernSwimming.crouched(player),"cast fixture did not crouch");
        cast(player,player.y+player.method_1378());
        mc.world.method_154(x,y+1,z,1,0);player.field_161.field_2536=false;player.tick();
        check(ModernSwimming.compact(player),"cast fixture did not crawl");cast(player,player.y+player.method_1378());
        double feet=player.boundingBox.minY;
        mc.world.isRemote=true;
        try {cast(player,player.y+1.62-player.field_1631);} finally {mc.world.isRemote=false;}
        var other=new ClientPlayerEntity(mc,mc.world,mc.session,player.dimensionId);
        var state=((SwimPlayer)other).power$swim();state.compact=true;other.spacingY=.6f;other.field_1631=.4f;
        other.method_1341(x+.5,y,z+.5,0,0);cast(other,other.y+1.62-other.field_1631);
        near(player.boundingBox.minY,feet,"casting moved physical feet");
      });
    } finally {
      mc.world.isRemote=false;
      for(int[] block:blocks)mc.world.method_154(block[0],block[1],block[2],block[3],block[4]);
      AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,mode);
      GameplayConfig.preview(settings);AccessRules.restore(mc,rules);player.read(saved);player.field_161=input;
      player.fishHook=hook;mc.paused=paused;mc.setScreen(screen);SprintController.reset();
    }
    log("PHYSICAL POSE 448 FAILURES "+failures); check(failures==0,"physical pose checks failed");
  }

  private static void cast(ClientPlayerEntity player,double originY) {
    var original=player.fishHook;
    try {
      var bobber=new FishingBobberEntity(player.world,player);
      near(bobber.y,originY-(double).1f,"native cast height");
      near(bobber.x,player.x-net.minecraft.util.math.MathHelper.cos(player.yaw/180f*(float)Math.PI)*.16f,"native cast X");
      near(bobber.z,player.z-net.minecraft.util.math.MathHelper.sin(player.yaw/180f*(float)Math.PI)*.16f,"native cast Z");
      near(bobber.spacingXZ,.25,"native bobber width");near(bobber.spacingY,.25,"native bobber height");
      check(player.fishHook==bobber && Double.isFinite(bobber.velocityX) && Double.isFinite(bobber.velocityY)
          && Double.isFinite(bobber.velocityZ),"cast lost native ownership or trajectory");
      bobber.markDead();
    } finally {player.fishHook=original;}
  }

  private static void options(boolean lower) {
    var config=GameplayConfig.copy();config.modernCrawling=true;config.modernWaterMovement=true;
    config.crouchCameraHeight=lower;GameplayConfig.preview(config);
  }

  private static void reset(Minecraft mc,int x,double feet,int z) {
    var p=mc.player;var state=((SwimPlayer)p).power$swim();
    state.swimming=state.compact=state.crouched=state.lowCeiling=false;state.previousAmount=state.amount=0;state.resetCamera(1.62f);
    p.spacingXZ=.6f;p.spacingY=1.8f;p.field_1631=1.62f;p.field_1640=0;
    p.method_1341(x+.5,feet,z+.5,0,0);p.prevX=p.x;p.prevY=p.y;p.prevZ=p.z;
    p.velocityX=p.velocityZ=0;p.velocityY=-.08*(double).98f;p.field_1623=true;
    p.field_161=new class_41();SprintController.reset();
  }

  private static void scene(Minecraft mc,int x,int y,int z) {
    for(int bx=x-2;bx<=x+2;bx++)for(int bz=z-2;bz<=z+2;bz++)for(int by=y-1;by<=y+3;by++)
      mc.world.method_154(bx,by,bz,by==y-1?1:0,0);
  }
}
