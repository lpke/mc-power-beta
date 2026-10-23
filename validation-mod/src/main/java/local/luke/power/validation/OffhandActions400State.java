package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.HandUse;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.*;
import local.luke.power.render.CullingTerrain;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.Client253Access;
import local.luke.power.world.WorldCycles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Owns air-only cells and the entities spawned synchronously by actual native item use. */
final class OffhandActions400State implements AutoCloseable {
  final Minecraft mc;
  final AutoTools400State nativeState;
  final Path report;
  final Set<Entity> owned = Collections.newSetFromMap(new IdentityHashMap<>());
  private final com.google.gson.JsonObject config;
  private final RuleSet rules;
  private final class_53 maps;
  private final class_53 scratchMaps=new class_53(null);
  private final Object nativeRenderer;
  private final Map<Field,Object> nativeCache=new LinkedHashMap<>(),offhandCache=new LinkedHashMap<>();
  private final Map<Field,Object> weather=new LinkedHashMap<>();
  private final boolean third, hud, debug, bob;
  private final boolean raining,thundering;
  private final int rainTime,thunderTime;
  private final long daylight;
  private final double px, py, pz, prevX, prevY, prevZ;
  private final double renderPrevX,renderPrevY,renderPrevZ;
  private final Field front, style;
  private final Object ui, oldStyle;
  private final boolean oldFront;
  private boolean freshMap,scratchInstalled;
  private boolean visualPositionChosen;
  private double visualX,visualY,visualZ;

  OffhandActions400State(Minecraft mc) throws Exception {
    this.mc = mc; PowerConfig.flushPending(); config = PowerConfig.document(); rules = AccessRules.copy(mc);
    check(mc.player.fishHook == null, "reel the existing native fishing hook before this fixture");
    third = mc.options.thirdPerson; hud = mc.options.hideHud; debug = mc.options.debugHud; bob = mc.options.bobView;
    px = mc.player.x; py = mc.player.y; pz = mc.player.z; prevX = mc.player.prevX; prevY = mc.player.prevY; prevZ = mc.player.prevZ;
    renderPrevX=mc.player.field_1637;renderPrevY=mc.player.field_1638;renderPrevZ=mc.player.field_1639;
    maps = mc.world.field_222;
    var properties=mc.world.method_262();
    daylight=((WorldCycles)properties).power$daylightTime();
    raining=properties.getRaining();thundering=properties.getThundering();
    rainTime=properties.getRainTime();thunderTime=properties.getThunderTime();
    for(String name:List.of("field_205","field_206","field_207","field_208","field_209","field_210")){
      Field f=World.class.getDeclaredField(name);f.setAccessible(true);weather.put(f,f.get(mc.world));
    }
    nativeRenderer=mc.field_2818.field_2342;
    for(String name:List.of("field_2402","field_2403","field_2404","field_2407")){
      Field f=class_556.class.getDeclaredField(name);f.setAccessible(true);nativeCache.put(f,f.get(nativeRenderer));
    }
    for(String name:List.of("owner","world","rendered","equip","previousEquip","swing","previousSwing","swingTicks","changed")){
      Field f=OffhandRenderer.class.getDeclaredField(name);f.setAccessible(true);offhandCache.put(f,f.get(null));
    }
    front = Class.forName("local.luke.power.controls.util.ModOptions").getField("frontView"); oldFront = front.getBoolean(null);
    ui = Class.forName("local.luke.power.controls.ControlFeatures").getField("USER_INTERFACE_CONFIG").get(null);
    style = ui.getClass().getField("frontViewThirdPerson"); oldStyle = style.get(ui);
    report = FabricLoader.getInstance().getGameDir().resolve("power-beta-data/reports/offhand-actions-400").resolve(UUID.randomUUID().toString());
    Files.createDirectories(report); nativeState = new AutoTools400State(mc);
    for (int x = -1; x <= 3; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
      check(mc.world.getBlockId(nativeState.x+x,nativeState.y+y,nativeState.z+z)==0
          && mc.world.method_1777(nativeState.x+x,nativeState.y+y,nativeState.z+z)==null, "owned item-use margin is not air");
  }
  void reset() throws Exception {
    clearEntities(); nativeState.reset(); nativeState.attack.set(false); clearBlocks();
    freshMap=true;
    if(!scratchInstalled){
      // The retained original references can themselves be maps. Never render those
      // against scratch storage, because native map lookup may change their item ID.
      for(Field f:nativeCache.keySet())switch(f.getName()){
        case "field_2402"->f.set(nativeRenderer,null);
        case "field_2403","field_2404"->f.setFloat(nativeRenderer,0);
        case "field_2407"->f.setInt(nativeRenderer,0);
        default->{}
      }
      for(Field f:offhandCache.keySet())switch(f.getName()){
        case "rendered"->f.set(null,null);
        case "equip","previousEquip","swing","previousSwing"->f.setFloat(null,0);
        case "swingTicks"->f.setInt(null,0);
        case "changed"->f.setBoolean(null,false);
        default->{}
      }
      scratchInstalled=true;
    }
    mc.world.field_222 = scratchMaps; mc.player.fishHook = null; mc.field_2823 = null;
    mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = -15;
    var tools = InventoryToolsConfig.copy(); tools.toolSwitch = tools.restock = false; InventoryToolsConfig.preview(tools);
    mc.player.inventory.main[9] = tagged(35, 117, 6, "storage"); mc.player.inventory.armor[0] = tagged(313, 1, 13, "armor");
  }
  static ItemStack tagged(int id, int count, int damage, String label) throws Exception { return DeathChecks.tagged(id,count,damage,"offhand-actions-400:"+label); }
  void hands(ItemStack main, ItemStack off) { mc.player.inventory.main[0]=main; Offhand.set(mc.player.inventory,off); }
  void use() throws Exception {
    Set<Entity> before = Collections.newSetFromMap(new IdentityHashMap<>()); for(Object e:mc.world.field_198)before.add((Entity)e);
    OffhandUse.newPress(mc); ((Client253Access)(Object)mc).power$use(1);
    for(Object e:mc.world.field_198)if(!before.contains(e))owned.add((Entity)e);
    nativeState.finish(); check(!OffhandUse.active() && HandUse.hand(mc.player.inventory)==HandUse.Hand.MAIN
        && !HandUse.routesSelectedSetter(mc.player.inventory),"native hand-use scope leaked");
    check(mc.player.inventory.selectedSlot==0,"native item use changed selected slot");
  }
  String[] items() throws Exception {
    var items=DeathItems.snapshot(mc.player.inventory,mc.player.playerContainer);String[] data=new String[46];
    for(int i=0;i<46;i++)data[i]=AutoTools400State.bytes(items[i]);return data;
  }
  Map<String,Object> renderState(float delta)throws Exception {
    Map<String,Object> out=new LinkedHashMap<>();
    for(Field f:nativeCache.keySet())out.put("main."+f.getName(),f.getName().equals("field_2402")
        ?AutoTools400State.bytes((ItemStack)f.get(nativeRenderer)):f.get(nativeRenderer));
    for(Field f:offhandCache.keySet())switch(f.getName()){
      case "owner"->out.put("offhand.ownerMatches",f.get(null)==mc.player);
      case "world"->out.put("offhand.worldMatches",f.get(null)==mc.world);
      case "rendered"->out.put("offhand.rendered",AutoTools400State.bytes((ItemStack)f.get(null)));
      default->out.put("offhand."+f.getName(),f.get(null));
    }
    out.put("main.interpolatedSwing",mc.player.method_930(delta));
    out.put("offhand.interpolatedSwing",OffhandRenderer.swing(mc.player,delta));
    return out;
  }
  void unchanged(String[] before, int... changed) throws Exception {
    Set<Integer> admitted=new HashSet<>();for(int i:changed)admitted.add(i);String[] actual=items();
    for(int i=0;i<46;i++)if(!admitted.contains(i))check(Objects.equals(before[i],actual[i]),"native use changed protected slot"+i);
  }
  int arrows() { int n=0;for(Entity e:owned)if(e instanceof class_410&&!e.dead)n++;return n; }
  void hookTick() {
    var hook=mc.player.fishHook;check(hook!=null,"native use did not create a hook");
    hook.method_1340(nativeState.x+1.5,nativeState.y+.5,nativeState.z+.5);hook.velocityX=hook.velocityY=hook.velocityZ=0;hook.tick();
  }
  void target(int id) {
    clearBlocks();
    for(int x=0;x<3;x++)for(int z=-1;z<=1;z++)mc.world.method_200(nativeState.x+x,nativeState.y-1,nativeState.z+z,1);
    mc.world.method_200(nativeState.x+1,nativeState.y,nativeState.z,id);
    // Air admission covers this margin, not the path from the player's original pose.
    // Stand beside the target on the owned floor so natural terrain cannot intercept use.
    double nativeY=mc.player.y+nativeState.y-mc.player.boundingBox.minY;
    mc.player.method_1340(nativeState.x+1.5,nativeY,nativeState.z+1.5);
    mc.player.prevX=mc.player.field_1637=mc.player.x;
    mc.player.prevY=mc.player.field_1638=mc.player.y;
    mc.player.prevZ=mc.player.field_1639=mc.player.z;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
    double tx=nativeState.x+1.5,ty=nativeState.y+.5,tz=nativeState.z+.5;
    double eyeY=mc.player.y+1.62-mc.player.field_1631,dx=tx-mc.player.x,dy=ty-eyeY,dz=tz-mc.player.z;
    mc.player.yaw=mc.player.prevYaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
    mc.player.pitch=mc.player.prevPitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
    mc.field_2823=mc.world.method_161(Vec3d.createCached(mc.player.x,eyeY,mc.player.z),Vec3d.createCached(tx,ty,tz),id==8||id==9);
    check(mc.field_2823!=null&&mc.field_2823.field_1984==nativeState.x+1&&mc.field_2823.field_1985==nativeState.y&&mc.field_2823.field_1986==nativeState.z,
        "native ray missed owned target "+(nativeState.x+1)+","+nativeState.y+","+nativeState.z
            +" from "+mc.player.x+","+eyeY+","+mc.player.z+" hit="+(mc.field_2823==null?"null":mc.field_2823.field_1984+","+mc.field_2823.field_1985+","+mc.field_2823.field_1986));
    if(id==1){int[] p=adjacent();mc.world.method_200(p[0],p[1],p[2],0);}
  }
  int[] adjacent() {
    int[][] d={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};int[] v=d[mc.field_2823.field_1987];
    return new int[]{mc.field_2823.field_1984+v[0],mc.field_2823.field_1985+v[1],mc.field_2823.field_1986+v[2]};
  }
  void clearBlocks() {for(int x=-1;x<=3;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)mc.world.method_200(nativeState.x+x,nativeState.y+y,nativeState.z+z,0);}
  private void clearEntities() {for(Entity e:owned)e.markDead();mc.player.fishHook=null;}
  @SuppressWarnings({"unchecked","rawtypes"}) void visual(boolean thirdPerson, boolean frontView) throws Exception {
    AccessRules.set(mc,GameRule.ADVANCE_TIME,false);AccessRules.set(mc,GameRule.ADVANCE_WEATHER,false);
    var properties=mc.world.method_262();
    ((WorldCycles)properties).power$daylightTime(6000);
    properties.setRaining(false);properties.setThundering(false);
    for(Field f:weather.keySet())if(f.getType()==float.class)f.setFloat(mc.world,0);else f.setInt(mc.world,0);
    AccessRules.set(mc,GameRule.ALLOW_CREATIVE_MODE,true);CreativeVehicleChecks.mode(mc,"CREATIVE");
    Field restoring=playerField("power$restoringFlight");boolean was=restoring.getBoolean(mc.player);restoring.setBoolean(mc.player,true);
    try{Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer").getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);}finally{restoring.setBoolean(mc.player,was);}
    mc.options.thirdPerson=thirdPerson;mc.options.hideHud=false;mc.options.debugHud=false;mc.options.bobView=false;
    style.set(ui,Enum.valueOf((Class)style.getType(),"NORMAL"));front.setBoolean(null,frontView);
    visualPosition();
    mc.player.yaw=mc.player.prevYaw=35;mc.player.pitch=mc.player.prevPitch=frontView?-8:8;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;mc.field_2823=null;
  }
  private void visualPosition() throws Exception {
    int x=nativeState.x+1,z=nativeState.z;
    check(CullingTerrain.supported(mc.world),"visual camera clearance requires a supported loaded-chunk source");
    for(int cx=(x-5)>>4;cx<=((x+5)>>4);cx++)for(int cz=(z-5)>>4;cz<=((z+5)>>4);cz++){
      var chunk=CullingTerrain.chunk(mc.world,cx,cz);
      check(chunk!=null&&chunk.method_858(cx,cz)&&mc.world.method_239(cx*16,64,cz*16),
          "visual camera margin crosses unloaded terrain");
    }
    if(!visualPositionChosen){
      for(int y:new int[]{124,120,116,112})if(visualAir(x,y,z)){
        visualX=x+.5;visualY=y;visualZ=z+.5;visualPositionChosen=true;break;
      }
      check(visualPositionChosen,"no loaded 11x11x7 clear-air camera margin above the fixture");
      log("OFFHAND ACTIONS400 visual camera air center="+visualX+","+visualY+","+visualZ+" radius=5 height=7");
    }
    check(visualAir(x,(int)visualY,z),"visual camera margin changed after admission");
    mc.player.method_1340(visualX,visualY,visualZ);
    mc.player.prevX=mc.player.field_1637=visualX;
    mc.player.prevY=mc.player.field_1638=visualY;
    mc.player.prevZ=mc.player.field_1639=visualZ;
    mc.player.field_1623=false;
  }
  private boolean visualAir(int x,int y,int z) {
    for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)for(int dy=-3;dy<=3;dy++)
      if(CullingTerrain.block(mc.world,x+dx,y+dy,z+dz)!=0)return false;
    return true;
  }
  ItemStack map() {
    // Native map rendering remains real. An in-memory storage with no save handler cannot
    // write map/id files; a different dimension prevents the native map tick loading terrain.
    check(mc.world.field_222==scratchMaps,"fixture map storage ownership changed");
    String key="map_32000";class_198 data=(class_198)mc.world.method_176(class_198.class,key);
    if(data==null||freshMap){data=new class_198(key);data.field_720=(int)mc.player.x;data.field_721=(int)mc.player.z;data.field_722=(byte)(mc.world.dimension.id+7);data.field_723=0;
      for(int i=0;i<data.field_724.length;i++)data.field_724[i]=(byte)(12+(i/128/8+i%128/8)%10*4);mc.world.method_180(key,data);}
    freshMap=false;
    return new ItemStack(358,1,32000);
  }
  private Field playerField(String name) throws Exception {for(Class<?> c=mc.player.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException absent){}throw new NoSuchFieldException(name);}
  @Override public void close() throws Exception {
    try {
      clearEntities();clearBlocks();
      // AutoTools' drop cleanup must not claim natural entities born during visual frames.
      Field existing=AutoTools400State.class.getDeclaredField("existing");existing.setAccessible(true);
      @SuppressWarnings("unchecked") Set<Entity> retained=(Set<Entity>)existing.get(nativeState);
      for(Object e:mc.world.field_198)if(!owned.contains(e))retained.add((Entity)e);
      nativeState.close();
    } finally {
      try{PowerConfig.write(config);}finally{
        for(var entry:nativeCache.entrySet())entry.getKey().set(nativeRenderer,entry.getValue());
        for(var entry:offhandCache.entrySet())entry.getKey().set(null,entry.getValue());
        var properties=mc.world.method_262();
        ((WorldCycles)properties).power$daylightTime(daylight);
        properties.setRaining(raining);properties.setThundering(thundering);
        properties.setRainTime(rainTime);properties.getThunderTime(thunderTime);
        for(var entry:weather.entrySet())entry.getKey().set(mc.world,entry.getValue());
        AccessRules.restore(mc,rules);mc.world.field_222=maps;mc.options.thirdPerson=third;mc.options.hideHud=hud;mc.options.debugHud=debug;mc.options.bobView=bob;
        front.setBoolean(null,oldFront);style.set(ui,oldStyle);mc.player.method_1340(px,py,pz);mc.player.prevX=prevX;mc.player.prevY=prevY;mc.player.prevZ=prevZ;
        mc.player.field_1637=renderPrevX;mc.player.field_1638=renderPrevY;mc.player.field_1639=renderPrevZ;
      }
    }
    check(PowerConfig.document().equals(config),"native actions did not restore config/user data");
    check(mc.world.field_222==maps&&AccessRules.copy(mc).equals(rules),"native actions did not restore map/rule ownership");
    var properties=mc.world.method_262();
    check(((WorldCycles)properties).power$daylightTime()==daylight&&properties.getRaining()==raining
        &&properties.getThundering()==thundering&&properties.getRainTime()==rainTime&&properties.getThunderTime()==thunderTime,
        "native actions did not restore daylight/weather properties");
    for(var entry:weather.entrySet())check(Objects.equals(entry.getKey().get(mc.world),entry.getValue()),"native actions did not restore "+entry.getKey().getName());
    for(int x=-1;x<=3;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)check(mc.world.getBlockId(nativeState.x+x,nativeState.y+y,nativeState.z+z)==0,"owned native item-use cell not restored");
  }
}
