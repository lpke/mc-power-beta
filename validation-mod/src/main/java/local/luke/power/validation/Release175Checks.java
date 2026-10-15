package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.render.RenderControls;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_42;
import net.minecraft.class_56;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;

public final class Release175Checks {
  public static boolean capture;
  public static int bubbles;

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("save") || action.equals("saved")) {
      var session=SettingsRegistry.open(mc);var setting=find(session,"visual.underwaterDarkness");
      if (action.equals("saved")) {
        check(VisualConfig.current().underwaterDarkness==37,"darkness did not survive restart");
        setting.parse("0");session.preview(true);session.discard();
        check(VisualConfig.current().underwaterDarkness==37,"Cancel lost persisted darkness");
      }
      setting.parse(action.equals("save")?"37":"100");session.save(Path.of(".").toAbsolutePath());
      log("PASS darkness persistence "+action);return;
    }
    failures=0;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);FreecamPreviewChecks.active(false);
    var session=SettingsRegistry.open(mc);
    var cheats=(CheatWorld)mc.world.method_262();boolean oldCheats=cheats.power$cheatsEnabled();
    String oldMode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    try {
      cheats.power$cheatsEnabled(true);RenderControls.reset(mc);
      if(action.equals("oxygen")) oxygen(mc);
      else if(action.equals("lighting")) lighting(mc);
      else if(action.equals("flight")) {
        find(session,"power_camera:config.enabled").parse("true");session.preview(true);flight(mc);
      }
      else throw new IllegalArgumentException(action);
    } finally {
      capture=false;FreecamPreviewChecks.active(false);session.discard();
      CreativeVehicleChecks.mode(mc,oldMode);cheats.power$cheatsEnabled(oldCheats);
      mc.player.air=300;RenderControls.reset(mc);
    }
    log("RELEASE 175 "+action.toUpperCase()+" FAILURES "+failures);
  }

  private static void oxygen(Minecraft mc) throws Exception {
    for(int x=31;x<=33;x++)for(int z=31;z<=33;z++)for(int y=109;y<=114;y++)mc.world.method_154(x,y,z,y==109?1:9,0);
    mc.player.method_1340(32.5,111.62,32.5);capture=true;
    for(String mode:new String[]{"CREATIVE","SPECTATOR"}) {
      test(mode+" refills oxygen immediately and never drowns or emits drowning bubbles",()->{
        CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.air=-19;
        CreativeVehicleChecks.mode(mc,mode);check(mc.player.air==300,"mode change kept depleted oxygen");
        check(!mc.interactionManager.method_1722(),"oxygen HUD remains visible");
        mc.player.baseTick();mc.player.air=-19;bubbles=0;int health=mc.player.health;
        for(int i=0;i<700;i++)mc.player.baseTick();
        check(mc.player.air==300 && mc.player.health==health,"oxygen depleted or drowning damage occurred");
        check(bubbles==0,"drowning particles emitted: "+bubbles);
        CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.baseTick();
        check(mc.player.air==300-1 && mc.player.health==health,"return to Survival did not get a full breath");
      });
      test(mode+" reload restores oxygen and Survival keeps its native countdown",()->{
        CreativeVehicleChecks.mode(mc,mode);var tag=new NbtCompound();mc.player.writeNbt(tag);
        CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.air=-19;mc.player.readtNbt(tag);
        check(mc.player.air==300 && mc.player.canBreatheInWater(),"protected reload kept stale oxygen");
        CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.air=-19;mc.player.field_1613=0;int health=mc.player.health;bubbles=0;
        mc.player.baseTick();check(mc.player.health==health-2 && bubbles==8,"native Survival drowning changed");
        mc.player.health=20;mc.player.field_1613=0;
      });
    }
  }

  private static void lighting(Minecraft mc) throws Exception {
    CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.tick(mc);
    var original=VisualConfig.copy();var session=SettingsRegistry.open(mc);
    int skyDarkness=mc.world.field_202;
    // Separate open-water, roofed-water, and dry columns. Set fixture light directly
    // so assertions do not depend on pending native propagation work.
    for(int x=32;x<=34;x++)for(int y=78;y<128;y++) {
      int id=y==78?1:y<=100&&x!=34?9:0;
      if(x==33&&y==101)id=1;
      mc.world.method_154(x,y,32,id,0);
      mc.world.method_205(class_56.SKY,x,y,32,y>101||x==34?15:0);
      mc.world.method_205(class_56.BLOCK,x,y,32,0);
    }
    var view=new class_42(mc.world,31,77,31,35,127,33);
    try {
      mc.world.field_202=0;
      test("darkness slider is independent, proportional and render-only",()->{
        var setting=find(session,"visual.underwaterDarkness");
        check(setting.slider() && setting.defaultValue.getAsInt()==100,"slider metadata/default");
        float nativeLight=mc.world.method_1782(32,80,32);
        int actualSky=mc.world.method_164(class_56.SKY,32,80,32),actualBlock=mc.world.method_164(class_56.BLOCK,32,80,32);
        for(int value:new int[]{100,75,50,25,0}) {
          setting.parse(Integer.toString(value));session.preview(true);
          RenderControls.beginRender();
          try {
            float wanted=nativeLight+(1-nativeLight)*(100-value)/100F;
            check(Math.abs(mc.world.method_1782(32,80,32)-wanted)<.00001,"world shade "+value);
            check(Math.abs(view.method_1784(32,80,32,0)-wanted)<.00001,"chunk shading "+value);
            check(mc.world.method_1782(33,80,32)==nativeLight,"roof shadow lost");
            check(mc.world.method_1782(34,80,32)==1,"dry sky changed");
            check(view.method_1784(32,80,32,15)==1,"emission darkened");
            check(mc.world.method_164(class_56.SKY,32,80,32)==actualSky && mc.world.method_164(class_56.BLOCK,32,80,32)==actualBlock,"saved light changed");
            var worker=new FutureTask<Float>(() -> mc.world.method_1782(32,80,32));new Thread(worker,"water-light-check").start();
            check(worker.get()==nativeLight,"render override escaped to worker");
          } finally {RenderControls.endRender();}
          check(mc.world.method_1782(32,80,32)==nativeLight,"render override escaped scope");
          check(VisualConfig.current().underwaterFogDensity==original.underwaterFogDensity,"darkness changed fog");
        }
      });
      test("night, new roofs, opaque AO and full bright keep their own lighting",()->{
        var settings=VisualConfig.copy();settings.underwaterDarkness=0;VisualConfig.preview(settings);
        mc.world.field_202=11;RenderControls.beginRender();
        try {
          check(mc.world.method_1782(32,80,32)==mc.world.dimension.field_2178[4],"night became full bright");
          check(view.method_1784(32,78,32,0)==mc.world.dimension.field_2178[0],"opaque AO changed");
        } finally {RenderControls.endRender();}
        mc.world.field_202=0;mc.world.method_154(32,102,32,1,0);
        for(int y=79;y<=102;y++)mc.world.method_205(class_56.SKY,32,y,32,0);
        RenderControls.beginRender();
        try {check(mc.world.method_1782(32,80,32)==mc.world.dimension.field_2178[0],"new roof shade="+mc.world.method_1782(32,80,32)+" sky="+mc.world.method_164(class_56.SKY,32,80,32));}
        finally {RenderControls.endRender();}
        settings.spectatorFullBright=true;VisualConfig.preview(settings);CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.tick(mc);
        RenderControls.beginRender();
        try {check(RenderControls.fullBright()&&mc.world.method_1782(32,80,32)==1,"full bright did not override darkness");}
        finally {RenderControls.endRender();}
        CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.tick(mc);
      });
      test("repeated shading is cached within a frame and Cancel restores darkness",()->{
        long start=System.nanoTime();RenderControls.beginRender();float sum=0;
        try {for(int i=0;i<100000;i++)sum+=view.method_1784(32,79+i%20,32,0);}
        finally {RenderControls.endRender();}
        log("WATER LIGHT 100000 cached samples ms="+(System.nanoTime()-start)/1_000_000.0+" sum="+sum);
        session.discard();check(VisualConfig.current().underwaterDarkness==original.underwaterDarkness,"Cancel failed");
      });
    } finally {mc.world.field_202=skyDarkness;session.discard();VisualConfig.preview(original);}
  }

  private static void flight(Minecraft mc) throws Exception {
    CreativeVehicleChecks.mode(mc,"CREATIVE");mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
    Class<?> runtime=Class.forName("local.luke.power.creative.ClientRuntime");runtime.getMethod("tick",Minecraft.class).invoke(null,mc);
    mc.player.method_1340(32.5,120,32.5);FreecamPreviewChecks.active(true);
    test("detached Creative flight settles limb interpolation without repeated frame jumps",()->{
      mc.player.field_1048=.4F;mc.player.field_1049=.8F;mc.player.field_1050=4;
      mc.player.velocityX=.7;mc.player.velocityY=.2;mc.player.velocityZ=.4;
      double x=mc.player.x,y=mc.player.y,z=mc.player.z;
      for(int i=0;i<50;i++) {
        float phase=mc.player.field_1050,amplitude=mc.player.field_1049;
        mc.player.method_945(0,1);
        check(mc.player.field_1048==amplitude,"previous limb endpoint stale");
        check(Math.abs((mc.player.field_1050-mc.player.field_1049)-phase)<.00001,"animation jumped backwards at tick boundary");
        check(mc.player.field_1049<amplitude,"frozen animation tick="+i+" before="+amplitude+" after="+mc.player.field_1049+" flying="+mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player)+" camera="+local.luke.power.input.MovementOwnership.cameraControlsMovement());
        check(mc.player.x==x && mc.player.y==y && mc.player.z==z,"frozen body moved");
      }
      check(mc.player.field_1049<.000001,"body still jitters across frame fractions");
      check(mc.player.velocityX==0 && mc.player.velocityY==0 && mc.player.velocityZ==0,"flight momentum persisted");
    });
  }
}
