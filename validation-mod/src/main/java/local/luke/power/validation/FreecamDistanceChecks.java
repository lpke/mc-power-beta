package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.input.DetachedCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Observe the real world renderer's distance gates without drawing test models. */
public final class FreecamDistanceChecks {
  public static boolean recording;
  public static final Set<Entity> entities = new HashSet<>();
  public static final Set<BlockEntity> blocks = new HashSet<>();

  static void run(Minecraft mc) throws Exception {
    failures=0; DeathChecks.alive(mc); mc.setScreen(null); mc.field_2807=mc.player;
    var visualBefore = local.luke.power.visual.VisualConfig.copy();
    Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object settings=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    var enabled=settings.getClass().getField("enabled"); Object before=enabled.get(settings);
    var active=camera.getClass().getMethod("setActive",boolean.class);
    var position=camera.getClass().getMethod("setCameraPosition",double.class,double.class,double.class);
    double x=mc.player.x,y=mc.player.boundingBox.minY,z=mc.player.z;
    float yaw=mc.player.yaw,pitch=mc.player.pitch;
    var list=(List<BlockEntity>)UiChecks.field(mc.worldRenderer,"field_1795");
    var original=new ArrayList<>(list);
    var near=new net.minecraft.class_142(mc.world,.5,101.62,3.5,new net.minecraft.item.ItemStack(1,1,0));
    var far=new net.minecraft.class_142(mc.world,96.5,101.62,3.5,new net.minecraft.item.ItemStack(1,1,0));
    var nearSign=new net.minecraft.block.entity.SignBlockEntity();
    var farSign=new net.minecraft.block.entity.SignBlockEntity();
    try {
      var visible = local.luke.power.visual.VisualConfig.copy(); visible.hideEntities = false;
      local.luke.power.visual.VisualConfig.preview(visible);
      enabled.set(settings,true);active.invoke(camera,false);
      for(int bx:new int[]{0,96}) mc.world.method_154(bx,99,3,1,0);
      mc.world.method_210(near); mc.world.method_210(far);
      nearSign.world=farSign.world=mc.world;
      nearSign.x=0;farSign.x=96; nearSign.y=farSign.y=101;nearSign.z=farSign.z=3;
      list.clear();list.add(nearSign);list.add(farSign);
      mc.player.method_1341(.5,100,.5,0,0);
      test("dropped-item and block-entity distance follows freecam and restores on exit",()->{
        for(int round=0;round<3;round++) {
          active.invoke(camera,false); render(mc);
          check(entities.contains(near)&&!entities.contains(far),"normal item distance changed");
          check(blocks.contains(nearSign)&&!blocks.contains(farSign),"normal block-entity distance changed");
          position.invoke(camera,96.5,101.62,.5);active.invoke(camera,true);render(mc);
          check(DetachedCamera.x()==96.5,"camera position bridge stale");
          check(!entities.contains(near)&&entities.contains(far),"items still use player position");
          check(!blocks.contains(nearSign)&&blocks.contains(farSign),"block entities still use player position");
          camera.getClass().getField("allowPlayerMovement").setBoolean(camera,true);render(mc);
          check(entities.contains(far)&&blocks.contains(farSign),"movement ownership changed distance origin");
          position.invoke(camera,48.5,101.62,.5);render(mc);
          check(!entities.contains(near)&&!entities.contains(far),"moving camera kept old item distance");
          active.invoke(camera,false);render(mc);
          check(entities.contains(near)&&!entities.contains(far),"exit did not restore item distance");
          check(blocks.contains(nearSign)&&!blocks.contains(farSign),"exit did not restore block distance");
        }
        check(mc.player.x==.5&&mc.player.z==.5,"rendering moved the player");
        check(near.field_564.count==1&&far.field_564.count==1&&!near.dead&&!far.dead,"rendering changed dropped items");
      });
      test("disabling freecam immediately restores player distance",()->{
        position.invoke(camera,96.5,101.62,.5);active.invoke(camera,true);
        enabled.set(settings,false);render(mc);
        check(!DetachedCamera.isActive()&&entities.contains(near)&&!entities.contains(far),"disabled camera retained distance override");
      });
    } finally {
      local.luke.power.visual.VisualConfig.preview(visualBefore);
      recording=false;active.invoke(camera,false);enabled.set(settings,before);
      near.markDead();far.markDead();list.clear();list.addAll(original);
      mc.player.method_1341(x,y,z,yaw,pitch);entities.clear();blocks.clear();
    }
    log("FREECAM DISTANCE FAILURES "+failures);
  }

  private static void render(Minecraft mc) throws Exception {
    UiChecks.field(mc.worldRenderer,"field_1783",0);
    entities.clear();blocks.clear();recording=true;
    try {
      mc.worldRenderer.method_1544(Vec3d.createCached(mc.player.x,mc.player.y,mc.player.z),new net.minecraft.class_68(){
        public boolean method_2007(Box box){return true;}
        public void method_2006(double x,double y,double z){}
      },1F);
    } finally {recording=false;}
  }
}
