package local.luke.power.validation.mixin;

import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerScreen.class)
public interface ContainerInput {
  @Accessor("backgroundWidth") int power$width();
  @Accessor("backgroundHeight") int power$height();
}
