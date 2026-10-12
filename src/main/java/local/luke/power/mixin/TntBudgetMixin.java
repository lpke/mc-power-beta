package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.gameplay.*;
import net.minecraft.class_365;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class TntBudgetMixin {
  @Unique private final TntBudget<class_365> power$tnt = new TntBudget<>();
  @Unique private boolean power$draining;
  @Unique private long power$tick;
  @Inject(method = "method_227", at = @At("HEAD"))
  private void power$begin(CallbackInfo ci) {
    power$tick++;
    if (GameplayConfig.current().limitTnt && !((World)(Object)this).isRemote) power$tnt.begin();
    else power$tnt.clear();
  }
  @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "method_193")
  private void power$tick(Entity entity, boolean loaded, Operation<Void> original) {
    if (!power$draining && loaded && !entity.world.isRemote && GameplayConfig.current().limitTnt && entity instanceof class_365 tnt) {
      ((local.luke.power.render.TntMotion.Access)tnt).power$tntMotion().begin(power$tick, tnt.x, tnt.y, tnt.z);
      power$tnt.offer(tnt);
    } else {
      if (!power$draining && entity instanceof class_365)
        ((local.luke.power.render.TntMotion.Access)entity).power$tntMotion().clear();
      original.call(entity, loaded);
    }
  }
  @Inject(method = "method_227", at = @At("TAIL"))
  private void power$drain(CallbackInfo ci) {
    World world = (World)(Object)this;
    GameplaySettings settings = GameplayConfig.current();
    if (!settings.limitTnt || world.isRemote) return;
    power$draining = true;
    try {
      power$tnt.run(128, settings.tntPerTick, settings.tntMillis * 1_000_000L,
          tnt -> !tnt.dead, tnt -> tnt.field_1390 <= 0,
          tnt -> {
            world.method_193(tnt, true);
            ((local.luke.power.render.TntMotion.Access)tnt).power$tntMotion().target(tnt.x, tnt.y, tnt.z);
          }, System::nanoTime);
    } finally { power$draining = false; }
  }
}
