package local.luke.power.mixin;

import net.minecraft.class_52;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The merged World storage getter is absent from the client. */
@Mixin(World.class)
public interface WorldStorageAccessor {
  @Accessor("field_219") class_52 power$storage();
}
