package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.bow.*;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the native entity, renderer, pickup and save identity, with opted-in arrow simulation. */
@Mixin(class_410.class)
public abstract class ModernArrowMixin extends Entity implements ModernArrowData {
  @Shadow private int field_1577, field_1578, field_1579, field_1580, field_1581, field_1583, field_1584;
  @Shadow private boolean field_1582;
  @Shadow public int field_1575;
  @Shadow public LivingEntity field_1576;
  @Unique private boolean power$modern, power$critical, power$creative, power$leftOwner;
  protected ModernArrowMixin(World world) { super(world); }
  public void power$modernArrow(boolean critical, boolean creative) { power$modern=true;power$critical=critical;power$creative=creative; }
  public boolean power$isModernArrow() { return power$modern; }
  @WrapMethod(method="tick")
  private void power$tick(Operation<Void> original) {
    if (!power$modern || !ModernBow.enabled() || world.isRemote) { original.call(); return; }
    super.tick();
    // Beta collision boxes and native block callbacks remain the source of terrain geometry.
    if (!field_1582) {
      int bx=(int)Math.floor(x),by=(int)Math.floor(y),bz=(int)Math.floor(z),id=world.getBlockId(bx,by,bz);
      if(id>0&&Block.BLOCKS[id]!=null){
        Block block=Block.BLOCKS[id];block.method_1616(world,bx,by,bz);
        Box shape=block.method_1624(world,bx,by,bz);
        if(shape!=null&&shape.method_88(Vec3d.createCached(x,y,z))){
          field_1577=bx;field_1578=by;field_1579=bz;field_1580=id;field_1581=world.method_1778(bx,by,bz);
          field_1582=true;velocityX=velocityY=velocityZ=0;
        }
      }
    }
    if (field_1575>0)field_1575--;
    if (field_1582) {
      if(world.getBlockId(field_1577,field_1578,field_1579)!=field_1580
          ||world.method_1778(field_1577,field_1578,field_1579)!=field_1581) {
        field_1582=false;velocityX*=random.nextFloat()*.2f;velocityY*=random.nextFloat()*.2f;velocityZ*=random.nextFloat()*.2f;field_1583=field_1584=0;
      } else if(++field_1583>=1200)markDead();
      return;
    }
    field_1584++;
    if(!power$leftOwner&&!power$nearOwner())power$leftOwner=true;
    boolean water=method_1334();
    // Official 26.3 keeps the movement captured before water inertia for this tick's ray.
    double moveX=velocityX,moveY=velocityY,moveZ=velocityZ;
    if(water){velocityX*=BowPhysics.WATER_DRAG;velocityY*=BowPhysics.WATER_DRAG;velocityZ*=BowPhysics.WATER_DRAG;}
    if(power$critical)BowCriticalParticle.trail(world,x,y,z,moveX,moveY,moveZ);
    Vec3d start=Vec3d.createCached(x,y,z),end=Vec3d.createCached(x+moveX,y+moveY,z+moveZ);
    class_27 hit=world.method_162(start,end,false,true);
    // Beta's block ray mutates its start vector. Modern Vec3 is immutable, so rebuild both endpoints before testing entities.
    start=Vec3d.createCached(x,y,z);
    end=hit==null?Vec3d.createCached(x+moveX,y+moveY,z+moveZ)
        :Vec3d.createCached(hit.field_1988.x,hit.field_1988.y,hit.field_1988.z);
    Entity target=null;Vec3d contact=null;double nearest=Double.POSITIVE_INFINITY;
    for(Object value:world.getEntities(this,boundingBox.add(moveX,moveY,moveZ).expand(1,1,1))){
      Entity entity=(Entity)value;
      if(!entity.method_1356()||field_1576!=null&&power$root(entity)==power$root(field_1576)&&!power$leftOwner)continue;
      class_27 candidate=entity.boundingBox.expand(.3,.3,.3).method_89(start,end);
      if(candidate==null)continue;
      double distance=start.distanceTo(candidate.field_1988);
      if(distance<nearest){nearest=distance;target=entity;contact=candidate.field_1988;}
    }
    if(target!=null){
      method_1340(contact.x,contact.y,contact.z);
      int damage=BowPhysics.damage(velocityX,velocityY,velocityZ);
      if(power$critical)damage+=random.nextInt(BowPhysics.criticalBound(damage));
      if(target.damage(field_1576 == null ? this : field_1576,damage)){power$hitSound();markDead();return;}
      // Modern rejected hits reverse velocity and stop being critical.
      velocityX*=.2;velocityY*=.2;velocityZ*=.2;velocityX=-velocityX;velocityY=-velocityY;velocityZ=-velocityZ;
      power$critical=false;yaw+=180;prevYaw+=180;
      if(velocityX*velocityX+velocityY*velocityY+velocityZ*velocityZ<1e-7)markDead();
    }else if(hit!=null){
      field_1577=hit.field_1984;field_1578=hit.field_1985;field_1579=hit.field_1986;
      field_1580=world.getBlockId(field_1577,field_1578,field_1579);field_1581=world.method_1778(field_1577,field_1578,field_1579);
      method_1340(hit.field_1988.x-Math.signum(velocityX)*(double).05f,
          hit.field_1988.y-Math.signum(velocityY)*(double).05f,hit.field_1988.z-Math.signum(velocityZ)*(double).05f);
      velocityX=velocityY=velocityZ=0;
      field_1582=true;field_1575=7;power$critical=false;power$hitSound();return;
    }else method_1340(x+moveX,y+moveY,z+moveZ);
    float newYaw=(float)Math.toDegrees(Math.atan2(moveX,moveZ));
    float newPitch=(float)Math.toDegrees(Math.atan2(moveY,Math.hypot(moveX,moveZ)));
    while(newYaw-prevYaw < -180)prevYaw-=360;while(newYaw-prevYaw>=180)prevYaw+=360;
    while(newPitch-prevPitch < -180)prevPitch-=360;while(newPitch-prevPitch>=180)prevPitch+=360;
    yaw=prevYaw+(newYaw-prevYaw)*.2f;pitch=prevPitch+(newPitch-prevPitch)*.2f;
    if(water)for(int i=0;i<4;i++)world.addParticle("bubble",x-velocityX*.25,y-velocityY*.25,z-velocityZ*.25,velocityX,velocityY,velocityZ);
    if(!water){velocityX*=BowPhysics.AIR_DRAG;velocityY*=BowPhysics.AIR_DRAG;velocityZ*=BowPhysics.AIR_DRAG;}
    velocityY-=BowPhysics.GRAVITY;
  }
  @Unique private boolean power$nearOwner(){
    if(field_1576==null)return false;
    Box sweep=boundingBox.add(velocityX,velocityY,velocityZ).expand(1,1,1);
    Entity passenger=power$root(field_1576);
    for(int depth=0;depth<32&&passenger!=null;depth++){
      if(sweep.intersects(passenger.boundingBox))return true;
      Entity next=passenger.field_1594;
      if(next==passenger)break;passenger=next;
    }
    return false;
  }
  @Unique private static Entity power$root(Entity entity){
    for(int depth=0;depth<32&&entity.field_1595!=null;depth++)entity=entity.field_1595;
    return entity;
  }
  @Unique private void power$hitSound(){local.luke.power.audio.SoundCategoryScope.run("passive", () -> world.method_150(x,y,z,"power_beta:entity.arrow.hit",1,1.2f/(random.nextFloat()*.2f+.9f)));}
  @Inject(method="writeNbt",at=@At("TAIL"))
  private void power$save(NbtCompound nbt,CallbackInfo ci){
    if(!power$modern)return;nbt.putBoolean("PowerBetaModernBow",true);nbt.putBoolean("PowerBetaCritical",power$critical);
    nbt.putBoolean("PowerBetaCreativeArrow",power$creative);nbt.putBoolean("PowerBetaLeftOwner",power$leftOwner);
  }
  @Inject(method="readtNbt",at=@At("TAIL"))
  private void power$load(NbtCompound nbt,CallbackInfo ci){
    power$modern=nbt.getBoolean("PowerBetaModernBow");if(!power$modern)return;
    power$critical=nbt.getBoolean("PowerBetaCritical");power$creative=nbt.getBoolean("PowerBetaCreativeArrow");power$leftOwner=nbt.getBoolean("PowerBetaLeftOwner");
  }
  @Inject(method="method_1354",at=@At("HEAD"),cancellable=true)
  private void power$creativePickup(PlayerEntity player,CallbackInfo ci){
    if(!power$modern||!ModernBow.enabled()||!power$creative)return;
    if(!world.isRemote&&field_1582&&field_1575<=0&&ModernBow.creative(player))markDead();ci.cancel();
  }
}
