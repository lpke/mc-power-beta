package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.ExtraSounds;
import net.minecraft.class_242;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(class_242.class)
public abstract class BoneMealSoundMixin {
  @WrapMethod(method = "method_444")
  private boolean power$fertilize(ItemStack stack, PlayerEntity player, World world, int x, int y,
      int z, int side, Operation<Boolean> original) {
    boolean watch = !world.isRemote && AudioConfig.current().boneMealSounds && stack.getDamage() == 15;
    int block = watch ? world.getBlockId(x, y, z) : 0;
    watch &= block == 2 || block == 6 || block == 59;
    int count = stack.count, metadata = watch ? world.method_1778(x, y, z) : 0;
    boolean used = original.call(stack, player, world, x, y, z, side);
    // StationAPI can handle fertilization before the vanilla count-write sites.
    if (watch && used && (stack.count < count || world.getBlockId(x, y, z) != block
        || world.method_1778(x, y, z) != metadata))
      ExtraSounds.play(world, x + .5, y + .5, z + .5, "item.bone_meal.use");
    return used;
  }
}
