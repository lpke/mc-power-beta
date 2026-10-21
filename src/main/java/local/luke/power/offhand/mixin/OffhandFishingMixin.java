package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.offhand.Offhand;
import local.luke.power.offhand.OffhandFishing;
import local.luke.power.inventory.HandUse;
import net.minecraft.class_607;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
public class OffhandFishingMixin implements OffhandFishing {
  @Unique private boolean power$leftCast;
  public boolean power$offhandCast() { return power$leftCast; }
  public void power$offhandCast(boolean value) { power$leftCast=value; }
  @Inject(method="<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;)V",at=@At("RETURN"))
  private void power$castHand(net.minecraft.world.World world,PlayerEntity player,CallbackInfo ci) {
    power$leftCast=HandUse.hand(player.inventory)==HandUse.Hand.OFF;
  }
  @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;method_502()Lnet/minecraft/item/ItemStack;"))
  private ItemStack power$eitherRod(PlayerEntity player,Operation<ItemStack> original) {
    ItemStack main=original.call(player);
    if(main!=null&&main.getItem() instanceof class_607)return main;
    ItemStack off=Offhand.enabled(player)?Offhand.get(player.inventory):null;
    return off!=null&&off.getItem() instanceof class_607?off:main;
  }
}
