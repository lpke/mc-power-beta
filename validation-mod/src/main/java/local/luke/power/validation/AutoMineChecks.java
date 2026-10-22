package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.building.config.Config;
import local.luke.power.building.mining.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;

final class AutoMineChecks {
  private static boolean observing;
  private static int observedTicks, physicalTicks, matchingTicks, activeTicks, attackTicks, swingTicks;
  private static final Map<net.minecraft.client.option.KeyBinding,Integer> displaced = new IdentityHashMap<>();
  private static Map<String,Integer> savedModifiers;
  private static int savedKey;
  static void observe(Minecraft mc) {
    if (!observing || mc.player == null) return;
    observedTicks++; if (Bindings.physical(AutoMine.KEY.code)) physicalTicks++;
    if (Bindings.down(AutoMine.KEY)) matchingTicks++;
    if (AutoMine.active()) activeTicks++;
    if (AttackInput.held()) attackTicks++;
    if (mc.player.method_930(.5f) > 0) swingTicks++;
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("chord")) {
      AutoMine.stop(); Bindings.configure(Map.of("Auto-mine (toggle)", Chord.CTRL));
      log("AUTO MINE chord=Ctrl+M"); return;
    }
    if (action.equals("setup")) {
      (AccessRules.of(mc.world.method_262())).allAllowed(true); CreativeVehicleChecks.mode(mc,"SURVIVAL");
      (AccessRules.of(mc.world.method_262())).allAllowed(false);
      mc.player.closeScreen(); mc.player.health = 20; mc.player.fire = 0;
      for (int x = -3; x <= 3; x++) for (int z = -3; z <= 5; z++) {
        mc.world.method_200(x,100,z,1); for (int y=101; y<108; y++) mc.world.method_200(x,y,z,0);
      }
      for (int x=-1;x<=1;x++) for(int y=101;y<=103;y++) mc.world.method_200(x,y,3,1);
      mc.player.method_1340(.5,103,.5); mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 0;
      mc.player.inventory.main[0] = new ItemStack(278,1,0); mc.player.inventory.selectedSlot=0;
      restoreBindings(mc); savedKey=AutoMine.KEY.code; savedModifiers=Bindings.modifiers();
      for(var key:mc.options.allKeys)if(key!=AutoMine.KEY&&key.code==50){displaced.put(key,key.code);key.code=0;}
      AutoMine.KEY.code = 50;var modifiers=new HashMap<>(savedModifiers);modifiers.remove("Auto-mine (toggle)");
      Bindings.configure(modifiers); Bindings.register(mc.options.allKeys);
      var s=Config.current().copy();Config.preview(s);AutoMine.stop();
      AutoMine.tick(mc); observing=true; observedTicks=physicalTicks=matchingTicks=activeTicks=attackTicks=swingTicks=0;
      log("AUTO MINE SETUP key=M blocks=9"); return;
    }
    if (action.equals("state") || action.equals("mined") || action.equals("off") || action.equals("focus")) {
      if (action.equals("off") || action.equals("focus")) check(!AutoMine.active() && !AttackInput.held(), "auto-mine retained attack");
      if (action.equals("focus")) check(!org.lwjgl.opengl.Display.isActive(), "test window still focused");
      int blocks=0;for(int x=-1;x<=1;x++)for(int y=101;y<=103;y++)if(mc.world.getBlockId(x,y,3)==1)blocks++;
      log("AUTO MINE active="+AutoMine.active()+" attack="+AttackInput.held()+" HUD="+TweakIndicators.label(TweakIndicators.Tweak.AUTO_MINE)
          +" blocks="+blocks+" toolDamage="+(mc.player.inventory.main[0]==null?-1:mc.player.inventory.main[0].getDamage())+" focus="+org.lwjgl.opengl.Display.isActive()
          +" grabbed="+mc.field_2778+" mouseGrabbed="+org.lwjgl.input.Mouse.isGrabbed()+" health="+mc.player.health+" dead="+mc.player.dead
          +" menu="+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getName())+" carrying="+InteractionState.carryingContainer
          +" camera="+MovementOwnership.cameraControlsMovement()+" detached="+DetachedCamera.isActive()
          +" vehicle="+(mc.player.field_1595==null?"none":mc.player.field_1595.getClass().getName())
          +" ownerWorld="+(owner("world")==mc.world)+" ownerPlayer="+(owner("player")==mc.player)
          +" key="+AutoMine.KEY.code+" physical="+Bindings.physical(AutoMine.KEY.code)
          +" matches="+Bindings.down(AutoMine.KEY)+" modifiers="+Bindings.modifiers("Auto-mine (toggle)")
          +" observed="+observedTicks+" physicalTicks="+physicalTicks+" matchingTicks="+matchingTicks+" activeTicks="+activeTicks+" attackTicks="+attackTicks+" swingTicks="+swingTicks);
      if (action.equals("mined")) check(blocks<9 && mc.player.inventory.main[0]!=null && mc.player.inventory.main[0].getDamage()>0 && swingTicks>0,
          "normal mining did not break/damage the tool and animate the hand");
      if(action.equals("off")||action.equals("focus")){observing=false;restoreBindings(mc);}return;
    }
    if (action.equals("menu")) {mc.setScreen(new ChatScreen());check(!AutoMine.active()&&!AttackInput.held(),"menu retained attack");log("PASS Auto-mine stops for menus");return;}
    check(mc.player!=null&&mc.player.health>0&&!mc.player.dead,"use a living player for Auto-mine tests");
    failures=0;mc.setScreen(null);mc.field_2778=true;org.lwjgl.input.Mouse.setGrabbed(true);AutoMine.tick(mc);
    var original=Config.current().copy();
    try {
      test("Auto-mine binding is unbound by default and messages remain under Mining",()->{
        var session=SettingsRegistry.open(mc);var row=Release110Checks.find(session,"keys.Auto-mine (toggle)");
        check(row.defaultValue.getAsInt()==0&&SettingAccess.visible(session,row),"binding default/access changed");
        var messages=Release110Checks.find(session,"tweaks.autoMineAnnounceToggle");
        check(messages.page.equals("Building")&&messages.group.equals("Mining")&&!messages.defaultValue.getAsBoolean(),"wrong message location/default");
      });
      test("menu Cancel never resumes mining or changes inventory",()->{
        var settings=Config.current().copy();Config.preview(settings);
        byte[] before=Files.readAllBytes(PowerConfig.path());var stack=mc.player.inventory.getSelectedItem();
        start(mc);check(AutoMine.active()&&AttackInput.held(),"toggle not active");
        mc.setScreen(new ChatScreen());var session=SettingsRegistry.open(mc);
        Release110Checks.find(session,"tweaks.autoMineAnnounceToggle").parse("true");session.preview(true);
        check(!AutoMine.active()&&!AttackInput.held(),"menu kept attacking");session.discard();mc.setScreen(null);mc.field_2778=true;
        check(!AutoMine.active(),"Cancel resumed attack");
        check(mc.player.inventory.getSelectedItem()==stack&&Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"menu edit changed inventory/config");
      });
      test("menus, death, detached camera and world identity changes stop attack",()->{
        start(mc);mc.setScreen(new ChatScreen());check(!AutoMine.active(),"menu did not stop");mc.setScreen(null);
        start(mc);int health=mc.player.health;mc.player.health=0;check(!AutoMine.active(),"death did not stop");mc.player.health=health;
        start(mc);var world=mc.world;mc.world=null;AutoMine.tick(mc);mc.world=world;AutoMine.tick(mc);check(!AutoMine.active(),"world change resumed attack");
        start(mc);Field owner=AutoMine.class.getDeclaredField("world");owner.setAccessible(true);owner.set(null,new Object());
        check(!AutoMine.active(),"changed world retained attack before the next input tick");AutoMine.tick(mc);
        start(mc);MovementOwnership.registerCamera(()->true);check(!AutoMine.active(),"camera did not stop");
        Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
        MovementOwnership.registerCamera(()->{try{return (boolean)camera.getClass().getMethod("isActive").invoke(camera)
            && !camera.getClass().getField("allowPlayerMovement").getBoolean(camera);}catch(Exception e){throw new RuntimeException(e);}});
        check(!AutoMine.active(),"camera exit resumed attack");
      });
      test("HUD inclusion never enables or disables gameplay",()->{
        start(mc);check(TweakIndicators.label(TweakIndicators.Tweak.AUTO_MINE)!=null,"live HUD state missing");
        var status=local.luke.power.status.StatusConfig.copy();status.autoMine=false;local.luke.power.status.StatusConfig.preview(status);
        check(AutoMine.active(),"HUD disabled gameplay");AutoMine.stop();check(TweakIndicators.label(TweakIndicators.Tweak.AUTO_MINE)==null,"stale active HUD");
        status.autoMine=true;local.luke.power.status.StatusConfig.preview(status);
      });
    } finally {AutoMine.stop();Config.preview(original);mc.setScreen(null);}
    log("AUTO MINE FAILURES "+failures);
  }
  private static Object owner(String name) throws Exception {
    Field field=AutoMine.class.getDeclaredField(name);field.setAccessible(true);return field.get(null);
  }
  private static void restoreBindings(Minecraft mc) {
    if(savedModifiers==null)return;
    AutoMine.stop();if(AutoMine.KEY.code==50)AutoMine.KEY.code=savedKey;
    displaced.forEach((key,code)->{if(key.code==0)key.code=code;});displaced.clear();
    Bindings.configure(savedModifiers);savedModifiers=null;Bindings.register(mc.options.allKeys);
  }
  private static void start(Minecraft mc) throws Exception {
    mc.field_2778=true;org.lwjgl.input.Mouse.setGrabbed(true);AutoMine.tick(mc);
    Field field=AutoMine.class.getDeclaredField("TOGGLE");field.setAccessible(true);var toggle=(AutoMineToggle)field.get(null);
    toggle.stop();toggle.update(false,true);toggle.update(true,true);toggle.update(false,true);
  }
}
