package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Actual Options rows, temporary persistence and native inventory-to-map ownership. */
final class Release440Checks {
  static void run(Minecraft mc)throws Exception {
    check(mc.world!=null&&mc.player!=null,"load a disposable world");
    check(org.lwjgl.opengl.Display.isActive(),"focus the isolated game window");
    failures=0;
    Screen previous=mc.currentScreen;var maps=MapConfig.copy();boolean hud=mc.options.hideHud;
    var inventory=mc.player.inventory;
    var cursor=net.minecraft.item.ItemStack.clone(inventory.getCursorStack());
    try {
      test("RELEASE440 retired settings and shared search",()->{
        var options=new PowerOptionsScreen(null);mc.setScreen(options);
        var ids=options.session().settings().stream().map(s->s.id).toList();
        for(String id:List.of("tweaks.placement.enabled","tweaks.placement.restrictionEnabled","tweaks.sneak.enabled","visual.hideEntities"))
          check(!ids.contains(id),"retired setting remains "+id);
        check(ids.contains("keys.Placement restriction (toggle)"),"restriction action missing");
        var scope=PowerOptionsScreen.class.getDeclaredMethod("searchScopeVisible");scope.setAccessible(true);
        var page=PowerOptionsScreen.class.getDeclaredField("page");page.setAccessible(true);
        var layout=PowerOptionsScreen.class.getDeclaredMethod("layout");layout.setAccessible(true);
        page.set(options,"Controls");layout.invoke(options);
        check(!(boolean)scope.invoke(options),"Controls has a scope button");
        page.set(options,"Video");layout.invoke(options);
        check((boolean)scope.invoke(options),"ordinary tab lost scope choice");
        var scopeClass=Class.forName("local.luke.power.ui.OptionsSearchScope");
        var reset=scopeClass.getDeclaredMethod("reset");reset.setAccessible(true);
        var thisTab=scopeClass.getDeclaredMethod("thisTab",String.class);thisTab.setAccessible(true);
        var toggle=scopeClass.getDeclaredMethod("toggle",String.class);toggle.setAccessible(true);
        reset.invoke(null);
        try {
          for(String tab:List.of("General","Video","Audio"))check(!(boolean)thisTab.invoke(null,tab),"initial scope is not All");
          toggle.invoke(null,"Audio");
          for(String tab:List.of("General","Video","Audio","Controls"))check((boolean)thisTab.invoke(null,tab),"scope choice diverged");
          toggle.invoke(null,"Video");
          check((boolean)thisTab.invoke(null,"Controls"),"Controls scope changed");
        } finally {reset.invoke(null);}
        Path output=Path.of("power-beta-data/reports/release-440");Files.createDirectories(output);
        page.set(options,"Controls");layout.invoke(options);
        SettingsSnapshot.render(mc,options,480,270,output.resolve("controls.png"));
      });
      test("RELEASE440 gameplay state is not serialized",()->{
        var building=local.luke.power.building.config.Config.current().copy();
        building.placement.setEnabled(true);building.sneak.enabled=true;
        var json=new Gson().toJsonTree(building).getAsJsonObject();
        check(!json.getAsJsonObject("placement").has("enabled")&&!json.getAsJsonObject("placement").has("restrictionEnabled")
            &&!json.getAsJsonObject("sneak").has("enabled"),"temporary building state persisted");
        var old=json.deepCopy();old.getAsJsonObject("placement").addProperty("enabled",true);
        old.getAsJsonObject("placement").addProperty("restrictionEnabled",true);old.getAsJsonObject("sneak").addProperty("enabled",true);
        var restarted=new Gson().fromJson(old,local.luke.power.building.config.Settings.class);
        check(!restarted.placement.enabled&&!restarted.placement.restrictionEnabled&&!restarted.sneak.enabled,"old saved state reactivated");
        var visual=local.luke.power.visual.VisualConfig.copy();visual.hideEntities=true;
        check(!new Gson().toJsonTree(visual).getAsJsonObject().has("hideEntities"),"entity toggle persisted");
      });
      test("RELEASE440 unrelated Options previews preserve temporary actions",()->{
        var original=local.luke.power.building.config.Config.current().copy();
        var visible=local.luke.power.visual.VisualConfig.copy();
        try {
          var active=original.copy();active.placement.setEnabled(true);active.sneak.enabled=true;
          local.luke.power.building.config.Config.preview(active);
          var hidden=local.luke.power.visual.VisualConfig.copy();hidden.hideEntities=true;local.luke.power.visual.VisualConfig.preview(hidden);
          var session=SettingsRegistry.open(mc);
          var rate=find(session,"tweaks.placement.attemptsPerTick");rate.parse(rate.value.getAsInt()==16?"1":"16");
          var items=find(session,"visual.hideItems");items.parse(Boolean.toString(!items.value.getAsBoolean()));
          session.preview();
          check(local.luke.power.building.config.Config.current().placement.enabled&&local.luke.power.building.config.Config.current().placement.restrictionEnabled
              &&local.luke.power.building.config.Config.current().sneak.enabled&&local.luke.power.visual.VisualConfig.current().hideEntities,"Options preview reset temporary action state");
          session.discard();
          check(local.luke.power.building.config.Config.current().placement.enabled&&local.luke.power.building.config.Config.current().sneak.enabled
              &&local.luke.power.visual.VisualConfig.current().hideEntities,"Options Cancel reset temporary action state");
        } finally {local.luke.power.building.config.Config.preview(original);local.luke.power.visual.VisualConfig.preview(visible);}
      });
      mc.options.hideHud=false;
      var draft=MapConfig.copy();draft.minimap=draft.worldMap=draft.showInInventory=true;draft.hideWithDebug=false;
      draft.size=64;draft.corner=0;draft.offsetX=draft.offsetY=8;MapConfig.preview(draft);Maps.tick(mc);
      for(boolean pause:new boolean[]{false,true})test("RELEASE440 map returns to same "+(pause?"pause":"inventory"),()->{
        Screen host=pause?new net.minecraft.class_525():new net.minecraft.class_585(mc.player);
        mc.setScreen(host);host.init(mc,480,270);host.render(-1,-1,1);
        Object container=mc.player.container;
        var stack=DeathChecks.tagged(35,7,5,"map-return-440");inventory.setCursorStack(stack);
        String data=local.luke.power.death.DeathItems.encode(stack);
        var map=Minimap.layout(MapConfig.current(),host.width,host.height,false);
        check(MinimapMapVisit.open(mc,map.x()+map.size()/2,map.y()+map.size()/2),"minimap did not open World Map");
        check(mc.currentScreen instanceof WorldMapScreen,"wrong map screen");
        check(mc.player.container==container&&data.equals(local.luke.power.death.DeathItems.encode(inventory.getCursorStack())),"map entry closed native container");
        ((UiScreen)mc.currentScreen).layerKey('\0',Keyboard.KEY_ESCAPE);
        check(mc.currentScreen==host&&mc.player.container==container,"Escape did not restore original host");
        check(data.equals(local.luke.power.death.DeathItems.encode(inventory.getCursorStack())),"map visit changed native cursor data");
        inventory.setCursorStack(null);
      });
      for(String action:List.of("Set destination","Center here"))test("RELEASE440 nested waypoint "+action+" returns retained inventory",()->{
        Screen host=new net.minecraft.class_585(mc.player);mc.setScreen(host);host.init(mc,480,270);host.render(-1,-1,1);
        Object container=mc.player.container;
        var stack=DeathChecks.tagged(35,7,5,"map-nested-440");inventory.setCursorStack(stack);
        String before=local.luke.power.death.DeathItems.encode(stack);
        var map=Minimap.layout(MapConfig.current(),host.width,host.height,false);
        check(MinimapMapVisit.open(mc,map.x()+map.size()/2,map.y()+map.size()/2),"nested map did not open");
        Map<java.lang.reflect.Field,Object> mapState=new LinkedHashMap<>();
        for(String name:List.of("waypoints","destination","destinationArrivalArmed","pointsRevision","points")) {
          var field=Maps.class.getDeclaredField(name);field.setAccessible(true);mapState.put(field,field.get(null));
        }
        try {
          var point=new Waypoint();point.x=(int)mc.player.x;point.z=(int)mc.player.z;point.dimension=Maps.dimension();
          var data=new WaypointStore.Data();data.waypoints.add(point);
          var store=new WaypointStore(Path.of("power-beta-data/reports/release-440/unused-waypoints"),ignored->{});
          set(store,"data",data);set(store,"ready",true);
          for(var field:mapState.keySet())if(field.getName().equals("waypoints"))field.set(null,store);
          var waypoints=new WaypointsScreen(mc.currentScreen);mc.setScreen(waypoints);set(waypoints,"selected",point.id);
          var invoke=WaypointsScreen.class.getDeclaredMethod("contextAction",String.class);invoke.setAccessible(true);invoke.invoke(waypoints,action);
          if(action.equals("Center here")) {
            check(mc.currentScreen instanceof WorldMapScreen,"Center here did not create its map");
            ((UiScreen)mc.currentScreen).layerKey('\0',Keyboard.KEY_ESCAPE);
          }
          check(mc.currentScreen==host&&mc.player.container==container,"nested waypoint action lost the original native host");
          check(before.equals(local.luke.power.death.DeathItems.encode(inventory.getCursorStack())),"nested waypoint action changed cursor NBT/count");
          inventory.setCursorStack(null);
        } finally {for(var entry:mapState.entrySet())entry.getKey().set(null,entry.getValue());}
      });
      test("RELEASE440 unrelated native screen retires the retained container",()->{
        Screen host=new net.minecraft.class_585(mc.player);mc.setScreen(host);host.init(mc,480,270);host.render(-1,-1,1);
        var entities=Collections.newSetFromMap(new IdentityHashMap<net.minecraft.entity.Entity,Boolean>());
        for(Object entity:mc.world.field_198)entities.add((net.minecraft.entity.Entity)entity);
        inventory.setCursorStack(DeathChecks.tagged(35,7,5,"map-native-cleanup-440"));
        try {
          var map=Minimap.layout(MapConfig.current(),host.width,host.height,false);
          check(MinimapMapVisit.open(mc,map.x()+map.size()/2,map.y()+map.size()/2),"cleanup map did not open");
          Screen unrelated=new Screen(){};mc.setScreen(unrelated);
          check(mc.currentScreen==unrelated,"unrelated native screen was replaced by retained host");
          var retained=MinimapMapVisit.class.getDeclaredField("host");retained.setAccessible(true);
          check(retained.get(null)==null,"unrelated native screen retained a stale map visit");
          if(!(mc.player instanceof local.luke.power.input.PlayerMode mode&&mode.power$spectator()))
            check(inventory.getCursorStack()==null,"unrelated native screen skipped ordinary cursor cleanup");
        } finally {
          inventory.setCursorStack(null);
          for(Object entity:List.copyOf(mc.world.field_198))if(entity instanceof net.minecraft.class_142 drop&&!entities.contains(drop))drop.markDead();
        }
      });
    } finally {inventory.setCursorStack(cursor);MapConfig.preview(maps);mc.options.hideHud=hud;mc.setScreen(previous);}
    log("RELEASE440 FAILURES "+failures);
  }
  private static void set(Object target,String name,Object value)throws Exception {
    var field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
  }

}
