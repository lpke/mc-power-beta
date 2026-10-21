package local.luke.power.offhand;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.PlayerEntity;

/** Render-only model owner; includes armor and nested inventory previews. */
public final class OffhandPose implements AutoCloseable {
  private static final ThreadLocal<OffhandPose> CURRENT=new ThreadLocal<>();
  private final OffhandPose previous;
  private final PlayerEntity player;
  private final float delta;
  public OffhandPose(PlayerEntity player,float delta) {
    this.player=player;this.delta=delta;previous=CURRENT.get();CURRENT.set(this);
  }
  public static void apply(BipedEntityModel model) {
    OffhandPose pose=CURRENT.get();if(pose==null||!Offhand.enabled(pose.player))return;
    float swing=OffhandRenderer.swing(pose.player,pose.delta);if(swing<=0)return;
    float root=(float)Math.sin(Math.sqrt(swing)*Math.PI);
    model.leftArm.pitch-=root*1.2f;
    model.leftArm.yaw-=root*.4f;
    model.leftArm.roll+=(float)Math.sin(swing*Math.PI)*.4f;
  }
  @Override public void close() { if(previous==null)CURRENT.remove();else CURRENT.set(previous); }
}
