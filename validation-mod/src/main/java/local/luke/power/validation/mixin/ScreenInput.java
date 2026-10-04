package local.luke.power.validation.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInput {
  @org.spongepowered.asm.mixin.gen.Accessor("buttons")
  java.util.List<net.minecraft.client.gui.widget.ButtonWidget> power$buttons();
  @Invoker("mouseClicked")
  void power$click(int x, int y, int button);

  @Invoker("keyPressed")
  void power$key(char c, int code);
}
