package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Release310Checks.*;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.class_564;

/** Scroll ownership and defaults follow-up checks. Only install in disposable clients. */
final class Polish310Checks {
  private static WorldMapScreen armed;
  private static PowerOptionsScreen panel;
  private static MapSettings beforeMap;
  private static boolean beforeAuto;
  private static double centerX, centerZ;
  static void run(Minecraft mc, String mode) throws Exception {
    if (mode.startsWith("polish-scroll-")) { scroll(mc, mode); return; }
    failures = 0;
    var previous = mc.currentScreen; var mapSettings = MapConfig.copy(); boolean auto = MenuPreferences.current().autoApply;
    PowerConfig.flushPending(); byte[] original = Files.readAllBytes(PowerConfig.path());
    try {
      MenuPreferences.current().autoApply = false;
      test("sidebar pointer motion retains scrollbar until actual release", () -> {
        var map = new WorldMapScreen(); mc.setScreen(map); map.init(mc, 960, 540); call(map, "toggleSettings");
        var options = (PowerOptionsScreen)field(map, "settingsPanel");
        ((Set<?>)field(options, "collapsed")).clear(); call(options, "layout");
        var track = (ScrollBar.Track)call(options, "contentTrack"); check(track.visible(), "need scrollable map settings");
        var input = (ScreenInput)(Object)map;
        int x = track.x(), y = (int)track.y() + track.thumb()/2;
        input.power$click(x, y, 0);
        check((Boolean)field(field(options, "contentBar"), "dragging"), "thumb did not acquire drag");
        input.power$release(x, y + 40, -1);
        check((Boolean)field(map, "panelPress") && (Boolean)field(field(options, "contentBar"), "dragging"), "native motion released scrollbar");
        input.power$release(900, y + 40, 0);
        check(!(Boolean)field(map, "panelPress") && !(Boolean)field(field(options, "contentBar"), "dragging"), "outside release retained scrollbar");
        check(!(Boolean)field(map, "dragging"), "sidebar press became map pan");
      });
      test("footer wraps only when actual action widths overlap", () -> {
        for (boolean pause : List.of(false, true)) for (String page : List.of("General", "Map", "Controls", "Video")) {
          var options = new PowerOptionsScreen(null, pause); mc.setScreen(options); field(options, "page", page);
          for (int width : new int[]{280, 320, 360, 400, 470, 699, 700}) {
            options.init(mc, width, 540); field(options, "page", page); call(options, "layout");
            int actions = page.equals("Controls") || page.equals("Video") ? (int)call(options, "disabledX")
                : (int)invoke(options,"actionX",new Class<?>[]{int.class},0);
            boolean fits = (int)call(options, "resetX") + 92 <= actions;
            check((int)call(options,"footerY") == 540 - (fits ? 28 : 51), "wrong footer at " + width + "/" + page + "/" + pause);
          }
        }
        var options = new PowerOptionsScreen(null); mc.setScreen(options); field(options,"page","Map");
        render(mc,options,"polish-footer-360",360,240);
        check((int)call(options,"footerY")==212,"360px map footer unnecessarily stacked");
      });
      test("appearance launcher opens Minimap without losing direct-setting highlights", () -> {
        var options = new PowerOptionsScreen(null); mc.setScreen(options);
        invoke(options,"activate",new Class<?>[]{Setting.class,int.class},setting(options,"maps.hudEditor"),0);
        var hud = mc.currentScreen;
        check(hud instanceof HudEditorScreen && field(hud,"selected").equals("minimap"),"appearance selected wrong HUD element");
        check(field(hud,"highlightedSetting").equals("") && field(hud,"initialSetting").equals(""),"launcher retained highlight");
        render(mc,hud,"polish-minimap-editor",960,540);
        check(field(hud,"highlightedSetting").equals(""),"resize reintroduced highlight");
        mc.setScreen(options); invoke(options,"openHudSetting",new Class<?>[]{String.class},"maps.zoom");
        check(field(mc.currentScreen,"highlightedSetting").equals("maps.zoom"),"direct related jump lost highlight");
      });
      test("untouched templates follow base with fresh editor rows and preserve manual drafts", () -> {
        RuleDefaults.resetLatest();
        var options = new PowerOptionsScreen(null); mc.setScreen(options);
        field(options,"session",SettingsRegistry.openDefaults(mc));
        var draft = setting(options,"rules.survival.allow_sleep"); draft.value = new JsonPrimitive(!draft.value.getAsBoolean());
        var wanted = draft.value.deepCopy();
        DefaultBases.use(DefaultBases.Base.BETA, Path.of(".")); call(options,"refreshRuleTemplates");
        check(!RuleDefaults.review().required(),"untouched switch opened defaults review");
        check(!setting(options,"rules.survival.allow_sprinting").value.getAsBoolean(),"editor retained old sprint default");
        check(setting(options,draft.id).value.equals(wanted),"base change erased manual draft");
        DefaultBases.use(DefaultBases.Base.POWER_BETA,Path.of(".")); call(options,"refreshRuleTemplates");
        check(!RuleDefaults.review().required() && setting(options,"rules.survival.allow_sprinting").value.getAsBoolean(),"return base did not follow");
        var custom = PowerConfig.section("gameRules");custom.getAsJsonObject("survival").getAsJsonObject("values").addProperty("allow_sleep",false);PowerConfig.put("gameRules",custom);
        DefaultBases.use(DefaultBases.Base.BETA,Path.of("."));
        check(custom.equals(PowerConfig.section("gameRules")) && RuleDefaults.review().required(),"custom template silently reset");
      });
    } finally {
      MapConfig.preview(mapSettings); MenuPreferences.current().autoApply = auto;
      PowerConfig.flushPending(); PowerConfig.write(JsonParser.parseString(new String(original,StandardCharsets.UTF_8)).getAsJsonObject());
      DefaultBases.reload(); mc.setScreen(previous);
    }
    log("POLISH310 COMPLETE failures=" + failures);
  }
  private static void scroll(Minecraft mc,String mode) throws Exception {
    if (mode.equals("polish-scroll-arm")) {
      check(armed==null && mc.world!=null,"open disposable world, finish previous drag");
      beforeMap=MapConfig.copy(); beforeAuto=MenuPreferences.current().autoApply; MenuPreferences.current().autoApply=false;
      var active=MapConfig.copy(); active.worldMap=active.minimap=true;MapConfig.preview(active);Maps.tick(mc);
      armed=new WorldMapScreen();mc.setScreen(armed);
      var size=new class_564(mc.options,mc.displayWidth,mc.displayHeight);armed.init(mc,size.method_1857(),size.method_1858());
      call(armed,"toggleSettings");panel=(PowerOptionsScreen)field(armed,"settingsPanel");
      ((Set<?>)field(panel,"collapsed")).clear();field(panel,"scroll",0d);call(panel,"layout");
      var track=(ScrollBar.Track)call(panel,"contentTrack");check(track.visible(),"sidebar not scrollable");
      var view=(MapMath.View)field(armed,"view");centerX=view.x;centerZ=view.z;
      double sx=mc.displayWidth/(double)size.method_1857(),sy=mc.displayHeight/(double)size.method_1858();
      Files.writeString(REPORT.resolve("scroll.json"),Catalog.JSON.toJson(Map.of("x",(int)((track.x()+1)*sx),"y",(int)((track.y()+track.thumb()/2)*sy),"endY",(int)((track.top()+track.height()-10)*sy),"outsideX",mc.displayWidth-30)));
      log("POLISH SCROLL ARMED");return;
    }
    check(armed!=null && mc.currentScreen==armed,"drag host changed");
    if(mode.equals("polish-scroll-held")) {
      check((Boolean)field(armed,"panelPress") && (Boolean)field(field(panel,"contentBar"),"dragging"),"physical motion cancelled drag");
      check((double)field(panel,"scroll")>100,"physical scrollbar did not move content");
      log("PASS physical sidebar held drag scroll="+field(panel,"scroll"));return;
    }
    try {
      check(!(Boolean)field(armed,"panelPress") && !(Boolean)field(field(panel,"contentBar"),"dragging"),"physical release outside retained drag");
      check(centerX==((MapMath.View)field(armed,"view")).x && centerZ==((MapMath.View)field(armed,"view")).z,"sidebar gesture moved map");
      log("PASS physical sidebar release outside, no map pan");
    } finally { panel.session().discard();MapConfig.preview(beforeMap);MenuPreferences.current().autoApply=beforeAuto;mc.setScreen(null);armed=null; }
  }
}
