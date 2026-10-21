package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.inventory.HandUse;
import local.luke.power.offhand.Offhand;
import net.minecraft.class_367;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_367.class)
public class OffhandBowMixin {
  @WrapOperation(method="method_451",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerInventory;method_676(I)Z"))
  private boolean power$oppositeHandAmmo(PlayerInventory inventory,int item,Operation<Boolean> original) {
    if(!Offhand.enabled(inventory.player))return original.call(inventory,item);
    boolean off=HandUse.hand(inventory)==HandUse.Hand.OFF;
    ItemStack ammunition=off?inventory.main[inventory.selectedSlot]:Offhand.get(inventory);
    if(ammunition==null||ammunition.itemId!=item||ammunition.count<=0)return original.call(inventory,item);
    var mc = (net.minecraft.client.Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    var refill = local.luke.power.inventory.tools.Restock.before(mc, ammunition, off ? HandUse.Hand.MAIN : HandUse.Hand.OFF);
    if(--ammunition.count<=0) {
      if(off)inventory.main[inventory.selectedSlot]=null;else Offhand.set(inventory,null);
    }
    inventory.markDirty();local.luke.power.inventory.tools.Restock.after(mc, refill);return true;
  }
}
