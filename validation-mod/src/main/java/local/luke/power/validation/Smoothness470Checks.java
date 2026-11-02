package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import local.luke.power.input.*;
import local.luke.power.render.*;
import local.luke.power.visual.VisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Opt-in block-action/frame profiler. Primitive buffers only while sampling; reports flush after stopping. */
public final class Smoothness470Checks {
  public static final int PLACE=0,BREAK=1,NOTIFY=2,MESH=3,UPLOAD=4,COMPACT=5,GROW=6,FULL_USE=7,SWAP=8,SYNC=9,POLL=10,CLIENT_TICK=11,FULL_RENDER=12;
  private static final String[] NAMES={"nativePlace","nativeBreak","blockNotify","terrainMesh","vboUpload","vboCompact","vboGrow","nativeUseDispatch","swapBuffers","frameLimiter","eventPoll","clientTick","fullRender"};
  private static final int LIMIT=65536;
  private static Task task;
  private Smoothness470Checks() {}
  public static void run(Minecraft mc,String command)throws Exception{
    String[] args=command.trim().split("\\s+");
    if(args[0].equals("status")){log(task==null?"SMOOTHNESS470 idle":"SMOOTHNESS470 "+task.label+" frames="+task.frames);return;}
    if(args[0].equals("stop")){finish("stopped");return;}
    if(args[0].equals("water-off-check")){waterOff(mc);return;}
    check(task==null,"stop the current smoothness observer first");
    check(args.length>=2&&Set.of("observe","edits").contains(args[0]),"smoothness-470 observe|edits LABEL [SECONDS], water-off-check, status, stop");
    check(args[1].matches("[a-zA-Z0-9_-]{1,60}"),"invalid smoothness report label");
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&mc.player.world==mc.world&&mc.currentScreen==null&&Display.isActive(),"focus a disposable singleplayer world without a menu");
    SilentAudioChecks.ready(mc);check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable client");
    int seconds=args.length>2?Integer.parseInt(args[2]):8;check(seconds>=2&&seconds<=30,"sample duration must be 2..30 seconds");
    int block=args.length>3?Integer.parseInt(args[3]):4;
    check(Set.of(4,5,20,44,50).contains(block),"profile block must be cobblestone4, planks5, glass20, slab44 or torch50");
    AutoTools400State edits=null;
    try{
      if(args[0].equals("edits")){
        check(!Mouse.isButtonDown(0)&&!Mouse.isButtonDown(1)&&!UseInput.held(),"release attack and Use before automated block edits");
        edits=new AutoTools400State(mc);edits.reset();edits.attack.set(false);CreativeVehicleChecks.mode(mc,"CREATIVE");
        edits.scene(4,1);edits.lookAtTarget();mc.world.method_200(edits.x,edits.y,edits.z,0);mc.player.inventory.main[0]=new ItemStack(block,64,0);
      }
      task=new Task(mc,args[1],seconds,edits,block);log("SMOOTHNESS470 START "+args[1]+" mode="+args[0]+" warmup=2s sample="+seconds+"s");
    }catch(Exception|Error failure){FramePresentation470Checks.restore();if(edits!=null)edits.close();throw failure;}
  }
  public static boolean observing(){return task!=null;}
  public static void stop()throws Exception{finish("stopped");}
  public static long start(){Task t=task;return t!=null&&Thread.currentThread()==t.thread&&System.nanoTime()>=t.from?System.nanoTime():0;}
  public static void measured(int kind,long started){
    Task t=task;if(started==0||t==null||Thread.currentThread()!=t.thread)return;
    double elapsed=(System.nanoTime()-started)/1e6;t.work[kind].add(elapsed);t.counts[kind]++;t.times[kind]+=elapsed;
  }
  public static void beforeFrame(Minecraft mc){
    Task t=task;if(t==null||mc.world!=t.world)return;long now=System.nanoTime();if(now<t.from)return;
    if(t.allocated<0)t.allocated=allocated();
    if(t.lastFrame!=0)t.intervals.add((now-t.lastFrame)/1e6);t.lastFrame=now;t.frameStart=now;
  }
  public static void afterFrame(){
    Task t=task;if(t==null||t.frameStart==0)return;double elapsed=(System.nanoTime()-t.frameStart)/1e6;
    t.cpu.add(elapsed);int row=t.frames++;
    if(row<LIMIT){t.frameCpu[row]=elapsed;for(int k=0;k<NAMES.length;k++){t.frameCounts[k][row]=t.counts[k];t.frameTimes[k][row]=t.times[k];}}
    Arrays.fill(t.counts,0);Arrays.fill(t.times,0);t.frameStart=0;
  }
  public static void tickStarted(long started){Task t=task;if(started!=0&&t!=null)t.tickSequence++;}
  public static void renderEnded(long started){
    Task t=task;if(started==0||t==null)return;long now=System.nanoTime();t.lastRenderEnd=now;t.renderSequence++;
    measured(FULL_RENDER,started);
  }
  public static long beforeSwap(){
    Task t=task;if(t==null||Thread.currentThread()!=t.thread)return 0;long now=System.nanoTime();if(now<t.from)return 0;
    if(t.lastPresent!=0)t.present.add((now-t.lastPresent)/1e6);t.lastPresent=now;
    int row=t.swaps++;
    if(row<LIMIT){t.swapStarts[row]=now;t.swapRenderEnd[row]=t.lastRenderEnd;t.swapRenderSequence[row]=t.renderSequence;t.swapTickSequence[row]=t.tickSequence;}
    return now;
  }
  public static void afterSwap(long started){
    Task t=task;if(started==0||t==null)return;int row=t.swaps-1;
    if(row>=0&&row<LIMIT&&t.swapStarts[row]==started)t.swapEnds[row]=System.nanoTime();measured(SWAP,started);
  }
  public static void tick(Minecraft mc){
    Task t=task;if(t==null)return;
    try{
      if(mc.world!=t.world||mc.player!=t.player||mc.currentScreen!=null||!Display.isActive()){finish("context-changed");return;}
      if(System.nanoTime()>=t.until){finish("complete");return;}
      if(t.edits!=null){
        boolean success;int entityStart=mc.world.field_198.size();
        if(mc.player.inventory.selectedSlot!=0||mc.player.inventory.main[0]!=t.stack){
          t.decline=diagnostic(t,"fixture hand ownership changed");finish("native-action-declined");return;
        }
        // This disposable inventory belongs to the fixture. Preserve user unlimited-item preferences and refill outside the timed callback.
        t.stack.count=64;t.actions++;
        if(mc.world.getBlockId(t.edits.x,t.edits.y,t.edits.z)==0){
          mc.field_2823=new net.minecraft.class_27(t.edits.x,t.edits.y-1,t.edits.z,1,
              net.minecraft.util.math.Vec3d.createCached(t.edits.x+.5,t.edits.y,t.edits.z+.5));
          local.luke.power.offhand.OffhandUse.newPress(mc);
          ((local.luke.power.validation.mixin.Client253Access)(Object)mc).power$use(1);
          success=mc.world.getBlockId(t.edits.x,t.edits.y,t.edits.z)==t.block;
        }else success=mc.interactionManager.method_1716(t.edits.x,t.edits.y,t.edits.z,1);
        // Native drops append synchronously. Retire only item entities born inside this owned native action.
        for(int i=entityStart;i<mc.world.field_198.size();i++)if(mc.world.field_198.get(i) instanceof net.minecraft.class_142 item){item.markDead();t.drops++;}
        if(!success){t.decline=diagnostic(t,"native action left an unexpected target");log("SMOOTHNESS470 DECLINED "+t.decline);finish("native-action-declined");return;}
      }
    }catch(Exception failure){try{finish("failed");}catch(Exception cleanup){failure.addSuppressed(cleanup);}throw new IllegalStateException(failure);}
  }
  private static Map<String,Object> diagnostic(Task t,String reason){
    Minecraft mc=t.mc;ItemStack stack=mc.player.inventory.getSelectedItem();var out=new LinkedHashMap<String,Object>();
    out.put("reason",reason);out.put("actions",t.actions);out.put("target",List.of(t.edits.x,t.edits.y,t.edits.z));out.put("expected",t.block);
    out.put("actualBlock",mc.world.getBlockId(t.edits.x,t.edits.y,t.edits.z));out.put("selectedSlot",mc.player.inventory.selectedSlot);
    out.put("hand",stack==null?"empty":List.of(stack.itemId,stack.count,stack.getDamage()));out.put("creative",mc.player instanceof PlayerMode mode&&mode.power$creative());
    out.put("focused",Display.isActive());out.put("gameFocus",mc.field_2778);out.put("mouseGrabbed",Mouse.isGrabbed());out.put("paused",mc.paused);
    out.put("manager",mc.interactionManager.getClass().getName());out.put("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getName());
    out.put("nativePlanBusy",local.luke.power.inventory.NativeInventoryPlan.busy());out.put("carrying",InteractionState.carryingContainer);out.put("detached",DetachedCamera.isActive());
    var hit=mc.field_2823;out.put("hit",hit==null?"none":List.of(hit.field_1983.toString(),hit.field_1984,hit.field_1985,hit.field_1986,hit.field_1987));return out;
  }
  private static void finish(String status)throws Exception{
    Task t=task;if(t==null)return;if(!status.equals("complete"))FramePresentation470Checks.restore();task=null;long stopped=System.nanoTime(),sampleAllocated=t.allocated<0?0:allocated()-t.allocated;boolean restored=t.edits==null;
    if(t.edits!=null&&t.mc.world==t.world&&t.mc.player==t.player){t.edits.close();restored=true;}
    Map<String,Object> report=new LinkedHashMap<>();report.put("label",t.label);report.put("presentationMode",t.presentationMode);report.put("presentationModeAtEnd",FramePresentation470Checks.mode());report.put("actualSwaps",t.swaps);report.put("nativeActionsIncludingWarmup",t.actions);report.put("ownedDropsRetired",t.drops);if(t.decline!=null)report.put("decline",t.decline);report.put("status",status);report.put("restored",restored);
    report.put("blockId",t.block);report.put("warmupSeconds",2);report.put("sampleSeconds",Math.max(0,(stopped-t.from)/1e9));report.put("frames",t.frames);
    report.put("window",List.of(t.mc.displayWidth,t.mc.displayHeight));report.put("renderCpuMs",t.cpu.report());report.put("frameIntervalsMs",t.intervals.report());report.put("presentIntervalsMs",t.present.report());
    report.put("endpointCountersIncludeWarmup",true);report.put("waterPasses",WaterTerrain.secondPasses()-t.waterPasses);report.put("neutralPasses",WaterTerrain.neutralPasses()-t.neutralPasses);
    report.put("heldLightRequests",HeldLighting.requests()-t.heldRequests);report.put("waterDarkness",List.of(t.darkness,t.outside));
    report.put("allocatedMainThreadBytes",sampleAllocated);var work=new LinkedHashMap<String,Object>();for(int k=0;k<NAMES.length;k++)work.put(NAMES[k],t.work[k].report());report.put("work",work);
    List<Map<String,Object>> frames=new ArrayList<>();for(int i=0;i<Math.min(t.frames,LIMIT);i++){
      Map<String,Object> row=new LinkedHashMap<>();row.put("index",i);row.put("renderCpuMs",t.frameCpu[i]);
      for(int k=0;k<NAMES.length;k++)if(t.frameCounts[k][i]!=0){row.put(NAMES[k]+"Calls",t.frameCounts[k][i]);row.put(NAMES[k]+"Ms",t.frameTimes[k][i]);}frames.add(row);
    }
    report.put("frameWork",frames);
    List<Map<String,Object>> swaps=new ArrayList<>();
    for(int i=0;i<Math.min(t.swaps,LIMIT);i++){
      var row=new LinkedHashMap<String,Object>();row.put("sequence",i);row.put("swapStartNs",t.swapStarts[i]);row.put("swapEndNs",t.swapEnds[i]);
      row.put("fullRenderSequence",t.swapRenderSequence[i]);row.put("renderEndNs",t.swapRenderEnd[i]);row.put("tickSequence",t.swapTickSequence[i]);swaps.add(row);
    }
    report.put("swapSamples",swaps);Path file=Path.of("power-beta-data/reports/smoothness-470",t.label+"-"+System.currentTimeMillis()+".json");
    Files.createDirectories(file.getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
    log("SMOOTHNESS470 "+status+" restored="+restored+" report="+file);
  }
  private static void waterOff(Minecraft mc)throws Exception{
    check(task==null&&mc.world!=null&&mc.player!=null&&!RenderControls.fullBright(),"stop profiling and disable full bright in the disposable world");
    var old=VisualConfig.copy();boolean began=false;
    try{
      var value=VisualConfig.copy();value.underwaterDarkness=value.underwaterDarknessOutside=0;VisualConfig.preview(value);WaterTerrain.prepare(mc);
      check(WaterTerrain.prepared(),"water opt-in did not prepare endpoint work");
      value.underwaterDarkness=value.underwaterDarknessOutside=100;VisualConfig.preview(value);WaterTerrain.prepare(mc);
      RenderControls.beginRender(false);try{began=WaterTerrain.begin(false);if(began)WaterTerrain.end();}finally{RenderControls.endRender();}
      log("SMOOTHNESS470 WATER OFF active="+WaterTerrain.prepared()+" endpointWork="+began);
      check(!WaterTerrain.prepared()&&!began,"disabled water retained optional mesh/column work");
      long requests=WaterTerrain.preparations();
      value.underwaterDarknessOutside=0;VisualConfig.preview(value);WaterTerrain.prepare(mc);check(WaterTerrain.prepared(),"water re-enable failed to prepare endpoints");
      check(WaterTerrain.preparations()==requests,"unchanged off/on rebuilt retained endpoints");
      value.underwaterDarknessOutside=100;VisualConfig.preview(value);WaterTerrain.prepare(mc);
      RenderControls.beginRender(false);try{began=WaterTerrain.begin(true);if(began)WaterTerrain.end();}finally{RenderControls.endRender();}
      check(began,"disabled water blocked independent held-light endpoint capture");
      WaterTerrain.beginMesh(mc.world,0,0,0);WaterTerrain.endMesh();
      value.underwaterDarknessOutside=0;VisualConfig.preview(value);WaterTerrain.prepare(mc);
      check(WaterTerrain.preparations()==requests+1,"skipped disabled mesh did not request one catch-up");
      value.underwaterDarknessOutside=25;VisualConfig.preview(value);WaterTerrain.prepare(mc);
      check(WaterTerrain.preparations()==requests+1,"active slider change rebuilt terrain");
      log("SMOOTHNESS470 WATER OFF PASS retained=true heldIndependent=true catchup=1 activeSliderRebuilds=0");
    }finally{VisualConfig.preview(old);WaterTerrain.prepare(mc);}
  }
  private static long allocated(){return ((com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().getId());}
  private static final class Metric{
    final double[] values=new double[LIMIT];int count;long calls;double sum,sumSquares,max;
    void add(double value){calls++;sum+=value;sumSquares+=value*value;max=Math.max(max,value);if(count<LIMIT)values[count++]=value;}
    Map<String,Object> report(){double[] sorted=Arrays.copyOf(values,count);Arrays.sort(sorted);Map<String,Object> out=new LinkedHashMap<>();out.put("calls",calls);out.put("max",max);out.put("total",sum);out.put("mean",calls==0?0:sum/calls);out.put("stddev",calls==0?0:Math.sqrt(Math.max(0,sumSquares/calls-(sum/calls)*(sum/calls))));out.put("min",count==0?0:sorted[0]);
      for(int p:new int[]{1,5,50,95,99})out.put("p"+p,count==0?0:sorted[Math.min(count-1,(int)Math.ceil(count*p/100d)-1)]);
      for(double threshold:new double[]{8.333333,16.666667,33.333333}){int over=0;for(int i=0;i<count;i++)if(values[i]>threshold)over++;out.put("over"+(threshold<10?8:threshold<20?16:33)+"ms",over);}out.put("truncated",calls>count);return out;}
  }
  private static final class Task{
    final Minecraft mc;final World world;final PlayerEntity player;final Thread thread=Thread.currentThread();final String label;final AutoTools400State edits;final int block;final ItemStack stack;final String presentationMode=FramePresentation470Checks.mode();
    final long from=System.nanoTime()+2_000_000_000L,until;long allocated=-1;final int darkness=VisualConfig.current().underwaterDarkness,outside=VisualConfig.current().underwaterDarknessOutside;
    final long waterPasses=WaterTerrain.secondPasses(),neutralPasses=WaterTerrain.neutralPasses(),heldRequests=HeldLighting.requests();
    final Metric cpu=new Metric(),intervals=new Metric(),present=new Metric();final Metric[] work=new Metric[NAMES.length];
    final double[] frameCpu=new double[LIMIT],times=new double[NAMES.length];final int[] counts=new int[NAMES.length];
    final int[][] frameCounts=new int[NAMES.length][LIMIT];final double[][] frameTimes=new double[NAMES.length][LIMIT];
    final long[] swapStarts=new long[LIMIT],swapEnds=new long[LIMIT],swapRenderEnd=new long[LIMIT],swapRenderSequence=new long[LIMIT],swapTickSequence=new long[LIMIT];
    int frames,actions,drops,swaps;Map<String,Object> decline;long frameStart,lastFrame,lastPresent,lastRenderEnd,renderSequence,tickSequence;
    Task(Minecraft mc,String label,int seconds,AutoTools400State edits,int block){this.block=block;this.mc=mc;world=mc.world;player=mc.player;this.label=label;this.edits=edits;stack=edits==null?null:mc.player.inventory.main[0];until=from+seconds*1_000_000_000L;for(int k=0;k<work.length;k++)work[k]=new Metric();}
  }
}
