package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.visual.DamageCameraState;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class FireDamageSourceMixin implements DamageCameraState {
  @Unique private int power$fireDamageDepth;
  @Unique private boolean power$fireHurt;

  public boolean power$isFireHurt() { return power$fireHurt; }
  public void power$recordHurt() { power$fireHurt = power$fireDamageDepth > 0; }

  @WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$burning(Entity entity, Entity attacker, int amount, Operation<Boolean> original) {
    power$fireDamageDepth++;
    try { return original.call(entity, attacker, amount); }
    finally { power$fireDamageDepth--; }
  }

  @WrapOperation(method = "move", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/Entity;method_1392(I)V"))
  private void power$fireContact(Entity entity, int amount, Operation<Void> original) {
    // Lava and lightning share Beta's contact-damage helper. Scope only fire contact.
    boolean fire = !entity.world.method_169(entity.boundingBox.method_104(0.001, 0.001, 0.001), Material.LAVA);
    if (fire) power$fireDamageDepth++;
    try { original.call(entity, amount); }
    finally { if (fire) power$fireDamageDepth--; }
  }
}
