package local.luke.power.fastplace.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.fastplace.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/** Observe the common native callback so either hand can start a placement session. */
@Mixin(InteractionManager.class)
public class InteractionManagerMixin {
  @WrapMethod(method="method_1713")
  private boolean power$first(PlayerEntity player,World world,ItemStack stack,int x,int y,int z,int face,Operation<Boolean> original) {
    Minecraft mc=(Minecraft)FabricLoader.getInstance().getGameInstance();
    boolean first=FastPlace.observingFirst();
    PlacementTarget target=first&&FastPlace.eligible(mc,stack)?PlacementTarget.at(world,stack,x,y,z,face):null;
    int slot=player.inventory.selectedSlot;float yaw=player.yaw;
    boolean result=original.call(player,world,stack,x,y,z,face);
    if(first)FastPlace.firstPlacement(mc,stack,slot,face,yaw,target,result);
    return result;
  }
}
