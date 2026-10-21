package local.luke.power.inventory.tools.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.inventory.tools.Restock;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;

@Mixin(InteractionManager.class)
public class RestockUseMixin {
  @Shadow @Final protected Minecraft minecraft;
  @WrapMethod(method = "method_1713")
  private boolean power$placed(PlayerEntity player, World world, ItemStack stack,
      int x, int y, int z, int face, Operation<Boolean> original) {
    var action = Restock.before(minecraft, stack);
    boolean result = original.call(player, world, stack, x, y, z, face);
    Restock.after(minecraft, action); return result;
  }
  @WrapMethod(method = "method_1712")
  private boolean power$used(PlayerEntity player, World world, ItemStack stack, Operation<Boolean> original) {
    var action = Restock.before(minecraft, stack);
    boolean result = original.call(player, world, stack);
    Restock.after(minecraft, action); return result;
  }
  @WrapMethod(method = "attackEntity")
  private void power$attacked(PlayerEntity player, net.minecraft.entity.Entity target, Operation<Void> original) {
    var action = Restock.before(minecraft, player.inventory.getSelectedItem());
    original.call(player, target);
    Restock.after(minecraft, action);
  }
}
