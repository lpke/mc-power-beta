package local.luke.power.validation;

import static local.luke.power.validation.Validation.check;
import static local.luke.power.validation.UiChecks.field;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.heldlight.*;
import local.luke.power.input.DetachedCamera;
import local.luke.power.offhand.*;
import local.luke.power.render.*;
import local.luke.power.rules.*;
import local.luke.power.visual.*;
import local.luke.power.world.WorldCycles;
import net.minecraft.class_56;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Owns air-only fixture blocks and restores the original runtime settings and inventory references. */
final class HeldLight400Scene {
  private record Position(int x,int y,int z) {}
  private record BlockState(int id,int metadata) {}
  final Minecraft mc;
  final World world;
  final Object player;
  final int x,z;
  final int floor=110;
  private final Map<Position,BlockState> blocks=new LinkedHashMap<>();
  private final HeldLightSettings held;
  private final OffhandSettings offhand;
  private final VisualSettings visual;
  private final RuleSet rules;
  private final ItemStack main,off;
  private final int slot;
  private final boolean flying,hud,third,bob,stereo;
  private final String mode;
  private final double px,py,pz,vx,vy,vz;
  private final float yaw,pitch;
  private final long daylight;
  private final FullBrightState brightness;
  private final Map<Field,Object> brightState=new LinkedHashMap<>();
  private final Object cameraPose;
  private final Set<Object> existingEntities=Collections.newSetFromMap(new IdentityHashMap<>());
  private boolean mined;

  HeldLight400Scene(Minecraft mc) throws Exception {
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&mc.player.world==mc.world,"load a disposable singleplayer world");
    check(mc.currentScreen==null&&mc.field_2807==mc.player&&!DetachedCamera.isActive(),"close menus and exit freecam first");
    check(!mc.player.dead&&mc.player.health>0&&mc.player.field_1595==null,"fixture requires a living, unmounted player");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable client before starting");
    this.mc=mc;world=mc.world;player=mc.player;x=Math.floorDiv((int)Math.floor(mc.player.x),16)*16;z=Math.floorDiv((int)Math.floor(mc.player.z),16)*16;
    check((int)field(mc.worldRenderer,"field_1810")<=65&&(int)field(mc.worldRenderer,"field_1812")<=65,"reduce render distance before bounded isometric capture");
    for(int cx=(x>>4)-2;cx<=(x>>4)+2;cx++) for(int cz=(z>>4)-2;cz<=(z>>4)+2;cz++) {
      var chunk=CullingTerrain.chunk(world,cx,cz);
      check(chunk!=null&&chunk.method_858(cx,cz)&&world.method_239(cx*16,64,cz*16),"fixture neighborhood is not fully loaded");
    }
    for(int bx=x-9;bx<=x+9;bx++) for(int bz=z-9;bz<=z+9;bz++) for(int by=floor;by<=floor+8;by++)
      check(world.getBlockId(bx,by,bz)==0,"fixture needs empty loaded air at "+bx+","+by+","+bz);
    held=HeldLightConfig.copy();offhand=OffhandConfig.copy();visual=VisualConfig.copy();rules=AccessRules.copy(mc);
    slot=mc.player.inventory.selectedSlot;main=mc.player.inventory.main[slot];off=Offhand.get(mc.player.inventory);
    flying=(boolean)mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player);
    mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    px=mc.player.x;py=mc.player.y;pz=mc.player.z;vx=mc.player.velocityX;vy=mc.player.velocityY;vz=mc.player.velocityZ;
    yaw=mc.player.yaw;pitch=mc.player.pitch;hud=mc.options.hideHud;third=mc.options.thirdPerson;bob=mc.options.bobView;stereo=mc.options.anaglyph3d;
    daylight=((WorldCycles)world.method_262()).power$daylightTime();
    brightness=(FullBrightState)staticField(RenderControls.class,"brightness");
    for(Field f:FullBrightState.class.getDeclaredFields()) { f.setAccessible(true);brightState.put(f,f.get(brightness)); }
    Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object pose=camera.getClass().getMethod("getCameraPosition").invoke(camera);
    cameraPose=pose.getClass().getMethod("clone").invoke(pose);
    existingEntities.addAll(world.field_198);
  }

  void build() throws Exception {
    AccessRules.set(mc,GameRule.ALLOW_CREATIVE_MODE,true);AccessRules.set(mc,GameRule.ADVANCE_TIME,false);
    AccessRules.set(mc,GameRule.ADVANCE_WEATHER,false);AccessRules.set(mc,GameRule.SPAWN_MONSTERS,false);AccessRules.set(mc,GameRule.SPAWN_ANIMALS,false);
    ((WorldCycles)world.method_262()).power$daylightTime(18000);
    CreativeVehicleChecks.mode(mc,"CREATIVE");mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
    var v=VisualConfig.copy();v.spectatorFullBright=false;v.underwaterDarkness=v.underwaterDarknessOutside=100;
    v.removeUnderwaterOverlay=true;VisualConfig.preview(v);
    brightness.update(null,false,false,false);RenderControls.tick(mc);HeldLighting.reset();
    var o=OffhandConfig.copy();o.enabled=true;OffhandConfig.preview(o);enabled(false);hands(0,0);
    mc.options.hideHud=true;mc.options.thirdPerson=false;mc.options.bobView=false;stereo(false);
    for(int bx=x-9;bx<=x+9;bx++) for(int bz=z-9;bz<=z+9;bz++) {
      put(bx,floor,bz,1,0);
      if(bx==x-9||bx==x+9||bz==z-9||bz==z+9) for(int by=floor+1;by<=floor+6;by++) put(bx,by,bz,1,0);
      if(bz>=z+5) put(bx,floor+8,bz,1,0);
    }
    for(int bx=x-4;bx<=x+4;bx++) for(int by=floor+1;by<=floor+3;by++) put(bx,by,z+2,35,(bx-x+4)%3==0?14:(bx-x+4)%3==1?5:11);
    for(int bx=x-4;bx<=x-2;bx++) put(bx,floor+1,z-1,44,0);
    for(int bx=x+5;bx<=x+8;bx++) for(int bz=z+1;bz<=z+6;bz++) for(int by=floor+1;by<=floor+5;by++)
      if(bx==x+5||bx==x+8||bz==z+1||bz==z+6||by==floor+5) put(bx,by,bz,20,0);
    for(int bx=x+6;bx<=x+7;bx++) for(int bz=z+2;bz<=z+5;bz++) for(int by=floor+1;by<=floor+4;by++) put(bx,by,bz,9,0);
    basePose();markDirty();
  }
  private void put(int bx,int by,int bz,int id,int metadata) {
    world.method_154(bx,by,bz,id,metadata);blocks.put(new Position(bx,by,bz),new BlockState(id,metadata));
  }
  void enabled(boolean value) { var s=HeldLightConfig.copy();s.enabled=value;HeldLightConfig.preview(s); }
  void offhandEnabled(boolean value) { var s=OffhandConfig.copy();s.enabled=value;OffhandConfig.preview(s); }
  void hands(int mainId,int offId) {
    mc.player.inventory.selectedSlot=slot;mc.player.inventory.main[slot]=mainId==0?null:new ItemStack(mainId,1,0);
    Offhand.set(mc.player.inventory,offId==0?null:new ItemStack(offId,1,0));mc.player.inventory.markDirty();
  }
  void basePose() { pose(x+.5,floor+1,z-2.5,0,10,true); }
  void creative() throws Exception {
    CreativeVehicleChecks.mode(mc,"CREATIVE");mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
  }
  void mining() throws Exception {CreativeVehicleChecks.mode(mc,"SURVIVAL");hands(278,50);mined=false;}
  void mine() throws Exception {
    check(!mined&&world.getBlockId(x,floor+2,z+2)==35,"owned mining probe is missing");
    ItemStack torch=Offhand.get(mc.player.inventory),tool=mc.player.inventory.main[slot];
    String before=local.luke.power.death.DeathItems.encode(torch);
    check(mc.interactionManager.method_1716(x,floor+2,z+2,1)&&world.getBlockId(x,floor+2,z+2)==0,"native successful block break did not remove the probe");
    mined=true;
    check(Offhand.get(mc.player.inventory)==torch&&before.equals(local.luke.power.death.DeathItems.encode(torch)),"native mining changed the physical offhand source");
    check(mc.player.inventory.main[slot]==tool&&tool.getDamage()>0,"native Survival break did not wear the main-hand pickaxe");
  }
  void miningEndpoints() throws Exception {
    var sections=(net.minecraft.class_66[])field(mc.worldRenderer,"field_1809");
    int width=(int)field(mc.worldRenderer,"field_1810"),height=(int)field(mc.worldRenderer,"field_1811"),depth=(int)field(mc.worldRenderer,"field_1812");
    for(int cx=Math.floorDiv(x-1,16);cx<=Math.floorDiv(x+1,16);cx++)
      for(int cy=Math.floorDiv(floor+1,16);cy<=Math.floorDiv(floor+3,16);cy++) {
        int cz=Math.floorDiv(z+2,16),index=(Math.floorMod(cz,depth)*height+cy)*width+Math.floorMod(cx,width);
        var section=sections[index];
        check(section!=null&&section.field_229==world&&section.field_231==cx*16&&section.field_232==cy*16&&section.field_233==cz*16,"mining affected a missing render section");
        check(((WaterTerrainSection)section).power$heldLightReady()||section.field_244[0]&&section.field_244[1],"native break discarded an established held-light endpoint");
      }
  }
  void waterPose() { pose(x+6.5,floor+1.1,z+3.5,180,5,true); }
  void pose(double bx,double feet,double bz,float yaw,float pitch,boolean settle) {
    double previousX=mc.player.x,previousY=mc.player.y,previousZ=mc.player.z;
    mc.player.method_1341(bx,feet,bz,yaw,pitch);
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
    if(!settle) { mc.player.prevX=mc.player.field_1637=previousX;mc.player.prevY=mc.player.field_1638=previousY;mc.player.prevZ=mc.player.field_1639=previousZ; }
    mc.player.prevYaw=yaw;mc.player.prevPitch=pitch;
    mc.field_2807=mc.player;
  }
  void bright(boolean value) {
    if(RenderControls.fullBright()!=value) {brightness.toggle();RenderControls.tick(mc);}
    check(RenderControls.fullBright()==value,"full-bright runtime transition failed");
  }
  void water(int darkness) {var s=VisualConfig.copy();s.underwaterDarkness=s.underwaterDarknessOutside=darkness;VisualConfig.preview(s);}
  void stereo(boolean value) {
    if(mc.options.anaglyph3d==value)return;
    mc.options.anaglyph3d=value;mc.textureManager.method_1096();mc.worldRenderer.method_1537();
  }
  void markDirty() {mc.worldRenderer.method_1150(x-9,floor,z-9,x+9,floor+8,z+9);}
  long lightHash() {
    long result=1;
    for(int bx=x-3;bx<=x+3;bx+=2) for(int bz=z-3;bz<=z+3;bz+=2) {
      result=result*31+world.method_164(class_56.BLOCK,bx,floor+1,bz);
      result=result*31+world.method_164(class_56.SKY,bx,floor+1,bz);
    }
    return result;
  }
  void restore() throws Exception {
    var failures=new ArrayList<String>();
    HeldLighting.reset();HeldLightConfig.preview(held);OffhandConfig.preview(offhand);VisualConfig.preview(visual);
    if(mc.world!=world||mc.player!=player) throw new IllegalStateException("world or player changed; global previews restored, original scene and player require clone cleanup");
    FreecamPreviewChecks.active(false);
    if(mined&&world.getBlockId(x,floor+2,z+2)==0)world.method_154(x,floor+2,z+2,35,5);
    for(Object entity:new ArrayList<>(world.field_198))if(entity instanceof net.minecraft.class_142 drop&&!existingEntities.contains(entity))drop.markDead();
    // Remove owned water before its glass enclosure. Never overwrite unexpected replacement blocks.
    for(boolean water:new boolean[]{true,false}) for(var entry:blocks.entrySet()) {
      var pos=entry.getKey();var block=entry.getValue();if((block.id==9)!=water)continue;
      if(!world.method_239(pos.x,pos.y,pos.z)) {failures.add("unloaded "+pos);continue;}
      int actual=world.getBlockId(pos.x,pos.y,pos.z),metadata=world.method_1778(pos.x,pos.y,pos.z);
      if(actual!=block.id||metadata!=block.metadata) {failures.add("changed "+pos+" expected="+block+" actual="+actual+":"+metadata);continue;}
      world.method_154(pos.x,pos.y,pos.z,0,0);
    }
    mc.player.inventory.selectedSlot=slot;mc.player.inventory.main[slot]=main;Offhand.set(mc.player.inventory,off);
    ((WorldCycles)world.method_262()).power$daylightTime(daylight);
    CreativeVehicleChecks.mode(mc,mode);mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,flying);
    AccessRules.restore(mc,rules);
    mc.player.method_1341(px,py-mc.player.field_1631,pz,yaw,pitch);mc.player.prevX=mc.player.field_1637=px;mc.player.prevY=mc.player.field_1638=py;mc.player.prevZ=mc.player.field_1639=pz;
    mc.player.velocityX=vx;mc.player.velocityY=vy;mc.player.velocityZ=vz;
    for(var entry:brightState.entrySet())entry.getKey().set(brightness,entry.getValue());RenderControls.tick(mc);
    mc.options.hideHud=hud;mc.options.thirdPerson=third;mc.options.bobView=bob;stereo(stereo);
    Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    camera.getClass().getMethod("setCameraPositionAndRotation",double.class,double.class,double.class,float.class,float.class,float.class)
        .invoke(camera,(double)field(cameraPose,"x"),(double)field(cameraPose,"y"),(double)field(cameraPose,"z"),(float)field(cameraPose,"pitch"),(float)field(cameraPose,"yaw"),(float)field(cameraPose,"roll"));
    check(failures.isEmpty(),"owned-scene restoration conflicts: "+failures.stream().limit(6).toList());
  }
  private static Object staticField(Class<?> type,String name) throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
}
