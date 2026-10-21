package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.field;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.heldlight.HeldLightConfig;
import local.luke.power.render.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

/** Phased, real-frame checks. Dispatch and native frame/tick callbacks are installed by Validation. */
public final class HeldLight400Checks {
  private static final int SAMPLE_FRAMES=120;
  private static Task task;
  private HeldLight400Checks() {}
  private enum Phase {
    BASELINE(0), MAIN(14), OFFHAND_ONLY(15), BOTH_HANDS(15), OFFHAND_DISABLED(0), EMPTY(0), LAVA(15),
    MOVING(14), FREECAM_FIXED(14), FREECAM_PLAYER_MOVING(14), FREECAM_MOVING(14), STEREO(14), FULL_BRIGHT(0),
    WATER_NATIVE_OFF(0), WATER_CLEAR_OFF(0), WATER_CLEAR_ON(14), WATER_MID_ON(14), WATER_NATIVE_ON(14),
    PREVIEW_OFF(0), CANCEL(14), COLD_CAPTURE_BASE(0), COLD_CAPTURE(14), WARM_CAPTURE(14), FINAL_DISABLED(0);
    final int emission;
    Phase(int emission){this.emission=emission;}
    boolean moving(){return this==MOVING||this==FREECAM_PLAYER_MOVING||this==FREECAM_MOVING;}
    boolean water(){return name().startsWith("WATER_");}
    boolean dormant(){return emission==0;}
    int warmup(){return this==BASELINE?80:30;}
    String file(){return name().toLowerCase(Locale.ROOT).replace('_','-');}
  }

  public static void run(Minecraft mc,String command) throws Exception {
    if(command.equals("status")) {log(task==null?"HELD LIGHT 400 idle":"HELD LIGHT 400 "+task.phase+" ticks="+task.phaseTicks+" samples="+task.samples.frames+" report="+task.root);return;}
    if(command.equals("stop")) {if(task!=null)finish("stopped");return;}
    if(command.equals("capture-cold")||command.equals("capture-warm")) {
      check(task!=null,"start the phased fixture first");
      task.jump=command.equals("capture-cold")?Phase.COLD_CAPTURE_BASE:Phase.WARM_CAPTURE;return;
    }
    check(command.equals("start")||command.equals("capture-pair"),"use held-light-400 start|capture-pair|status|stop|capture-cold|capture-warm");
    check(task==null,"held-light fixture already running");
    Task next=new Task(mc);next.capturePair=command.equals("capture-pair");task=next;
    try {
      check(next.currentReload()==null,"finish the current resource reload before starting");
      next.checkShader();next.scene.build();next.expectReload();next.begin(Phase.BASELINE);log("HELD LIGHT 400 START "+next.root);
    }
    catch(Throwable failure){next.fail(failure);finish("failed");}
  }

  public static void tick(Minecraft mc) {
    if(task==null)return;
    try {
      if(task.failure!=null){finish("failed");return;}
      task.tick(mc);
    }catch(Throwable failure){if(task!=null)task.fail(failure);try{finish("failed");}catch(Exception ignored){}}
  }
  public static void beforeTick(Minecraft mc) {if(task!=null&&!task.inCapture)task.frames.beforeTick(task.measuring()?task.samples:null);}
  public static void afterTick(Minecraft mc) {if(task!=null&&!task.inCapture)task.frames.afterTick();}
  public static void beforeFrame(Minecraft mc) {
    if(task==null||task.inCapture||task.failure!=null||task.expectedReload!=null)return;
    try {
      task.frameActive=true;task.frameAdmissions=HeldLighting.captures();task.frameLightHash=task.scene.lightHash();
      task.frames.beforeFrame(task.measuring()?task.samples:null);
    }catch(Throwable failure){task.fail(failure);}
  }
  public static void afterFrame(Minecraft mc,float delta) {
    if(task==null||task.inCapture)return;
    try {
      if(!task.frameActive)return;
      task.frames.afterFrame();task.frameActive=false;
      if(task.failure==null)task.frame(mc,delta);
    }catch(Throwable failure){task.fail(failure);}
  }

  private static void finish(String status) throws Exception {
    Task finished=task;if(finished==null)return;task=null;
    for(ConfigSession session:new ConfigSession[]{finished.preview,finished.brightnessPreview}) {
      try {if(session!=null)session.discard();}
      catch(Throwable failure){finished.fail(failure);status="restore-failed";}
    }
    try {finished.scene.restore();}
    catch(Throwable failure){finished.fail(failure);status="restore-failed";}
    finally {
      try {finished.frames.close();}catch(Throwable failure){finished.fail(failure);status="restore-failed";}
      finished.write(status);
    }
    log("HELD LIGHT 400 "+status.toUpperCase(Locale.ROOT)+" report="+finished.root+" failure="+finished.failure);
  }
  private static Object staticField(Class<?> type,String name) throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}

  private static final class Task {
    final Minecraft mc;
    final HeldLight400Scene scene;
    final HeldLight400Frames frames;
    final Path root=Path.of("power-beta-data/reports/held-light-400/run-"+System.currentTimeMillis());
    final Map<String,HeldLight400Frames.Samples> results=new LinkedHashMap<>();
    final Map<String,Object> captures=new LinkedHashMap<>();
    final long started=System.nanoTime();
    Phase phase,jump;
    HeldLight400Frames.Samples samples;
    ConfigSession preview,brightnessPreview;
    int phaseTicks;
    long phaseStarted,requests,admissions,neutrals,waters,mismatches,frameAdmissions,frameLightHash;
    boolean advance,frameActive,inCapture,capturePair;
    Object expectedReload;
    long reloadStarted,stalePauseStarted;
    boolean reloadLogged;
    int completedReloads;
    String failure;
    double baselineMean;

    Task(Minecraft mc) throws Exception {
      this.mc=mc;scene=new HeldLight400Scene(mc);Files.createDirectories(root);frames=new HeldLight400Frames();
    }
    void checkShader() throws Exception {
      Object shader=Class.forName("net.mine_diver.smoothbeta.client.render.Shaders").getMethod("getTerrainShader").invoke(null);
      int program=(int)shader.getClass().getMethod("getProgramRef").invoke(shader);
      check(GL20.glGetAttribLocation(program,"PowerWaterColor")==5&&GL20.glGetAttribLocation(program,"PowerLightMetaHigh")==6,"held-light attributes are not linked at 5/6");
      for(String name:List.of("PowerHeldStrength","PowerHeldEmission","PowerHeldSourceRegion","PowerHeldCurve[0]"))
        check(GL20.glGetUniformLocation(program,name)>=0,"missing terrain uniform "+name);
      log("HELD LIGHT 400 GPU "+GL11.glGetString(GL11.GL_VENDOR)+" / "+GL11.glGetString(GL11.GL_RENDERER)+" timers="+frames.gpuTimers());
    }
    boolean measuring(){return phase!=null&&failure==null&&expectedReload==null&&!advance&&phaseTicks>=phase.warmup()&&samples.frames<SAMPLE_FRAMES;}
    Object currentReload() throws Exception {
      var manager=Class.forName("net.modificationstation.stationapi.api.client.resource.ReloadScreenManager");
      return ((Optional<?>)manager.getMethod("getCurrentReload").invoke(null)).orElse(null);
    }
    void expectReload() throws Exception {
      Object reload=currentReload();
      if(reload==null)return;
      check(expectedReload==null,"fixture started overlapping resource reloads");
      expectedReload=reload;reloadStarted=System.nanoTime();stalePauseStarted=0;reloadLogged=false;
      log("HELD LIGHT 400 REQUESTED RELOAD "+phase+" "+inputState());
    }
    String inputState() {
      return "screen="+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getName())
          +" paused="+mc.paused+" displayActive="+Display.isActive()+" nativeFocus="+mc.field_2778
          +" grabbed="+Mouse.isGrabbed()+" phaseTicks="+phaseTicks+" expectedReload="+(expectedReload!=null);
    }
    boolean awaitingReload() throws Exception {
      check(Display.isActive(),"fixture lost display focus: "+inputState());
      if(expectedReload==null) {
        check(mc.currentScreen==null&&!mc.paused,"fixture menu/pause guard: "+inputState());
        return false;
      }
      check(System.nanoTime()-reloadStarted<15_000_000_000L,"fixture resource reload timed out: "+inputState());
      Object current=currentReload();
      check(current==null||current==expectedReload,"resource reload ownership changed: "+inputState());
      if(mc.currentScreen!=null) {
        check(current==expectedReload
            &&mc.currentScreen.getClass().getName().equals("net.modificationstation.stationapi.api.client.resource.ReloadScreen")
            &&field(mc.currentScreen,"parent")==null,"unrelated menu during fixture reload: "+inputState());
        check(!(boolean)field(mc.currentScreen,"exceptionThrown"),"native resource reload failed: "+inputState());
        if(!reloadLogged){log("HELD LIGHT 400 WAIT RELOAD "+phase+" "+inputState());reloadLogged=true;}
        return true;
      }
      if(current!=null)return true; // Its own screen is scheduled for the next render start.
      if(mc.paused) {
        check(mc.field_2778&&Mouse.isGrabbed(),"reload ended without gameplay focus: "+inputState());
        if(stalePauseStarted==0){stalePauseStarted=System.nanoTime();log("HELD LIGHT 400 WAIT NATIVE PAUSE "+inputState());}
        check(System.nanoTime()-stalePauseStarted<1_000_000_000L,"native pause remained after fixture reload: "+inputState());
        return true; // Minecraft.run recomputes paused at frame end; never force its flag.
      }
      checkShader();expectedReload=null;completedReloads++;phaseTicks=0;phaseStarted=System.nanoTime();
      log("HELD LIGHT 400 RELOAD COMPLETE "+phase+" warmup restarted "+inputState());
      return false;
    }
    void fail(Throwable error) {
      if(failure==null)failure=error.toString();else failure+="; "+error;
      error.printStackTrace();try{log("FAIL HELD LIGHT 400 "+phase+" "+error);}catch(Exception ignored){}
    }
    void begin(Phase next) throws Exception {
      phase=next;phaseTicks=0;phaseStarted=System.nanoTime();advance=false;
      samples=new HeldLight400Frames.Samples();results.put(phase.file(),samples);
      if(preview!=null&&phase!=Phase.CANCEL){preview.discard();preview=null;}
      FreecamPreviewChecks.active(false);
      boolean stereoChanged=mc.options.anaglyph3d!=(phase==Phase.STEREO);
      scene.stereo(phase==Phase.STEREO);if(stereoChanged)expectReload();
      scene.bright(phase==Phase.FULL_BRIGHT);
      scene.water(100);scene.offhandEnabled(true);scene.enabled(!phase.dormant()||phase==Phase.FULL_BRIGHT);scene.hands(50,0);scene.basePose();
      switch(phase) {
        case BASELINE -> {
          brightnessPreview=SettingsRegistry.open(mc);find(brightnessPreview,"native.brightness").parse("0");brightnessPreview.preview(true);
          scene.enabled(false);scene.hands(50,0);
        }
        case OFFHAND_ONLY -> scene.hands(0,89);
        case BOTH_HANDS -> scene.hands(50,89);
        case OFFHAND_DISABLED -> {scene.enabled(true);scene.hands(0,89);scene.offhandEnabled(false);}
        case EMPTY -> {scene.enabled(true);scene.hands(0,0);}
        case LAVA -> scene.hands(327,0);
        case FREECAM_FIXED,FREECAM_PLAYER_MOVING,FREECAM_MOVING -> {FreecamPreviewChecks.active(true);FreecamPreviewChecks.pose(scene.x+3,scene.floor+4,scene.z+7,15,-20);}
        case WATER_NATIVE_OFF,WATER_CLEAR_OFF,WATER_CLEAR_ON,WATER_MID_ON,WATER_NATIVE_ON -> {
          scene.waterPose();scene.water(phase==Phase.WATER_CLEAR_OFF||phase==Phase.WATER_CLEAR_ON?0:phase==Phase.WATER_MID_ON?50:100);
        }
        case PREVIEW_OFF -> {
          scene.enabled(true);preview=SettingsRegistry.open(mc);find(preview,"heldLight.enabled").parse("false");preview.preview(true);
          check(!HeldLightConfig.current().enabled,"held-light preview did not apply");
        }
        case CANCEL -> {check(preview!=null,"missing held-light preview draft");preview.discard();preview=null;check(HeldLightConfig.current().enabled,"Cancel did not restore held light");}
        case COLD_CAPTURE_BASE -> {scene.enabled(false);scene.markDirty();}
        case COLD_CAPTURE -> isometric("cold",true);
        case WARM_CAPTURE -> isometric("warm",false);
        default -> {}
      }
      requests=HeldLighting.requests();admissions=HeldLighting.captures();neutrals=WaterTerrain.neutralPasses();waters=WaterTerrain.secondPasses();mismatches=WaterTerrain.mismatches();
      log("HELD LIGHT 400 PHASE "+phase+" position="+mc.player.x+","+mc.player.y+","+mc.player.z);write("running");
    }
    void tick(Minecraft client) throws Exception {
      check(client==mc&&mc.world==scene.world&&mc.player==scene.player,"fixture world or player changed");
      check(System.nanoTime()-started<240_000_000_000L,"held-light fixture exceeded four-minute watchdog");
      if(awaitingReload())return;
      check(System.nanoTime()-phaseStarted<25_000_000_000L,"phase did not receive enough native frames within 25 seconds");
      if(jump!=null){Phase next=jump;jump=null;begin(next);return;}
      if(advance){
        if(capturePair&&phase==Phase.BASELINE){begin(Phase.COLD_CAPTURE_BASE);return;}
        int next=phase.ordinal()+1;if(next==Phase.values().length){finish("passed");return;}begin(Phase.values()[next]);return;
      }
      phaseTicks++;
      if(phase==Phase.MOVING||phase==Phase.FREECAM_PLAYER_MOVING) scene.pose(scene.x+.5+Math.sin(phaseTicks*.09)*2,scene.floor+1,scene.z-2.5,0,10,false);
      else if(phase.water())scene.waterPose();else scene.basePose();
      if(phase==Phase.FREECAM_MOVING)FreecamPreviewChecks.pose(scene.x+3+Math.sin(phaseTicks*.07)*2,scene.floor+4,scene.z+7,15,-20);
    }
    void frame(Minecraft client,float delta) throws Exception {
      check(client==mc&&mc.world==scene.world,"rendered a different world");
      check(HeldLighting.captures()-frameAdmissions<=1,"ordinary frame or stereo eyes admitted more than one held mesh");
      check(scene.lightHash()==frameLightHash,"native integer light samples changed during rendering");
      check(HeldLighting.emission()==phase.emission,"source emission expected="+phase.emission+" actual="+HeldLighting.emission());
      float strength=(float)staticField(HeldLighting.class,"strength");
      if(phase.emission>0) {
        double sx=mc.player.prevX+(mc.player.x-mc.player.prevX)*delta;
        double sy=mc.player.prevY+(mc.player.y-mc.player.prevY)*delta-mc.player.field_1631+1;
        double sz=mc.player.prevZ+(mc.player.z-mc.player.prevZ)*delta;
        check(Math.abs((double)staticField(HeldLighting.class,"x")-sx)<1e-6
            &&Math.abs((double)staticField(HeldLighting.class,"y")-sy)<1e-6
            &&Math.abs((double)staticField(HeldLighting.class,"z")-sz)<1e-6,"held source detached from interpolated physical player");
        if(strength>=.99f&&samples.firstWarmMillis<0)samples.firstWarmMillis=(System.nanoTime()-phaseStarted)/1_000_000;
      } else {
        check(strength==0,"dormant source retained strength");
        check(HeldLighting.requests()==requests&&HeldLighting.captures()==admissions&&WaterTerrain.neutralPasses()==neutrals,"disabled/empty/full-bright source performed held preparation");
      }
      check(WaterTerrain.retainedBytes()<=24L*1024*1024,"held/water retained scratch exceeds 24 MiB");
      if(!measuring())return;
      samples.frames++;samples.minimumStrength=Math.min(samples.minimumStrength,strength);samples.maximumStrength=Math.max(samples.maximumStrength,strength);
      if(samples.frames<SAMPLE_FRAMES)return;
      samples.requests=HeldLighting.requests()-requests;samples.admissions=HeldLighting.captures()-admissions;
      samples.neutralPasses=WaterTerrain.neutralPasses()-neutrals;samples.waterPasses=WaterTerrain.secondPasses()-waters;samples.mismatches=WaterTerrain.mismatches()-mismatches;
      check(samples.mismatches==0,"native fixture geometry mismatched secondary lighting pass");
      if(phase.emission>0&&!phase.moving())check(samples.minimumStrength>=.99,"stationary source did not stay prepared after warmup: "+samples.minimumStrength);
      checkVertexArrays();
      check(GL11.glGetError()==GL11.GL_NO_ERROR,"OpenGL error in held-light native phase");
      samples.imageMean=HeldLight400Frames.centerMean(HeldLight400Frames.screenshot(mc,root.resolve(phase.file()+".png")));
      if(phase==Phase.BASELINE)baselineMean=samples.imageMean;
      if(phase==Phase.MAIN)check(samples.imageMean>baselineMean+.5,"actual rendered main-hand image did not brighten the center region: "+baselineMean+" -> "+samples.imageMean);
      if(phase==Phase.COLD_CAPTURE_BASE)check(coldInfluence()>0,"cold capture setup retained all held endpoints");
      if(phase==Phase.FINAL_DISABLED) {
        var updates=(WaterTerrainUpdates)mc.worldRenderer;long before=HeldLighting.requests();
        check(!updates.power$prepareHeldLight(null,scene.x,scene.floor+2,scene.z),"wrong world reported ready");
        check(!updates.power$prepareHeldLight(scene.world,scene.x+1_000_000,scene.floor+2,scene.z),"missing renderer grid reported ready");
        check(HeldLighting.requests()==before,"missing renderer coverage queued terrain");
      }
      advance=true;log("PASS HELD LIGHT 400 "+phase+" "+samples.report());write("running");
    }
    void checkVertexArrays() throws Exception {
      var pools=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
      for(var section:(net.minecraft.class_66[])field(mc.worldRenderer,"field_1809")) {
        if(section==null||section.field_229!=scene.world||HeldLightMath.sectionDistanceSquared(mc.player.x,mc.player.y-mc.player.field_1631+1,mc.player.z,section.field_231,section.field_232,section.field_233)>=HeldLightMath.RADIUS*HeldLightMath.RADIUS)continue;
        for(int pass=0;pass<2;pass++) {
          Object buffer=section.getClass().getMethod("smoothbeta_getBuffer",int.class).invoke(section,pass);
          Object pool=field(buffer,"pool");if(pool!=null)pools.add(pool);
        }
      }
      check(!pools.isEmpty(),"no native terrain pool in source influence");
      int previous=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
      var enabled=BufferUtils.createIntBuffer(4);
      try {
        for(Object pool:pools) {
          check((int)field(pool,"vertexBytes")==32,"native terrain pool stride changed");
          GL30.glBindVertexArray((int)field(pool,"vertexArrayId"));
          for(int attribute:new int[]{5,6}) {
            enabled.clear();GL20.glGetVertexAttrib(attribute,GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED,enabled);
            check(enabled.get(0)==0,"lighting attribute "+attribute+" remained enabled on terrain VAO");
          }
        }
      }finally{GL30.glBindVertexArray(previous);}
    }
    int coldInfluence() throws Exception {
      int cold=0;
      for(var section:(net.minecraft.class_66[])field(mc.worldRenderer,"field_1809")) {
        if(section==null||section.field_229!=scene.world||HeldLightMath.sectionDistanceSquared(mc.player.x,mc.player.y-mc.player.field_1631+1,mc.player.z,section.field_231,section.field_232,section.field_233)>=HeldLightMath.RADIUS*HeldLightMath.RADIUS)continue;
        if(!section.field_244[0]||!section.field_244[1]) {
          check(!section.field_249,"cold source-area mesh is still dirty");
          if(!((WaterTerrainSection)section).power$heldLightReady())cold++;
        }
      }
      return cold;
    }
    void isometric(String name,boolean cold) throws Exception {
      if(cold)check(coldInfluence()>0,"cold capture did not begin with unprepared native meshes");
      long before=HeldLighting.captures(),light=scene.lightHash(),time=System.nanoTime();
      Object grid=field(mc.worldRenderer,"field_1809");
      int[] origins=origins();
      Object config=Class.forName("local.luke.power.capture.Config").getField("config").get(null);
      Field scale=config.getClass().getField("isometricPhotoScale");Object oldScale=scale.get(config);
      Class<?> type=Class.forName("local.luke.power.capture.IsometricScreenshotRenderer");
      Path directory=root.resolve("isometric-"+name);Files.createDirectories(directory);inCapture=true;
      int imageWidth,imageHeight,captureScale,tilesX,tilesY;
      try {
        Object renderer=type.getConstructor(Minecraft.class,java.io.File.class).newInstance(mc,directory.toFile());
        int span=(int)field(renderer,"width")+(int)field(renderer,"length");
        int vertical=(int)field(renderer,"height")+span/2;
        check(span>0&&vertical>0&&mc.displayWidth>0&&mc.displayHeight>0,"invalid native capture dimensions");
        captureScale=Math.max(2,Math.max(mc.displayWidth/span+1,mc.displayHeight/vertical+1));
        check(captureScale<=32&&(long)span*vertical*captureScale*captureScale<=16_777_216L,
            "four-tile capture exceeds 16 megapixels; reduce the native window size first");
        imageWidth=span*captureScale;imageHeight=vertical*captureScale;
        tilesX=(imageWidth+mc.displayWidth-1)/mc.displayWidth;tilesY=(imageHeight+mc.displayHeight-1)/mc.displayHeight;
        check(tilesX>=2&&tilesY>=2,"capture must cross both native tile axes");
        scale.set(config,captureScale);type.getMethod("doRender").invoke(renderer);
      }finally{scale.set(config,oldScale);inCapture=false;}
      Path image;try(var files=Files.list(directory.resolve("screenshots"))){image=files.filter(p->p.toString().endsWith(".png")).findFirst().orElseThrow();}
      try(var input=new java.io.DataInputStream(Files.newInputStream(image))) {
        check(input.skipBytes(16)==16&&input.readInt()==imageWidth&&input.readInt()==imageHeight,"native PNG dimensions differ from tile plan");
      }
      check(HeldLighting.captures()-before<=27,"synchronous capture exceeded 27 admissions");
      check((float)staticField(HeldLighting.class,"strength")==1,"complete capture influence did not publish full strength");
      check((int)staticField(HeldLighting.class,"captureDepth")==0&&!RenderControls.rendering()&&!WaterTerrain.meshing(),"capture/render/mesh scope leaked");
      check(scene.lightHash()==light,"native integer light changed during isometric draw");
      check(field(mc.worldRenderer,"field_1809")==grid&&Arrays.equals(origins,origins()),"capture changed native renderer grid");
      captures.put(name,Map.of("image",image.toString(),"admissions",HeldLighting.captures()-before,"elapsedMs",(System.nanoTime()-time)/1e6,"strength",1,
          "scale",captureScale,"width",imageWidth,"height",imageHeight,"tilesX",tilesX,"tilesY",tilesY));
      log("PASS HELD LIGHT 400 isometric "+name+" "+captures.get(name));
    }
    int[] origins() throws Exception {
      var sections=(net.minecraft.class_66[])field(mc.worldRenderer,"field_1809");int[] result=new int[sections.length*3];
      for(int i=0;i<sections.length;i++){result[i*3]=sections[i].field_231;result[i*3+1]=sections[i].field_232;result[i*3+2]=sections[i].field_233;}return result;
    }
    void write(String status) throws Exception {
      var report=new LinkedHashMap<String,Object>();report.put("status",status);report.put("phase",phase==null?"setup":phase.file());report.put("failure",failure);
      report.put("renderer",GL11.glGetString(GL11.GL_RENDERER));report.put("gpuTimers",frames.gpuTimers());report.put("window",List.of(mc.displayWidth,mc.displayHeight));
      report.put("inputState",inputState());report.put("completedFixtureReloads",completedReloads);
      report.put("capturePairOnly",capturePair);
      report.put("scene",List.of(scene.x,scene.floor,scene.z));report.put("retainedBytes",WaterTerrain.retainedBytes());report.put("maxNeutralNanos",WaterTerrain.maxNeutralNanos());
      var phases=new LinkedHashMap<String,Object>();results.forEach((name,data)->phases.put(name,data.report()));report.put("phases",phases);report.put("isometric",captures);
      report.put("limits",List.of("CPU/GPU samples cover the native world-render callback, not presentation or all game work.","Tick CPU samples require beforeTick/afterTick callbacks; frame intervals include pacing.","Movement phases move the real player/camera on ticks and render natively, but do not synthesize keyboard input.","PNG comparison checks one center-region brightness mean; a human must review color/AO/water and isometric tile seams.","No far-coordinate teleport, real item consumption, death/world switch, custom upload faults or multi-GPU baseline is claimed. Stereo phases exercise their two native resource reloads.","Integer checks read 32 native block/sky samples; complete light arrays and map caches are not modified or fully hashed."));
      Files.writeString(root.resolve("report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
  }
}
