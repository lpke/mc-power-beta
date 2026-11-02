package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.bow.ModernBow;
import net.minecraft.class_367;
import net.minecraft.class_410;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_367.class)
public class ModernBowItemMixin {
  @Inject(method="method_451", at=@At("HEAD"), cancellable=true)
  private void power$draw(ItemStack stack, World world, PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
    if (!ModernBow.enabled() || world.isRemote) return;
    ModernBow.begin(player, stack);
    cir.setReturnValue(stack);
  }

  @WrapOperation(method="method_451", at=@At(value="INVOKE",
      target="Lnet/minecraft/world/World;method_210(Lnet/minecraft/entity/Entity;)Z"))
  private boolean power$creativeNativeArrow(World world, Entity entity, Operation<Boolean> spawn,
      ItemStack stack, World useWorld, PlayerEntity player) {
    // The native player-arrow flag is saved by Beta and controls ammo pickup.
    // Do not mark this arrow modern or change its instant launch/flight behavior.
    if (!world.isRemote && entity instanceof class_410 arrow && ModernBow.creative(player)) arrow.field_1574 = false;
    return spawn.call(world, entity);
  }
}
