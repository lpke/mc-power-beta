package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.OffhandActions400State.tagged;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.offhand.*;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

/** Actual Minecraft use pipeline and screenshots taken only after ordinary world frames. */
public final class OffhandActions400Checks {
  private static Visual visual;
  private static int checks;
  private OffhandActions400Checks() {}
  public static void run(Minecraft mc,String phase)throws Exception {
    if(phase.equals("status")){log(visual==null?"OFFHAND ACTIONS400 idle":"OFFHAND ACTIONS400 VISUAL "+visual.phase+" frames="+visual.frames+" report="+visual.state.report);return;}
    if(phase.equals("stop")){finish("stopped");return;}
    check(visual==null,"stop the existing offhand visual task first");guard(mc);
    if(phase.equals("visual")){
      visual=new Visual(new OffhandActions400State(mc));
      try{visual.begin(0);log("OFFHAND ACTIONS400 VISUAL START "+visual.state.report);}catch(Exception|Error e){finish("failed");throw e;}return;
    }
    check(phase.equals("native"),"offhand-actions-400 native|visual|status|stop");failures=0;checks=0;Path report;
    try(var state=new OffhandActions400State(mc)){report=state.report;nativeChecks(state);}
    Files.writeString(report.resolve("native-result.json"),Catalog.JSON.toJson(Map.of("checks",checks,"failures",failures,"restored",true))+"\n");
    log("OFFHAND ACTIONS400 NATIVE checks="+checks+" failures="+failures+" report="+report);
  }
  private static void guard(Minecraft mc){
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&WorldCompatibility.ready(mc.world)&&mc.player.world==mc.world,
        "load a verified disposable singleplayer world");
    check(mc.currentScreen==null&&mc.field_2807==mc.player&&mc.player.container==mc.player.playerContainer&&mc.player.health>0&&!mc.player.dead
        &&mc.player.field_1595==null&&!DetachedCamera.isActive()&&!InteractionState.carryingContainer,"use a living unmounted physical player with no menus/freecam/carry");
    check(Display.isActive()&&mc.options.musicVolume==0&&mc.options.soundVolume==0&&!Mouse.isButtonDown(0)&&!Mouse.isButtonDown(1),"focus the muted clone and release physical buttons");
    check(!InventoryActions.busy()&&!NativeInventoryPlan.busy(),"finish native inventory plans first");
  }
  private static void probe(OffhandActions400State s,String name,Check body)throws Exception{checks++;test("OFFHAND ACTIONS400 "+name,()->{s.reset();body.run();});}
  private static void exact(OffhandActions400State s,String[] expected)throws Exception{check(Arrays.equals(expected,s.items()),"native use changed unexpected full item bytes");}
  private static String count(String bytes,int count)throws Exception{if(count==0)return null;ItemStack item=DeathItems.decode(bytes);item.count=count;return DeathItems.encode(item);}
  private static void nativeChecks(OffhandActions400State s)throws Exception{
    Minecraft mc=s.mc;var inv=mc.player.inventory;
    for(boolean off:new boolean[]{false,true})for(int amount:new int[]{1,3}){
      final boolean hand=off;final int arrows=amount;
      probe(s,(off?"OFF":"MAIN")+" bow consumes opposite-hand arrows count"+amount+" once",()->{
        ItemStack bow=tagged(261,1,0,"bow"),ammo=tagged(262,arrows,0,"opposite-ammo");s.hands(hand?ammo:bow,hand?bow:ammo);
        inv.main[9]=tagged(262,7,0,"storage-ammo");String[] expected=s.items();expected[hand?0:45]=count(expected[hand?0:45],arrows-1);
        s.use();check(s.arrows()==1,"native bow did not spawn exactly one arrow");exact(s,expected);
      });
    }
    probe(s,"two bows preserve MAIN priority and consume storage ammunition only once",()->{
      s.hands(tagged(261,1,0,"main-bow"),tagged(261,1,0,"off-bow"));inv.main[10]=tagged(262,3,0,"stored");
      String[] expected=s.items();expected[10]=count(expected[10],2);s.use();check(s.arrows()==1,"both bows fired on one native use");exact(s,expected);
    });
    probe(s,"OFF bow uses ordinary storage ammunition without changing its main item",()->{
      s.hands(tagged(280,13,0,"main-stick"),tagged(261,1,0,"off-bow"));inv.main[10]=tagged(262,3,0,"stored");
      String[] expected=s.items();expected[10]=count(expected[10],2);s.use();check(s.arrows()==1,"off bow did not use native storage ammo");exact(s,expected);
    });
    probe(s,"two empty bows neither fire nor rewrite native data",()->{
      s.hands(tagged(261,1,0,"main-bow"),tagged(261,1,0,"off-bow"));String[] before=s.items();s.use();check(s.arrows()==0,"empty bow fired");exact(s,before);
    });
    for(boolean off:new boolean[]{false,true}){
      final boolean hand=off;
      probe(s,(off?"OFF":"MAIN")+" native rod casts, survives native tick and reels without a second cast",()->{
        s.hands(tagged(hand?280:346,1,hand?0:9,"main"),tagged(346,1,17,"off-rod"));String[] before=s.items();
        s.use();var hook=mc.player.fishHook;check(hook!=null&&OffhandFishing.left(hook)==hand,"native cast hand flag differs");
        s.hookTick();check(!hook.dead&&mc.player.fishHook==hook,"native rod validation rejected the held hand");
        s.use();check(hook.dead&&mc.player.fishHook==null,"native reel did not remove its exact hook");exact(s,before);
      });
    }
    probe(s,"OFF cast remains valid after durable hand swap and reels through MAIN",()->{
      s.hands(tagged(280,13,0,"stick"),tagged(346,1,19,"rod"));s.use();var hook=mc.player.fishHook;
      check(hook!=null&&OffhandFishing.left(hook),"off cast missing");check(Offhand.swap(mc),"native swap declined");s.nativeState.finish();
      String[] before=s.items();s.hookTick();check(!hook.dead,"hand swap killed an owned rod");s.use();check(mc.player.fishHook==null&&hook.dead,"swapped rod did not reel");exact(s,before);
    });
    for(boolean disabled:new boolean[]{false,true}){
      final boolean disable=disabled;
      probe(s,"native hook retires when OFF rod is "+(disabled?"disabled":"removed"),()->{
        s.hands(tagged(280,13,0,"stick"),tagged(346,1,19,"rod"));s.use();var hook=mc.player.fishHook;check(hook!=null,"off cast missing");
        if(disable){var o=OffhandConfig.copy();o.enabled=false;OffhandConfig.preview(o);}else Offhand.set(inv,null);
        String[] before=s.items();s.hookTick();check(hook.dead&&mc.player.fishHook==null,"unavailable rod retained a live hook");exact(s,before);
      });
    }
    for(boolean off:new boolean[]{false,true}){
      final boolean hand=off;
      probe(s,(off?"OFF":"MAIN priority")+" native food consumes once and preserves the other hand",()->{
        s.hands(tagged(hand?280:260,hand?13:1,0,"main"),tagged(260,1,0,"off-food"));mc.player.health=2;
        String[] expected=s.items();expected[hand?45:0]=null;s.use();check(mc.player.health==6,"native food healed the wrong number of times");exact(s,expected);
      });
      probe(s,(off?"OFF":"MAIN priority")+" native block placement changes only its owned hand",()->{
        s.target(1);int[] destination=s.adjacent();s.hands(tagged(hand?280:4,hand?13:3,0,"main"),tagged(5,4,0,"off-block"));
        String[] expected=s.items();expected[hand?45:0]=count(expected[hand?45:0],hand?3:2);s.use();
        check(mc.world.getBlockId(destination[0],destination[1],destination[2])==(hand?5:4),"native placement did not use the expected hand");exact(s,expected);
      });
      probe(s,(off?"OFF":"MAIN priority")+" native water bucket places fluid and returns one empty bucket",()->{
        check(!mc.world.dimension.field_2176,"water fixture requires an ordinary dimension");s.target(1);int[] destination=s.adjacent();
        s.hands(tagged(hand?280:326,hand?13:1,0,"main"),tagged(hand?326:260,1,0,"off"));mc.player.health=2;
        String[] expected=s.items();expected[hand?45:0]=DeathItems.encode(new ItemStack(325,1,0));s.use();
        int id=mc.world.getBlockId(destination[0],destination[1],destination[2]);check(id==8||id==9,"native filled bucket did not place water");
        check(mc.player.health==2,"offhand food ran after a MAIN world effect");exact(s,expected);
      });
    }
    probe(s,"terminal MAIN block placement failure does not fall through to OFF food",()->{
      s.target(1);int[] p=s.adjacent();mc.world.method_200(p[0],p[1],p[2],1);s.hands(tagged(4,3,0,"blocked"),tagged(260,1,0,"food"));mc.player.health=2;
      String[] before=s.items();s.use();check(mc.player.health==2,"failed block placement consumed offhand food");exact(s,before);
    });
    probe(s,"OFF empty bucket fills native source water without rewriting MAIN",()->{
      s.target(9);s.hands(tagged(280,13,0,"stick"),tagged(325,1,0,"empty"));String[] expected=s.items();expected[45]=DeathItems.encode(new ItemStack(326,1,0));
      s.use();check(mc.world.getBlockId(s.nativeState.x+1,s.nativeState.y,s.nativeState.z)==0,"native bucket did not remove water source");exact(s,expected);
    });
    probe(s,"OFF bucket uses the native cow callback and only replaces its own item",()->{
      Entity cow=class_206.method_732("Cow",mc.world);check(cow!=null,"native Cow registry missing");cow.method_1340(s.nativeState.x+1.5,s.nativeState.y,s.nativeState.z+.5);
      check(mc.world.method_210(cow),"native fixture Cow could not spawn");s.owned.add(cow);mc.field_2823=new class_27(cow);
      s.hands(tagged(280,13,0,"stick"),tagged(325,1,0,"empty"));String[] expected=s.items();expected[45]=DeathItems.encode(new ItemStack(335,1,0));s.use();exact(s,expected);
    });
    for(String context:List.of("offhand","disabled","absent","main"))probe(s,"native map holder/marker ownership "+context,()->{
      ItemStack map=s.map();class_198 data=Item.field_457.method_1730(map,mc.world);data.field_722=(byte)mc.world.dimension.id;
      s.hands(tagged(280,13,0,"stick"),map);
      if(context.equals("main"))s.hands(map,null);
      if(context.equals("absent"))Offhand.set(inv,null);
      if(context.equals("disabled")){var o=OffhandConfig.copy();o.enabled=false;OffhandConfig.preview(o);}
      String[] before=s.items();data.method_667(mc.player,map);boolean included=context.equals("offhand")||context.equals("main");
      check(!data.field_726.isEmpty()==included&&!data.field_727.isEmpty()==included,"native map tracker/marker disagrees with actual enabled hand ownership");exact(s,before);
    });
    probe(s,"count-only replacement retains offhand equip height while item/NBT/empty changes lower it",()->{
      ItemStack first=tagged(35,13,4,"count-animation");s.hands(tagged(280,1,0,"main"),first);
      for(int i=0;i<8;i++)OffhandRenderer.tick(mc);
      check(OffhandRenderer.equip(mc.player,1)==1,"offhand did not finish its initial equip");
      ItemStack countOnly=DeathItems.decode(DeathItems.encode(first));countOnly.count=12;Offhand.set(inv,countOnly);
      OffhandRenderer.tick(mc);check(OffhandRenderer.equip(mc.player,1)==1,"count replacement restarted offhand equip");
      Offhand.set(inv,tagged(35,12,4,"different-native-nbt"));OffhandRenderer.tick(mc);
      check(OffhandRenderer.equip(mc.player,1)<1,"native NBT replacement skipped equip animation");
      for(int i=0;i<8;i++)OffhandRenderer.tick(mc);Offhand.set(inv,null);OffhandRenderer.tick(mc);
      check(OffhandRenderer.equip(mc.player,1)<1,"empty offhand skipped stow animation");
    });
    for(String changed:List.of("owner","world"))probe(s,"new "+changed+" swing cannot revive the prior rendered item",()->{
      Map<Field,Object> saved=new LinkedHashMap<>();
      for(String name:List.of("owner","world","rendered","equip","previousEquip","swing","previousSwing","swingTicks","changed")){
        Field f=OffhandRenderer.class.getDeclaredField(name);f.setAccessible(true);saved.put(f,f.get(null));
      }
      try{
        for(Field f:saved.keySet())switch(f.getName()){
          case "owner"->f.set(null,changed.equals("owner")?new Object():mc.player);
          case "world"->f.set(null,changed.equals("world")?new Object():mc.world);
          case "rendered"->f.set(null,tagged(4,13,0,"old-rendered"));
          case "equip","previousEquip"->f.setFloat(null,.8f);
          case "swing","previousSwing"->f.setFloat(null,.6f);
          default->{}
        }
        check(OffhandRenderer.swing(mc.player,.5f)==0,"old owner/world swing interpolation leaked");
        String[] before=s.items();OffhandRenderer.swing(mc.player);
        for(Field f:saved.keySet())switch(f.getName()){
          case "rendered"->check(f.get(null)==null,"old rendered item survived new-context swing");
          case "equip","previousEquip"->check(f.getFloat(null)==0,"old equip state survived new-context swing");
          case "swingTicks"->check(f.getInt(null)==6,"new swing was not armed");
          default->{}
        }
        exact(s,before);
      }finally{for(var entry:saved.entrySet())entry.getKey().set(null,entry.getValue());}
    });
  }

  public static void afterFrame(Minecraft mc,float delta){
    if(visual==null)return;
    try{visual.frame(mc,delta);}catch(Throwable e){try{Files.writeString(visual.state.report.resolve("visual-failure.txt"),e+"\n");log("FAIL OFFHAND ACTIONS400 VISUAL "+e);finish("failed");}catch(Exception cleanup){e.addSuppressed(cleanup);}e.printStackTrace();}
  }
  private static void finish(String status)throws Exception{
    if(visual==null)return;Visual done=visual;visual=null;
    boolean restored=false;
    try{done.state.close();restored=true;}finally{
      String outcome=restored?status:"restore-failed";
      if(outcome.equals("complete")&&!done.timingComplete)outcome="timing-incomplete";
      Files.writeString(done.state.report.resolve("visual-result.json"),Catalog.JSON.toJson(Map.of("status",outcome,"captures",done.captures,
          "phases",done.phaseResults,"timingComplete",done.timingComplete,"restored",restored))+"\n");
      log("OFFHAND ACTIONS400 VISUAL "+outcome.toUpperCase(Locale.ROOT)+" report="+done.state.report);
    }
  }
  private static final class Visual{
    final OffhandActions400State state;
    final List<String> phases=List.of("asymmetric-first","generated-first","handheld-first","bow-first","armored-rear","armored-front","main-map","off-map","both-maps","empty-offhand","fishing-off-first","fishing-off-third","fishing-swapped-both-third");
    final List<Map<String,Object>> captures=new ArrayList<>();
    final List<Map<String,Object>> phaseResults=new ArrayList<>();
    final Set<Integer> observedTicks=new LinkedHashSet<>();
    final List<Pending> pending=new ArrayList<>();
    final OffhandActions400Frames framebuffer=new OffhandActions400Frames();
    int phase,frames,startedTick,warmupFrames,warmupTicks,stableFrames,lastTick,lastCaptureTick;
    long started,lastFrame;String[] expected;ItemStack targetMain,targetOff;
    boolean warming,timingComplete=true;String action;
    private record Pending(Path file,OffhandActions400Frames.Frame image) {}
    Visual(OffhandActions400State state){this.state=state;}
    void begin(int next)throws Exception{
      phase=next;frames=stableFrames=0;observedTicks.clear();pending.clear();started=System.nanoTime();state.reset();Minecraft mc=state.mc;String name=phases.get(phase);
      boolean third=name.startsWith("armored")||name.endsWith("third");
      state.visual(third,name.equals("armored-front"));
      targetMain=tagged(267,1,37,"visible-tool");targetOff=tagged(4,13,0,"visible-block");
      if(name.equals("generated-first"))targetOff=tagged(50,16,0,"visible-torch");
      if(name.equals("handheld-first"))targetOff=tagged(278,1,12,"visible-pickaxe");
      if(name.equals("bow-first"))targetOff=tagged(261,1,12,"visible-bow");
      state.hands(targetMain,targetOff);action="settle-pose";
      if(name.startsWith("armored"))for(int i=0;i<4;i++)mc.player.inventory.armor[i]=tagged(313-i,1,11+i,"visible-armor"+i);
      if(name.equals("main-map")){targetMain=state.map();action="hands";}
      if(name.equals("off-map")){targetOff=state.map();action="hands";}
      if(name.equals("both-maps")){
        targetMain=state.map();targetOff=state.map();state.hands(targetMain,tagged(4,13,0,"outgoing-block"));action="hands";
      }
      if(name.equals("empty-offhand")){
        targetMain=state.map();targetOff=null;state.hands(targetMain,state.map());action="hands";
      }
      if(name.startsWith("fishing")){
        state.hands(tagged(280,13,0,"visible-stick"),tagged(346,1,17,"visible-rod"));action="native-offhand-cast";
        if(name.contains("swapped")){
          state.use();check(mc.player.fishHook!=null,"visual offhand rod did not cast");
          check(Offhand.swap(mc),"visual native rod swap declined");state.nativeState.finish();
          Offhand.set(mc.player.inventory,tagged(346,1,23,"other-rod"));action="native-main-swing-after-durable-swap";
        }
      }
      framebuffer.prepare(mc);expected=state.items();startedTick=lastTick=ticks(mc);lastFrame=System.nanoTime();warming=true;
      log("OFFHAND ACTIONS400 VISUAL WARMUP "+name);
    }
    private static int ticks(Minecraft mc){return ((local.luke.power.validation.mixin.Client253Access)(Object)mc).power$ticks();}
    void frame(Minecraft mc,float delta)throws Exception{
      check(mc==state.mc&&mc.player.world==mc.world&&Display.isActive()&&mc.currentScreen==null&&!mc.paused,"ordinary offhand capture lost its live gameplay context");
      check(System.nanoTime()-started<20_000_000_000L,"offhand capture did not receive normal frames/ticks");frames++;
      exact(state,expected);String name=phases.get(phase);
      int nowTick=ticks(mc),elapsed=nowTick-startedTick;long now=System.nanoTime();
      if(warming){
        stableFrames=nowTick-lastTick<=1&&nowTick>=lastTick&&now-lastFrame<100_000_000L?stableFrames+1:0;
        lastTick=nowTick;lastFrame=now;
        if(frames<30||elapsed<12||stableFrames<8)return;
        warmupFrames=frames;warmupTicks=elapsed;frames=0;startedTick=nowTick;lastCaptureTick=Integer.MIN_VALUE;
        if(action.equals("hands"))state.hands(targetMain,targetOff);
        else if(action.equals("native-offhand-cast")){
          state.use();check(mc.player.fishHook!=null,"visual offhand rod did not cast");
        }else if(action.equals("native-main-swing-after-durable-swap"))mc.player.method_500();
        expected=state.items();warming=false;
        // No framebuffer read, encoding, JSON or log write in the event frame.
        return;
      }
      boolean transition=name.contains("map")||name.equals("empty-offhand")||name.startsWith("fishing");
      if(transition&&lastCaptureTick!=elapsed&&(elapsed<=3||observedTicks.isEmpty())){
        observedTicks.add(elapsed);capture(name+"-observed-tick"+elapsed+"-frame"+frames,delta,elapsed);return;
      }
      if(frames<30||elapsed<12||lastCaptureTick==elapsed)return;
      if(name.startsWith("fishing"))check(mc.player.fishHook!=null&&!mc.player.fishHook.dead,"ordinary fishing pose lost its native hook");
      capture(name+"-settled-tick"+elapsed,delta,elapsed);
      List<Integer> wanted=name.startsWith("fishing")?List.of(2,3):transition?List.of(1,3):List.of();
      List<Integer> missing=wanted.stream().filter(tick->!observedTicks.contains(tick)).toList();
      timingComplete&=missing.isEmpty();
      phaseResults.add(Map.of("phase",name,"action",action,"warmupFrames",warmupFrames,"warmupTicks",warmupTicks,
          "observedEarlyTicks",List.copyOf(observedTicks),"missingTargets",missing));
      // PNG/JSON work begins only after the early transition and settled image were read.
      for(Pending image:pending)OffhandActions400Frames.write(image.image,image.file);
      pending.clear();
      Files.writeString(state.report.resolve("captures.json"),Catalog.JSON.toJson(captures)+"\n");
      Files.writeString(state.report.resolve("phase-results.json"),Catalog.JSON.toJson(phaseResults)+"\n");
      log("OFFHAND ACTIONS400 VISUAL PHASE "+name+" observedTicks="+observedTicks+" missingTargets="+missing);
      if(phase+1==phases.size())finish("complete");else begin(phase+1);
    }
    void capture(String name,float delta,int elapsed)throws Exception{
      Minecraft mc=state.mc;check(GL11.glGetError()==GL11.GL_NO_ERROR,"native offhand render left an OpenGL error before "+name);
      check(pending.size()<5,"offhand phase exceeded its bounded raw frame queue");
      Map<String,Object> item=new LinkedHashMap<>();item.put("file",name+".png");item.put("frame",frames);item.put("thirdPerson",mc.options.thirdPerson);
      item.put("phase",phases.get(phase));item.put("action",action);item.put("elapsedTicks",elapsed);item.put("partialTick",delta);
      item.put("position",List.of(mc.player.x,mc.player.y,mc.player.z));item.put("previousPosition",List.of(mc.player.prevX,mc.player.prevY,mc.player.prevZ));
      item.put("renderState",state.renderState(delta));
      item.put("main",AutoTools400State.bytes(mc.player.inventory.main[0]));item.put("offhand",AutoTools400State.bytes(Offhand.get(mc.player.inventory)));
      item.put("hook",mc.player.fishHook!=null);if(mc.player.fishHook!=null)item.put("castLeft",OffhandFishing.left(mc.player.fishHook));
      long readStarted=System.nanoTime();OffhandActions400Frames.Frame image=framebuffer.read(mc);
      item.put("readbackMs",(System.nanoTime()-readStarted)/1e6);item.put("width",image.width());item.put("height",image.height());
      pending.add(new Pending(state.report.resolve(name+".png"),image));captures.add(item);lastCaptureTick=elapsed;
    }
  }
}
