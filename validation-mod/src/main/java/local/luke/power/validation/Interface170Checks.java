package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.FreecamPreviewChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.hud.*;
import local.luke.power.input.*;
import local.luke.power.render.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;

final class Interface170Checks {
  private static void enableCamera() throws Exception {
    Object config=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    config.getClass().getField("enabled").set(config,true);
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("save")) {
      var saved=SettingsRegistry.open(mc);
      for(String id:List.of("interface.useFeetYCoordinates","gameplay.torchShovel","veinMining.enabled"))find(saved,id).parse("true");
      for(String id:List.of("interface.blinkingUiCursor","gameplay.fireSpread"))find(saved,id).parse("false");
      find(saved,"veinMining.timePercent").parse("37");
      saved.save(Path.of(".").toAbsolutePath());ClickMining.toggle();log("Saved 1.7 preferences for restart");return;
    }
    if (action.equals("saved")) {
      check(MenuPreferences.useFeetYCoordinates() && !MenuPreferences.blinkingUiCursor(),"UI preferences lost");
      check(local.luke.power.gameplay.GameplayConfig.current().torchShovel && !local.luke.power.gameplay.GameplayConfig.current().fireSpread,"gameplay preferences lost");
      check(local.luke.power.mining.VeinConfig.current().enabled && local.luke.power.mining.VeinConfig.current().timePercent==37,"vein preferences lost");
      check(!ClickMining.inverted(),"temporary click state survived restart");log("PASS 1.7 preferences survive restart; temporary click state clears");return;
    }
    if (action.equals("keys")) {
      mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);active(false);
      ((local.luke.power.permissions.CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);CreativeVehicleChecks.mode(mc,"SURVIVAL");
      ((local.luke.power.permissions.CheatWorld)mc.world.method_262()).power$cheatsEnabled(false);
      local.luke.power.gameplay.MiningControls.CLICK_MINING.code=114;ClickMining.reset();
      var building=local.luke.power.building.config.Config.current().copy();building.clickMining=false;local.luke.power.building.config.Config.preview(building);
      Bindings.configure(Map.of());log("KEYS ready F20");return;
    }
    if (action.equals("key-state")) {log("KEYS inverted="+ClickMining.inverted()+" label="+TweakIndicators.label(TweakIndicators.Tweak.CLICK_MINING));return;}
    if (action.startsWith("ghost")) {
      mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);enableCamera();var cheats=(local.luke.power.permissions.CheatWorld)mc.world.method_262();cheats.power$cheatsEnabled(true);
      active(false);CreativeVehicleChecks.mode(mc,action.contains("spectator")?"SPECTATOR":"SURVIVAL");
      mc.player.method_1340(32.5,112.62,38.5);mc.player.yaw=mc.player.prevYaw=180;mc.player.pitch=mc.player.prevPitch=0;
      pose(32.5,112.62,38.5,0,0);active(action.contains("freecam"));
      mc.options.thirdPerson=true;mc.options.hideHud=false;mc.options.debugHud=true;
      mc.options.bobView=false;
      if(action.contains("look")){var building=local.luke.power.building.config.Config.current().copy();building.freeLook=true;building.freeLookToggle=true;building.freeLookPerspective=local.luke.power.building.camera.Perspective.THIRD_PERSON;local.luke.power.building.config.Config.preview(building);local.luke.power.building.camera.FreeLook.begin(mc);local.luke.power.building.camera.FreeLook.turn(mc.player,450,100);}
      log("GHOST visible="+GhostHead.visible(mc)+" y="+HudCoordinates.y(mc.player));return;
    }
    if(action.equals("text")) {
      var screen=MenuUpdateChecks.open(mc,"Interface");search(screen,"alpha beta gamma");
      ((TextInput)field(screen,"search")).focused=true;screen.render(-1,-1,0);
      log("TEXT GEOMETRY "+call(screen,"searchLeft")+" 28 "+call(screen,"searchWidth")+" scale="+(mc.displayWidth/(double)screen.width));return;
    }
    if(action.equals("state")) {
      if(mc.currentScreen instanceof PowerOptionsScreen screen){var input=(TextInput)field(screen,"search");log("TEXT STATE "+input.text()+" cursor="+input.cursor()+" selection="+input.start()+":"+input.end());}return;
    }
    failures=0;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);enableCamera();PowerConfig.flushPending();var document=PowerConfig.document();
    var oldHud=InfoHudConfig.copy();var hud=InfoHudConfig.copy();hud.enabled=true;InfoHudConfig.preview(hud);
    var session=SettingsRegistry.open(mc);boolean third=mc.options.thirdPerson,debug=mc.options.debugHud,hidden=mc.options.hideHud;
    double px=mc.player.x,py=mc.player.y,pz=mc.player.z;
    var cheats=(local.luke.power.permissions.CheatWorld)mc.world.method_262();boolean hadCheats=cheats.power$cheatsEnabled();
    String oldMode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    try {
      active(false);cheats.power$cheatsEnabled(true);CreativeVehicleChecks.mode(mc,"SURVIVAL");cheats.power$cheatsEnabled(false);
      mc.player.method_1340(3.5,11.01+mc.player.field_1631,3.5);
      test("normal F3 and HUD share eye/feet policy and Cancel restores it",()->{
        var feet=find(session,"interface.useFeetYCoordinates");check(!feet.defaultValue.getAsBoolean(),"feet default on");
        for(boolean on:new boolean[]{false,true}) {
          feet.parse(Boolean.toString(on));session.preview();mc.options.debugHud=true;mc.options.hideHud=false;
          mc.inGameHud.render(1,false,0,0);for(int i=0;i<4;i++)InfoHud.tick(mc);
          double expected=on?mc.player.boundingBox.minY:mc.player.y;
          check(HudCoordinates.y(mc.player)==expected,"height policy");
          check(DebugOverlay.lastLayout().stream().anyMatch(line->line.text().equals("y: "+expected)),"F3 coordinate differs");
          var cluster=new InfoHudSettings.Cluster();cluster.coordinates=true;cluster.direction=false;
          check(InfoHud.lines(cluster).contains("3, "+(on?11:12)+", 3"),"custom coordinate differs "+InfoHud.lines(cluster));
        }
        session.discard();InfoHudConfig.preview(hud);check(HudCoordinates.y(mc.player)==mc.player.y,"Cancel failed");
      });
      test("freecam and spectator always use the head base, regardless of preference",()->{
        for(boolean feet:new boolean[]{false,true}) {
          MenuPreferences.previewFeetY(feet);pose(3.5,12.62,3.5,0,0);active(true);
          check(Math.abs(HudCoordinates.y(mc.player)-HeadGeometry.baseY(12.62))<1e-9,"freecam base");
          active(false);cheats.power$cheatsEnabled(true);CreativeVehicleChecks.mode(mc,"SPECTATOR");
          check(Math.abs(HudCoordinates.y(mc.player)-(mc.player.y-mc.player.field_1631+HeadGeometry.BASE_ABOVE_FEET))<1e-9,"spectator base");
          mc.options.thirdPerson=true;check(GhostHead.visible(mc),"third-person head absent");
          GhostHead.render(mc,.5F);check(GL11.glGetError()==0,"ghost GL error");
          mc.options.thirdPerson=false;check(!GhostHead.visible(mc),"first-person ghost visible");
          CreativeVehicleChecks.mode(mc,"SURVIVAL");cheats.power$cheatsEnabled(false);
        }
        MenuPreferences.previewFeetY(false);
      });
      test("manual full bright and its key are available without cheats",()->{
        var controls=SettingsRegistry.open(mc);check(SettingAccess.visible(controls,find(controls,"keys.power_beta.fullBright")),"key hidden");
        int code=RenderControls.FULL_BRIGHT.code;RenderControls.FULL_BRIGHT.code=118;
        try{mc.setScreen(null);mc.paused=false;RenderControls.reset(mc);Bindings.configure(Map.of());RenderControls.press(118);check(RenderControls.fullBright(),"manual toggle blocked");RenderControls.press(118);check(!RenderControls.fullBright(),"manual toggle stuck");}
        finally{RenderControls.FULL_BRIGHT.code=code;RenderControls.reset(mc);}
      });
      test("cluster labels change only in search and all four identities remain distinct",()->{
        var s=SettingsRegistry.open(mc);
        for(int i=0;i<4;i++) {
          var setting=find(s,"infoHud.clusters."+i+".coordinates");
          check(SettingLabels.display(setting,false).equals(setting.label),"ordinary label changed");
          check(SettingLabels.display(setting,true).equals(setting.label+" (cluster "+(i+1)+")"),"search cluster label missing");
        }
      });
      test("search chevrons toggle both ways while expanded titles navigate at narrow and wide sizes",()->{
        for(int[] size:new int[][]{{320,240},{640,420}}) {
          var s=MenuUpdateChecks.open(mc,"Interface");s.init(mc,size[0],size[1]);((Set<?>)field(s,"collapsed")).clear();search(s,"coordinates");field(s,"scroll",0.0);
          int left=(int)call(s,"left"),top=(int)call(s,"top");
          var rows=(List<?>)field(s,"rows");Object heading=rows.stream().filter(r->{try{return call(r,"setting")==null;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
          int rowY=top+(int)call(heading,"y")-(int)(double)field(s,"scroll");
          click(s,left+1,rowY+5);check(((Set<?>)field(s,"collapsed")).size()==1,"chevron did not collapse at "+left+","+rowY+" groups="+field(s,"collapsed"));
          check(((TextInput)field(s,"search")).text().equals("coordinates"),"chevron navigated");
          click(s,left+1,rowY+5);check(((Set<?>)field(s,"collapsed")).isEmpty(),"chevron did not expand");
          click(s,left+1,rowY+5);click(s,left+35,rowY+5);check(((Set<?>)field(s,"collapsed")).isEmpty(),"collapsed title did not expand");
          click(s,left+35,rowY+5);check(((TextInput)field(s,"search")).text().isEmpty(),"expanded title did not navigate");
          s.session().discard();mc.setScreen(null);
        }
      });
      test("custom and native text inputs support pointer selection, deletion and length limits",()->{
        var s=MenuUpdateChecks.open(mc,"Interface");s.init(mc,640,420);search(s,"alpha beta gamma");s.render(-1,-1,0);
        var editor=(TextInput)field(s,"search");int left=(int)call(s,"searchLeft");
        click(s,left+4+mc.textRenderer.getWidth("alpha "),34);TextDrag.event(s,left+4+mc.textRenderer.getWidth("alpha beta"),0,false,true);
        check(editor.text().substring(editor.start(),editor.end()).equals("beta"),"custom pointer selection");editor.key('X',Keyboard.KEY_X);check(editor.text().equals("alpha X gamma"),"custom replace selection");
        var nativeField=new TextFieldWidget(s,mc.textRenderer,10,80,100,18,"abcdef");nativeField.setMaxLength(6);TextDrag.context(s,true);nativeField.render();
        nativeField.mouseClicked(14+mc.textRenderer.getWidth("a"),85,0);TextDrag.event(s,14+mc.textRenderer.getWidth("abcd"),0,false,true);
        nativeField.keyPressed('X',Keyboard.KEY_X);check(nativeField.getText().equals("aXef"),"native replace selection "+nativeField.getText());
        nativeField.keyPressed('Y',Keyboard.KEY_Y);nativeField.keyPressed('Z',Keyboard.KEY_Z);nativeField.keyPressed('Q',Keyboard.KEY_Q);check(nativeField.getText().length()==6,"native limit");
        TextDrag.cancel();s.session().discard();mc.setScreen(null);
      });
      test("UI blink has an independent default-on preview and rollback",()->{
        var s=SettingsRegistry.open(mc);var ui=find(s,"interface.blinkingUiCursor");var chat=find(s,"interface.blinkingChatCursor");
        check(ui.defaultValue.getAsBoolean() && !chat.defaultValue.getAsBoolean(),"blink defaults");
        ui.parse("false");s.preview();check(!InterfaceState.blinkingUiCursor && InterfaceState.uiCursorVisible(),"steady UI cursor");
        check(!InterfaceState.blinkingChatCursor,"UI blink changed chat");s.discard();check(InterfaceState.blinkingUiCursor,"UI rollback failed");
      });
    } finally {
      active(false);local.luke.power.building.camera.FreeLook.reset(mc);session.discard();InfoHudConfig.preview(oldHud);PowerConfig.write(document);
      mc.setScreen(null);mc.player.method_1340(px,py,pz);mc.options.thirdPerson=third;mc.options.debugHud=debug;mc.options.hideHud=hidden;
      cheats.power$cheatsEnabled(true);CreativeVehicleChecks.mode(mc,oldMode);cheats.power$cheatsEnabled(hadCheats);
    }
    log("INTERFACE 170 FAILURES "+failures);
  }
}
