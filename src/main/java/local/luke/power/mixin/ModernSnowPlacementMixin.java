package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import local.luke.power.gameplay.GameplayConfig;
import net.minecraft.block.Block;
import net.minecraft.class_533;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_533.class)
public abstract class ModernSnowPlacementMixin {
  @Shadow private int field_2216;

  @Inject(method = "method_444", at = @At("HEAD"))
  private void power$beginSnowPlacement(ItemStack stack, PlayerEntity player, World world,
      int x, int y, int z, int face, CallbackInfoReturnable<Boolean> ci,
      @Share("snowFace") LocalIntRef originalFace, @Share("modernSnow") LocalBooleanRef modern) {
    originalFace.set(face);
    // Restrict this preference to native blocks and the local world. Custom items keep their own rules.
    modern.set(!world.isRemote && GameplayConfig.current().modernSnowPlacement
        && ((Object) this).getClass().getName().startsWith("net.minecraft.")
        && field_2216 > 0 && field_2216 <= 96 && Block.BLOCKS[field_2216] != null
        && Block.BLOCKS[field_2216].getClass().getName().startsWith("net.minecraft."));
  }

  @WrapOperation(method = "method_444", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;getBlockId(III)I"))
  private int power$chooseSnowTarget(World world, int x, int y, int z, Operation<Integer> original,
      @Share("modernSnow") LocalBooleanRef modern,
      @Share("keepSnowFace") LocalBooleanRef keepFace) {
    int id = original.call(world, x, y, z);
    if (!modern.get() || !power$nativeSnow(world, x, y, z)) return id;
    keepFace.set(true);
    return id;
  }

  @ModifyVariable(method = "method_444", at = @At("STORE"), index = 7, argsOnly = true)
  private int power$keepClickedSnowFace(int nativeFace, @Share("snowFace") LocalIntRef face,
      @Share("keepSnowFace") LocalBooleanRef keepFace) {
    return keepFace.get() ? face.get() : nativeFace;
  }

  @Unique private static boolean power$nativeSnow(World world, int x, int y, int z) {
    return world.getBlockId(x, y, z) == 78 && Block.BLOCKS[78] == Block.field_1867
        && Block.field_1867.getClass() == net.minecraft.class_224.class;
  }
}
