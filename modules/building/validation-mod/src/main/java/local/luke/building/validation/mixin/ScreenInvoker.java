package local.luke.building.validation.mixin;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(Screen.class)
public interface ScreenInvoker {
  @Invoker("mouseClicked")
  void validation$click(int x, int y, int button);

  @Invoker("buttonClicked")
  void validation$button(ButtonWidget button);

  @Accessor("buttons")
  List<ButtonWidget> validation$buttons();
}
