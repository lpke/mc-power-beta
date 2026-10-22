package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.command.vanilla.Help;
import local.luke.power.commands.util.*;
import local.luke.power.input.*;
import local.luke.power.map.Minimap;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.world.WorldRuleChanges;
import local.luke.power.worldedit.MinecraftWorld;
import local.luke.power.worldedit.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;

/** Native commands, attack, camera rays and travel in an already-loaded disposable chunk. */
public final class Release420Checks {
  private Release420Checks() {}
  public static void beforeTick(Minecraft mc){HoldUse420Checks.beforeTick(mc);}
  public static void afterTick(Minecraft mc){HoldUse420Checks.afterTick(mc);DebugKeys420Checks.tick(mc);DebugToggles420Checks.tick(mc);}
  static void run(Minecraft mc,String section) throws Exception {
    if(section.startsWith("hold-use")){HoldUse420Checks.run(mc,section);return;}
    if(section.startsWith("debug-toggles")){DebugToggles420Checks.run(mc,section);return;}
    if(section.startsWith("debug-hotbar")){DebugKeys420Checks.run(mc,section);return;}
    if(section.equals("line-lock")){LineLock420Checks.run(mc);return;}
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&mc.player.health>0&&!mc.player.dead,
        "open a disposable singleplayer world with a living player");
    check(Display.isActive()&&!DetachedCamera.isActive()&&mc.player.field_1595==null,
        "focus the disposable client, leave vehicles and freecam");
    check(mc.player.container==mc.player.playerContainer,"close world-owned containers first");
    failures=0;
    try(var state=new State(mc)) {
      mc.setScreen(null);mc.paused=false;mc.field_2778=true;mc.field_2807=mc.player;
      var allowed=state.rules.copy();
      for(GameRule rule:GameRule.values())if(rule.access())allowed.set(rule,true);
      AccessRules.restore(mc,allowed);CreativeVehicleChecks.mode(mc,"SURVIVAL");
      call(mc.player,"power_flightOverride",new Class<?>[]{int.class},0);
      call(mc.player,"creative_setFlying",new Class<?>[]{boolean.class},false);
      switch(section) {
        case "commands" -> commands(mc,state);
        case "sprint" -> sprint(mc,state);
        case "spectator" -> spectator(mc,state);
        default -> throw new IllegalArgumentException("Use release-420 commands, sprint or spectator");
      }
    }
    log("RELEASE420 "+section+" failures="+failures);
    check(failures==0,"release420 "+section+" checks failed="+failures);
  }
  private static void commands(Minecraft mc,State state) throws Exception {
    var source=new SharedCommandSource(mc.player);var help=new Help();
    test("420 instakill aliases use native Player.attack and retain fists/inventory",()->{
      InstantKill.clear();mc.player.inventory.main[mc.player.inventory.selectedSlot]=null;
      check(command(mc,"/instakill").stream().anyMatch(line->line.contains("enabled")),"/instakill feedback missing");
      check(InstantKill.active(mc.player,mc.world),"/instakill did not activate");
      var mob=new net.minecraft.class_443(mc.world);mob.health=20;mob.method_1340(mc.player.x,mc.player.y,mc.player.z);
      mc.player.attack(mob);check(mob.health<=0,"native attack did not kill in one hit");
      command(mc,"/ik");check(!InstantKill.active(mc.player,mc.world),"/ik did not switch off");
      var normal=new net.minecraft.class_443(mc.world);normal.health=20;normal.method_1340(mc.player.x,mc.player.y,mc.player.z);
      mc.player.attack(normal);check(normal.health>0&&normal.health<20,"ordinary bare-fist attack no longer uses native damage");
      check(mc.player.inventory.main[mc.player.inventory.selectedSlot]==null,"attack inserted an item into empty hand");
    });
    test("420 help index, aliases and suggestions share instakill availability",()->{
      var lines=help.lines(source);
      check(lines.stream().anyMatch(line->line.startsWith("/instakill "))&&lines.stream().anyMatch(line->line.startsWith("/ik ")),"help index omits instakill aliases");
      check(Arrays.asList(help.suggestion(source,1,"insta","/help insta")).contains("kill"),"help completion omits instakill");
      check(command(mc,"/instakill help").stream().anyMatch(line->line.contains("allow_survival_assistance")),"individual help omits permission");
      command(mc,"/ik");
      var denied=AccessRules.copy(mc);denied.set(GameRule.ALLOW_SURVIVAL_ASSISTANCE,false);WorldRuleChanges.apply(mc,denied);
      check(!InstantKill.active(mc.player,mc.world),"permission revocation retained instakill");
      check(help.lines(source).stream().noneMatch(line->line.startsWith("/instakill ")||line.startsWith("/ik ")),"denied aliases remain in help index");
      check(!Arrays.asList(help.suggestion(source,1,"insta","/help insta")).contains("kill"),"denied alias remains in help suggestions");
      var output=ChatChecks.submit(mc,"/ik");check(output.stream().anyMatch(line->line.contains("allow_survival_assistance")),"denied /ik omitted its owner");
      denied.set(GameRule.ALLOW_SURVIVAL_ASSISTANCE,true);WorldRuleChanges.apply(mc,denied);command(mc,"/instakill");
      @SuppressWarnings("unchecked") var resets=(Map<String,Runnable>)field(TweakLifecycle.class,"resets").get(null);
      check(resets.containsKey("instakill"),"instakill session-exit owner is not registered");resets.get("instakill").run();
      check(!InstantKill.active(mc.player,mc.world),"registered lifecycle reset retained instakill");
    });
    test("420 denied command manuals expose only their permitted variants",()->{
      for(GameRule rule:List.of(GameRule.ALLOW_TELEPORTATION,GameRule.ALLOW_TIME_EDITING,GameRule.ALLOW_SURVIVAL_ASSISTANCE,
          GameRule.ALLOW_CREATIVE_MODE,GameRule.ALLOW_SPECTATOR_MODE))AccessRules.set(mc,rule,false);
      for(String text:List.of("/waypoint","/waypoint help","/help waypoint")) {
        String output=String.join("\n",command(mc,text)).toLowerCase(Locale.ROOT);
        check(!output.contains("teleport")&&!output.contains("/waypoint tp"),text+" advertises denied teleportation: "+output);
        check(output.contains("destination")&&output.contains("list"),text+" lost safe waypoint actions");
      }
      for(String text:List.of("/time help","/help time")) {
        String output=String.join("\n",command(mc,text));
        check(output.contains("/time query")&&!output.contains("/time set")&&!output.contains("/time add"),text+" exposes time writes");
      }
      for(String text:List.of("/clear help","/help clear")) {
        String output=String.join("\n",command(mc,text));
        check(output.contains("<item> 0")&&!output.contains("Remove matching"),text+" exposes inventory removal");
      }
      for(String text:List.of("/gamemode","/gamemode help","/help gamemode")) {
        String output=String.join("\n",command(mc,text));
        check(output.contains("<survival>")&&!output.contains("|creative")&&!output.contains("|spectator"),text+" exposes denied modes: "+output);
      }
    });
  }
  private static List<String> command(Minecraft mc,String text) throws Exception {
    var result=ChatChecks.submit(mc,text);
    check(!result.isEmpty()&&result.stream().noneMatch(line->line.contains("§c")),text+": "+result);
    return result.stream().map(line->line.replaceAll("§.","")).toList();
  }
  private static void sprint(Minecraft mc,State saved) throws Exception {
    AccessRules.set(mc,GameRule.ALLOW_SPRINTING,true);
    var input=new net.minecraft.class_41();mc.player.field_161=input;input.field_2533=1;
    var settings=SprintConfig.copy();settings.activation=1;settings.doubleTap=false;settings.stamina=true;settings.noSprintInCombat=false;
    settings.sprintLadders=false;settings.speed=150;settings.stopAtWalls=true;settings.particles=false;settings.jumpBoost=false;
    SprintConfig.preview(settings);saved.keys.put(17,(byte)1);saved.keys.put(29,(byte)1);
    var state=((SprintPlayer)mc.player).power$sprint();
    int x=((int)Math.floor(mc.player.x)>>4)*16+7,z=((int)Math.floor(mc.player.z)>>4)*16+6,y=110;
    try(var scene=new Scene(mc,x,y,z)) {
      scene.clear();scene.floor();place(mc,x+.5,y+.01,z+.5);mc.player.field_1623=true;
      test("420 real wall preference and line-lock policy govern sprint without consuming stamina when stopped",()->{
        SprintController.reset();state.cancel(false);state.stamina=100;state.exhausted=false;state.combatTicks=0;mc.player.field_1624=true;
        SprintController.movement(mc.player);check(!state.sprinting&&state.stamina==100,"wall failed to stop default sprint without draining stamina");
        settings.stopAtWalls=false;SprintConfig.preview(settings);SprintController.movement(mc.player);check(state.sprinting,"disabled wall cancellation still stopped sprint");
        settings.stopAtWalls=true;SprintConfig.preview(settings);MovementOwnership.registerAutoWalkLineLock(()->true);
        SprintController.movement(mc.player);check(state.sprinting,"line-lock wall collision canceled sprint");
        MovementOwnership.registerAutoWalkLineLock(()->false);
      });
      test("420 native ladder travel scales ascent and drains Survival stamina",()->{
        for(int py=y;py<y+4;py++){scene.set(x,py,z+1,1,0);scene.set(x,py,z,65,2);}
        double[] ascent=new double[2],vertical=new double[2];
        for(int index=0;index<2;index++) {
          settings.sprintLadders=index==1;settings.stopAtWalls=index==1;SprintConfig.preview(settings);
          place(mc,x+.5,y+.01,z+.69);mc.player.yaw=0;mc.player.velocityX=mc.player.velocityY=0;mc.player.velocityZ=.2;
          mc.player.field_1624=true;mc.player.field_1623=false;state.cancel(false);state.stamina=100;state.exhausted=false;state.rechargeTicks=state.combatTicks=0;
          check(mc.player.method_932(),"fixture is not on a native ladder");SprintController.reset();SprintController.movement(mc.player);
          check(state.sprinting,"native ladder did not admit sprint");
          mc.player.method_945(0,1);vertical[index]=mc.player.velocityY;
          double before=mc.player.boundingBox.minY;SprintController.movement(mc.player);mc.player.method_945(0,1);ascent[index]=mc.player.boundingBox.minY-before;
          check(state.stamina<100,"ladder sprint did not drain stamina");
        }
        check(vertical[1]>vertical[0]*1.5&&ascent[1]>ascent[0]*1.5,"native ladder boost absent: velocity="+Arrays.toString(vertical)+", ascent="+Arrays.toString(ascent));
      });
      scene.clear();scene.floor();place(mc,x+.5,y+.01,z+.5);mc.player.field_1623=true;mc.player.field_1624=false;
      settings.stopAtWalls=false;settings.sprintLadders=false;SprintConfig.preview(settings);
      test("420 sprint starts and continues in supported inventories, text and modals exclude it",()->{
        var inventory=new net.minecraft.class_585(mc.player);mc.setScreen(inventory);mc.paused=false;mc.field_2778=true;
        check(MovementScreens.allows(inventory),"player inventory is not eligible for native movement");
        state.cancel(false);state.stamina=100;state.exhausted=false;SprintController.reset();SprintController.movement(mc.player);
        check(state.sprinting,"sprint could not start in supported inventory");
        double before=mc.player.z;mc.player.method_945(0,1);SprintController.movement(mc.player);
        check(state.sprinting&&mc.player.z>before,"inventory sprint failed actual native displacement/continuation");
        InventoryTextInput.focus(inventory,true);SprintController.movement(mc.player);check(!state.sprinting,"text entry retained sprint");InventoryTextInput.focus(inventory,false);
        Object modal=new Object();InventoryPanels.modal(inventory,modal,true);
        try{SprintController.movement(mc.player);check(!state.sprinting,"modal retained sprint");}finally{InventoryPanels.modal(inventory,modal,false);}
        mc.setScreen(null);
      });
    }
  }
  private static void spectator(Minecraft mc,State saved) throws Exception {
    int x=((int)Math.floor(mc.player.x)>>4)*16+7,z=((int)Math.floor(mc.player.z)>>4)*16+6,y=110;
    var chunks=(local.luke.power.mixin.CullingChunkCacheAccessor)mc.world.method_259();var loaded=new HashSet<>(chunks.power$loadedChunks().keySet());
    try(var scene=new Scene(mc,x,y,z)) {
      scene.clear();CreativeVehicleChecks.mode(mc,"SPECTATOR");place(mc,x+.5,y+.01,z+.5);mc.player.yaw=mc.player.prevYaw=0;mc.player.pitch=mc.player.prevPitch=0;
      mc.field_2807=mc.player;Minimap.frameDelta(1);
      var config=Class.forName("local.luke.power.creative.config.Config");Object draft=call(config,"current",new Class<?>[0]);
      draft=call(draft,"copy",new Class<?>[0]);field(draft.getClass(),"entityReach").setInt(draft,30);
      final Object preview=draft;final Object nativeTarget=mc.field_2823;final Object container=mc.player.container;
      test("420 spectator crosshair follows loaded native use targets and shared block reach",()->{
        scene.set(x,y+1,z+4,54,0);field(preview.getClass(),"blockReach").setInt(preview,30);call(config,"preview",new Class<?>[]{preview.getClass()},preview);
        check(!SpectatorTarget.interactable(),"target beyond configured block reach remained interactable");
        field(preview.getClass(),"blockReach").setInt(preview,60);call(config,"preview",new Class<?>[]{preview.getClass()},preview);
        check(SpectatorTarget.interactable(),"loaded chest inside shared reach was not interactable");
        scene.set(x,y+1,z+4,1,0);check(!SpectatorTarget.interactable(),"ordinary stone shows an interaction crosshair");
        scene.set(x,y+1,z+4,58,0);check(SpectatorTarget.interactable(),"native workbench override was not recognized");
        check(mc.field_2823==nativeTarget&&mc.player.container==container,"read-only target changed native target or container ownership");
        check(loaded.equals(chunks.power$loadedChunks().keySet()),"read-only crosshair loaded terrain");
      });
    }
  }
  private static void place(Minecraft mc,double x,double feet,double z)throws Exception {
    mc.player.method_1340(x,feet+mc.player.field_1631-mc.player.field_1640,z);
    mc.player.field_1637=mc.player.prevX=mc.player.x;mc.player.field_1638=mc.player.prevY=mc.player.y;mc.player.field_1639=mc.player.prevZ=mc.player.z;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;field(mc.player.getClass(),"field_1636").setFloat(mc.player,0);
  }
  private static Object call(Object target,String name,Class<?>[] types,Object... arguments)throws Exception {
    boolean statics=target instanceof Class<?>;
    for(Class<?> type=statics?(Class<?>)target:target.getClass();type!=null;type=type.getSuperclass())try {
      Method method=type.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(statics?null:target,arguments);
    } catch(NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
  private static Field field(Class<?> type,String name)throws Exception {
    for(;type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(name);
  }
  static final class State implements AutoCloseable {
    final Minecraft mc;final RuleSet rules;final Screen screen;final String mode;
    final SprintSettings settings;final Object creative;final Class<?> creativeConfig;
    final ByteBuffer keys;final byte[] keyboard;final Map<String,Integer> modifiers=Bindings.modifiers();
    final Map<KeyBinding,Integer> keyCodes=new LinkedHashMap<>();final List<Fields> fields=new ArrayList<>();
    final net.minecraft.item.ItemStack[] main,armor;final Object cursor,offhand;final int selected;final Set<Object> entities;
    State(Minecraft mc)throws Exception {
      this.mc=mc;rules=AccessRules.copy(mc);screen=mc.currentScreen;mode=call(mc.player,"power_mode",new Class<?>[0]).toString();settings=SprintConfig.copy();
      creativeConfig=Class.forName("local.luke.power.creative.config.Config");Object current=call(creativeConfig,"current",new Class<?>[0]);creative=call(current,"copy",new Class<?>[0]);
      keys=(ByteBuffer)field(Keyboard.class,"keyDownBuffer").get(null);keyboard=new byte[keys.capacity()];for(int index=0;index<keyboard.length;index++)keyboard[index]=keys.get(index);
      main=mc.player.inventory.main.clone();armor=mc.player.inventory.armor.clone();cursor=mc.player.inventory.getCursorStack();offhand=local.luke.power.offhand.Offhand.get(mc.player.inventory);selected=mc.player.inventory.selectedSlot;entities=new HashSet<>(mc.world.field_198);
      for(Object object:new Object[]{mc,mc.player,mc.player.boundingBox,((SprintPlayer)mc.player).power$sprint(),SprintController.class,MovementOwnership.class,InstantKill.class})fields.add(new Fields(object));
      for(var key:mc.options.allKeys)if(key.translationKey.equals("key.forward")||key.translationKey.equals("powerbeta.sprint")){keyCodes.put(key,key.code);key.code=key.translationKey.equals("key.forward")?17:29;}
      Bindings.configure(Map.of());for(int index=0;index<keyboard.length;index++)keys.put(index,(byte)0);
    }
    public void close()throws Exception {
      CreativeVehicleChecks.mode(mc,mode);SprintConfig.preview(settings);call(creativeConfig,"preview",new Class<?>[]{creative.getClass()},creative);
      for(Object object:new ArrayList<>(mc.world.field_198))if(object instanceof Entity entity&&entity!=mc.player&&!entities.contains(object))entity.markDead();
      System.arraycopy(main,0,mc.player.inventory.main,0,main.length);System.arraycopy(armor,0,mc.player.inventory.armor,0,armor.length);mc.player.inventory.setCursorStack((net.minecraft.item.ItemStack)cursor);
      local.luke.power.offhand.Offhand.set(mc.player.inventory,(net.minecraft.item.ItemStack)offhand);mc.player.inventory.selectedSlot=selected;
      for(var entry:keyCodes.entrySet())entry.getKey().code=entry.getValue();Bindings.configure(modifiers);
      for(int index=0;index<keyboard.length;index++)keys.put(index,keyboard[index]);
      mc.setScreen(screen);for(int index=fields.size()-1;index>=0;index--)fields.get(index).close();AccessRules.restore(mc,rules);
    }
  }
  static final class Fields implements AutoCloseable {
    final Object target;final Map<Field,Object> values=new LinkedHashMap<>();
    Fields(Object object)throws Exception {
      boolean statics=object instanceof Class<?>;target=statics?null:object;
      for(Class<?> type=statics?(Class<?>)object:object.getClass();type!=null;type=type.getSuperclass())for(var field:type.getDeclaredFields())
        if(!Modifier.isFinal(field.getModifiers())&&Modifier.isStatic(field.getModifiers())==statics){field.setAccessible(true);values.put(field,field.get(target));}
    }
    public void close()throws Exception{for(var entry:values.entrySet())entry.getKey().set(target,entry.getValue());}
  }
  static final class Scene implements AutoCloseable {
    final MinecraftWorld world;final Map<Pos,BlockValue> original=new LinkedHashMap<>();final int x,y,z;
    Scene(Minecraft mc,int x,int y,int z) {
      world=new MinecraftWorld(mc.world);this.x=x;this.y=y;this.z=z;
      for(int px=x-2;px<=x+2;px++)for(int py=y-1;py<=y+5;py++)for(int pz=z-2;pz<=z+5;pz++) {
        var point=new Pos(px,py,pz);check(world.loaded(point),"fixture scene must already be loaded");original.put(point,world.get(point));
      }
    }
    void clear(){original.keySet().forEach(point->world.set(point,BlockValue.AIR));}
    void floor(){for(int px=x-2;px<=x+2;px++)for(int pz=z-2;pz<=z+5;pz++)set(px,y-1,pz,1,0);}
    void set(int x,int y,int z,int id,int data){var point=new Pos(x,y,z);check(original.containsKey(point),"fixture left bounded scene");world.set(point,new BlockValue(id,data));}
    public void close(){original.forEach(world::set);}
  }
}
