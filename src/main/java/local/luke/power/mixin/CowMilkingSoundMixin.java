package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.ExtraSounds;
import net.minecraft.class_466;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_466.class)
public abstract class CowMilkingSoundMixin {
  @WrapOperation(method = "method_1323", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/player/PlayerInventory;setStack(ILnet/minecraft/item/ItemStack;)V"))
  private void power$milk(PlayerInventory inventory, int slot, ItemStack filled, Operation<Void> original) {
    original.call(inventory, slot, filled);
    if (AudioConfig.current().bucketSounds) {
      Entity cow = (Entity) (Object) this;
      ExtraSounds.play(cow.world, cow.x, cow.y, cow.z, "entity.cow.milk");
    }
  }
}
