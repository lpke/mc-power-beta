package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.input.Bindings;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.render.RenderControls;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_556;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public final class Release173Checks {
  public static boolean capture;
  public static Entity splashEntity;
  public static int splashes, waterQuads, fireQuads, blockQuads, event, lastItem, tooltip, tooltipCount;
  public static boolean tooltipDepth, tooltipLighting;

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("editors")) { Draft173Checks.run(mc); return; }
    failures=0;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);
    FreecamPreviewChecks.active(false);
    var cheats=(CheatWorld)mc.world.method_262();boolean hadCheats=cheats.power$cheatsEnabled();
    String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var session=SettingsRegistry.open(mc);
    double px=mc.player.x,py=mc.player.y,pz=mc.player.z;
    try {
      cheats.power$cheatsEnabled(true);
      if(action.equals("water")) water(mc);
      else if(action.equals("overlay")) {
        CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.reset(mc);
        var setting=find(session,"visual.removeUnderwaterOverlay");
        var renderer=new class_556(mc);
        var water=class_556.class.getDeclaredMethod("method_1866",float.class);water.setAccessible(true);
        var fire=class_556.class.getDeclaredMethod("method_1867",float.class);fire.setAccessible(true);
        var block=class_556.class.getDeclaredMethod("method_1861",float.class,int.class);block.setAccessible(true);
        int oldKey=RenderControls.FULL_BRIGHT.code;RenderControls.FULL_BRIGHT.code=114;Bindings.configure(java.util.Map.of());
        capture=true;GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try {
          test("underwater texture keeps its default and follows live override/full bright",()->{
            check(!setting.defaultValue.getAsBoolean(),"underwater overlay removed by default");
            for(boolean full:new boolean[]{false,true,false}) {
              if(RenderControls.fullBright()!=full)RenderControls.press(114);
              check(RenderControls.fullBright()==full,"full-bright toggle did not activate");
              for(boolean removed:new boolean[]{false,true}) {
                setting.parse(Boolean.toString(removed));session.preview(true);
                waterQuads=0;water.invoke(renderer,1F);
                check(waterQuads==(removed||full?0:1),"overlay state differs "+removed+"/"+full);
                check(VisualConfig.current().removeUnderwaterOverlay==removed,"full bright changed saved preference");
                check(GL11.glGetError()==0,"overlay GL error");
              }
            }
          });
          test("underwater override leaves block and fire overlays intact",()->{
            find(session,"visual.removeUnderwaterOverlay").parse("true");session.preview(true);
            fireQuads=blockQuads=0;fire.invoke(renderer,1F);block.invoke(renderer,1F,1);
            check(fireQuads==2 && blockQuads==1,"another overlay was suppressed");
          });
          test("spectator full bright also hides water and restores it on exit",()->{
            setting.parse("false");find(session,"visual.spectatorFullBright").parse("true");session.preview(true);
            CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.tick(mc);
            check(RenderControls.fullBright(),"automatic spectator lighting absent");
            waterQuads=0;water.invoke(renderer,1F);check(waterQuads==0,"spectator water overlay remains");
            CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.tick(mc);
            waterQuads=0;water.invoke(renderer,1F);check(waterQuads==1,"water overlay not restored");
          });
          test("Cancel restores underwater overlay preference",()->{
            setting.parse(Boolean.toString(!setting.original().getAsBoolean()));session.preview(true);session.discard();
            check(VisualConfig.current().removeUnderwaterOverlay==setting.original().getAsBoolean(),"Cancel did not restore texture preference");
          });
        } finally {GL11.glPopMatrix();GL11.glPopAttrib();RenderControls.FULL_BRIGHT.code=oldKey;RenderControls.reset(mc);capture=false;}
      } else if(action.equals("tooltip")) tooltip(mc);
    } finally {
      capture=false;splashEntity=null;mc.setScreen(null);session.discard();
      CreativeVehicleChecks.mode(mc,mode);cheats.power$cheatsEnabled(hadCheats);mc.player.method_1340(px,py,pz);
    }
    log("RELEASE 173 "+action.toUpperCase()+" FAILURES "+failures);
  }

  private static void water(Minecraft mc) throws Exception {
    for(int x=31;x<=33;x++)for(int z=31;z<=33;z++)for(int y=109;y<=113;y++)mc.world.method_154(x,y,z,y==109?1:9,0);
    var wet=Entity.class.getDeclaredField("field_1612");wet.setAccessible(true);
    var first=Entity.class.getDeclaredField("field_1649");first.setAccessible(true);
    capture=true;splashEntity=mc.player;
    for(String mode:new String[]{"SURVIVAL","CREATIVE","SPECTATOR"}) test("water entry splash for "+mode,()->{
      CreativeVehicleChecks.mode(mc,mode);mc.player.method_1340(32.5,111.62,32.5);mc.player.velocityY=-.8;
      wet.setBoolean(mc.player,false);first.setBoolean(mc.player,false);splashes=0;
      mc.player.baseTick();
      check(wet.getBoolean(mc.player),"water entry did not occur");
      check(splashes==(mode.equals("SPECTATOR")?0:1),"wrong splash count "+splashes);
      splashes=0;mc.player.baseTick();check(splashes==0,"remaining in water splashed again");
    });
  }

  private static void tooltip(Minecraft mc) throws Exception {
    CreativeVehicleChecks.mode(mc,"CREATIVE");var session=SettingsRegistry.open(mc);
    var inventory=mc.player.inventory.main.clone();var cursor=mc.player.inventory.getCursorStack();
    var f=Keyboard.class.getDeclaredField("keyDownBuffer");f.setAccessible(true);var keys=(ByteBuffer)f.get(null);
    byte left=keys.get(Keyboard.KEY_LSHIFT),right=keys.get(Keyboard.KEY_RSHIFT);
    try {
      find(session,"creative.destroySlot").parse("true");find(session,"creative.shiftClearsInventory").parse("true");session.preview(true);
      for(int i=0;i<inventory.length;i++)mc.player.inventory.main[i]=new ItemStack(1,64,0);
      for(boolean normal:new boolean[]{true,false})for(int width:new int[]{320,640})for(int shift:new int[]{Keyboard.KEY_LSHIFT,Keyboard.KEY_RSHIFT})test("clear tooltip is last in "+(normal?"survival":"catalogue")+" view, width "+width+", shift "+shift,()->{
        keys.put(Keyboard.KEY_LSHIFT,(byte)0);keys.put(Keyboard.KEY_RSHIFT,(byte)0);keys.put(shift,(byte)1);
        var screen=new class_585(mc.player);mc.setScreen(screen);screen.init(mc,width,240);field(screen,"creative_normalGUI",normal);
        mc.player.inventory.setCursorStack(new ItemStack(264,32,0));
        int x=(width-176)/2-9,y=(240-166)/2+150;
        capture=true;event=lastItem=tooltip=tooltipCount=0;screen.render(x,y,1);capture=false;
        check(lastItem>0 && tooltip>lastItem && tooltipCount==1,"tooltip drawn before items or drawn twice: "+lastItem+"/"+tooltip+"/"+tooltipCount);
        check(!tooltipDepth && !tooltipLighting,"tooltip uses item depth/lighting");
        check(GL11.glGetError()==0,"tooltip GL error");
        keys.put(shift,(byte)0);capture=true;tooltipCount=0;screen.render(x,y,1);capture=false;
        check(tooltipCount==0,"tooltip shown without shift");
      });
    } finally {capture=false;keys.put(Keyboard.KEY_LSHIFT,left);keys.put(Keyboard.KEY_RSHIFT,right);mc.setScreen(null);System.arraycopy(inventory,0,mc.player.inventory.main,0,inventory.length);mc.player.inventory.setCursorStack(cursor);session.discard();}
  }
}
