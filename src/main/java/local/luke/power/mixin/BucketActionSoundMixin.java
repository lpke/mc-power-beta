package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.ExtraSounds;
import net.minecraft.block.Material;
import net.minecraft.class_220;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_220.class)
public abstract class BucketActionSoundMixin {
  @WrapOperation(method = "method_451", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_229(IIII)Z"))
  private boolean power$fill(World world, int x, int y, int z, int block, Operation<Boolean> original) {
    Material fluid = world.method_1779(x, y, z);
    boolean filled = original.call(world, x, y, z, block);
    if (filled && world.getBlockId(x, y, z) == 0 && AudioConfig.current().bucketSounds)
      ExtraSounds.play(world, x + .5, y + .5, z + .5,
          fluid == Material.LAVA ? "item.bucket.fill_lava" : "item.bucket.fill");
    return filled;
  }

  @WrapOperation(method = "method_451", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_201(IIIII)Z"))
  private boolean power$empty(World world, int x, int y, int z, int block, int metadata, Operation<Boolean> original) {
    boolean emptied = original.call(world, x, y, z, block, metadata);
    if (emptied && world.getBlockId(x, y, z) == block && AudioConfig.current().bucketSounds)
      ExtraSounds.play(world, x + .5, y + .5, z + .5,
          world.method_1779(x, y, z) == Material.LAVA ? "item.bucket.empty_lava" : "item.bucket.empty");
    return emptied;
  }

  @WrapOperation(method = "method_451", at = @At(value = "NEW",
      target = "(Lnet/minecraft/Item;)Lnet/minecraft/item/ItemStack;"))
  private ItemStack power$milk(Item item, Operation<ItemStack> original,
      @Local(argsOnly = true) World world, @Local(argsOnly = true) PlayerEntity player) {
    ItemStack result = original.call(item);
    if (item == Item.field_407 && AudioConfig.current().bucketSounds)
      ExtraSounds.play(world, player.x, player.y, player.z, "entity.cow.milk");
    return result;
  }
}
