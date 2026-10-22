package local.luke.power.gameplay;

import local.luke.power.input.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.class_385;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Local player physics only. Native travel still owns collision and limb animation. */
public final class ModernWater {
  private ModernWater() {}
  private static Minecraft client() { return (Minecraft)FabricLoader.getInstance().getGameInstance(); }
  public static boolean eligible(Entity entity) {
    return GameplayConfig.current().modernWaterMovement && localMovement(entity);
  }

  /** Common physical-player boundary; dry crawling does not require water settings or sprint access. */
  static boolean localMovement(Entity entity) {
    if (!(entity instanceof ClientPlayerEntity player)
        || player.world == null || player.world.isRemote || player.dead || player.health <= 0
        || player.field_1595 != null || player.method_943() || DetachedCamera.isActive()
        || !(player instanceof PlayerMode mode) || mode.power$spectator() || mode.power$flying()) return false;
    Minecraft mc=client();
    return mc!=null && mc.player==player && mc.world==player.world;
  }
  public static boolean push(ClientPlayerEntity player) {
    SwimState swim=((SwimPlayer)player).power$swim();
    swim.waterDepth=0;
    var box=player.boundingBox;var world=player.world;
    if (!Double.isFinite(box.minX)||!Double.isFinite(box.minY)||!Double.isFinite(box.minZ)
        ||!Double.isFinite(box.maxX)||!Double.isFinite(box.maxY)||!Double.isFinite(box.maxZ)) return false;
    double bottom=box.minY+.001;
    int x0=(int)Math.floor(box.minX+.001),x1=(int)Math.ceil(box.maxX-.001);
    int y0=Math.max(0,(int)Math.floor(bottom)),y1=Math.min(128,(int)Math.ceil(box.maxY-.001));
    int z0=(int)Math.floor(box.minZ+.001),z1=(int)Math.ceil(box.maxZ-.001);
    if(x1<=x0||y1<=y0||z1<=z0||x1-x0>4||y1-y0>4||z1-z0>4
        ||!world.method_155(x0-1,Math.max(0,y0-1),z0-1,x1+1,Math.min(127,y1+1),z1+1)) return false;
    double sx=0,sy=0,sz=0,depth=0;int cells=0;boolean wet=false;
    for(int x=x0;x<x1;x++)for(int y=y0;y<y1;y++)for(int z=z0;z<z1;z++){
      Block block=Block.BLOCKS[world.getBlockId(x,y,z)];
      if(block==null||block.field_1900!=Material.WATER)continue;
      double surface=world.method_1779(x,y+1,z)==Material.WATER?y+1:y+1-class_385.method_1218(world.method_1778(x,y,z));
      if(surface<bottom)continue;
      wet=true;depth=Math.max(depth,surface-bottom);
      Vec3d flow=Vec3d.createCached(0,0,0);block.method_1572(world,x,y,z,player,flow);
      double scale=depth<.4?depth:1;
      sx+=flow.x*scale;sy+=flow.y*scale;sz+=flow.z*scale;cells++;
    }
    if(cells>0){
      double x=sx/cells*.014,y=sy/cells*.014,z=sz/cells*.014,length=Math.sqrt(x*x+y*y+z*z);
      if(length>0 && Math.abs(player.velocityX)<.003 && Math.abs(player.velocityZ)<.003 && length<.0045){
        double scale=.0045/length;x*=scale;y*=scale;z*=scale;
      }
      player.velocityX+=x;player.velocityY+=y;player.velocityZ+=z;
    }
    swim.waterDepth=depth;
    return wet;
  }

  /** LivingEntity.aiStep settles motion before refreshing controls and applying Jump. */
  public static void livingStep(Entity entity) {
    if (!(entity instanceof ClientPlayerEntity player) || !(player instanceof SwimPlayer owner))return;
    SwimState state=owner.power$swim();
    if(state.waterJumpTicks>0)state.waterJumpTicks--;
    if(player.field_161==null || !player.field_161.field_2535)state.waterJumpTicks=0;
    if(!eligible(player) || !player.method_1334())return;
    player.velocityX=SwimState.settled(player.velocityX);
    player.velocityY=SwimState.settled(player.velocityY);
    player.velocityZ=SwimState.settled(player.velocityZ);
  }

  public static boolean shallowJump(Entity entity) {
    if(!eligible(entity) || !entity.field_1623 || !entity.method_1334())return false;
    double depth=((SwimPlayer)entity).power$swim().waterDepth;
    return depth>0 && depth<=.4;
  }
}
