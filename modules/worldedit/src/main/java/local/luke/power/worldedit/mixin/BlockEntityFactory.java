package local.luke.power.worldedit.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_395;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_395.class)
public interface BlockEntityFactory {
  @Invoker("method_1251")
  BlockEntity worldedit$create();
}
