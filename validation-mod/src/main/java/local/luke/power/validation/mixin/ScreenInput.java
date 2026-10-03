package local.luke.power.validation.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInput {
  @Invoker("mouseClicked")
  void power$click(int x, int y, int button);

  @Invoker("keyPressed")
  void power$key(char c, int code);
}
