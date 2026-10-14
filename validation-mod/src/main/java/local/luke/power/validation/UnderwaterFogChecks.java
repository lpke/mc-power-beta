package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.input.Bindings;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.render.RenderControls;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_555;
import net.minecraft.block.Material;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class UnderwaterFogChecks {
  private static void bright(boolean value) {
    if(RenderControls.fullBright()!=value)RenderControls.press(114);
    check(RenderControls.fullBright()==value,"full bright did not toggle; focus the test window");
  }
  private static void fluid(Minecraft mc,int id) {
    // Clear neighbours first so lava cannot react with water left by another case.
    for(int x=30;x<=34;x++)for(int z=30;z<=34;z++)for(int y=109;y<=114;y++)mc.world.method_154(x,y,z,0,0);
    for(int x=31;x<=33;x++)for(int z=31;z<=33;z++)for(int y=110;y<=113;y++)mc.world.method_154(x,y,z,id,0);
    mc.player.method_1340(32.5,111.62,32.5);
    check(mc.world.getBlockId(32,111,32)==id,"fixture fluid was replaced");
  }
  private static float[] colour() {
    var values=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_FOG_COLOR,values);
    float[] result=new float[4];values.get(result);return result;
  }
  static void run(Minecraft mc,String action)throws Exception {
    if(!action.equals("fog")){scene(mc,action);return;}
    failures=0;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);FreecamPreviewChecks.active(false);
    var session=SettingsRegistry.open(mc);var cheats=(CheatWorld)mc.world.method_262();boolean oldCheats=cheats.power$cheatsEnabled();
    String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    int key=RenderControls.FULL_BRIGHT.code;double x=mc.player.x,y=mc.player.y,z=mc.player.z;
    var camera=mc.field_2807;mc.field_2807=mc.player;
    var renderer=mc.field_2818;boolean cloud=(boolean)field(renderer,"field_2330");float distance=(float)field(renderer,"field_2350");
    var photo=Class.forName("local.luke.power.controls.util.ModOptions").getField("photoModeFogMultiplier");float photoBefore=photo.getFloat(null);
    Method fog=class_555.class.getDeclaredMethod("method_1842",int.class,float.class);fog.setAccessible(true);
    Method colour=class_555.class.getDeclaredMethod("method_1852",float.class);colour.setAccessible(true);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      cheats.power$cheatsEnabled(true);CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.reset(mc);
      RenderControls.FULL_BRIGHT.code=114;Bindings.configure(Map.of());field(renderer,"field_2330",false);field(renderer,"field_2350",128F);
      test("overlay removal alone retains native water fog",()->{
        find(session,"visual.removeUnderwaterOverlay").parse("true");session.preview(true);fluid(mc,9);bright(false);
        check(mc.player.isInFluid(Material.WATER),"water camera not submerged");fog.invoke(renderer,1,1F);
        check(GL11.glGetInteger(GL11.GL_FOG_MODE)==GL11.GL_EXP && Math.abs(GL11.glGetFloat(GL11.GL_FOG_DENSITY)-.1F)<.00001,"native water fog changed");
      });
      test("full bright uses air fog colour/distance underwater and respects density/photo controls",()->{
        bright(true);RenderControls.beginRender();
        try {
        for(int density:new int[]{0,100,200})for(float multiplier:new float[]{1,2})for(int pass:new int[]{-1,0,1}){
          find(session,"native.fogDensity").parse(Integer.toString(density));session.preview(true);photo.setFloat(null,multiplier);
          fluid(mc,0);colour.invoke(renderer,1F);fog.invoke(renderer,pass,1F);
          float start=GL11.glGetFloat(GL11.GL_FOG_START),end=GL11.glGetFloat(GL11.GL_FOG_END);float[] air=colour();
          fluid(mc,9);colour.invoke(renderer,1F);fog.invoke(renderer,pass,1F);
          check(GL11.glGetInteger(GL11.GL_FOG_MODE)==GL11.GL_LINEAR,"water retained exponential fog");
          check(GL11.glGetFloat(GL11.GL_FOG_START)==start && GL11.glGetFloat(GL11.GL_FOG_END)==end,"water ignored configured distance");
          check(Arrays.equals(air,colour()),"fog colour air="+Arrays.toString(air)+" water="+Arrays.toString(colour())+" density="+density+" pass="+pass+" fields="+field(renderer,"field_2346")+","+field(renderer,"field_2347")+","+field(renderer,"field_2348"));
          check(mc.player.isInFluid(Material.WATER),"fog changed actual fluid state");
          check((float)field(renderer,"field_2350")==128F,"fog multiplier leaked into renderer distance");
        }
        } finally { RenderControls.endRender(); }
      });
      test("fog material override is nested, thread-owned and released",()->{
        fluid(mc,9);bright(true);check(!RenderControls.clearWaterFog(),"fog scope leaked from renderer");
        RenderControls.beginFog();
        try {
          check(!mc.player.isInFluid(Material.WATER),"fog scope did not hide water");
          RenderControls.beginFog();
          try { check(RenderControls.clearWaterFog(),"nested scope missing"); }
          finally { RenderControls.endFog(); }
          check(RenderControls.clearWaterFog(),"nested scope ended outer scope");
          boolean[] worker={true};Thread thread=new Thread(()->worker[0]=RenderControls.clearWaterFog());
          thread.start();thread.join();check(!worker[0],"fog override escaped render thread");
        } finally { RenderControls.endFog(); }
        check(!RenderControls.clearWaterFog() && mc.player.isInFluid(Material.WATER),"water state not restored");
      });
      test("toggling full bright off restores native underwater fog immediately",()->{
        bright(false);fluid(mc,9);colour.invoke(renderer,1F);fog.invoke(renderer,1,1F);
        check(GL11.glGetInteger(GL11.GL_FOG_MODE)==GL11.GL_EXP && Math.abs(GL11.glGetFloat(GL11.GL_FOG_DENSITY)-.1F)<.00001,"native water fog not restored");
      });
      test("full bright leaves lava and cloud fog unchanged",()->{
        for(boolean clouds:new boolean[]{false,true}){
          fluid(mc,clouds?9:11);check(mc.player.isInFluid(clouds?Material.WATER:Material.LAVA),"wrong material for fog case");field(renderer,"field_2330",clouds);bright(false);colour.invoke(renderer,1F);fog.invoke(renderer,1,1F);
          float density=GL11.glGetFloat(GL11.GL_FOG_DENSITY);float[] nativeColour=colour();
          bright(true);colour.invoke(renderer,1F);fog.invoke(renderer,1,1F);
          check(GL11.glGetInteger(GL11.GL_FOG_MODE)==GL11.GL_EXP && GL11.glGetFloat(GL11.GL_FOG_DENSITY)==density && Arrays.equals(nativeColour,colour()),"clouds="+clouds+" native="+Arrays.toString(nativeColour)+" bright="+Arrays.toString(colour())+" mode="+GL11.glGetInteger(GL11.GL_FOG_MODE)+" densities="+density+"/"+GL11.glGetFloat(GL11.GL_FOG_DENSITY));
        }
        field(renderer,"field_2330",false);bright(false);
      });
      test("automatic spectator full bright removes water fog for one visit",()->{
        bright(false);field(renderer,"field_2330",false);
        find(session,"visual.spectatorFullBright").parse("true");session.preview(true);fluid(mc,9);
        CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.tick(mc);fog.invoke(renderer,1,1F);
        check(RenderControls.fullBright() && GL11.glGetInteger(GL11.GL_FOG_MODE)==GL11.GL_LINEAR,"spectator override retained water fog");
        CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.tick(mc);fog.invoke(renderer,1,1F);
        check(!RenderControls.fullBright() && GL11.glGetInteger(GL11.GL_FOG_MODE)==GL11.GL_EXP,"spectator fog override leaked on exit");
      });
      check(GL11.glGetError()==0,"fog GL error");
    } finally {
      RenderControls.FULL_BRIGHT.code=key;RenderControls.reset(mc);session.discard();photo.setFloat(null,photoBefore);
      field(renderer,"field_2330",cloud);field(renderer,"field_2350",distance);mc.field_2807=camera;
      CreativeVehicleChecks.mode(mc,mode);cheats.power$cheatsEnabled(oldCheats);mc.player.method_1340(x,y,z);GL11.glPopAttrib();
    }
    log("UNDERWATER FOG FAILURES "+failures);
  }
  private static void scene(Minecraft mc,String action)throws Exception {
    mc.setScreen(null);mc.paused=false;RenderControls.FULL_BRIGHT.code=114;Bindings.configure(Map.of());
    if(action.equals("fog-bright")){bright(true);return;}
    if(action.equals("fog-native")){bright(false);return;}
    if(action.equals("fog-above")){mc.player.method_1341(32.5,117,22.5,0,25);return;}
    DeathChecks.alive(mc);FreecamPreviewChecks.active(false);((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
    var visual=VisualConfig.copy();visual.spectatorFullBright=false;visual.removeUnderwaterOverlay=true;VisualConfig.preview(visual);
    CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.reset(mc);
    for(int x=28;x<=36;x++)for(int z=20;z<=64;z++)for(int y=108;y<=117;y++)
      mc.world.method_154(x,y,z,y==108 || x==28 || x==36 || z==20 || z==64?1:y<=114?9:0,0);
    for(int z:new int[]{29,39,53})for(int y=109;y<=113;y++)mc.world.method_154(33,y,z,35,0);
    mc.player.method_1341(32.5,111.62,22.5,0,0);mc.field_2807=mc.player;mc.options.hideHud=true;mc.options.thirdPerson=false;
    log("FOG SCENE position="+mc.player.x+","+mc.player.y+","+mc.player.z+" water="+mc.player.isInFluid(Material.WATER));
  }
}
