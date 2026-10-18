package local.luke.power.validation.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface Client253Access {
  @Accessor("ticksPlayed") int power$ticks();
  @Accessor("ticksPlayed") void power$ticks(int value);
  @Accessor("lastTickTime") void power$lastTick(long value);
}
