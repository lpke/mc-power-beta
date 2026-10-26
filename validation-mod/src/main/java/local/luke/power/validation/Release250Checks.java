package local.luke.power.validation;

import static local.luke.power.validation.Release240Checks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import local.luke.power.validation.mixin.ScreenInput;
import org.lwjgl.opengl.GL11;

/** Only dispatched in a muted disposable instance. Never included in the pack. */
final class Release250Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-250");
  private static void capture(Minecraft mc, Screen s, String name) throws Exception {
    SettingsSnapshot.render(mc, s, s.width, s.height, REPORT.resolve(name + ".png"));
  }
  static void run(Minecraft mc, String action) throws Exception {
    Files.createDirectories(REPORT); failures = 0;
    if (action.equals("setup")) {
      test("fresh startup presents a chooser before the title", () -> {
        check(mc.currentScreen instanceof DefaultsSetupScreen, "chooser missing: " + mc.currentScreen);
        mc.currentScreen.init(mc, 960, 540); capture(mc, mc.currentScreen, "first-launch");
        chooserTooltips(mc);
        click(mc.currentScreen, 480, 78, 0);
        check(!DefaultBases.needsSetup(), "choice not saved");
        check(!(mc.currentScreen instanceof DefaultsSetupScreen), "Power Beta needs an unnecessary restart");
        mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
        check(!(mc.currentScreen instanceof DefaultsSetupScreen), "chooser repeated");
      });
    } else if (action.equals("beta-setup")) {
      check(mc.world == null, "return to title first");
      var state = new DefaultBases.State(); PowerConfig.save("defaults", state); DefaultBases.reload();
      mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen()); mc.currentScreen.init(mc,960,540);
      click(mc.currentScreen,480,134,0);
      check(DefaultBases.current() == DefaultBases.Base.BETA && !DefaultBases.needsSetup(), "Beta setup not saved");
      check((boolean)get(mc.currentScreen,"restart"), "Beta setup did not request a restart");
      capture(mc,mc.currentScreen,"beta-restart");
    } else if (action.equals("defaults")) defaults(mc);
    else if (action.equals("ui")) ui(mc);
    else if (action.equals("audio")) audio(mc);
    else if (action.equals("world")) {
      check(mc.world != null, "create a disposable world first");
      var original = WorldRules.of(mc.world.method_262()).copy();
      defaults(mc);
      check(original.equals(WorldRules.of(mc.world.method_262())), "loading defaults changed this world's rules");
      var options = new PowerOptionsScreen(null); open(mc, options, 960, 540);
      var title = new TitleLetteringScreen(options); open(mc, title, 960, 540);
      set(title, "resetPrompt", true);
      // Reproduce world rendering's enabled depth state before the modal draw.
      GL11.glEnable(GL11.GL_DEPTH_TEST); title.render(480, 282, 0);
      capture(mc, title, "title-reset-in-world");
    } else if (action.equals("saved-rules")) {
      check(mc.world == null, "return to title first");
      var original = Files.readAllBytes(Path.of("saves/Power Beta release check/level.dat"));
      PowerOptionsScreen.openSavedWorldRules(mc,mc.currentScreen,"Power Beta release check");
      var screen = (PowerOptionsScreen)mc.currentScreen; screen.init(mc,960,540);
      check(n(screen,"span") <= 640 && n(screen,"sidebar") == 0,"saved rules too wide");
      capture(mc,screen,"saved-rules");
      check(Arrays.equals(original,Files.readAllBytes(Path.of("saves/Power Beta release check/level.dat"))),"viewing saved rules wrote metadata");
    } else if (action.equals("saved")) {
      test("defaults selection and F3 presets survive restart", () -> {
        check(!DefaultBases.needsSetup(), "setup repeated");
        check(MenuPreferences.current().debugPresets.size() > 0, "saved F3 preset missing");
        DebugPresets.checked(MenuPreferences.current().debugPresets);
      });
    } else throw new IllegalArgumentException(action);
    log("RELEASE 2.5.0 " + action + " FAILURES " + failures);
  }
  private static boolean same(JsonElement a, JsonElement b) {
    return a.equals(b) || a.isJsonPrimitive() && b.isJsonPrimitive() && a.getAsJsonPrimitive().isNumber()
        && b.getAsJsonPrimitive().isNumber() && Math.abs(a.getAsDouble() - b.getAsDouble()) < 0.00001;
  }
  private static void defaults(Minecraft mc) throws Exception {
    var game = Path.of(".").toAbsolutePath();
    test("all three complete loads validate and refresh every registered reset value", () -> {
      var registered = new HashSet<String>();
      for (var setting : SettingsRegistry.openDefaults(mc).settings()) registered.add(setting.id);
      try (var input = new java.io.InputStreamReader(DefaultBases.class.getResourceAsStream("/assets/powerbeta/default-bases.json"), java.nio.charset.StandardCharsets.UTF_8)) {
        var profiles = JsonParser.parseReader(input).getAsJsonObject();
        for (var profile : profiles.entrySet()) for (var key : profile.getValue().getAsJsonObject().keySet()) {
          String id = key.startsWith("rule.") ? "rules.survival." + key.substring(5) : key;
          check(registered.contains(id), "unused defaults override " + profile.getKey() + " " + key);
        }
      }
      var comparison = new JsonArray();
      for (var setting : SettingsRegistry.openDefaults(mc).settings()) {
        var row = new JsonObject();
        row.addProperty("id",setting.id); row.addProperty("page",setting.page); row.addProperty("group",setting.group);
        row.addProperty("label",setting.label); row.addProperty("description",setting.description);
        row.addProperty("kind",setting.kind.name()); row.add("choices",Catalog.JSON.toJsonTree(setting.choices));
        var values = new JsonObject();
        var displays = new JsonObject();
        for (var base : DefaultBases.Base.values()) values.add(base.id,DefaultBases.value(base,setting.id,setting.powerBetaDefault));
        for (var base : DefaultBases.Base.values()) displays.addProperty(base.id,setting.display(values.get(base.id)));
        row.add("values",values); row.add("display",displays); comparison.add(row);
      }
      Files.writeString(REPORT.resolve("defaults-comparison.json"),Catalog.JSON.toJson(comparison));
      for (var base : DefaultBases.Base.values()) {
        var before = PowerConfig.document().getAsJsonObject("settings").deepCopy();
        DefaultBases.use(base, game);
        var after = PowerConfig.document().getAsJsonObject("settings").deepCopy();
        before.remove("defaults"); after.remove("defaults"); check(before.equals(after), "Use as base changed values");
        var session = SettingsRegistry.openDefaults(mc);
        check(!session.settings().isEmpty(), "empty settings");
        for (var setting : session.settings()) {
          try { setting.validate(DefaultBases.value(base, setting.id, setting.powerBetaDefault)); }
          catch (Exception failure) { throw new AssertionError(base + " invalid default " + setting.id + ": " + failure, failure); }
        }
        DefaultBases.load(base, mc, game);
        check(DefaultBases.current() == base, "base not applied");
        var panorama = PowerConfig.section("power_controls:userinterface").getAsJsonObject("panoramaConfig");
        check(panorama.get("blurBackground").getAsBoolean(), "panorama blur default disabled for " + base);
        check(panorama.get("enablePanorma").getAsBoolean() == (base == DefaultBases.Base.POWER_BETA),"panorama enable flag not linked to base selection");
        var loaded = SettingsRegistry.openDefaults(mc);
        for (var id : List.of("creative.sprintToggle", "creative.spectatorSprintToggle", "power_camera:config.sprintToggle"))
          check(loaded.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow().value.getAsBoolean(),base + " not Toggle " + id);
        var speed=loaded.settings().stream().filter(s -> s.id.equals("power_camera:config.speed")).findFirst().orElseThrow();
        check(speed.display().equals("110%"),"freecam display/default differs: "+speed.display());
        for (var setting : loaded.settings()) if (setting.numeric()) {
          check(!setting.label.endsWith(" %"),"percent label retained "+setting.id);
          if(Catalog.text(Catalog.metadata(setting.id),"unit","").equals("%")) check(setting.display().endsWith("%"),"percent suffix missing "+setting.id);
        }
        check(loaded.settings().stream().filter(s -> s.id.equals("native.guiScale")).findFirst().orElseThrow().value.getAsInt() == 4,"GUI scale changed for " + base);
        for (var setting : loaded.settings()) if (!Set.of("audio.playlists", "audio.favourites", "logo.playlists", "interface.debugPresets", "interface.colours", "defaults.base", "defaults.setupComplete").contains(setting.id)) {
          check(same(setting.value, DefaultBases.value(base, setting.id, setting.powerBetaDefault)), "load mismatch " + base + " " + setting.id + " = " + setting.value + " expected " + DefaultBases.value(base, setting.id, setting.powerBetaDefault));
        }
      }
      DefaultBases.load(DefaultBases.Base.POWER_BETA, mc, game);
    });
    var options = new PowerOptionsScreen(mc.currentScreen); open(mc, options, 960, 540);
    test("Defaults page appears after Advanced and load confirmation owns input", () -> {
      set(options, "page", "Defaults"); call(options, "layout");
      var pages = options.visiblePages(); check(pages.indexOf("Defaults") == pages.indexOf("Advanced") + 1, "wrong Defaults location");
      capture(mc, options, "defaults-wide");
      Object panel = get(options, "defaultsPanel");
      options.render(-1, -1, 0);
      int hoverX=n(options,"left")+10;
      int rowHeight=(int)call(panel,"rowTop",n(options,"span"),1)-(int)call(panel,"rowTop",n(options,"span"),0);
      check(rowHeight==24,"Defaults row differs from normal settings");
      int rowSpan=(int)call(panel,"rowSpan",n(options,"span")),rowTop=(int)call(panel,"rowTop",n(options,"span"),0);
      check(rowSpan<=560,"wide defaults buttons too far from labels");
      int buttonY=(int)call(panel,"buttonY",rowSpan,rowTop);
      check(buttonY-rowTop==3,"Defaults buttons not centered in hover row");
      int hoverY=rowTop+8;
      options.render(hoverX,hoverY,0);
      Object delay=get(panel,"hoverDelay");
      check((int)get(delay,"ticks")==0,"tooltip appeared immediately");
      for(int i=0;i<13;i++)options.tick();
      options.render(hoverX,hoverY,0);
      GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glEnable(GL11.GL_LIGHTING);
      call(panel,"foregroundTooltip","tooltip state check",hoverX,90);
      check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST) && GL11.glIsEnabled(GL11.GL_LIGHTING),"tooltip leaked GL state");
      SettingsSnapshot.render(mc,options,960,540,REPORT.resolve("defaults-tooltip.png"),hoverX,hoverY);
      click(options,(int)call(panel,"loadX",n(options,"left"),rowSpan)+4,buttonY+6,0);
      check((boolean)call(panel,"modal"), "Load did not warn");
      byte[] before = Files.readAllBytes(PowerConfig.path());
      click(options, n(options,"origin")+3, 28, 0);
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "modal leaked input");
      capture(mc, options, "defaults-warning"); key(options, Keyboard.KEY_ESCAPE);
      check(!(boolean)call(panel,"modal"), "Escape failed to cancel");
      for (int[] size : new int[][]{{320,240},{640,420}}) {
        options.init(mc,size[0],size[1]); capture(mc,options,"defaults-"+size[0]);
        panel=get(options,"defaultsPanel");
        call(panel,"wheel",-120,n(options,"left"),n(options,"span"));options.render(-1,-1,0);
        rowSpan=(int)call(panel,"rowSpan",n(options,"span"));
        int last=(int)call(panel,"rowTop",n(options,"span"),2);
        int lastButton=(int)call(panel,"buttonY",rowSpan,last);
        check(lastButton+18<=size[1]-44,"last defaults choice cannot be reached");
        click(options,(int)call(panel,"loadX",n(options,"left"),rowSpan)+4,lastButton+6,0);
        check((boolean)call(panel,"modal"),"scrolled Load did not hit rendered button");key(options,Keyboard.KEY_ESCAPE);
        capture(mc,options,"defaults-scrolled-"+size[0]);
      }
    });
  }
  private static void chooserTooltips(Minecraft mc) throws Exception {
    var screen=mc.currentScreen;
    screen.render(480,78,0);
    check((int)get(get(screen,"hoverDelay"),"ticks")==0,"welcome tooltip appeared immediately");
    for(int i=0;i<13;i++)screen.tick();
    SettingsSnapshot.render(mc,screen,960,540,REPORT.resolve("welcome-tooltip.png"),480,78);
  }
  private static void ui(Minecraft mc) throws Exception {
    MenuPreferences.update(v -> v.autoApply = false);
    var options = new PowerOptionsScreen(mc.currentScreen); open(mc, options, 960, 540);
    test("HUD editor is first in General's Interface group", () -> {
      set(options,"page","General"); UiChecks.search(options,""); call(options,"layout");
      var ordered = SettingsLayout.ordered(options.session().settings()).stream().filter(s -> s.page.equals("General") && s.group.equals("Interface")).toList();
      check(ordered.get(0).id.equals(HudEditorSettings.ENTRY),"HUD editor misplaced"); capture(mc,options,"general");
      UiChecks.search(options,"HUD editor");options.render(-1,-1,0);
      Object launcher=null;
      for(Object row:(List<?>)get(options,"rows")) {
        Object setting=call(row,"setting");
        if(setting instanceof Setting s && s.id.equals(HudEditorSettings.ENTRY)) {launcher=row;break;}
      }
      check(launcher!=null,"launcher missing in search");
      check(call(launcher,"group").equals("General / Interface"),"launcher search group hardcoded");
      var setting=(Setting)call(launcher,"setting");
      check((int)call(options,"controlEnd",setting)==n(options,"right")-14,"launcher still reserves reset width");
      set(options,"scroll",Math.max(0,((Number)call(launcher,"y")).doubleValue()-24));options.render(-1,-1,0);
      int cy=n(options,"top")+n(launcher,"y")-((Number)get(options,"scroll")).intValue()+9;
      capture(mc,options,"hud-launcher-search");
      click(options,n(options,"right")-24,cy,0);
      check(mc.currentScreen instanceof HudEditorScreen,"old reset position did not open HUD editor");
      open(mc,options,960,540);UiChecks.search(options,"");
    });
    test("F3 selection reveals settings and range dragging stays in its nested draft", () -> {
      var hud = new HudEditorScreen(options,HudEditorSettings.ENTRY); open(mc,hud,960,540);
      var outer = draft(hud); var before = outer.snapshot(); call(hud,"beginReorder"); hud.render(-1,-1,0);
      Object reorder = get(hud,"reorder"); var nested = (SettingsDraft)call(reorder,"draft");
      int left = ((Number)get(reorder,"left")).intValue(), top = ((Number)get(reorder,"top")).intValue();
      String selectedSetting=DebugLines.find(DebugOrder.read(nested.get("interface.debugOrder").value).get(0)).setting();
      click(hud,left+30,top+7,0); ((ScreenInput)(Object)hud).power$release(left+30,top+7,0);
      check(get(hud,"highlightedSetting").equals(selectedSetting),"selection not linked to setting");
      check((int)call(hud,"controlY",nested.get(selectedSetting))>=n(hud,"controlsTop"),"setting not revealed");
      var f=Keyboard.class.getDeclaredField("keyDownBuffer");f.setAccessible(true);var keys=(ByteBuffer)f.get(null);byte prior=keys.get(Keyboard.KEY_LSHIFT);
      try { keys.put(Keyboard.KEY_LSHIFT,(byte)1); click(hud,left+30,top+31,0); ((ScreenInput)(Object)hud).power$release(left+30,top+31,0); }
      finally { keys.put(Keyboard.KEY_LSHIFT,prior); }
      check(get(hud,"highlightedSetting").equals(""),"range retained single highlight");
      check(((Number)get(reorder,"first")).intValue()==0 && ((Number)get(reorder,"last")).intValue()==2,"range not inclusive");
      click(hud,left+30,top+19,0);
      ((ScreenInput)(Object)hud).power$release(left+30,top+7+12*9,0);
      check((boolean)call(reorder,"changed"),"group did not move");
      check(before.equals(outer.snapshot()),"nested edits escaped early"); capture(mc,hud,"f3-range");
      key(hud,Keyboard.KEY_ESCAPE);
      check((boolean)call(get(hud,"reorderExit"),"active"),"dirty Escape did not warn");
      capture(mc,hud,"f3-warning"); key(hud,Keyboard.KEY_ESCAPE);
      check((boolean)call(reorder,"active"),"keep editing closed F3");
      call(hud,"finishReorder",false); check(before.equals(outer.snapshot()),"discard changed parent");
      call(hud,"beginReorder"); hud.render(-1,-1,0); nested=(SettingsDraft)call(reorder,"draft");
      var order = new ArrayList<>(DebugOrder.read(nested.get("interface.debugOrder").value)); Collections.swap(order,0,1); nested.get("interface.debugOrder").value=Catalog.JSON.toJsonTree(order);
      int toolbar=((Number)get(reorder,"toolbarTop")).intValue(), w=(int)call(reorder,"toolbarButtonWidth");
      click(hud,left+w+12,toolbar+6,0);
      Object name=get(reorder,"name"); call(name,"setText","Release layout"); key(hud,Keyboard.KEY_RETURN);
      check(nested.get("interface.debugPresets").value.getAsJsonArray().size()>0,"preset not saved in nested draft");
      call(hud,"finishReorder",true); check(outer.changed(),"Save did not reach HUD draft");
      check(options.session().changes()==0,"Save escaped HUD draft");
      call(hud,"save"); call(options,"save",false,false);
      check(MenuPreferences.current().debugPresets.size()>0,"preset not durable");
      var clean = new HudEditorScreen(options,HudEditorSettings.ENTRY); open(mc,clean,960,540); call(clean,"beginReorder"); key(clean,Keyboard.KEY_ESCAPE);
      check(mc.currentScreen==clean && !(boolean)call(get(clean,"reorder"),"active"),"clean Escape left HUD editor");
      for(int[] size:new int[][]{{320,240},{480,270},{640,360},{640,420},{1720,720},{540,960}}) { clean.init(mc,size[0],size[1]); call(clean,"beginReorder"); capture(mc,clean,"f3-"+size[0]); call(clean,"finishReorder",false); }
    });
    test("compact HUD panels and toggles preserve nested edits, selection, scroll and warnings", () -> {
      var hud = new HudEditorScreen(options,HudEditorSettings.ENTRY); open(mc,hud,320,240);
      var outer=draft(hud); var original=outer.snapshot();
      hud.render(-1,-1,0);
      check((boolean)call(hud,"controlsVisible"),"normal controls start hidden");
      int toggleX=n(hud,"panelToggleX")+8;
      double scrollBefore=((Number)get(hud,"scroll")).doubleValue();
      click(hud,toggleX,38,0); hud.render(-1,-1,0);
      check(!(boolean)call(hud,"controlsVisible"),"hide control failed");
      check(n(hud,"previewTop")==n(hud,"controlsTop"),"hidden panel still reserves space");
      check(original.equals(outer.snapshot()),"hiding changed settings");
      click(hud,toggleX,38,0); hud.render(-1,-1,0);
      check((boolean)call(hud,"controlsVisible"),"show control failed");
      check(((Number)get(hud,"scroll")).doubleValue()==scrollBefore,"showing reset scroll");
      int eyeX=toggleX+22;
      check(call(hud,"hoverHelp",toggleX,38).equals("Hide settings"),"settings tooltip differs");
      check(call(hud,"hoverHelp",eyeX,38).equals("Hide preview"),"preview tooltip differs");
      click(hud,eyeX,38,0);hud.render(-1,-1,0);
      check(!(boolean)call(hud,"previewVisible"),"eye did not hide preview");
      check(n(hud,"controlsBottom")==n(hud,"previewBottom"),"hidden preview still reserves space");
      check(original.equals(outer.snapshot()),"hiding preview changed settings");
      check(call(hud,"hoverHelp",eyeX,38).equals("Show preview"),"closed eye tooltip differs");
      capture(mc,hud,"hud-preview-hidden");
      click(hud,toggleX,38,0);hud.render(-1,-1,0);
      check((boolean)call(hud,"previewVisible") && !(boolean)call(hud,"controlsVisible"),"both panels hidden");
      click(hud,toggleX,38,0);hud.render(-1,-1,0);
      call(hud,"beginReorder"); hud.render(-1,-1,0);
      Object reorder=get(hud,"reorder"); var nested=(SettingsDraft)call(reorder,"draft");
      check(!(boolean)call(hud,"controlsVisible"),"compact F3 not expanded");
      String firstId=DebugOrder.read(nested.get("interface.debugOrder").value).get(0);
      String firstSetting=DebugLines.find(firstId).setting();
      int left=((Number)get(reorder,"left")).intValue(),top=((Number)get(reorder,"top")).intValue();
      click(hud,left+30,top+7,0); ((ScreenInput)(Object)hud).power$release(left+30,top+7,0);
      check(get(hud,"highlightedSetting").equals(firstSetting),"hidden selection lost");
      click(hud,toggleX,38,0); hud.render(-1,-1,0);
      check((boolean)call(reorder,"active") && (boolean)call(hud,"controlsVisible"),"showing controls exited F3");
      int selectedY=((Number)call(hud,"controlY",nested.get(firstSetting))).intValue();
      check(selectedY>=n(hud,"controlsTop")+24 && selectedY+18<=n(hud,"controlsBottom"),"selected row not revealed below sticky heading");
      var order=new ArrayList<>(DebugOrder.read(nested.get("interface.debugOrder").value));Collections.swap(order,0,1);
      nested.get("interface.debugOrder").value=Catalog.JSON.toJsonTree(order);
      var before=nested.snapshot(); click(hud,toggleX,38,0);hud.render(-1,-1,0);
      check(before.equals(nested.snapshot()),"panel toggle changed F3 draft");
      Object toolbar=call(hud,"toolbar");
      var toggles=(List<?>)call(toolbar,"toggles");
      for(Object toggle:toggles) {
        String id=(String)call(toggle,"id");
        boolean old=(boolean)call(hud,"toggleEnabled",id);
        click(hud,n(toggle,"x")+4,n(toggle,"y")+6,0);
        check((boolean)call(hud,"toggleEnabled",id)!=old,"compact toggle failed "+id);
        check((boolean)call(reorder,"active"),"preview toggle exited F3");
      }
      check(before.equals(nested.snapshot()),"preview toggles changed F3 settings");
      click(hud,eyeX,38,0);hud.render(-1,-1,0);
      check((boolean)call(reorder,"active") && !(boolean)call(hud,"previewVisible") && (boolean)call(hud,"controlsVisible"),"eye exited F3 or hid both panels");
      check(before.equals(nested.snapshot()),"eye changed nested draft");
      capture(mc,hud,"hud-f3-preview-hidden");
      click(hud,eyeX,38,0);click(hud,toggleX,38,0);hud.render(-1,-1,0);
      key(hud,Keyboard.KEY_ESCAPE);check((boolean)call(get(hud,"reorderExit"),"active"),"compact dirty Escape did not warn");
      click(hud,toggleX,38,0);check(!(boolean)call(hud,"controlsVisible"),"warning leaked panel input");
      click(hud,eyeX,38,0);check((boolean)call(hud,"previewVisible"),"warning leaked eye input");
      key(hud,Keyboard.KEY_ESCAPE);call(hud,"finishReorder",false);
      check((boolean)call(hud,"controlsVisible"),"F3 exit did not restore panel");
      check(original.equals(outer.snapshot()),"compact edits escaped discard");
      hud.init(mc,1720,720);hud.render(-1,-1,0);
      check(!(boolean)call(hud,"compact"),"large screen shows compact controls");
      check(n(hud,"controlsSpan")==360,"large screen layout changed");
      capture(mc,hud,"hud-wide-unchanged");
      for(int[] size : new int[][]{{320,240},{480,320},{540,360},{640,420},{800,600},{960,540}}) {
        hud.init(mc,size[0],size[1]);hud.render(-1,-1,0);
        Object bar=call(hud,"toolbar");
        var buttons=(List<?>)call(bar,"toggles");
        if(size[0]>=480) for(Object b:buttons) check(n(b,"y")==30,"toggles wrapped early at "+size[0]);
        check(n(hud,"previewBottom")==size[1]-35,"unused footer margin remains");
        capture(mc,hud,"hud-responsive-"+size[0]);
      }
    });
    test("creation rules stay centered with a narrower bounded content width", () -> {
      PowerOptionsScreen.openCreationRules(mc,options,new CreationRules(RulePresets.survival(),RulePresets.cheats()));
      var rules=(PowerOptionsScreen)mc.currentScreen;
      for(int[] size:new int[][]{{320,240},{960,540},{1720,720}}) {
        rules.init(mc,size[0],size[1]); int left=n(rules,"left"),span=n(rules,"span");
        check(n(rules,"sidebar")==0,"sidebar remains"); check(span<=640,"rules too wide"); check(Math.abs(left+span/2d-size[0]/2d)<=1,"rules not centered"); capture(mc,rules,"rules-"+size[0]);
      }
    });
  }
  private static void audio(Minecraft mc) throws Exception {
    test("music library shares percentage labels at every width", () -> {
      PowerOptionsScreen.openMusic(mc,mc.currentScreen,false,"music:calm1.ogg");
      var screen=(PowerOptionsScreen)mc.currentScreen;
      var volume=screen.session().settings().stream().filter(s->s.id.equals("audio.sound.music:calm1.ogg")).findFirst().orElseThrow();
      check(volume.display().equals(volume.editText()+"%"),"track percentage differs from exact entry");
      for(int[] size:new int[][]{{320,240},{960,540},{1720,720}}) {
        screen.init(mc,size[0],size[1]);capture(mc,screen,"music-percent-"+size[0]);
      }
    });
    test("next/previous handoffs never expose transient Starting labels", () -> {
      var config=AudioConfig.copy();config.master=100;AudioConfig.preview(config);mc.options.musicVolume=1;
      for(int i=0;i<12;i++) {
        if(i%2==0) AudioController.next(); else AudioController.previous();
        String expected=AudioController.status();
        check(expected.startsWith("Playing:"),"transient status: "+expected);
        for(int frame=0;frame<10;frame++) check(expected.equals(AudioController.status()),"handoff label flickered");
      }
    });
  }
}
