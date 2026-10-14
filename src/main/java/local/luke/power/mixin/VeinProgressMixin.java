package local.luke.power.mixin;

import local.luke.power.mining.VeinMining;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_339;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = "net.modificationstation.stationapi.api.block.AbstractBlockState", remap = false)
public class VeinProgressMixin {
  @WrapMethod(method = "calcBlockBreakingDelta")
  private float power$speed(PlayerEntity player, BlockView world, class_339 pos, Operation<Float> original) {
    return VeinMining.progress(original.call(player, world, pos), player, pos.field_2100, pos.field_2101, pos.field_2102);
  }
}
