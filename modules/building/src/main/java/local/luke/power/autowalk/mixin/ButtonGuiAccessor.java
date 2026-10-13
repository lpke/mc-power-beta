package local.luke.power.autowalk.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ButtonWidget.class)
public interface ButtonGuiAccessor {
  @Accessor("width") int power$guiWidth();
  @Accessor("height") int power$guiHeight();
}
