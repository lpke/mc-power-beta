package local.luke.power.validation.mixin;

import java.util.Random;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mapped access for detached audio probes; raw source field names differ at runtime. */
@Mixin(Entity.class)
public interface FireState448Access {
  @Accessor("fireImmune") void power$fireImmune(boolean value);
  @Accessor("random") Random power$random();
  @Accessor("field_1649") void power$firstTick(boolean value);
}
