package local.luke.power.validation.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface Client253Access {
  @Accessor("ticksPlayed") int power$ticks();
  @Accessor("ticksPlayed") void power$ticks(int value);
  @Accessor("lastTickTime") void power$lastTick(long value);
  @Invoker("method_2110") void power$heldMining(int button, boolean held);
  @Invoker("method_2107") void power$use(int button);
}
