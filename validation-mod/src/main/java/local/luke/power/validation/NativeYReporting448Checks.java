package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.hud.*;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.opengl.Display;

/** Native reporting paths against an unspawned player. No terrain or inventory writes. */
final class NativeYReporting448Checks {
  private NativeYReporting448Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(),
        "focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    failures = 0;
    var owner = mc.player; var camera = mc.field_2807;
    var rules = AccessRules.copy(mc); var hud = InfoHudConfig.copy();
    var saved = snapshot(DetachedCamera.class, MenuPreferences.class, InfoHud.class);
    var map = snapshotFields(Maps.class, "world", "x", "y", "z", "yaw", "dimension");
    var report = snapshot(ReportedPosition.class);
    boolean debug = mc.options.debugHud;
    Object creative = Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null);
    Field flight = creative.getClass().getField("flight"); Object previousFlight = flight.get(creative);
    Object compat = Class.forName("local.luke.power.compat.Config").getField("config").get(null);
    var compatFields = new LinkedHashMap<Field,Object>();
    for (String name : List.of("addFacingToDebugOverlay", "addTotalPlayTimeToDebugOverlay", "addBlockLightToDebugOverlay",
        "addBiomeToDebugOverlay", "addDayCounterToDebugOverlay", "addSlimeChunkToDebugOverlay")) {
      Field field = compat.getClass().getField(name); compatFields.put(field, field.get(compat));
    }
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    var probe = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
    try {
      mc.player = probe; mc.field_2807 = probe; mc.options.debugHud = true;
      AccessRules.restore(mc, RuleSet.cheats()); flight.set(creative, true);
      for (Field field : compatFields.keySet()) field.set(compat, field.getName().equals("addBlockLightToDebugOverlay"));
      log("YREPORT448 compat handler "+debugHandler(mc).getName());
      DetachedCamera.register(() -> false);
      var style = InfoHudConfig.copy(); style.enabled = true;
      for (var cluster : style.clusters) { cluster.enabled = cluster.coordinates = true; cluster.biome = false; }
      InfoHudConfig.preview(style); field(Maps.class,"world").set(null,mc.world);
      for (String mode : List.of("SURVIVAL", "CREATIVE", "SPECTATOR")) {
        applyMode(probe, mode);
        for (boolean flying : new boolean[]{false,true}) {
          probe.getClass().getMethod("power_flightOverride",int.class).invoke(probe,1);
          probe.getClass().getMethod("creative_setFlying",boolean.class).invoke(probe,flying);
          for (boolean feet : new boolean[]{false,true}) for (float eye : new float[]{1.62f,1.27f,.4f})
            for (double velocity : new double[]{-.8,0,.8}) {
              final String label = mode+" flying="+flying+" feet="+feet+" eye="+eye+" velocity="+velocity;
              test("YREPORT448 "+label, () -> {
                MenuPreferences.previewFeetY(feet);
                probe.field_1631=eye; probe.field_1640=0; probe.spacingY=eye==.4f?.6f:eye==1.27f?1.5f:1.8f;
                probe.method_1341(owner.x,64.25,owner.z,45,-20);
                probe.prevX=probe.x;probe.prevY=probe.y-.5;probe.prevZ=probe.z;
                probe.velocityY=velocity;probe.field_1623=velocity==0&&!flying;
                double expected=mode.equals("SPECTATOR")?probe.y-eye+HeadGeometry.BASE_ABOVE_FEET
                    :feet?probe.boundingBox.minY:probe.y;
                outputs(mc,expected);
                for(float delta:new float[]{0,.5f,1})
                  near(Maps.framePose(mc,delta).y(),expected-.5*(1-delta),"map frame interpolation");
              });
            }
        }
      }
      test("YREPORT448 freecam head base ignores feet preference and stationary player's pose", () -> {
        DetachedCamera.register(() -> true,()->owner.x+8,()->83.75,()->owner.z-8,()->25,()->270);
        for(boolean feet:new boolean[]{false,true}) {
          MenuPreferences.previewFeetY(feet);double expected=HeadGeometry.baseY(83.75);
          outputs(mc,expected);near(Maps.framePose(mc,.25f).y(),expected,"freecam frame head base");
        }
      });
      test("YREPORT448 F3 light follows reported Y outside world height without loading terrain", () -> {
        DetachedCamera.register(() -> true,()->owner.x,()->200,()->owner.z,()->0,()->0);
        DebugOverlay.begin(); String handler=invokeDebug(mc);
        var lines=DebugOverlay.layout(640,mc.textRenderer::getWidth).stream().map(DebugOverlay.Line::text).toList();
        check(lines.contains("Block light: Unknown"),"F3 expected Block light: Unknown; handler="+handler+" captured="+lines);
      });
      test("YREPORT448 new waypoint fallback retains checkbox when a provider is unavailable", () -> {
        DetachedCamera.register(() -> false);applyMode(probe,"CREATIVE");
        ReportedPosition.register(()->null,()->null);
        for(boolean feet:new boolean[]{false,true}) {
          MenuPreferences.previewFeetY(feet);
          check(MapKeys.newWaypoint().y==(int)Math.floor(HudCoordinates.y(probe)),"waypoint fallback ignored checkbox");
        }
      });
      check(Arrays.equals(disk,Files.readAllBytes(PowerConfig.path())),"Y reporting wrote saved preferences");
    } finally {
      restore(report); restore(map); restore(saved); InfoHudConfig.preview(hud);
      flight.set(creative,previousFlight);for(var entry:compatFields.entrySet())entry.getKey().set(compat,entry.getValue());
      AccessRules.restore(mc,rules);mc.player=owner;mc.field_2807=camera;mc.options.debugHud=debug;DebugOverlay.begin();
    }
    log("YREPORT448 FAILURES "+failures);check(failures==0,"native Y reporting failures="+failures);
  }

  private static void outputs(Minecraft mc,double expected) throws Exception {
    near(HudCoordinates.y(mc.player),expected,"shared HUD Y");
    var pose=ReportedPosition.current(mc.player,0,-999,0,0,0);near(pose.y(),expected,"live registered report provider");
    Method f3=Arrays.stream(mc.inGameHud.getClass().getDeclaredMethods())
        .filter(m->m.getName().contains("power$debugY")&&Arrays.equals(m.getParameterTypes(),new Class<?>[]{double.class}))
        .findFirst().orElseThrow();f3.setAccessible(true);near((double)f3.invoke(mc.inGameHud,-999d),expected,"native F3 Y hook");
    field(InfoHud.class,"ticks").setInt(null,0);InfoHud.tick(mc);
    String coordinates=HudText.coordinates((int)Math.floor(pose.x()),(int)Math.floor(expected),(int)Math.floor(pose.z()));
    check(InfoHud.data().coordinates().equals(coordinates),"custom HUD cluster coordinates differ");
    for(var cluster:InfoHudConfig.current().clusters)
      check(InfoHud.lines(cluster).get(0).startsWith(coordinates),"cluster bypassed shared coordinates");
    field(Maps.class,"x").setDouble(null,pose.x());field(Maps.class,"y").setDouble(null,expected);field(Maps.class,"z").setDouble(null,pose.z());
    var settings=new MapSettings();settings.coordinates=true;settings.biome=settings.clock=false;
    check(Minimap.frame(mc,settings,640,360,false,false).labels().get(0).text().equals(coordinates),"minimap Y differs");
    check(MapKeys.newWaypoint().y==Math.max(0,Math.min(127,(int)Math.floor(expected))),"new waypoint Y differs");
    DebugOverlay.begin();String handler=invokeDebug(mc);
    int by=(int)Math.floor(expected),bx=(int)Math.floor(pose.x()),bz=(int)Math.floor(pose.z());
    String light=by>=0&&by<128&&mc.world.method_239(bx,64,bz)
        ?Integer.toString(mc.world.method_164(net.minecraft.class_56.BLOCK,bx,by,bz)):"Unknown";
    var lines=DebugOverlay.layout(640,mc.textRenderer::getWidth).stream().map(DebugOverlay.Line::text).toList();
    check(lines.contains("Block light: "+light),
        "F3 expected Block light: "+light+" at "+bx+","+by+","+bz+"; handler="+handler+" captured="+lines);
  }
  private static Method debugHandler(Minecraft mc) {
    Class<?>[] signature={float.class,boolean.class,int.class,int.class,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class};
    var matches=Arrays.stream(mc.inGameHud.getClass().getDeclaredMethods())
        .filter(m->(m.getName().equals("power$debug")||m.getName().endsWith("$power$debug"))
            &&Arrays.equals(m.getParameterTypes(),signature)).toList();
    check(matches.size()==1,"expected one compat debug handler, found "+matches);
    return matches.get(0);
  }
  private static String invokeDebug(Minecraft mc) throws Exception {
    Method method=debugHandler(mc);
    method.setAccessible(true);method.invoke(mc.inGameHud,1f,false,0,0,null);
    return method.getName();
  }
  private static void applyMode(ClientPlayerEntity player,String name) throws Exception {
    Class<?> type=Class.forName("local.luke.power.creative.api.GameMode");
    player.getClass().getMethod("power_applyMode",type).invoke(player,type.getMethod("valueOf",String.class).invoke(null,name));
  }
  private static void near(double actual,double expected,String label) {
    check(Math.abs(actual-expected)<1e-6,label+": "+actual+" expected "+expected);
  }
  private static Field field(Class<?> type,String name) throws Exception {Field value=type.getDeclaredField(name);value.setAccessible(true);return value;}
  private static Map<Field,Object> snapshot(Class<?>... types) throws Exception {
    var result=new LinkedHashMap<Field,Object>();
    for(Class<?> type:types)for(Field field:type.getDeclaredFields())if(Modifier.isStatic(field.getModifiers())&&!Modifier.isFinal(field.getModifiers())) {
      field.setAccessible(true);result.put(field,field.get(null));
    }
    return result;
  }
  private static Map<Field,Object> snapshotFields(Class<?> type,String... names) throws Exception {
    var result=new LinkedHashMap<Field,Object>();for(String name:names){Field field=field(type,name);result.put(field,field.get(null));}return result;
  }
  private static void restore(Map<Field,Object> saved) throws Exception {for(var entry:saved.entrySet())entry.getKey().set(null,entry.getValue());}
}
