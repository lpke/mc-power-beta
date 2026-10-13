package local.luke.power.validation.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ButtonWidget.class)
public interface ButtonBounds {
  @Accessor("width") int power$width();
  @Accessor("height") int power$height();
}
