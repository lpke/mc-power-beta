package local.luke.building.validation.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface ClickInvoker {
  @Invoker("method_2110")
  void validate$held(int button, boolean down);

  @Invoker("method_2107")
  void validate$click(int button);
}
