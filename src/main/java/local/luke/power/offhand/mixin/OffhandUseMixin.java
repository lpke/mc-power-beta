package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.offhand.OffhandUse;
import local.luke.power.offhand.OffhandEquipment;
import net.minecraft.client.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class OffhandUseMixin {
  @WrapMethod(method="method_2107")
  private void power$hands(int button, Operation<Void> original) {
    if(button == 1 && local.luke.power.inventory.tools.Restock.waitingForUse((Minecraft)(Object)this))return;
    if(OffhandEquipment.pending((Minecraft)(Object)this,button))return;
    OffhandUse use = OffhandUse.begin((Minecraft)(Object)this, button);
    if (use == null) { original.call(button); return; }
    try (use) { original.call(button); use.offerOffhand(); }
  }
  // A HEAD cancellation by carrying, WorldEdit or another input owner never reaches this field read.
  @Inject(method="method_2107",at=@At(value="FIELD",target="Lnet/minecraft/client/Minecraft;field_2823:Lnet/minecraft/class_27;",ordinal=0))
  private void power$nativePath(int button,CallbackInfo ci) { OffhandUse.nativePath(); }
  @WrapOperation(method="method_2107", at=@At(value="INVOKE", target="Lnet/minecraft/client/InteractionManager;method_1712(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;)Z"))
  private boolean power$air(InteractionManager manager, PlayerEntity player, World world, ItemStack stack, Operation<Boolean> original) {
    return OffhandUse.air((Minecraft)(Object)this, stack, () -> original.call(manager, player, world, stack));
  }
  @WrapOperation(method="method_2107", at=@At(value="INVOKE", target="Lnet/minecraft/client/InteractionManager;method_1713(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)Z"))
  private boolean power$block(InteractionManager manager, PlayerEntity player, World world, ItemStack stack,
      int x, int y, int z, int face, Operation<Boolean> original) {
    return OffhandUse.block((Minecraft)(Object)this,stack,x,y,z,()->original.call(manager, player, world, stack, x, y, z, face));
  }
  @WrapOperation(method="method_2107", at=@At(value="INVOKE", target="Lnet/minecraft/client/InteractionManager;interactEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"))
  private void power$entity(InteractionManager manager, PlayerEntity player, Entity entity, Operation<Void> original) {
    OffhandUse.entity((Minecraft)(Object)this, entity, () -> original.call(manager, player, entity));
  }
}
