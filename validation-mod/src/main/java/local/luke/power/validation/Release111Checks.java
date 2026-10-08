package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.Path;
import java.util.List;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;

final class Release111Checks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("debug")) {
      var rows=DebugOverlay.lastLayout();
      check(rows.stream().anyMatch(r->r.text().startsWith("Facing:")),"facing missing");
      check(rows.stream().anyMatch(r->r.text().startsWith("Music:")),"music missing");
      check(rows.stream().anyMatch(r->r.text().startsWith("Seed:")),"seed missing");
      check(rows.stream().anyMatch(r->r.text().startsWith("[Culling]")),"platform debug rows missing");
      for(int a=0;a<rows.size();a++) for(int b=a+1;b<rows.size();b++) {
        var x=rows.get(a);var y=rows.get(b);
        check(!(x.x()<y.x()+y.width() && x.x()+x.width()>y.x() && x.y()<y.y()+9 && x.y()+9>y.y()),"overlap: "+x.text()+" / "+y.text());
      }
      log("DEBUG ROWS "+rows.size()+" COLOR "+Integer.toHexString(InterfaceState.debugTextColor));
      for(var row:rows)log("DEBUG "+row.x()+","+row.y()+" "+row.text());
      return;
    }
    if (action.equals("debug-on")) {
      var s=SettingsRegistry.open(mc);
      find(s,"power_compat:config.addFacingToDebugOverlay").parse("true");
      find(s,"power_environment:config.MUSIC_CONFIG.overlayForMusicInDebug").parse("true");
      s.save(Path.of("."));mc.setScreen(null);mc.options.debugHud=true;return;
    }
    if (action.equals("saved")) {
      check(MenuPreferences.blinkingChatCursor(),"blink lost on restart");
      check(MenuPreferences.debugTextColor().equals("#55FF55"),"colour lost on restart");
      log("PASS presentation settings survived restart");return;
    }
    failures=0;
    test("steady cursor and white debug text defaults",()->{
      var s=SettingsRegistry.open(mc);
      check(!find(s,"interface.blinkingChatCursor").defaultValue.getAsBoolean(),"blink default");
      check(find(s,"interface.debugTextColor").defaultValue.getAsString().equals("#FFFFFF"),"colour default");
      for(int t=0;t<24;t++) check(InterfaceState.cursorVisible(t),"default cursor blinks");
    });
    test("presentation previews cancel together and invalid colours fail",()->{
      var s=SettingsRegistry.open(mc);
      find(s,"interface.blinkingChatCursor").parse("true");find(s,"interface.debugTextColor").parse("#55FF55");s.preview();
      check(!InterfaceState.cursorVisible(6)&&InterfaceState.debugTextColor==0x55FF55,"preview missing");
      s.discard();check(InterfaceState.cursorVisible(6)&&InterfaceState.debugTextColor==0xFFFFFF,"Cancel failed");
      var bad=SettingsRegistry.open(mc);find(bad,"interface.debugTextColor").parse("bad");
      boolean rejected=false;try{bad.preview();}catch(IllegalArgumentException expected){rejected=true;}
      check(rejected&&InterfaceState.debugTextColor==0xFFFFFF,"invalid colour accepted");
    });
    test("world overrides have short labels, scope tooltips and cheat visibility",()->{
      var s=SettingsRegistry.open(mc);find(s,"world.cheats").parse("true");
      check(find(s,"world.cheats").group.equals("Game")&&find(s,"world.cheats").label.contains("this world"),"cheats moved or scope lost");
      for(String id:List.of("daylightCycle","weatherCycle","hostileSpawning","passiveSpawning")) {
        var row=find(s,"world."+id);
        check(row.group.equals("World overrides")&&!row.label.contains("this world"),"wrong group/label");
        check(Tooltips.setting(row,"").contains("each singleplayer world")&&SettingAccess.visible(s,row),"scope/visibility");
        find(s,"world.cheats").parse("false");check(!SettingAccess.visible(s,row),"row visible without cheats");find(s,"world.cheats").parse("true");
      }
    });
    test("presentation Apply preserves other interface preferences",()->{
      var old=PowerConfig.section("interface");
      var s=SettingsRegistry.open(mc);find(s,"interface.blinkingChatCursor").parse("true");find(s,"interface.debugTextColor").parse("#55FF55");s.save(Path.of("."));
      var saved=PowerConfig.section("interface");
      check(saved.get("blinkingChatCursor").getAsBoolean()&&saved.get("debugTextColor").getAsString().equals("#55FF55"),"settings not saved");
      for(var entry:old.entrySet()) if(!entry.getKey().equals("blinkingChatCursor")&&!entry.getKey().equals("debugTextColor")) check(saved.get(entry.getKey()).equals(entry.getValue()),"unrelated preference changed");
    });
    log("RELEASE 1.1.1 FAILURES "+failures);
  }
  private static Setting find(ConfigSession s,String id){return Release110Checks.find(s,id);}
}
