package local.luke.power.validation.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TitleScreen.class)
public interface TitleSplash471Access {
  @Accessor("splashText") String power$splashText();
  @Accessor("splashText") void power$splashText(String text);
}
