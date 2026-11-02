package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.bow.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.inventory.HandUse;
import local.luke.power.offhand.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Runs only in a focused, muted disposable singleplayer clone. Restores settings and native items. */
public final class ModernBow470Checks {
  private static Visual visual;
  private record Cue(String id,float volume,float pitch,String category) {}
  private static List<Cue> cues;
  public static void captureSound(String id,float volume,float pitch){
    if(cues==null||!id.startsWith("power_beta:entity.arrow.")&&!id.equals("power_beta:entity.item.break"))return;
    try{
      var method=local.luke.power.audio.SoundCategoryScope.class.getDeclaredMethod("current");method.setAccessible(true);
      cues.add(new Cue(id,volume,pitch,(String)method.invoke(null)));
    }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
  }
  private static void cue(String suffix,String category,float volume){
    List<Cue> matching=cues.stream().filter(cue->cue.id.equals("power_beta:"+suffix)).toList();
    check(matching.size()==1,"bow cue count "+suffix+" "+matching);
    Cue sound=matching.get(0);check(category.equals(sound.category)&&Math.abs(sound.volume-volume)<1e-6,"bow cue source category/gain mismatch "+sound);
  }
  private ModernBow470Checks() {}
  public static void run(Minecraft mc,String phase)throws Exception {
    if(phase.equals("stop")){finishVisual();return;}
    if(phase.equals("status")){log("MODERN BOW470 CAPTURE "+(visual==null?"idle":visual.phase+" "+visual.report));return;}
    if(phase.equals("capture")){
      check(visual==null,"stop the existing bow capture first");
      check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&mc.currentScreen==null&&Display.isActive()
          &&mc.options.soundVolume==0&&mc.options.musicVolume==0&&!Mouse.isButtonDown(1),"focus a muted disposable singleplayer client and release Use");
      visual=new Visual(mc);try{visual.begin(0);}catch(Exception|Error failure){finishVisual();throw failure;}return;
    }
    check(visual==null,"stop the bow capture first");
    check(phase.isEmpty() || phase.equals("native"),"modern-bow-470 native|capture|status|stop");run(mc);
  }
  public static void run(Minecraft mc)throws Exception {
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&mc.currentScreen==null,"load a disposable singleplayer world");
    check(Display.isActive()&&mc.options.soundVolume==0&&mc.options.musicVolume==0&&!Mouse.isButtonDown(1),"focus the muted clone and release Use");
    GameplaySettings old=GameplayConfig.copy();Field provider=UseInput.class.getDeclaredField("held");provider.setAccessible(true);
    BooleanSupplier previous=(BooleanSupplier)provider.get(null);failures=0;cues=new ArrayList<>();
    try(var s=new OffhandActions400State(mc)){
      UseInput.register(()->true);
      for(boolean off:new boolean[]{false,true})for(int charge:new int[]{0,2,3,10,20,40}){
        final boolean hand=off;final int duration=charge;
        test("MODERN BOW470 "+(off?"OFF":"MAIN")+" draw "+charge+" ticks",()->{
          cues.clear();s.reset();mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;mode(true);ItemStack bow=OffhandActions400State.tagged(261,1,0,"modern-bow"),ammo=OffhandActions400State.tagged(262,4,0,"modern-ammo");
          s.hands(hand?ammo:bow,hand?bow:ammo);String[] before=s.items();s.use();
          check(ModernBow.using(mc.player,bow),"native Use did not begin drawing");check(Arrays.equals(before,s.items()),"starting draw consumed item data");check(cues.isEmpty(),"drawing emitted a release sound");
          for(int i=ModernBow.ticks(mc.player,bow);i<duration;i++)ModernBow.tick(mc);
          Set<Entity> entities=entities(mc);ModernBow.release(mc);s.nativeState.finish();capture(s,entities);
          boolean fired=duration>=3;check(s.arrows()==(fired?1:0),"release projectile count mismatch");
          check(Offhand.stack(mc.player,hand?HandUse.Hand.MAIN:HandUse.Hand.OFF).count==(fired?3:4)&&Offhand.stack(mc.player,hand?HandUse.Hand.OFF:HandUse.Hand.MAIN).getDamage()==(fired?1:0),"release ammo/durability mismatch");
          if(!fired)check(cues.isEmpty(),"undercharge emitted a bow sound");
          if(fired){cue("entity.arrow.shoot","players",1);class_410 arrow=arrow(s);check(((ModernArrowData)(Object)arrow).power$isModernArrow(),"spawn was not marked modern");
            double speed=Math.sqrt(arrow.velocityX*arrow.velocityX+arrow.velocityY*arrow.velocityY+arrow.velocityZ*arrow.velocityZ);
            double expected=BowPhysics.power(duration)*3;check(Math.abs(speed-expected)<.12,"draw velocity outside official triangular uncertainty");
            NbtCompound tag=new NbtCompound();arrow.writeNbt(tag);check(tag.getBoolean("PowerBetaCritical")== (duration>=20),"full-draw critical mismatch");
            class_410 restored=new class_410(mc.world);restored.readtNbt(tag);check(((ModernArrowData)(Object)restored).power$isModernArrow(),"arrow mode did not survive native NBT");
          }
          ModernBow.release(mc);check(s.arrows()==(fired?1:0),"duplicate release fired twice");s.unchanged(before,0,45);
        });
      }
      test("MODERN BOW470 disabled native instant fire and undamageable bow",()->{
        s.reset();mode(false);ItemStack bow=new ItemStack(261,1,87);s.hands(bow,new ItemStack(262,4,0));s.use();
        check(s.arrows()==1&&bow.getDamage()==87&&bow.getMaxDamage()==0,"disabled bow changed native immediate/durability behavior");
        check(ModernBow.hand(mc.player)==null,"disabled bow retained modern work");
      });
      test("MODERN BOW470 release identity and toggle cancellation",()->{
        s.reset();mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;mode(true);ItemStack bow=new ItemStack(261,1,0),ammo=new ItemStack(262,4,0);s.hands(bow,ammo);s.use();
        for(int i=ModernBow.ticks(mc.player,bow);i<20;i++)ModernBow.tick(mc);mc.player.inventory.main[0]=new ItemStack(261,1,0);ModernBow.tick(mc);
        check(ModernBow.hand(mc.player)==null&&ammo.count==4&&s.arrows()==0,"replaced stack released an arrow");
        s.hands(bow,ammo);s.use();mode(false);ModernBow.tick(mc);check(ModernBow.hand(mc.player)==null&&ammo.count==4,"disabling did not cancel draw");
      });
      test("MODERN BOW470 exact 384-use break and ammo preservation",()->{
        cues.clear();s.reset();mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;mode(true);ItemStack bow=new ItemStack(261,1,383),ammo=new ItemStack(262,4,0);s.hands(bow,ammo);s.use();
        for(int i=ModernBow.ticks(mc.player,bow);i<20;i++)ModernBow.tick(mc);Set<Entity> before=entities(mc);ModernBow.release(mc);s.nativeState.finish();capture(s,before);
        check(mc.player.inventory.main[0]==null&&Offhand.get(mc.player.inventory).count==3&&s.arrows()==1,"384th shot did not break the used bow once");
        cue("entity.item.break","players",.8f);cue("entity.arrow.shoot","players",1);
      });
      test("MODERN BOW470 25-tick flight matches official closed form",()->{
        s.reset();mode(true);class_410 arrow=projectile(s,false);arrow.method_1340(s.nativeState.x+.5,140,s.nativeState.z+.5);arrow.velocityX=arrow.velocityZ=0;arrow.velocityY=3;
        double start=arrow.y,drag=BowPhysics.AIR_DRAG;int n=25;
        for(int i=1;i<=n;i++){
          arrow.tick();double geometric=(1-Math.pow(drag,i))/(1-drag);
          double distance=3*geometric-BowPhysics.GRAVITY/(1-drag)*(i-geometric);
          check(Math.abs(arrow.y-start-distance)<1e-9,"native flight differs from official position at tick "+i);
          check(Math.abs(arrow.velocityY-(3*Math.pow(drag,i)-BowPhysics.GRAVITY*geometric))<1e-9,"native flight velocity mismatch at tick "+i);
        }
      });
      test("MODERN BOW470 water movement and inertia ordering",()->{
        s.reset();mode(true);int x=s.nativeState.x,y=s.nativeState.y,z=s.nativeState.z;
        mc.world.method_200(x,y,z,9);class_410 arrow=projectile(s,false);arrow.method_1340(x+.5,y+.25,z+.2);arrow.velocityZ=.5;
        arrow.tick();check(Math.abs(arrow.z-(z+.7))<1e-9,"water shortened the captured current-tick ray");
        check(Math.abs(arrow.velocityZ-.5*BowPhysics.WATER_DRAG)<1e-9&&Math.abs(arrow.velocityY+BowPhysics.GRAVITY)<1e-9,"water inertia/gravity mismatch");
      });
      test("MODERN BOW470 block collision and containment",()->{
        s.reset();mode(true);int x=s.nativeState.x,y=s.nativeState.y,z=s.nativeState.z;mc.world.method_200(x,y,z,1);
        class_410 arrow=projectile(s,false);arrow.method_1340(x+.5,y+.5,z-.5);arrow.velocityZ=2;arrow.tick();
        NbtCompound tag=new NbtCompound();arrow.writeNbt(tag);check(tag.getByte("inGround")==1&&arrow.z<z,"flight failed to stop at native block face");
        check(arrow.velocityX==0&&arrow.velocityY==0&&arrow.velocityZ==0,"embedded arrow retained flight velocity");
        // Stay inside the fixture's owned air corridor and cross one face, avoiding
        // both a native corner-ray tie and the nearby player's expanded hit box.
        class_410 diagonal=projectile(s,false);diagonal.method_1340(x+2.5,y+.5,z+.2);diagonal.velocityX=-2;diagonal.velocityZ=.4;diagonal.tick();
        tag=new NbtCompound();diagonal.writeNbt(tag);
        double expectedX=x+1+(double).05f,expectedZ=z+.5-(double).05f;
        check(tag.getByte("inGround")==1&&Math.abs(diagonal.x-expectedX)<1e-6&&Math.abs(diagonal.z-expectedZ)<1e-6,
            "diagonal impact mismatch actual="+diagonal.x+","+diagonal.y+","+diagonal.z+" expected="+expectedX+","+(y+.5)+","+expectedZ+" embedded="+tag.getByte("inGround"));
        class_410 embedded=projectile(s,false);embedded.method_1340(x+.5,y+.5,z+.5);embedded.velocityZ=3;embedded.tick();
        tag=new NbtCompound();embedded.writeNbt(tag);check(tag.getByte("inGround")==1&&embedded.z==z+.5,"arrow escaped initial solid containment");
      });
      test("MODERN BOW470 Creative release retains bow and creates no pickup ammo",()->{
        s.reset();mode(true);CreativeVehicleChecks.mode(mc,"CREATIVE");ItemStack bow=new ItemStack(261,1,383);s.hands(bow,null);String[] before=s.items();s.use();
        for(int t=ModernBow.ticks(mc.player,bow);t<20;t++)ModernBow.tick(mc);Set<Entity> prior=entities(mc);ModernBow.release(mc);capture(s,prior);
        check(s.arrows()==1&&Arrays.equals(before,s.items())&&!arrow(s).field_1574,"Creative release consumed/created inventory ammunition");
      });
      test("MODERN BOW470 pending release rejects foreign inventory mutation",()->{
        s.reset();mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;mode(true);ItemStack bow=new ItemStack(261,1,0),ammo=new ItemStack(262,4,0);s.hands(bow,ammo);s.use();
        for(int t=ModernBow.ticks(mc.player,bow);t<20;t++)ModernBow.tick(mc);Set<Entity> prior=entities(mc);ModernBow.release(mc);
        mc.player.inventory.main[35]=new ItemStack(280,2,0);s.nativeState.finish();capture(s,prior);
        check(s.arrows()==0&&bow.getDamage()==0&&ammo.count==4&&mc.player.inventory.main[35].count==2,"stale durable bow plan published or overwrote foreign data");
      });
      test("MODERN BOW470 native entity collision speed damage",()->{
        cues.clear();s.reset();mode(true);class_410 arrow=projectile(s,false);double x=s.nativeState.x+2.5,y=s.nativeState.y+.25,z=s.nativeState.z-.5;
        arrow.method_1340(x,y,z);arrow.velocityX=0;arrow.velocityY=0;arrow.velocityZ=3;
        Probe target=new Probe(mc.world);target.method_1340(x,y,z+1.5);s.owned.add(target);check(mc.world.method_210(target),"native damage probe spawn rejected");arrow.tick();
        check(arrow.dead&&target.received==6,"native damage mismatch dead="+arrow.dead+" damage="+target.received+" arrow="+arrow.x+","+arrow.y+","+arrow.z+" target="+target.x+","+target.y+","+target.z);
        cue("entity.arrow.hit","passive",1);
      });
      test("MODERN BOW470 critical collision damage bounds",()->{
        s.reset();mode(true);for(int i=0;i<24;i++){
          class_410 arrow=projectile(s,true);double x=s.nativeState.x+2.5,y=s.nativeState.y+.25,z=s.nativeState.z-.5;arrow.method_1340(x,y,z);arrow.velocityZ=3;
          Probe target=new Probe(mc.world);target.method_1340(x,y,z+1.5);s.owned.add(target);check(mc.world.method_210(target),"native critical probe spawn rejected");arrow.tick();
          check(arrow.dead&&target.received>=6&&target.received<=10,"native critical mismatch sample="+i+" dead="+arrow.dead+" damage="+target.received+" y="+y);target.markDead();
        }
      });
    }finally{cues=null;ModernBow.cancel();GameplayConfig.preview(old);UseInput.register(previous);}
    log("MODERN BOW470 checks complete failures="+failures);
  }
  public static void afterFrame(Minecraft mc,float delta){
    if(visual==null)return;
    try{visual.frame(mc);}catch(Exception|Error failure){
      try{log("FAIL MODERN BOW470 CAPTURE "+failure);}catch(Exception ignored){}
      try{finishVisual();}catch(Exception cleanup){failure.addSuppressed(cleanup);}failure.printStackTrace();
    }
  }
  private static void finishVisual()throws Exception{
    Visual done=visual;visual=null;if(done==null)return;ModernBow.cancel();
    try{done.state.close();}finally{GameplayConfig.preview(done.settings);UseInput.register(done.held);}
    log("MODERN BOW470 CAPTURE COMPLETE "+done.report);
  }
  private static final class Visual {
    final OffhandActions400State state;final GameplaySettings settings;final BooleanSupplier held;
    final OffhandActions400Frames frames=new OffhandActions400Frames();final java.nio.file.Path report;
    int phase,count;long started;
    Visual(Minecraft mc)throws Exception{
      settings=GameplayConfig.copy();Field field=UseInput.class.getDeclaredField("held");field.setAccessible(true);held=(BooleanSupplier)field.get(null);
      state=new OffhandActions400State(mc);report=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("power-beta-data/reports/modern-bow-470").resolve(UUID.randomUUID().toString());
      java.nio.file.Files.createDirectories(report);UseInput.register(()->true);
    }
    void begin(int next)throws Exception{
      phase=next;count=0;started=System.nanoTime();state.reset();mode(true);state.visual(phase>=2,phase>=2);
      boolean off=phase%2==1;ItemStack bow=OffhandActions400State.tagged(261,1,12,"visual-bow"),ammo=new ItemStack(262,16,0);
      state.hands(off?ammo:bow,off?bow:ammo);
      for(int i=0;i<4;i++)state.mc.player.inventory.armor[i]=new ItemStack(313-i,1,0);
      state.use();for(int i=ModernBow.ticks(state.mc.player,bow);i<20;i++)ModernBow.tick(state.mc);frames.prepare(state.mc);
      log("MODERN BOW470 CAPTURE START "+phase+" "+report);
    }
    void frame(Minecraft mc)throws Exception{
      check(mc==state.mc&&mc.world==mc.player.world&&mc.currentScreen==null&&Display.isActive(),"bow capture lost its gameplay context");
      check(System.nanoTime()-started<30_000_000_000L,"bow capture timed out");
      if(++count<30)return;
      String name=(phase%2==0?"main":"off")+(phase<2?"-first":"-third");
      OffhandActions400Frames.write(frames.read(mc),report.resolve(name+".png"));
      if(phase==3)finishVisual();else begin(phase+1);
    }
  }
  private static void mode(boolean enabled){var settings=GameplayConfig.copy();settings.modernBowHandling=enabled;GameplayConfig.preview(settings);ModernBow.cancel();}
  private static Set<Entity> entities(Minecraft mc){Set<Entity> out=Collections.newSetFromMap(new IdentityHashMap<>());for(Object e:mc.world.field_198)out.add((Entity)e);return out;}
  private static void capture(OffhandActions400State s,Set<Entity> before){for(Object e:s.mc.world.field_198)if(!before.contains(e))s.owned.add((Entity)e);}
  private static class_410 arrow(OffhandActions400State s){return (class_410)s.owned.stream().filter(e->e instanceof class_410&&!e.dead).findFirst().orElseThrow();}
  private static class_410 projectile(OffhandActions400State s,boolean critical){class_410 arrow=new class_410(s.mc.world);arrow.field_1576=s.mc.player;((ModernArrowData)(Object)arrow).power$modernArrow(critical,false);s.owned.add(arrow);return arrow;}
  private static final class Probe extends Entity {
    int received;
    Probe(World world){super(world);setBoundingBoxSpacing(.6f,1.8f);}
    protected void method_1310(){} public void writeNbt(NbtCompound nbt){}public void readtNbt(NbtCompound nbt){}
    public boolean method_1356(){return !dead;}
    public boolean damage(Entity source,int amount){received=amount;return true;}
  }
}
