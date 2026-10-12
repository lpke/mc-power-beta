package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.render.EntityVisibility;
import local.luke.power.gameplay.GameplayConfig;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.class_365;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class EntityVisibilityMixin {
  @Unique private int power$renderedTnt;
  @Inject(method = "method_1544", at = @At("HEAD"))
  private void power$begin(CallbackInfo ci) { power$renderedTnt = 0; }
  @WrapOperation(method = "method_1544", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;method_1367(Lnet/minecraft/util/math/Vec3d;)Z"))
  private boolean power$visible(Entity entity, net.minecraft.util.math.Vec3d position, Operation<Boolean> original) {
    return !EntityVisibility.hidden(entity) && original.call(entity, position);
  }
  @WrapOperation(method = "method_1544", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;method_1921(Lnet/minecraft/entity/Entity;F)V"))
  private void power$entity(EntityRenderDispatcher dispatcher, Entity entity, float delta, Operation<Void> original) {
    if (EntityVisibility.hidden(entity)) return;
    if (entity instanceof class_365 && !entity.world.isRemote && GameplayConfig.current().limitTnt && power$renderedTnt++ >= 256) return;
    original.call(dispatcher, entity, delta);
  }
}
