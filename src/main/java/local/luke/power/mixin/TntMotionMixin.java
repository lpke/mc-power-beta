package local.luke.power.mixin;

import local.luke.power.render.TntMotion;
import net.minecraft.class_365;
import org.spongepowered.asm.mixin.*;

@Mixin(class_365.class)
public class TntMotionMixin implements TntMotion.Access {
  @Unique private final TntMotion power$motion = new TntMotion();
  @Override public TntMotion power$tntMotion() { return power$motion; }
}
