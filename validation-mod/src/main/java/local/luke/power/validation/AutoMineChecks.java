package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.building.config.Config;
import local.luke.power.building.mining.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;

final class AutoMineChecks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("chord")) {
      AutoMine.stop(); Bindings.configure(Map.of("Auto-mine (toggle)", Chord.CTRL));
      log("AUTO MINE chord=Ctrl+M"); return;
    }
    if (action.equals("setup")) {
      ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc,"SURVIVAL");
      ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(false);
      mc.player.closeScreen(); mc.player.health = 20; mc.player.fire = 0;
      for (int x = -3; x <= 3; x++) for (int z = -3; z <= 5; z++) {
        mc.world.method_200(x,100,z,1); for (int y=101; y<108; y++) mc.world.method_200(x,y,z,0);
      }
      for (int x=-1;x<=1;x++) for(int y=101;y<=103;y++) mc.world.method_200(x,y,3,1);
      mc.player.method_1340(.5,103,.5); mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 0;
      mc.player.inventory.main[0] = new ItemStack(278,1,0); mc.player.inventory.selectedSlot=0;
      AutoMine.KEY.code = 50; Bindings.configure(Map.of()); Bindings.register(mc.options.allKeys);
      var s=Config.current().copy();s.autoMine=true;Config.preview(s);AutoMine.stop();
      log("AUTO MINE SETUP key=M blocks=9"); return;
    }
    if (action.equals("state") || action.equals("mined") || action.equals("off") || action.equals("focus")) {
      if (action.equals("off") || action.equals("focus")) check(!AutoMine.active() && !AttackInput.held(), "auto-mine retained attack");
      if (action.equals("focus")) check(!org.lwjgl.opengl.Display.isActive(), "test window still focused");
      int blocks=0;for(int x=-1;x<=1;x++)for(int y=101;y<=103;y++)if(mc.world.getBlockId(x,y,3)==1)blocks++;
      if (action.equals("mined")) check(blocks<9 && mc.player.inventory.main[0].getDamage()>0, "normal mining did not break/damage tool");
      log("AUTO MINE active="+AutoMine.active()+" attack="+AttackInput.held()+" HUD="+TweakIndicators.label(TweakIndicators.Tweak.AUTO_MINE)
          +" blocks="+blocks+" toolDamage="+mc.player.inventory.main[0].getDamage()+" focus="+org.lwjgl.opengl.Display.isActive());return;
    }
    if (action.equals("menu")) {mc.setScreen(new ChatScreen());check(!AutoMine.active()&&!AttackInput.held(),"menu retained attack");log("PASS Auto-mine stops for menus");return;}
    failures=0;mc.setScreen(null);AutoMine.tick(mc);
    var original=Config.current().copy();
    try {
      test("Auto-mine is unbound, independent of cheats and grouped under Mining",()->{
        var session=SettingsRegistry.open(mc);var row=Release110Checks.find(session,"tweaks.autoMine");
        check(AutoMine.KEY.code==0 && Release110Checks.find(session,"keys.Auto-mine (toggle)").defaultValue.getAsInt()==0,"key not unbound");
        check(row.page.equals("Building")&&row.group.equals("Mining")&&SettingAccess.visible(session,row),"wrong location/access");
        check(!Config.current().autoMineAnnounceToggle,"messages default on");
      });
      test("Cancel restores availability without resuming mining or changing inventory",()->{
        var settings=Config.current().copy();settings.autoMine=true;Config.preview(settings);
        byte[] before=Files.readAllBytes(PowerConfig.path());var stack=mc.player.inventory.getSelectedItem();
        start();check(AutoMine.active()&&AttackInput.held(),"toggle not active");
        var session=SettingsRegistry.open(mc);Release110Checks.find(session,"tweaks.autoMine").parse("false");session.preview(true);
        check(!AutoMine.active()&&!AttackInput.held(),"disabled kept attacking");session.discard();
        check(Config.current().autoMine&&!AutoMine.active(),"Cancel resumed attack");
        check(mc.player.inventory.getSelectedItem()==stack&&Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"menu edit changed inventory/config");
      });
      test("menus, death, detached camera and world identity changes stop attack",()->{
        start();mc.setScreen(new ChatScreen());check(!AutoMine.active(),"menu did not stop");mc.setScreen(null);
        start();int health=mc.player.health;mc.player.health=0;check(!AutoMine.active(),"death did not stop");mc.player.health=health;
        start();var world=mc.world;mc.world=null;AutoMine.tick(mc);mc.world=world;AutoMine.tick(mc);check(!AutoMine.active(),"world change resumed attack");
        start();Field owner=AutoMine.class.getDeclaredField("world");owner.setAccessible(true);owner.set(null,new Object());
        check(!AutoMine.active(),"changed world retained attack before the next input tick");AutoMine.tick(mc);
        start();MovementOwnership.registerCamera(()->true);check(!AutoMine.active(),"camera did not stop");
        Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
        MovementOwnership.registerCamera(()->{try{return (boolean)camera.getClass().getMethod("isActive").invoke(camera)
            && !camera.getClass().getField("allowPlayerMovement").getBoolean(camera);}catch(Exception e){throw new RuntimeException(e);}});
        check(!AutoMine.active(),"camera exit resumed attack");
      });
      test("HUD inclusion never enables or disables gameplay",()->{
        start();check(TweakIndicators.label(TweakIndicators.Tweak.AUTO_MINE)!=null,"live HUD state missing");
        var status=local.luke.power.status.StatusConfig.copy();status.autoMine=false;local.luke.power.status.StatusConfig.preview(status);
        check(AutoMine.active(),"HUD disabled gameplay");AutoMine.stop();check(TweakIndicators.label(TweakIndicators.Tweak.AUTO_MINE)==null,"stale active HUD");
        status.autoMine=true;local.luke.power.status.StatusConfig.preview(status);
      });
    } finally {AutoMine.stop();Config.preview(original);mc.setScreen(null);}
    log("AUTO MINE FAILURES "+failures);
  }
  private static void start() throws Exception {
    Field field=AutoMine.class.getDeclaredField("TOGGLE");field.setAccessible(true);var toggle=(AutoMineToggle)field.get(null);
    toggle.stop();toggle.update(false,true);toggle.update(true,true);toggle.update(false,true);
  }
}
