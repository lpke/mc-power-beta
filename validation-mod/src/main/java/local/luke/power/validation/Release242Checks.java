package local.luke.power.validation;

import static local.luke.power.validation.Release240Checks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.class_267;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Regression checks dispatched only in a muted disposable client. */
final class Release242Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-242");
  private static HudEditorScreen highlighted;
  private static Object audio(String name, Object... args) throws Exception {
    for (var method : AudioController.class.getDeclaredMethods())
      if (method.getName().equals(name) && method.getParameterCount() == args.length) {
        method.setAccessible(true); return method.invoke(null, args);
      }
    throw new NoSuchMethodException(name);
  }
  private static void shot(Minecraft mc, Screen screen, String name) throws Exception {
    SettingsSnapshot.render(mc, screen, screen.width, screen.height, REPORT.resolve(name + ".png"));
  }
  static void run(Minecraft mc, String action) throws Exception {
    Files.createDirectories(REPORT);
    if (action.equals("expiry")) {
      check(highlighted != null && !(boolean)call(highlighted, "highlighting"), "highlight did not expire after four seconds");
      shot(mc, highlighted, "highlight-expired"); log("PASS highlight expired on the real clock"); return;
    }
    failures = 0;
    if (action.equals("ui")) ui(mc);
    else if (action.equals("audio")) music(mc);
    else if (action.equals("saved-rules")) {
      check(mc.world == null, "return to title before editing saved rules");
      var original = Files.readAllBytes(Path.of("saves/Power Beta release check/level.dat"));
      PowerOptionsScreen.openSavedWorldRules(mc, mc.currentScreen, "Power Beta release check");
      rules(mc, (PowerOptionsScreen)mc.currentScreen, "saved");
      check(Arrays.equals(original, Files.readAllBytes(Path.of("saves/Power Beta release check/level.dat"))), "viewing rules wrote metadata");
    } else throw new IllegalArgumentException(action);
    log("RELEASE 2.4.2 " + action + " FAILURES " + failures);
  }
  private static void rules(Minecraft mc, PowerOptionsScreen screen, String name) throws Exception {
    test(name + " rules are centered at the original content width without sidebar input", () -> {
      for (int[] size : new int[][]{{320,240},{640,420},{960,540},{1720,720},{540,960}}) {
        screen.init(mc, size[0], size[1]);
        int available = Math.min(size[0], size[1] * 16 / 9);
        int expected = available - Math.max(90, Math.min(155, available / 4)) - 30;
        int left = n(screen,"left"), span = n(screen,"span");
        check(n(screen,"sidebar") == 0, "sidebar remains");
        check(span == expected, "content width changed");
        check(Math.abs(left + span / 2d - size[0] / 2d) <= 1, "content not centered");
        set(screen,"scroll",46d); set(screen,"sideScroll",0d);
        click(screen, n(screen,"origin")+2, 35, 0);
        check(((Number)get(screen,"scroll")).doubleValue()==46, "former sidebar claimed a click");
        set(screen,"scroll",0d);
        shot(mc, screen, name + "-rules-" + size[0] + "x" + size[1]);
      }
      UiChecks.search(screen, "doFireTick");
      check(!((List<?>)get(screen,"rows")).isEmpty(), "rule aliases not searchable");
      UiChecks.search(screen, "");
    });
  }
  private static void ui(Minecraft mc) throws Exception {
    MenuPreferences.update(value -> value.autoApply = false);
    var options = new PowerOptionsScreen(mc.currentScreen); open(mc, options, 960, 540);
    test("Interface starts with HUD editor and an open General group without a divider", () -> {
      set(options,"page","Interface"); UiChecks.search(options,""); call(options,"layout");
      var rows = (List<?>)get(options,"rows");
      check(((Setting)call(rows.get(0),"setting")).id.equals(HudEditorSettings.ENTRY), "HUD editor not first");
      check(call(rows.get(1),"group").equals("General") && (boolean)call(rows.get(1),"heading"), "General group missing");
      check(rows.stream().noneMatch(row -> { try { return (boolean)call(row,"separator"); } catch(Exception e) { throw new RuntimeException(e); } }), "Interface retains divider");
      check(options.session().settings().stream().filter(s -> s.page.equals("Interface") && s.group.isEmpty())
          .allMatch(s -> s.id.equals(HudEditorSettings.ENTRY)), "ungrouped Interface setting remains");
      shot(mc, options, "interface");
    });
    test("preview toggles share wide selector rows and remain reachable after resizing", () -> {
      var hud = new HudEditorScreen(options, HudEditorSettings.ENTRY);
      for (int[] size : new int[][]{{320,240},{640,420},{960,540},{1720,720},{540,960}}) {
        open(mc,hud,size[0],size[1]); Object toolbar = call(hud,"toolbar");
        if(size[0]==1720) check(n(toolbar,"labelY")==35 && n(hud,"controlsTop")==55, "wide toolbar wastes a row");
        for(Object toggle : (List<?>)call(toolbar,"toggles")) {
          check(n(toggle,"x")>=10 && n(toggle,"x")+n(toggle,"width")<=size[0]-10,"toggle outside screen");
        }
        check(n(hud,"previewBottom")-n(hud,"previewTop")>=48, "preview lost space");
        shot(mc,hud,"hud-"+size[0]+"x"+size[1]);
      }
    });
    test("General preview clicks reveal their setting and outline only the clicked item for four seconds", () -> {
      highlighted = new HudEditorScreen(options,HudEditorSettings.ENTRY); open(mc,highlighted,960,540);
      var draft = draft(highlighted); Object preview = get(highlighted,"preview");
      for(String id : List.of("power_hud:config.disableCrosshair","power_hud:config.drawXboxXAndYButtons","power_controls:userinterface.hideAchievementToast"))
        draft.get(id).value = new JsonPrimitive(true);
      var before = draft.snapshot(); byte[] disk = Files.readAllBytes(PowerConfig.path());
      for(var entry : Map.of("crosshair","power_hud:config.disableCrosshair","controller","power_hud:config.drawXboxXAndYButtons","toast","power_controls:userinterface.hideAchievementToast").entrySet()) {
        call(highlighted,"select","debug"); ((Set<String>)get(highlighted,"collapsed")).add("layout/Visibility");
        highlighted.render(-1,-1,0);
        Object element = ((List<?>)call(preview,"elements")).stream().filter(e -> { try { return call(e,"id").equals(entry.getKey()); } catch(Exception ex) { throw new RuntimeException(ex); } }).findFirst().orElseThrow();
        double scale = ((Number)call(highlighted,"scale")).doubleValue();
        int x = (int)Math.round(((Number)call(highlighted,"canvasX")).doubleValue() + (n(element,"x")+n(element,"width")/2d)*scale);
        int y = (int)Math.round(((Number)call(highlighted,"canvasY")).doubleValue() + (n(element,"y")+n(element,"height")/2d)*scale);
        click(highlighted,x,y,0);
        check(get(highlighted,"selected").equals("layout") && get(highlighted,"highlightedSetting").equals(entry.getValue()), "click missed " + entry.getKey() + ": selected=" + get(highlighted,"selected"));
        check(get(highlighted,"highlightedElement").equals(entry.getKey()) && (boolean)call(highlighted,"highlighting"), "preview outline missing");
        long remaining = ((Number)get(highlighted,"highlightUntil")).longValue()-System.nanoTime();
        check(remaining>3_500_000_000L && remaining<=4_000_000_000L, "highlight duration differs");
        check(!((Set<?>)get(highlighted,"collapsed")).contains("layout/Visibility"), "highlighted setting remains collapsed");
        int row = (int)call(highlighted,"controlY",draft.get(entry.getValue()));
        check(row>=n(highlighted,"controlsTop")+20 && row+19<=n(highlighted,"controlsBottom"), "highlighted setting offscreen");
        shot(mc,highlighted,"highlight-"+entry.getKey());
      }
      check(before.equals(draft.snapshot()) && Arrays.equals(disk,Files.readAllBytes(PowerConfig.path())), "highlight changed settings");
    });
    PowerOptionsScreen.openCreationRules(mc,options,new CreationRules(RulePresets.survival(),RulePresets.cheats()));
    rules(mc,(PowerOptionsScreen)mc.currentScreen,"creation");
    mc.setScreen(highlighted);
  }
  private static void music(Minecraft mc) throws Exception {
    AudioSettings original = AudioConfig.copy(); float volume = mc.options.musicVolume;
    try {
      var config = new AudioSettings(); config.menuMusic=AudioSettings.MenuMusic.WORLD;
      config.shuffle=false; AudioConfig.preview(config); mc.options.musicVolume=1;
      AudioController.quiet();
      test("waiting reservation survives volume, timing, sound and menu-control edits", () -> {
        var reserved = (class_267)audio("upcoming"); check(reserved!=null,"no initial reservation");
        String id=AudioController.trackId(reserved);
        for(int i=1;i<=80;i++) {
          var next=AudioConfig.copy(); next.master=10+i; next.gapMinSeconds=100+i; next.menuControlsOffsetX=i;
          next.sounds.put("random.click",i); next.groupVolumes.put("minecraft",i);
          AudioConfig.preview(next);
          check(audio("upcoming")==reserved,"settings re-rolled "+id+" at edit "+i);
        }
        AudioController.rulesChanged();
        check(audio("upcoming")==reserved,"ambient rule refresh re-rolled reservation");
        check(AudioController.status().contains(AudioController.musicLabel(id)),"waiting UI lost advertised track");
      });
      test("excluded reservations change once and playback consumes the advertised track", () -> {
        var first=(class_267)audio("upcoming"); var next=AudioConfig.copy();
        next.disabledTracks.add(AudioController.trackId(first)); AudioConfig.preview(next);
        var replacement=(class_267)audio("upcoming");
        check(replacement!=null && !AudioController.trackId(replacement).equals(AudioController.trackId(first)), "excluded track retained");
        check(audio("upcoming")==replacement,"replacement did not stay reserved");
        check(AudioController.choose(List.of())==replacement,"playback differed from advertised track");
      });
    } finally { AudioController.quiet(); AudioConfig.preview(original); mc.options.musicVolume=volume; }
  }
}
