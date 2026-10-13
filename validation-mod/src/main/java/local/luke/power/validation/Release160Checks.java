package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.autowalk.*;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.hud.*;
import local.luke.power.input.*;
import local.luke.power.mixin.ItemHealthAccessor;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.world.WorldCycles;
import net.minecraft.class_142;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Actual configuration, input, item damage and HUD hooks in an isolated client. */
final class Release160Checks {
  private static final String HUD = "power_hud:hudpositions.";
  static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "save" -> {
        var hud = InfoHudConfig.copy(); hud.enabled = true; hud.clusters[1].textColor = "#12ABEF";
        hud.clusters[1].textSize = 1.7; hud.clusters[1].offsetX = -21; hud.clusters[1].opacity = 43;
        hud.clusters[1].hideWithDebug = false; InfoHudConfig.save(hud);
        MenuPreferences.update(v -> v.debugTimeOfDay = true);
        var game = GameplayConfig.copy(); game.lavaProofItems = true; GameplayConfig.save(game);
        var walking = Config.current().copy(); walking.autoWalkCancelOnLostFocus = true; Config.apply(walking);
        log("PASS saved 1.6.0 settings");
      }
      case "saved" -> {
        var h = InfoHudConfig.current();
        check(h.enabled && h.clusters[1].textColor.equals("#12ABEF") && h.clusters[1].textSize == 1.7
            && h.clusters[1].offsetX == -21 && h.clusters[1].opacity == 43 && !h.clusters[1].hideWithDebug,
            "cluster state lost on restart");
        check(MenuPreferences.debugTimeOfDay() && GameplayConfig.current().lavaProofItems
            && Config.current().autoWalkCancelOnLostFocus, "preferences lost on restart");
        inactive(mc);
        log("PASS all new settings survive restart with inactive tweaks");
      }
      case "activate" -> activate(mc);
      case "inactive" -> { inactive(mc); log("PASS all active tweaks reset"); }
      case "exit" -> { activate(mc); mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen()); mc.setWorld(null); inactive(mc); PowerConfig.flushPending(); log("PASS world exit cleared and saved active tweaks"); }
      case "stop" -> { activate(mc); mc.scheduleStop(); log("PASS requested shutdown with active tweaks"); }
      case "check" -> checks(mc);
      case "hud-scene", "placement-water", "placement-lava" -> scene(mc, action);
      case "debug" -> mc.options.debugHud = !mc.options.debugHud;
      case "background-key" -> {
        check(!org.lwjgl.opengl.Display.isActive() && AutoWalk.isWalking(), "requires unfocused auto-walk");
        mc.player.method_136(mc.options.forwardKey.code, true);
        mc.player.method_136(mc.options.forwardKey.code, false);
        check(AutoWalk.isWalking(), "a background movement event cancelled auto-walk");
        log("PASS background movement event preserves auto-walk");
      }
      case "hud-key", "hud-chord", "hud-mouse" -> {
        mc.setScreen(null);
        var key = local.luke.power.input.GameplayKeys.INFO_HUD;
        key.code = action.equals("hud-mouse") ? -98 : org.lwjgl.input.Keyboard.KEY_H;
        var modifiers = new HashMap<>(Bindings.modifiers());
        modifiers.put(key.translationKey, action.equals("hud-chord") ? Chord.CTRL : 0);
        Bindings.configure(modifiers); Bindings.register(mc.options.allKeys);
      }
      case "hud-state" -> {
        var state = new com.google.gson.JsonObject();
        state.add("settings", Catalog.JSON.toJsonTree(InfoHudConfig.current()));
        state.add("lines", Catalog.JSON.toJsonTree(InfoHud.lines(InfoHudConfig.current().clusters[0])));
        state.addProperty("debug", mc.options.debugHud);
        state.addProperty("hidden", mc.options.hideHud);
        Files.writeString(Path.of("power-beta-160-hud-state.json"), Catalog.JSON.toJson(state));
      }
      case "hud-toggle-check" -> hudToggleCheck(mc);
      default -> throw new IllegalArgumentException("Unknown release-160 action");
    }
  }

  private static void hudToggleCheck(Minecraft mc) throws Exception {
    var before = InfoHudConfig.copy();
    try {
      var edited = InfoHudConfig.copy(); edited.enabled = true;
      edited.clusters[1].enabled = false; edited.clusters[1].hideWithDebug = false;
      edited.clusters[1].textColor = "#123456"; edited.clusters[1].offsetX = -17;
      InfoHudConfig.save(edited);
      String clusters = Catalog.JSON.toJson(edited.clusters);
      for (int i = 0; i < 3; i++) InfoHudConfig.toggle();
      check(!InfoHudConfig.current().enabled, "master toggle lost an edge");
      check(clusters.equals(Catalog.JSON.toJson(InfoHudConfig.current().clusters)), "toggle changed a cluster");
      PowerConfig.flushPending();
      var stored = PowerConfig.read("infoHud", InfoHudSettings.class);
      check(!stored.enabled && clusters.equals(Catalog.JSON.toJson(stored.clusters)), "toggle save differs");
      var session = SettingsRegistry.open(mc);
      var setting = session.settings().stream().filter(s -> s.id.equals("infoHud.enabled")).findFirst().orElseThrow();
      setting.value = new com.google.gson.JsonPrimitive(true); session.preview();
      check(InfoHudConfig.current().enabled, "HUD menu preview missing");
      session.discard();
      check(!InfoHudConfig.current().enabled, "Cancel did not restore the hotkey state");
      log("PASS custom HUD toggles preserve cluster settings, save and menu rollback");
    } finally { InfoHudConfig.save(before); }
  }

  private static void scene(Minecraft mc, String action) throws Exception {
    mc.setScreen(null); mc.options.debugHud = mc.options.hideHud = false;
    ((local.luke.power.permissions.CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
    CreativeVehicleChecks.mode(mc,"CREATIVE");
    mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
    for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=99;y<=105;y++)
      mc.world.method_229(x,y,z,y==99?1:0);
    mc.world.method_229(0,101,1,1);
    if(!action.equals("hud-scene")) mc.world.method_229(0,101,0,action.equals("placement-water")?9:11);
    mc.player.method_1340(.5,101.5,-2.5);
    mc.player.prevX=mc.player.field_1637=mc.player.x;
    mc.player.prevY=mc.player.field_1638=mc.player.y;
    mc.player.prevZ=mc.player.field_1639=mc.player.z;
    mc.player.yaw=mc.player.prevYaw=0;mc.player.pitch=mc.player.prevPitch=0;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
    mc.player.inventory.main[0]=new ItemStack(1,64,0);mc.player.inventory.selectedSlot=0;
    var settings=Config.current().copy();settings.flexible.enabled=true;Config.preview(settings);
    local.luke.power.flexible.Keys.ALL[3].code=org.lwjgl.input.Keyboard.KEY_G;
    Bindings.register(mc.options.allKeys);
    var hud=new InfoHudSettings();hud.enabled=action.equals("hud-scene");
    hud.clusters[1].textSize=1.4;hud.clusters[1].textColor="#55FFAA";hud.clusters[1].opacity=70;
    hud.clusters[2].enabled=true;hud.clusters[2].hideWithDebug=false;
    InfoHudConfig.preview(hud);InfoHud.tick(mc);
    MenuPreferences.previewDebugTime(true);
    ((WorldCycles)mc.world.method_262()).power$daylightTime(6000);
  }

  private static void activate(Minecraft mc) throws Exception {
    var settings = Config.current().copy(); settings.sneak.enabled = settings.placement.enabled = true;
    settings.placement.restrictionEnabled = settings.slabs.enabled = settings.flexible.enabled = true;
    Config.apply(settings); mc.options.cinematicMode = true;
    var visual = local.luke.power.visual.VisualConfig.copy(); visual.hideEntities = true;
    local.luke.power.visual.VisualConfig.queue(visual);
    var light = local.luke.power.light.LightConfig.copy(); light.enabled = true;
    local.luke.power.light.LightConfig.save(light);
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    camera.getClass().getMethod("setActive", boolean.class).invoke(camera, true);
    camera.getClass().getField("allowPlayerMovement").setBoolean(camera, true);
  }
  private static void inactive(Minecraft mc) throws Exception {
    var s = Config.current();
    check(!s.sneak.enabled && !s.placement.enabled && !s.placement.restrictionEnabled && !s.slabs.enabled && !s.flexible.enabled,
        "persisted gameplay toggle still enabled");
    check(!AutoWalk.isWalking() && !local.luke.power.building.mining.AutoMine.active()
        && !local.luke.power.building.camera.FreeLook.active() && !mc.options.cinematicMode,
        "live input tweak still active");
    check(!local.luke.power.visual.VisualConfig.current().hideEntities && !local.luke.power.render.RenderControls.fullBright()
        && !local.luke.power.light.LightConfig.current().enabled, "render tweak still active");
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    check(!(boolean)camera.getClass().getMethod("isActive").invoke(camera)
        && !camera.getClass().getField("allowPlayerMovement").getBoolean(camera), "camera still active");
  }

  private static void checks(Minecraft mc) throws Exception {
    failures = 0;
    boolean autoApply = MenuPreferences.current().autoApply;
    MenuPreferences.update(v -> v.autoApply = false);
    test("custom HUD previews and Cancel keep four independent clusters without writing", () -> {
      ConfigSession session = SettingsRegistry.open(mc);
      byte[] before = Files.readAllBytes(PowerConfig.path());
      String hudBefore = Catalog.JSON.toJson(InfoHudConfig.current());
      find(session, "infoHud.enabled").parse("true");
      find(session, "infoHud.clusters.0.textColor").parse("#FF0088");
      find(session, "infoHud.clusters.1.textSize").parse("1.5");
      find(session, "infoHud.clusters.1.opacity").parse("40");
      find(session, "infoHud.clusters.1.offsetX").parse("-25");
      session.preview();
      check(InfoHudConfig.current().enabled && InfoHudConfig.current().clusters[0].textColor.equals("#FF0088"), "preview missing");
      check(InfoHudConfig.current().clusters[0].textSize == 1 && InfoHudConfig.current().clusters[1].textSize == 1.5, "clusters linked");
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "preview wrote settings");
      session.discard();
      check(Catalog.JSON.toJson(InfoHudConfig.current()).equals(hudBefore), "Cancel lost values");
    });
    test("collapse all covers every tab, mixed states, search and narrow widths", () -> {
      PowerOptionsScreen screen = new PowerOptionsScreen(null); mc.setScreen(screen);
      field(screen, "libraryOpen", false); field(screen, "changedOnly", false);
      field(screen, "relatedIds", List.of()); field(screen, "conflictIds", List.of());
      for (int[] size : new int[][]{{320,240},{427,240},{854,480}}) {
        screen.init(mc, size[0], size[1]);
        for (String page : screen.visiblePages()) {
          field(screen, "page", page); search(screen, "");
          Set<String> collapsed = (Set<String>)field(screen, "collapsed"); collapsed.clear(); call(screen, "layout");
          List<String> groups = (List<String>)call(screen, "sections");
          if (groups.isEmpty()) continue;
          if (groups.size() > 1) collapsed.add(groups.get(0));
          call(screen, "layout");
          int x = (int)call(screen, "sectionButtonX");
          click(screen, x + 8, 46);
          check(((List<?>)field(screen, "rows")).stream().allMatch(r -> { try { return call(r, "setting") == null; } catch(Exception e) { throw new RuntimeException(e); } }), "tab did not collapse " + page);
          click(screen, x + 8, 35); check(!collapsed.containsAll(groups), "tab did not expand " + page);
          screen.render(-1,-1,0); check(GL11.glGetError() == 0, "options GL state");
        }
        search(screen, "time");
        Set<String> collapsed = (Set<String>)field(screen,"collapsed");
        check(((List<?>)field(screen,"rows")).stream().anyMatch(r -> { try { return call(r,"setting") != null; } catch(Exception e) {throw new RuntimeException(e);} }), "search hid matches from collapsed tabs");
        int x = (int)call(screen, "sectionButtonX"), clear = (int)call(screen, "clearSearchX");
        check(clear + 18 < x && (int)call(screen, "searchWidth") > 30, "search buttons overlap");
        click(screen, x+8,35);
        check(collapsed.containsAll((List<String>)call(screen,"sections")),"search did not collapse");
        click(screen,(int)call(screen,"left")+8,(int)call(screen,"top")+8);
        check(!collapsed.containsAll((List<String>)call(screen,"sections")),"collapsed search header cannot expand");
        click(screen,x+8,35);click(screen,x+8,35);
        check(((TextInput)field(screen,"search")).text().equals("time"), "collapse cleared search");
      }
      screen.session().discard(); mc.setScreen(null);
    });
    test("cluster selector preserves drafts and shows only the chosen cluster", () -> {
      var screen = MenuUpdateChecks.open(mc, "Interface"); search(screen, "");
      ((Set<?>)field(screen,"collapsed")).clear(); call(screen,"layout");
      Setting first = find(screen.session(),"infoHud.clusters.0.offsetX"); first.parse("17");
      Setting selector = find(screen.session(),"infoHud.selectedCluster"); selector.parse("1"); screen.changed(selector);
      check(first.value.getAsInt() == 17, "selector lost draft");
      for (Object row : (List<?>)field(screen,"rows")) {
        Setting s = (Setting)call(row,"setting");
        check(s == null || !s.id.startsWith("infoHud.clusters.0."), "unselected cluster displayed");
      }
      screen.session().discard(); mc.setScreen(null);
    });
    test("all native HUD offsets and visibility preview immediately and roll back", () -> {
      var session = SettingsRegistry.open(mc);
      try {
        find(session,HUD+"hudPositioningSystem").parse("2");
        String[] names = {"HOTBAR", "HEARTS", "ARMOR", "OXYGEN", "OVERLAY_MESSAGE"};
        String[] fields = {"Hotbar", "Hearts", "Armor", "Oxygen", "Overlay"};
        for (int i=0;i<names.length;i++) {
          String prefix = HUD+"ADVANCED_HUD_POSITIONS_CONFIG."+names[i]+"_POSITION_CONFIG.";
          find(session,prefix+"horizontalPositionOffset").parse(Integer.toString(10+i));
          find(session,prefix+"verticalPositionOffset").parse(Integer.toString(-20-i));
        }
        session.preview(); mc.inGameHud.render(0,false,0,0);
        for (int i=0;i<fields.length;i++) {
          check((int)field(mc.inGameHud,"xOffset"+fields[i]) == 10+i, "horizontal offset stale "+fields[i]);
          check((int)field(mc.inGameHud,"yOffset"+fields[i]) == -20-i, "vertical offset stale "+fields[i]);
        }
        check(HudElements.heartsX == 11 && HudElements.heartsY == -21, "food preview position stale");
        for (String name : names) find(session,HUD+"ADVANCED_HUD_POSITIONS_CONFIG."+name+"_POSITION_CONFIG.enableVisibility").parse("false");
        session.preview(); mc.inGameHud.render(0,false,0,0);
        check(!HudElements.heartsVisible, "food preview ignores hidden hearts");
        for (String name : fields) check(!(boolean)field(mc.inGameHud,name.toLowerCase()+"Visibility"),"visibility ignored "+name);
      } finally { session.discard(); mc.inGameHud.render(0,false,0,0); }
      check((int)field(mc.inGameHud,"xOffsetHotbar") == 0, "Cancel left HUD offset");
    });
    test("F3 clock uses frozen daylight and custom HUD restores GL state", () -> {
      var cycles = (WorldCycles)mc.world.method_262(); long before = cycles.power$daylightTime();
      var hud = InfoHudConfig.copy(); boolean debug = mc.options.debugHud, pref = MenuPreferences.debugTimeOfDay();
      try {
        cycles.power$daylightTime(18000); MenuPreferences.previewDebugTime(true);
        InfoHud.tick(mc); check(InfoHud.timeLine().equals("Time: 12:00 AM"),"clock used simulation time");
        mc.options.debugHud = true; mc.inGameHud.render(0,false,0,0);
        check(DebugOverlay.lastLayout().stream().anyMatch(l -> l.text().equals("Time: 12:00 AM")),"F3 clock missing");
        var next = InfoHudConfig.copy(); next.enabled = true;
        for (int i=0;i<4;i++) { var c=next.clusters[i]; c.enabled=true; c.hideWithDebug=i%2==0; c.textSize=.5+i*.5; c.opacity=25+i*25; }
        InfoHudConfig.preview(next); InfoHud.tick(mc);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        InfoHud.render(mc);
        check(depth==GL11.glIsEnabled(GL11.GL_DEPTH_TEST) && blend==GL11.glIsEnabled(GL11.GL_BLEND) && GL11.glGetError()==0,"custom HUD GL leak");
      } finally { cycles.power$daylightTime(before); InfoHudConfig.preview(hud); mc.options.debugHud=debug; MenuPreferences.previewDebugTime(pref); }
    });
    lava(mc);
    MenuPreferences.update(v -> v.autoApply = autoApply);
    log("RELEASE 160 FAILURES " + failures);
  }

  private static void lava(Minecraft mc) throws Exception {
    var before = GameplayConfig.copy();
    try {
      for (boolean enabled : new boolean[]{false,true}) for (int block : new int[]{10,11}) test("lava item protection="+enabled+" block="+block, () -> {
        var settings = GameplayConfig.copy(); settings.lavaProofItems=enabled; GameplayConfig.preview(settings);
        mc.world.method_229(4,110,4,block);
        var stack = new ItemStack(267,1,37);
        var item = new class_142(mc.world,4.5,110.3,4.5,stack);
        for (int i=0;i<40&&!item.dead;i++) {
          item.method_1340(4.5,110.3,4.5); item.velocityX=item.velocityY=item.velocityZ=0; item.tick();
        }
        check(enabled ? !item.dead && ((ItemHealthAccessor)item).power$health()==5 : item.dead,"unexpected lava survival");
        check(item.field_564 == stack && stack.count==1 && stack.getDamage()==37,"lava altered native stack");
        if(enabled) {
          check(item.fire<=0,"protected lava ignited the item");
          for(int i=0;i<100;i++) { item.method_1340(7.5,110.3,7.5);item.tick(); }
          check(!item.dead && ((ItemHealthAccessor)item).power$health()==5,"item burned after leaving lava");
        }
        item.markDead(); mc.world.method_229(4,110,4,0);
      });
      test("lava setting preserves ordinary damage and multiplayer authority", () -> {
        var settings=GameplayConfig.copy(); settings.lavaProofItems=true; GameplayConfig.preview(settings);
        var item=new class_142(mc.world,4,110,4,new ItemStack(1,1,0));
        item.damage(null,2); check(((ItemHealthAccessor)item).power$health()==3,"ordinary damage suppressed");
        var method=Entity.class.getDeclaredMethod("method_1332");method.setAccessible(true);
        boolean remote=mc.world.isRemote; mc.world.isRemote=true;
        try { method.invoke(item); check(item.dead,"client changed server-owned lava behaviour"); }
        finally { mc.world.isRemote=remote; item.markDead(); }
      });
      test("lava setting leaves fire blocks dangerous", () -> {
        var settings=GameplayConfig.copy();settings.lavaProofItems=true;GameplayConfig.preview(settings);
        mc.world.method_229(4,109,4,87);
        mc.world.method_229(4,110,4,51);
        check(mc.world.getBlockId(4,110,4)==51,"fire fixture did not stay placed");
        var item=new class_142(mc.world,4.5,110.3,4.5,new ItemStack(1,1,0));
        for(int i=0;i<40&&!item.dead;i++) {item.method_1340(4.5,110.3,4.5);item.move(0,0,0);}
        check(item.dead,"fire immunity leaked");
      });
    } finally { GameplayConfig.preview(before); mc.world.method_229(4,110,4,0); mc.world.method_229(4,109,4,0); }
  }
}
